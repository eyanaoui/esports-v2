package com.esports.services;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductReviewApiService {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8090";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl;

    public ProductReviewApiService() {
        String configuredUrl = readConfig("REVIEW_API_URL");

        if (configuredUrl == null || configuredUrl.trim().isEmpty()) {
            this.baseUrl = DEFAULT_BASE_URL;
        } else {
            this.baseUrl = configuredUrl.trim();
        }
    }

    public RatingSummary getRatingSummary(int productId) {
        String url = baseUrl + "/api/products/" + productId + "/rating";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return new RatingSummary(0.0, 0);
            }

            String body = response.body().string();
            JSONObject json = new JSONObject(body);

            double averageRating = json.optDouble("average_rating", 0.0);
            int reviewCount = json.optInt("review_count", 0);

            return new RatingSummary(averageRating, reviewCount);

        } catch (Exception e) {
            System.out.println("❌ Review API rating error: " + e.getMessage());
            return new RatingSummary(0.0, 0);
        }
    }

    public List<ApiReview> getReviewsByProduct(int productId) {
        String url = baseUrl + "/api/products/" + productId + "/reviews";
        return getReviewsFromUrl(url);
    }

    public List<ApiReview> getAllReviewsForAdmin() {
        String url = baseUrl + "/api/admin/reviews";
        return getReviewsFromUrl(url);
    }

    private List<ApiReview> getReviewsFromUrl(String url) {
        List<ApiReview> reviews = new ArrayList<>();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return reviews;
            }

            String body = response.body().string();
            JSONArray array = new JSONArray(body);

            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                reviews.add(mapReview(json));
            }

        } catch (Exception e) {
            System.out.println("❌ Review API list error: " + e.getMessage());
        }

        return reviews;
    }

    public ReviewResult addReview(int productId, String customerName, int rating, String comment) {
        String url = baseUrl + "/api/reviews";

        try {
            JSONObject json = new JSONObject();
            json.put("product_id", productId);
            json.put("customer_name", customerName);
            json.put("rating", rating);
            json.put("comment", comment);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();

                if (!response.isSuccessful()) {
                    return ReviewResult.error("Review API error: " + responseBody);
                }

                return ReviewResult.success("Review added successfully.");
            }

        } catch (Exception e) {
            return ReviewResult.error("Review API connection error: " + e.getMessage());
        }
    }

    public ReviewResult hideReview(int reviewId) {
        return patchReviewStatus("/api/reviews/" + reviewId + "/hide");
    }

    public ReviewResult showReview(int reviewId) {
        return patchReviewStatus("/api/reviews/" + reviewId + "/show");
    }

    private ReviewResult patchReviewStatus(String path) {
        String url = baseUrl + path;

        RequestBody body = RequestBody.create("", JSON);

        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();

            if (!response.isSuccessful()) {
                return ReviewResult.error("Review API error: " + responseBody);
            }

            return ReviewResult.success("Review status updated.");

        } catch (Exception e) {
            return ReviewResult.error("Review API connection error: " + e.getMessage());
        }
    }

    public ReviewResult deleteReview(int reviewId) {
        String url = baseUrl + "/api/reviews/" + reviewId;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();

            if (!response.isSuccessful()) {
                return ReviewResult.error("Review API error: " + responseBody);
            }

            return ReviewResult.success("Review deleted successfully.");

        } catch (Exception e) {
            return ReviewResult.error("Review API connection error: " + e.getMessage());
        }
    }

    private ApiReview mapReview(JSONObject json) {
        ApiReview review = new ApiReview();

        review.setId(json.optInt("id"));
        review.setProductId(json.optInt("product_id"));
        review.setProductName(json.optString("product_name"));
        review.setCustomerName(json.optString("customer_name"));
        review.setRating(json.optInt("rating"));
        review.setComment(json.optString("comment"));
        review.setStatus(json.optString("status"));
        review.setCreatedAt(json.optString("created_at"));

        return review;
    }

    private String readConfig(String key) {
        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }

        return value;
    }

    public static class RatingSummary {
        private final double averageRating;
        private final int reviewCount;

        public RatingSummary(double averageRating, int reviewCount) {
            this.averageRating = averageRating;
            this.reviewCount = reviewCount;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public int getReviewCount() {
            return reviewCount;
        }
    }

    public static class ReviewResult {
        private final boolean success;
        private final String message;

        private ReviewResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ReviewResult success(String message) {
            return new ReviewResult(true, message);
        }

        public static ReviewResult error(String message) {
            return new ReviewResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ApiReview {
        private int id;
        private int productId;
        private String productName;
        private String customerName;
        private int rating;
        private String comment;
        private String status;
        private String createdAt;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getProductId() {
            return productId;
        }

        public void setProductId(int productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public int getRating() {
            return rating;
        }

        public void setRating(int rating) {
            this.rating = rating;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getStars() {
            StringBuilder stars = new StringBuilder();

            for (int i = 1; i <= 5; i++) {
                if (i <= rating) {
                    stars.append("★");
                } else {
                    stars.append("☆");
                }
            }

            return stars.toString();
        }
    }
}