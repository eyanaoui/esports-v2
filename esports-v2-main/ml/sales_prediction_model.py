import math
import pandas as pd
import numpy as np

from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error

from db_config import get_connection


FORECAST_DAYS = 30


def load_sales_data():
    connection = get_connection()

    query = """
        SELECT
            p.id AS product_id,
            p.name AS product_name,
            p.category,
            p.price,
            p.stock,
            DATE(o.created_at) AS order_date,
            SUM(oi.quantity) AS sold_qty
        FROM product p
        LEFT JOIN order_item oi ON oi.product_id = p.id
        LEFT JOIN `order` o ON o.id = oi.order_ref_id
        WHERE p.is_active = 1
        GROUP BY
            p.id,
            p.name,
            p.category,
            p.price,
            p.stock,
            DATE(o.created_at)
        ORDER BY p.id, order_date
    """

    df = pd.read_sql(query, connection)
    connection.close()

    return df


def prepare_training_data(df):
    df = df.copy()

    df["sold_qty"] = df["sold_qty"].fillna(0)

    df["order_date"] = pd.to_datetime(df["order_date"], errors="coerce")

    today = pd.Timestamp.today().normalize()

    df["order_date"] = df["order_date"].fillna(today)

    df["day"] = df["order_date"].dt.day
    df["month"] = df["order_date"].dt.month
    df["day_of_week"] = df["order_date"].dt.dayofweek

    category_dummies = pd.get_dummies(df["category"], prefix="category")

    features = pd.concat(
        [
            df[[
                "product_id",
                "price",
                "stock",
                "day",
                "month",
                "day_of_week"
            ]],
            category_dummies
        ],
        axis=1
    )

    target = df["sold_qty"]

    return features, target, df, category_dummies.columns.tolist()


def train_model(features, target):
    if len(features) < 5:
        return None, None

    X_train, X_test, y_train, y_test = train_test_split(
        features,
        target,
        test_size=0.25,
        random_state=42
    )

    model = RandomForestRegressor(
        n_estimators=100,
        random_state=42,
        max_depth=8
    )

    model.fit(X_train, y_train)

    predictions = model.predict(X_test)
    mae = mean_absolute_error(y_test, predictions)

    return model, mae


def fallback_prediction(product_row):
    stock = product_row["stock"]
    price = product_row["price"]

    base = 3

    if stock > 50:
        base += 4
    elif stock > 10:
        base += 2

    if price < 100:
        base += 4
    elif price < 250:
        base += 2

    return max(1, base)


def generate_forecasts(model, training_columns, df):
    products = (
        df[["product_id", "product_name", "category", "price", "stock"]]
        .drop_duplicates(subset=["product_id"])
        .copy()
    )

    today = pd.Timestamp.today().normalize()

    forecasts = []

    for _, product in products.iterrows():
        if model is None:
            predicted_daily = fallback_prediction(product)
            predicted_30_days = predicted_daily
        else:
            future_rows = []

            for day_offset in range(FORECAST_DAYS):
                future_date = today + pd.Timedelta(days=day_offset)

                row = {
                    "product_id": product["product_id"],
                    "price": product["price"],
                    "stock": product["stock"],
                    "day": future_date.day,
                    "month": future_date.month,
                    "day_of_week": future_date.dayofweek,
                }

                for column in training_columns:
                    row[column] = 0

                category_column = f"category_{product['category']}"
                if category_column in row:
                    row[category_column] = 1

                future_rows.append(row)

            future_df = pd.DataFrame(future_rows)

            for column in model.feature_names_in_:
                if column not in future_df.columns:
                    future_df[column] = 0

            future_df = future_df[list(model.feature_names_in_)]

            daily_predictions = model.predict(future_df)
            predicted_30_days = float(np.sum(daily_predictions))

        predicted_qty = max(0, round(predicted_30_days, 2))

        current_stock = int(product["stock"])
        recommended_reorder_qty = max(0, math.ceil(predicted_qty - current_stock))

        forecasts.append({
            "product_id": int(product["product_id"]),
            "predicted_qty": predicted_qty,
            "forecast_days": FORECAST_DAYS,
            "recommended_reorder_qty": recommended_reorder_qty
        })

    return forecasts


def save_forecasts(forecasts):
    connection = get_connection()
    cursor = connection.cursor()

    cursor.execute("DELETE FROM product_forecast")

    insert_query = """
        INSERT INTO product_forecast
        (product_id, predicted_qty, forecast_days, recommended_reorder_qty, generated_at)
        VALUES (%s, %s, %s, %s, NOW())
    """

    for forecast in forecasts:
        cursor.execute(
            insert_query,
            (
                forecast["product_id"],
                forecast["predicted_qty"],
                forecast["forecast_days"],
                forecast["recommended_reorder_qty"]
            )
        )

    connection.commit()
    cursor.close()
    connection.close()


def run_sales_prediction():
    print("========== SALES PREDICTION MODEL ==========")

    df = load_sales_data()

    if df.empty:
        print("No product/order data found.")
        return

    features, target, prepared_df, category_columns = prepare_training_data(df)

    model, mae = train_model(features, target)

    if model is None:
        print("Not enough data for full ML training. Using fallback prediction.")
    else:
        print(f"Model trained successfully. MAE = {mae:.2f}")

    forecasts = generate_forecasts(model, category_columns, prepared_df)

    save_forecasts(forecasts)

    print(f"Saved {len(forecasts)} product forecasts.")
    print("============================================")


if __name__ == "__main__":
    run_sales_prediction()