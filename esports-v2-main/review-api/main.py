from datetime import datetime
from typing import List, Optional

import mysql.connector
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field


DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "",
    "database": "esports_db",
}


app = FastAPI(
    title="E-Sports Review API",
    description="REST API for product reviews, ratings, comments and moderation.",
    version="1.1.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ReviewCreateRequest(BaseModel):
    product_id: int = Field(..., gt=0)
    customer_name: str = Field(..., min_length=2, max_length=120)
    rating: int = Field(..., ge=1, le=5)
    comment: str = Field(..., min_length=5)


class ReviewResponse(BaseModel):
    id: int
    product_id: int
    product_name: Optional[str] = None
    customer_name: str
    rating: int
    comment: str
    status: str
    created_at: Optional[str] = None


class RatingSummaryResponse(BaseModel):
    product_id: int
    average_rating: float
    review_count: int


def get_connection():
    return mysql.connector.connect(**DB_CONFIG)


@app.get("/")
def home():
    return {
        "message": "E-Sports Review API is running",
        "docs": "/docs"
    }


@app.post("/api/reviews", response_model=ReviewResponse)
def create_review(request: ReviewCreateRequest):
    connection = get_connection()
    cursor = connection.cursor(dictionary=True)

    try:
        cursor.execute(
            """
            SELECT id, name
            FROM product
            WHERE id = %s AND is_active = true
            """,
            (request.product_id,)
        )

        product = cursor.fetchone()

        if product is None:
            raise HTTPException(status_code=404, detail="Product not found or inactive.")

        cursor.execute(
            """
            INSERT INTO product_review
            (product_id, customer_name, rating, comment, status, created_at)
            VALUES (%s, %s, %s, %s, 'VISIBLE', NOW())
            """,
            (
                request.product_id,
                request.customer_name.strip(),
                request.rating,
                request.comment.strip(),
            )
        )

        connection.commit()
        review_id = cursor.lastrowid

        cursor.execute(
            """
            SELECT
                pr.id,
                pr.product_id,
                p.name AS product_name,
                pr.customer_name,
                pr.rating,
                pr.comment,
                pr.status,
                pr.created_at
            FROM product_review pr
            INNER JOIN product p ON p.id = pr.product_id
            WHERE pr.id = %s
            """,
            (review_id,)
        )

        review = cursor.fetchone()
        return format_review(review)

    except HTTPException:
        raise

    except Exception as e:
        connection.rollback()
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        cursor.close()
        connection.close()


@app.get("/api/products/{product_id}/rating", response_model=RatingSummaryResponse)
def get_product_rating(product_id: int):
    connection = get_connection()
    cursor = connection.cursor(dictionary=True)

    try:
        cursor.execute(
            """
            SELECT
                COALESCE(AVG(rating), 0) AS average_rating,
                COUNT(*) AS review_count
            FROM product_review
            WHERE product_id = %s
            AND status = 'VISIBLE'
            """,
            (product_id,)
        )

        row = cursor.fetchone()

        return {
            "product_id": product_id,
            "average_rating": round(float(row["average_rating"] or 0), 2),
            "review_count": int(row["review_count"] or 0),
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        cursor.close()
        connection.close()


@app.get("/api/reviews", response_model=List[ReviewResponse])
def get_visible_reviews():
    connection = get_connection()
    cursor = connection.cursor(dictionary=True)

    try:
        cursor.execute(
            """
            SELECT
                pr.id,
                pr.product_id,
                p.name AS product_name,
                pr.customer_name,
                pr.rating,
                pr.comment,
                pr.status,
                pr.created_at
            FROM product_review pr
            INNER JOIN product p ON p.id = pr.product_id
            WHERE pr.status = 'VISIBLE'
            ORDER BY pr.created_at DESC, pr.id DESC
            """
        )

        rows = cursor.fetchall()
        return [format_review(row) for row in rows]

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        cursor.close()
        connection.close()


@app.get("/api/admin/reviews", response_model=List[ReviewResponse])
def get_all_reviews_for_admin():
    connection = get_connection()
    cursor = connection.cursor(dictionary=True)

    try:
        cursor.execute(
            """
            SELECT
                pr.id,
                pr.product_id,
                p.name AS product_name,
                pr.customer_name,
                pr.rating,
                pr.comment,
                pr.status,
                pr.created_at
            FROM product_review pr
            INNER JOIN product p ON p.id = pr.product_id
            ORDER BY pr.created_at DESC, pr.id DESC
            """
        )

        rows = cursor.fetchall()
        return [format_review(row) for row in rows]

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        cursor.close()
        connection.close()


@app.get("/api/products/{product_id}/reviews", response_model=List[ReviewResponse])
def get_reviews_by_product(product_id: int):
    connection = get_connection()
    cursor = connection.cursor(dictionary=True)

    try:
        cursor.execute(
            """
            SELECT
                pr.id,
                pr.product_id,
                p.name AS product_name,
                pr.customer_name,
                pr.rating,
                pr.comment,
                pr.status,
                pr.created_at
            FROM product_review pr
            INNER JOIN product p ON p.id = pr.product_id
            WHERE pr.product_id = %s
            AND pr.status = 'VISIBLE'
            ORDER BY pr.created_at DESC, pr.id DESC
            """,
            (product_id,)
        )

        rows = cursor.fetchall()
        return [format_review(row) for row in rows]

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        cursor.close()
        connection.close()


@app.patch("/api/reviews/{review_id}/hide")
def hide_review(review_id: int):
    return update_review_status(review_id, "HIDDEN")


@app.patch("/api/reviews/{review_id}/show")
def show_review(review_id: int):
    return update_review_status(review_id, "VISIBLE")


@app.delete("/api/reviews/{review_id}")
def delete_review(review_id: int):
    connection = get_connection()
    cursor = connection.cursor()

    try:
        cursor.execute(
            """
            DELETE FROM product_review
            WHERE id = %s
            """,
            (review_id,)
        )

        connection.commit()

        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Review not found.")

        return {
            "success": True,
            "message": "Review deleted successfully.",
            "review_id": review_id
        }

    except HTTPException:
        raise

    except Exception as e:
        connection.rollback()
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        cursor.close()
        connection.close()


def update_review_status(review_id: int, status: str):
    connection = get_connection()
    cursor = connection.cursor()

    try:
        cursor.execute(
            """
            UPDATE product_review
            SET status = %s
            WHERE id = %s
            """,
            (status, review_id)
        )

        connection.commit()

        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Review not found.")

        return {
            "success": True,
            "review_id": review_id,
            "status": status
        }

    except HTTPException:
        raise

    except Exception as e:
        connection.rollback()
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        cursor.close()
        connection.close()


def format_review(row):
    created_at = row.get("created_at")

    if isinstance(created_at, datetime):
        created_at = created_at.strftime("%Y-%m-%d %H:%M:%S")

    return {
        "id": row["id"],
        "product_id": row["product_id"],
        "product_name": row.get("product_name"),
        "customer_name": row["customer_name"],
        "rating": row["rating"],
        "comment": row["comment"],
        "status": row["status"],
        "created_at": created_at,
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="127.0.0.1",
        port=8090,
        reload=True
    )