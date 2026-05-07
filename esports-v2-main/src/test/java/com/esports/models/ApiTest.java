package com.esports.models;

import okhttp3.*;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiTest {

    private static OkHttpClient client;
    private static final String FLASK_URL    = "http://127.0.0.1:5000";
    private static final String TEXTBLOB_URL = "http://127.0.0.1:5001";
    private static final MediaType JSON      = MediaType.parse("application/json");

    @BeforeAll
    static void setup() {
        client = new OkHttpClient();
    }

    // ===== HEALTH CHECK =====

    @Test
    @Order(1)
    @DisplayName("Flask API is running")
    void testFlaskHealth() throws Exception {
        Request request = new Request.Builder()
                .url(FLASK_URL + "/health")
                .get()
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertEquals("ok", result.getString("status"));
        System.out.println("✅ Flask API is running!");
    }

    // ===== SENTIMENT TESTS =====

    @Test
    @Order(2)
    @DisplayName("Sentiment: positive comment")
    void testSentimentPositive() throws Exception {
        JSONObject json = new JSONObject();
        json.put("comment", "This guide is amazing and very helpful!");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/sentiment")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertTrue(result.has("sentiment"));
        assertTrue(result.has("confidence"));
        assertEquals("positive", result.getString("sentiment"));
        System.out.println("✅ Positive sentiment: " + result.getString("sentiment")
                + " (" + result.getDouble("confidence") + ")");
    }

    @Test
    @Order(3)
    @DisplayName("Sentiment: negative comment")
    void testSentimentNegative() throws Exception {
        JSONObject json = new JSONObject();
        json.put("comment", "This guide is terrible and completely useless!");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/sentiment")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertEquals("negative", result.getString("sentiment"));
        System.out.println("✅ Negative sentiment: " + result.getString("sentiment")
                + " (" + result.getDouble("confidence") + ")");
    }

    @Test
    @Order(4)
    @DisplayName("Sentiment: empty comment returns error")
    void testSentimentEmpty() throws Exception {
        JSONObject json = new JSONObject();
        json.put("comment", "");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/sentiment")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(400, response.code());
        System.out.println("✅ Empty comment correctly rejected!");
    }

    // ===== DIFFICULTY TESTS =====

    @Test
    @Order(5)
    @DisplayName("Difficulty: Easy prediction")
    void testDifficultyEasy() throws Exception {
        JSONObject json = new JSONObject();
        json.put("description", "A simple beginner friendly guide easy to follow basic steps");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/difficulty")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertTrue(result.has("difficulty"));
        assertTrue(result.has("confidence"));
        assertEquals("Easy", result.getString("difficulty"));
        System.out.println("✅ Easy difficulty: " + result.getString("difficulty")
                + " (" + result.getDouble("confidence") + ")");
    }

    @Test
    @Order(6)
    @DisplayName("Difficulty: Hard prediction")
    void testDifficultyHard() throws Exception {
        JSONObject json = new JSONObject();
        json.put("description", "Advanced complex mechanics requiring high skill precision expert level mastery");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/difficulty")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertEquals("Hard", result.getString("difficulty"));
        System.out.println("✅ Hard difficulty: " + result.getString("difficulty")
                + " (" + result.getDouble("confidence") + ")");
    }

    @Test
    @Order(7)
    @DisplayName("Difficulty: Medium prediction")
    void testDifficultyMedium() throws Exception {
        JSONObject json = new JSONObject();
        json.put("description", "Intermediate guide requires some experience moderate challenge balanced gameplay");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/difficulty")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertEquals("Medium", result.getString("difficulty"));
        System.out.println("✅ Medium difficulty: " + result.getString("difficulty")
                + " (" + result.getDouble("confidence") + ")");
    }

    @Test
    @Order(8)
    @DisplayName("Difficulty: empty description returns error")
    void testDifficultyEmpty() throws Exception {
        JSONObject json = new JSONObject();
        json.put("description", "");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/difficulty")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(400, response.code());
        System.out.println("✅ Empty description correctly rejected!");
    }

    // ===== RECOMMENDER TESTS =====

    @Test
    @Order(9)
    @DisplayName("Recommender: returns similar games")
    void testRecommenderValid() throws Exception {
        JSONObject json = new JSONObject();
        json.put("game_name", "Minecraft");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/recommend")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertTrue(result.has("recommendations"));
        System.out.println("✅ Recommendations for Minecraft: "
                + result.getJSONArray("recommendations").toString());
    }

    @Test
    @Order(10)
    @DisplayName("Recommender: unknown game returns empty list")
    void testRecommenderUnknown() throws Exception {
        JSONObject json = new JSONObject();
        json.put("game_name", "UnknownGameXYZ123");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(FLASK_URL + "/predict/recommend")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertEquals(0, result.getJSONArray("recommendations").length());
        System.out.println("✅ Unknown game correctly returns empty list!");
    }

    // ===== TEXTBLOB TESTS =====

    @Test
    @Order(11)
    @DisplayName("TextBlob: HAPPY sentiment")
    void testTextBlobHappy() throws Exception {
        JSONObject json = new JSONObject();
        json.put("text", "This is an absolutely wonderful and amazing guide!");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(TEXTBLOB_URL + "/api/predict")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertTrue(result.has("sentiment"));
        assertTrue(result.has("score"));
        assertEquals("HAPPY", result.getString("sentiment"));
        System.out.println("✅ TextBlob HAPPY: " + result.getString("sentiment")
                + " (score: " + result.getDouble("score") + ")");
    }

    @Test
    @Order(12)
    @DisplayName("TextBlob: ANGRY sentiment")
    void testTextBlobAngry() throws Exception {
        JSONObject json = new JSONObject();
        json.put("text", "This is absolutely terrible horrible and disgusting!");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(TEXTBLOB_URL + "/api/predict")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertEquals("ANGRY", result.getString("sentiment"));
        System.out.println("✅ TextBlob ANGRY: " + result.getString("sentiment")
                + " (score: " + result.getDouble("score") + ")");
    }

    @Test
    @Order(13)
    @DisplayName("TextBlob: NEUTRAL sentiment")
    void testTextBlobNeutral() throws Exception {
        JSONObject json = new JSONObject();
        json.put("text", "This is a guide about playing games");
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(TEXTBLOB_URL + "/api/predict")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        JSONObject result = new JSONObject(response.body().string());
        assertEquals("NEUTRAL", result.getString("sentiment"));
        System.out.println("✅ TextBlob NEUTRAL: " + result.getString("sentiment")
                + " (score: " + result.getDouble("score") + ")");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("\n🎉 All API tests completed!");
    }
}