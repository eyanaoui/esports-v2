package com.esports.services;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating and validating CAPTCHA challenges.
 * 
 * Generates simple mathematical or text-based CAPTCHAs to prevent
 * automated bot attacks on login, registration, and password recovery.
 * 
 * Features:
 * - Mathematical challenges (e.g., "5 + 3 = ?")
 * - Text-based challenges with distortion
 * - Session-based validation
 * - Automatic expiration (5 minutes)
 */
public class CaptchaService {
    
    private static final SecureRandom random = new SecureRandom();
    private static final Map<String, CaptchaChallenge> activeChallenges = new HashMap<>();
    private static final int CAPTCHA_EXPIRY_MINUTES = 5;
    
    // Logic questions with answers
    private static final String[][] LOGIC_QUESTIONS = {
        {"Quelle couleur obtient-on en mélangeant rouge et bleu?", "violet", "mauve", "purple"},
        {"Combien de jours dans une semaine?", "7", "sept"},
        {"Quelle est la capitale de la France?", "paris"},
        {"Combien font 10 divisé par 2?", "5", "cinq"},
        {"Quel animal dit 'miaou'?", "chat"},
        {"Quelle couleur est le ciel par beau temps?", "bleu"},
        {"Combien de roues a une voiture?", "4", "quatre"},
        {"Quel fruit est jaune et courbé?", "banane"},
        {"Quelle saison vient après l'été?", "automne"},
        {"Combien de mois dans une année?", "12", "douze"}
    };
    
    // Color mixing questions
    private static final String[][] COLOR_QUESTIONS = {
        {"Rouge + Jaune = ?", "orange"},
        {"Bleu + Jaune = ?", "vert"},
        {"Rouge + Bleu = ?", "violet", "mauve"},
        {"Blanc + Noir = ?", "gris"},
        {"Rouge + Blanc = ?", "rose"}
    };
    
    /**
     * CAPTCHA types.
     */
    public enum CaptchaType {
        MATH,      // Mathematical challenge (e.g., "5 + 3 = ?")
        TEXT,      // Text-based challenge (e.g., "Enter: ABC123")
        LOGIC,     // Logic questions (e.g., "What color is the sky?")
        SEQUENCE,  // Number/letter sequences (e.g., "2, 4, 6, ?")
        COLOR      // Color-based questions (e.g., "Red + Blue = ?")
    }
    
    /**
     * Generate a new CAPTCHA challenge.
     * 
     * @param type The type of CAPTCHA to generate
     * @return CaptchaChallenge containing the challenge ID, question, and answer
     */
    public CaptchaChallenge generateCaptcha(CaptchaType type) {
        String challengeId = UUID.randomUUID().toString();
        String question;
        String answer;
        
        switch (type) {
            case MATH:
                int num1 = random.nextInt(10) + 1;  // 1-10
                int num2 = random.nextInt(10) + 1;  // 1-10
                int operation = random.nextInt(3);  // 0=add, 1=subtract, 2=multiply
                
                switch (operation) {
                    case 0: // Addition
                        question = "Combien font " + num1 + " + " + num2 + " ?";
                        answer = String.valueOf(num1 + num2);
                        break;
                    case 1: // Subtraction (ensure positive result)
                        if (num1 < num2) {
                            int temp = num1;
                            num1 = num2;
                            num2 = temp;
                        }
                        question = "Combien font " + num1 + " - " + num2 + " ?";
                        answer = String.valueOf(num1 - num2);
                        break;
                    case 2: // Multiplication (smaller numbers)
                        num1 = random.nextInt(5) + 1;  // 1-5
                        num2 = random.nextInt(5) + 1;  // 1-5
                        question = "Combien font " + num1 + " × " + num2 + " ?";
                        answer = String.valueOf(num1 * num2);
                        break;
                    default:
                        question = "Combien font " + num1 + " + " + num2 + " ?";
                        answer = String.valueOf(num1 + num2);
                }
                break;
                
            case TEXT:
                // Generate random alphanumeric string (6 characters)
                String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Exclude confusing chars
                StringBuilder code = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    code.append(chars.charAt(random.nextInt(chars.length())));
                }
                answer = code.toString();
                question = "Entrez le code: " + answer;
                break;
                
            case LOGIC:
                // Select random logic question
                String[] logicQ = LOGIC_QUESTIONS[random.nextInt(LOGIC_QUESTIONS.length)];
                question = logicQ[0];
                // Answer is any of the valid answers (index 1+)
                answer = logicQ[1]; // Primary answer
                break;
                
            case SEQUENCE:
                // Generate number or letter sequence
                int seqType = random.nextInt(3);
                switch (seqType) {
                    case 0: // Even numbers
                        int start = random.nextInt(5) * 2; // 0, 2, 4, 6, 8
                        question = start + ", " + (start + 2) + ", " + (start + 4) + ", ?";
                        answer = String.valueOf(start + 6);
                        break;
                    case 1: // Multiples
                        int mult = random.nextInt(3) + 2; // 2, 3, or 4
                        question = mult + ", " + (mult * 2) + ", " + (mult * 3) + ", ?";
                        answer = String.valueOf(mult * 4);
                        break;
                    case 2: // Letter sequence
                        char letter = (char) ('A' + random.nextInt(23)); // A-W (leave room for 3 more)
                        question = letter + ", " + (char)(letter + 1) + ", " + (char)(letter + 2) + ", ?";
                        answer = String.valueOf((char)(letter + 3));
                        break;
                    default:
                        question = "2, 4, 6, ?";
                        answer = "8";
                }
                break;
                
            case COLOR:
                // Select random color mixing question
                String[] colorQ = COLOR_QUESTIONS[random.nextInt(COLOR_QUESTIONS.length)];
                question = colorQ[0];
                answer = colorQ[1]; // Primary answer
                break;
                
            default:
                // Fallback to TEXT
                chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
                code = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    code.append(chars.charAt(random.nextInt(chars.length())));
                }
                answer = code.toString();
                question = "Entrez le code: " + answer;
                break;
        }
        
        CaptchaChallenge challenge = new CaptchaChallenge(challengeId, question, answer, type);
        
        // Store challenge with expiry
        activeChallenges.put(challengeId, challenge);
        
        // Clean up expired challenges
        cleanupExpiredChallenges();
        
        return challenge;
    }
    
    /**
     * Validate a CAPTCHA response.
     * 
     * @param challengeId The challenge ID
     * @param userAnswer The user's answer
     * @return true if answer is correct and challenge is valid
     */
    public boolean validateCaptcha(String challengeId, String userAnswer) {
        if (challengeId == null || userAnswer == null) {
            return false;
        }
        
        CaptchaChallenge challenge = activeChallenges.get(challengeId);
        
        if (challenge == null) {
            System.out.println("[CAPTCHA] Challenge not found or expired: " + challengeId);
            return false;
        }
        
        // Check if expired
        if (challenge.isExpired()) {
            activeChallenges.remove(challengeId);
            System.out.println("[CAPTCHA] Challenge expired: " + challengeId);
            return false;
        }
        
        // Validate answer (case-insensitive for text)
        String userAnswerTrimmed = userAnswer.trim().toLowerCase();
        boolean isValid = false;
        
        // For LOGIC questions, check all possible answers
        if (challenge.getType() == CaptchaType.LOGIC) {
            for (String[] logicQ : LOGIC_QUESTIONS) {
                if (logicQ[0].equals(challenge.getQuestion())) {
                    // Check all valid answers (index 1+)
                    for (int i = 1; i < logicQ.length; i++) {
                        if (logicQ[i].equalsIgnoreCase(userAnswerTrimmed)) {
                            isValid = true;
                            break;
                        }
                    }
                    break;
                }
            }
        } 
        // For COLOR questions, check all possible answers
        else if (challenge.getType() == CaptchaType.COLOR) {
            for (String[] colorQ : COLOR_QUESTIONS) {
                if (colorQ[0].equals(challenge.getQuestion())) {
                    // Check all valid answers (index 1+)
                    for (int i = 1; i < colorQ.length; i++) {
                        if (colorQ[i].equalsIgnoreCase(userAnswerTrimmed)) {
                            isValid = true;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        // For other types, simple comparison
        else {
            isValid = challenge.getAnswer().equalsIgnoreCase(userAnswerTrimmed);
        }
        
        // Remove challenge after validation (one-time use)
        activeChallenges.remove(challengeId);
        
        if (isValid) {
            System.out.println("[CAPTCHA] Challenge validated successfully: " + challengeId);
        } else {
            System.out.println("[CAPTCHA] Invalid answer for challenge: " + challengeId);
        }
        
        return isValid;
    }
    
    /**
     * Clean up expired challenges.
     */
    private void cleanupExpiredChallenges() {
        activeChallenges.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Get the number of active challenges (for monitoring).
     */
    public int getActiveChallengeCount() {
        cleanupExpiredChallenges();
        return activeChallenges.size();
    }
    
    /**
     * CAPTCHA challenge data.
     */
    public static class CaptchaChallenge {
        private final String challengeId;
        private final String question;
        private final String answer;
        private final CaptchaType type;
        private final long createdAt;
        
        public CaptchaChallenge(String challengeId, String question, String answer, CaptchaType type) {
            this.challengeId = challengeId;
            this.question = question;
            this.answer = answer;
            this.type = type;
            this.createdAt = System.currentTimeMillis();
        }
        
        public String getChallengeId() {
            return challengeId;
        }
        
        public String getQuestion() {
            return question;
        }
        
        public String getAnswer() {
            return answer;
        }
        
        public CaptchaType getType() {
            return type;
        }
        
        public boolean isExpired() {
            long ageMinutes = (System.currentTimeMillis() - createdAt) / (1000 * 60);
            return ageMinutes >= CAPTCHA_EXPIRY_MINUTES;
        }
        
        @Override
        public String toString() {
            return "CaptchaChallenge{" +
                    "challengeId='" + challengeId + '\'' +
                    ", question='" + question + '\'' +
                    ", type=" + type +
                    ", expired=" + isExpired() +
                    '}';
        }
    }
}
