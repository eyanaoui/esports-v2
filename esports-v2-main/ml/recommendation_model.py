import pandas as pd

from db_config import get_connection


MAX_RECOMMENDATIONS_PER_PRODUCT = 5


def load_order_items():
    connection = get_connection()

    query = """
        SELECT
            oi.order_ref_id,
            oi.product_id,
            p.name,
            p.category,
            p.price,
            p.stock,
            p.is_active
        FROM order_item oi
        INNER JOIN product p ON p.id = oi.product_id
        WHERE p.is_active = 1
    """

    df = pd.read_sql(query, connection)
    connection.close()

    return df


def load_products():
    connection = get_connection()

    query = """
        SELECT
            id AS product_id,
            name,
            category,
            price,
            stock,
            is_active
        FROM product
        WHERE is_active = 1
    """

    df = pd.read_sql(query, connection)
    connection.close()

    return df


def generate_copurchase_recommendations(order_items_df):
    recommendations = []

    if order_items_df.empty:
        return recommendations

    grouped_orders = order_items_df.groupby("order_ref_id")["product_id"].apply(list)

    scores = {}

    for product_list in grouped_orders:
        unique_products = list(set(product_list))

        for source_product in unique_products:
            for recommended_product in unique_products:
                if source_product == recommended_product:
                    continue

                key = (source_product, recommended_product)
                scores[key] = scores.get(key, 0) + 1

    for (source_product, recommended_product), score in scores.items():
        recommendations.append({
            "product_id": int(source_product),
            "recommended_product_id": int(recommended_product),
            "score": float(score)
        })

    return recommendations


def generate_category_fallback_recommendations(products_df):
    recommendations = []

    if products_df.empty:
        return recommendations

    for _, source in products_df.iterrows():
        same_category = products_df[
            (products_df["category"] == source["category"])
            & (products_df["product_id"] != source["product_id"])
            & (products_df["stock"] > 0)
        ].copy()

        same_category["price_distance"] = abs(same_category["price"] - source["price"])

        same_category = same_category.sort_values(
            by=["price_distance", "stock"],
            ascending=[True, False]
        ).head(MAX_RECOMMENDATIONS_PER_PRODUCT)

        rank = MAX_RECOMMENDATIONS_PER_PRODUCT

        for _, rec in same_category.iterrows():
            recommendations.append({
                "product_id": int(source["product_id"]),
                "recommended_product_id": int(rec["product_id"]),
                "score": float(rank) * 0.5
            })
            rank -= 1

    return recommendations


def merge_recommendations(copurchase_recs, fallback_recs):
    merged = {}

    for rec in fallback_recs:
        key = (rec["product_id"], rec["recommended_product_id"])
        merged[key] = rec["score"]

    for rec in copurchase_recs:
        key = (rec["product_id"], rec["recommended_product_id"])

        if key in merged:
            merged[key] += rec["score"] * 2
        else:
            merged[key] = rec["score"] * 2

    final_recommendations = []

    grouped = {}

    for (product_id, recommended_product_id), score in merged.items():
        grouped.setdefault(product_id, []).append({
            "product_id": product_id,
            "recommended_product_id": recommended_product_id,
            "score": score
        })

    for product_id, recs in grouped.items():
        sorted_recs = sorted(recs, key=lambda x: x["score"], reverse=True)

        final_recommendations.extend(
            sorted_recs[:MAX_RECOMMENDATIONS_PER_PRODUCT]
        )

    return final_recommendations


def save_recommendations(recommendations):
    connection = get_connection()
    cursor = connection.cursor()

    cursor.execute("DELETE FROM product_recommendation")

    insert_query = """
        INSERT INTO product_recommendation
        (product_id, recommended_product_id, score, generated_at)
        VALUES (%s, %s, %s, NOW())
    """

    for rec in recommendations:
        cursor.execute(
            insert_query,
            (
                rec["product_id"],
                rec["recommended_product_id"],
                rec["score"]
            )
        )

    connection.commit()
    cursor.close()
    connection.close()


def run_recommendation_model():
    print("========== RECOMMENDATION MODEL ==========")

    order_items_df = load_order_items()
    products_df = load_products()

    copurchase_recs = generate_copurchase_recommendations(order_items_df)
    fallback_recs = generate_category_fallback_recommendations(products_df)

    final_recommendations = merge_recommendations(copurchase_recs, fallback_recs)

    save_recommendations(final_recommendations)

    print(f"Co-purchase recommendations: {len(copurchase_recs)}")
    print(f"Fallback recommendations: {len(fallback_recs)}")
    print(f"Saved final recommendations: {len(final_recommendations)}")
    print("==========================================")


if __name__ == "__main__":
    run_recommendation_model()