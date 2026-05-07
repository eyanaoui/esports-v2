# 📚 Documentation des Métiers (Business Logic) - E-Sports Platform

## 📋 Table des Matières

1. [Services Métier](#services-métier)
2. [Couche d'Accès aux Données (DAO)](#couche-daccès-aux-données-dao)
3. [Modèles de Domaine](#modèles-de-domaine)
4. [Diagrammes d'Architecture](#diagrammes-darchitecture)

---

## 🎯 Services Métier

### 1. **SignatureAuthService** - Service d'Authentification par Signature

**Responsabilité** : Gestion complète de l'authentification biométrique par signature

**Fonctionnalités** :
- ✅ Authentification d'utilisateur via signature dessinée
- ✅ Comparaison de signatures avec seuil de similarité (75%)
- ✅ Rate limiting : 10 tentatives par minute maximum
- ✅ Verrouillage automatique après 5 échecs (5 minutes)
- ✅ Délai de 2 secondes après échec (protection brute force)
- ✅ Enregistrement des tentatives d'authentification

**Méthodes principales** :
```java
boolean authenticateWithSignature(String email, BufferedImage signature)
boolean isAccountLocked(String email)
void unlockAccount(String email)
List<SignatureAuthAttempt> getRecentAttempts(String email, int minutes)
```

**Sécurité** :
- Chiffrement AES-256-GCM des signatures
- Hachage SHA-256 pour comparaison
- Protection contre les attaques par force brute
- Audit complet des tentatives

---

### 2. **EncryptionService** - Service de Chiffrement

**Responsabilité** : Chiffrement et déchiffrement sécurisé des données sensibles

**Fonctionnalités** :
- ✅ Chiffrement AES-256-GCM (Galois/Counter Mode)
- ✅ Génération d'IV (Initialization Vector) aléatoire
- ✅ Gestion sécurisée des clés de chiffrement
- ✅ Conversion Base64 pour stockage

**Méthodes principales** :
```java
String encrypt(byte[] data)
byte[] decrypt(String encryptedData)
String generateSecureKey()
```

**Algorithmes** :
- **Chiffrement** : AES/GCM/NoPadding
- **Taille de clé** : 256 bits
- **IV** : 12 bytes aléatoires
- **Tag d'authentification** : 128 bits

---

### 3. **ImageComparator** - Service de Comparaison d'Images

**Responsabilité** : Comparaison de similarité entre deux images (signatures)

**Fonctionnalités** :
- ✅ Comparaison par histogramme de couleurs
- ✅ Calcul de corrélation de Pearson
- ✅ Normalisation des images (taille, format)
- ✅ Score de similarité (0.0 à 1.0)

**Méthodes principales** :
```java
double compareImages(BufferedImage img1, BufferedImage img2)
double calculateHistogramSimilarity(BufferedImage img1, BufferedImage img2)
BufferedImage normalizeImage(BufferedImage image, int width, int height)
```

**Algorithme** :
- Extraction d'histogrammes RGB
- Calcul de corrélation de Pearson
- Seuil de similarité : 75% (configurable)

---

### 4. **SignaturePasswordRecoveryService** - Récupération de Mot de Passe

**Responsabilité** : Réinitialisation de mot de passe via signature biométrique

**Fonctionnalités** :
- ✅ Vérification d'identité par signature
- ✅ Réinitialisation sécurisée du mot de passe
- ✅ Validation de la signature (seuil 75%)
- ✅ Vérification du statut du compte (non banni)

**Méthodes principales** :
```java
boolean verifySignatureAndResetPassword(String email, BufferedImage signature, String newPassword)
boolean verifyUserSignature(String email, BufferedImage signature)
```

**Flux de travail** :
1. Utilisateur entre son email
2. Dessine sa signature
3. Système vérifie la similarité
4. Si match ≥ 75% → réinitialisation autorisée
5. Nouveau mot de passe haché et stocké

---

### 5. **CaptchaService** - Service CAPTCHA

**Responsabilité** : Génération et validation de CAPTCHA pour protection anti-bot

**Fonctionnalités** :
- ✅ CAPTCHA mathématique (addition, soustraction)
- ✅ CAPTCHA textuel (chaînes aléatoires)
- ✅ Gestion de session avec expiration (5 minutes)
- ✅ Validation case-insensitive

**Méthodes principales** :
```java
String generateCaptcha(String sessionId, CaptchaType type)
boolean validateCaptcha(String sessionId, String userAnswer)
void clearExpiredCaptchas()
```

**Types de CAPTCHA** :
- **MATH** : "Combien font 7 + 3 ?"
- **TEXT** : "Entrez le code : XY7K9"

---

### 6. **OAuthService** - Service OAuth Google

**Responsabilité** : Authentification via Google OAuth 2.0

**Fonctionnalités** :
- ✅ Génération d'URL d'autorisation Google
- ✅ Échange de code contre tokens (access + refresh)
- ✅ Récupération du profil utilisateur Google
- ✅ Validation de state (protection CSRF)
- ✅ Gestion du refresh token

**Méthodes principales** :
```java
String getAuthorizationUrl(String state)
OAuthTokens exchangeCodeForTokens(String code)
GoogleUserProfile getUserProfile(String accessToken)
OAuthTokens refreshAccessToken(String refreshToken)
```

**Configuration** :
- Client ID et Secret dans `oauth.properties`
- Scopes : email, profile, openid
- Redirect URI : http://localhost:8080/oauth/callback

---

### 7. **OAuthCallbackServer** - Serveur de Callback OAuth

**Responsabilité** : Serveur HTTP local pour recevoir le callback OAuth

**Fonctionnalités** :
- ✅ Serveur HTTP embarqué (port 8080)
- ✅ Réception du code d'autorisation
- ✅ Validation du state
- ✅ Arrêt automatique après callback

**Méthodes principales** :
```java
void start()
void stop()
String waitForAuthorizationCode()
```

---

### 8. **AIService** - Service d'Intelligence Artificielle

**Responsabilité** : Intégration avec services ML Python (sentiment, difficulté, recommandations)

**Fonctionnalités** :
- ✅ Analyse de sentiment des commentaires
- ✅ Prédiction de difficulté des guides
- ✅ Système de recommandation de guides
- ✅ Communication avec API Python (Flask)

**Méthodes principales** :
```java
String analyzeSentiment(String text)
String predictDifficulty(String guideContent)
List<Integer> getRecommendations(int userId)
```

**Endpoints Python** :
- `http://localhost:5000/sentiment`
- `http://localhost:5000/difficulty`
- `http://localhost:5000/recommend`

---

### 9. **DiscordWebhookService** - Service Webhook Discord

**Responsabilité** : Notifications Discord pour événements importants

**Fonctionnalités** :
- ✅ Envoi de notifications vers Discord
- ✅ Formatage des messages (embeds)
- ✅ Gestion des erreurs réseau

**Méthodes principales** :
```java
void sendNotification(String message)
void sendEmbed(String title, String description, String color)
```

---

## 💾 Couche d'Accès aux Données (DAO)

### 1. **SignatureDAO** - Gestion des Signatures

**Responsabilité** : CRUD des signatures utilisateur

**Opérations** :
- ✅ Sauvegarde de signature (chiffrée)
- ✅ Récupération par userId
- ✅ Mise à jour de signature
- ✅ Suppression de signature
- ✅ Calcul de hash SHA-256

**Table** : `user_signatures`
```sql
CREATE TABLE user_signatures (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    signature_data TEXT NOT NULL,  -- Base64 encrypted
    signature_hash VARCHAR(64) NOT NULL,  -- SHA-256
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

### 2. **AuditLogDAO** - Journal d'Audit

**Responsabilité** : Traçabilité complète des actions système

**Opérations** :
- ✅ Enregistrement d'événements
- ✅ Filtrage par utilisateur, action, date
- ✅ Nettoyage automatique (90 jours)
- ✅ Recherche avancée

**Table** : `audit_log`
```sql
CREATE TABLE audit_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id INT,
    details TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Types d'actions** :
- `SIGNATURE_AUTH_SUCCESS`
- `SIGNATURE_AUTH_FAILURE`
- `PASSWORD_RESET`
- `USER_BANNED`
- `USER_UNBANNED`
- `LOGIN_SUCCESS`
- `LOGIN_FAILURE`

---

### 3. **OAuthTokenDAO** - Gestion des Tokens OAuth

**Responsabilité** : Stockage et gestion des tokens OAuth

**Opérations** :
- ✅ Sauvegarde de tokens (access + refresh)
- ✅ Récupération par userId
- ✅ Mise à jour de tokens
- ✅ Vérification d'expiration
- ✅ Révocation de tokens

**Table** : `oauth_tokens`
```sql
CREATE TABLE oauth_tokens (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    provider VARCHAR(50) NOT NULL,  -- 'GOOGLE'
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

### 4. **UserDAO** - Gestion des Utilisateurs

**Responsabilité** : CRUD des utilisateurs avec sécurité renforcée

**Opérations** :
- ✅ Création d'utilisateur (avec hachage bcrypt)
- ✅ Authentification (email/password)
- ✅ Bannissement/Débannissement
- ✅ Gestion des rôles (ADMIN, USER)
- ✅ Statistiques (total, bannis, actifs)

**Table** : `users`
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- bcrypt hash
    is_blocked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Méthodes spéciales** :
```java
int getTotalUsers()
int getBannedUsersCount()
int getActiveUsersCount()
List<User> getBannedUsers()
void banUser(int userId)
void unbanUser(int userId)
```

---

### 5. **GameDAO, GuideDAO, TeamDAO, TournamentDAO**

**Responsabilité** : Gestion des entités métier e-sports

**Opérations communes** :
- ✅ CRUD complet
- ✅ Recherche et filtrage
- ✅ Statistiques
- ✅ Relations entre entités

---

## 🏗️ Modèles de Domaine

### 1. **SignatureData** - Données de Signature

```java
public class SignatureData {
    private int id;
    private int userId;
    private byte[] signatureImage;  // Image brute
    private String encryptedData;   // Chiffré en base
    private String signatureHash;   // SHA-256
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### 2. **SignatureAuthAttempt** - Tentative d'Authentification

```java
public class SignatureAuthAttempt {
    private int id;
    private int userId;
    private boolean success;
    private double similarityScore;
    private String ipAddress;
    private LocalDateTime attemptTime;
}
```

---

### 3. **AuditLogEntry** - Entrée de Journal

```java
public class AuditLogEntry {
    private int id;
    private int userId;
    private String action;
    private String entityType;
    private int entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
}
```

---

### 4. **OAuthTokens** - Tokens OAuth

```java
public class OAuthTokens {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private LocalDateTime expiresAt;
}
```

---

### 5. **GoogleUserProfile** - Profil Google

```java
public class GoogleUserProfile {
    private String id;
    private String email;
    private boolean emailVerified;
    private String name;
    private String givenName;
    private String familyName;
    private String picture;
}
```

---

### 6. **User** - Utilisateur

```java
public class User {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;  // bcrypt hash
    private boolean isBlocked;
    private Set<UserRole> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 📊 Diagrammes d'Architecture

### Architecture en Couches

```
┌─────────────────────────────────────────────────────────┐
│                    Couche Présentation                   │
│  (Controllers: Login, Admin, User, SignatureRecovery)   │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                     Couche Métier                        │
│  (Services: SignatureAuth, Encryption, OAuth, CAPTCHA)  │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                  Couche Accès Données                    │
│     (DAO: Signature, User, AuditLog, OAuthToken)        │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    Base de Données                       │
│              (MySQL: users, signatures, etc.)           │
└─────────────────────────────────────────────────────────┘
```

---

### Flux d'Authentification par Signature

```
┌──────────┐         ┌──────────────────┐         ┌─────────────────┐
│          │         │                  │         │                 │
│  Client  │────────▶│ SignatureAuth    │────────▶│  SignatureDAO   │
│  (UI)    │         │    Service       │         │                 │
│          │         │                  │         │                 │
└──────────┘         └──────────────────┘         └─────────────────┘
     │                        │                            │
     │                        ↓                            │
     │               ┌─────────────────┐                  │
     │               │                 │                  │
     │               │ ImageComparator │                  │
     │               │                 │                  │
     │               └─────────────────┘                  │
     │                        │                            │
     │                        ↓                            │
     │               ┌─────────────────┐                  │
     │               │                 │                  │
     │               │ EncryptionService│                 │
     │               │                 │                  │
     │               └─────────────────┘                  │
     │                        │                            │
     │                        ↓                            ↓
     │               ┌──────────────────────────────────────┐
     │               │                                      │
     └──────────────▶│          AuditLogDAO                 │
                     │                                      │
                     └──────────────────────────────────────┘
```

---

### Flux OAuth Google

```
┌──────────┐         ┌──────────────┐         ┌──────────────┐
│          │         │              │         │              │
│  Client  │────────▶│ OAuthService │────────▶│ Google OAuth │
│          │         │              │         │   Server     │
└──────────┘         └──────────────┘         └──────────────┘
     │                      │                        │
     │                      ↓                        │
     │              ┌───────────────┐                │
     │              │               │                │
     │              │ OAuthCallback │◀───────────────┘
     │              │    Server     │
     │              └───────────────┘
     │                      │
     │                      ↓
     │              ┌───────────────┐
     │              │               │
     └─────────────▶│ OAuthTokenDAO │
                    │               │
                    └───────────────┘
```

---

## 🔐 Sécurité Implémentée

### 1. **Chiffrement**
- ✅ AES-256-GCM pour signatures
- ✅ Bcrypt pour mots de passe
- ✅ SHA-256 pour hachage de signatures

### 2. **Protection Brute Force**
- ✅ Rate limiting (10 tentatives/minute)
- ✅ Account lockout (5 échecs = 5 min)
- ✅ Délai après échec (2 secondes)

### 3. **Audit & Traçabilité**
- ✅ Logging de toutes les actions sensibles
- ✅ Enregistrement IP et timestamp
- ✅ Rétention 90 jours

### 4. **CAPTCHA**
- ✅ Protection anti-bot sur tous les formulaires
- ✅ Expiration 5 minutes
- ✅ Validation côté serveur

### 5. **OAuth**
- ✅ State validation (CSRF protection)
- ✅ Token refresh automatique
- ✅ Révocation de tokens

---

## 📈 Statistiques & Monitoring

### Métriques Disponibles

1. **Utilisateurs**
   - Total des comptes
   - Comptes actifs
   - Comptes bannis

2. **Authentification**
   - Tentatives réussies/échouées
   - Comptes verrouillés
   - Taux de similarité moyen

3. **Audit**
   - Actions par type
   - Actions par utilisateur
   - Timeline des événements

---

## 🧪 Tests Implémentés

### Tests Unitaires
- ✅ EncryptionServiceTest (29 tests)
- ✅ ImageComparatorTest (32 tests)
- ✅ SignatureDAOTest (17 tests)
- ✅ AuditLogDAOTest (12 tests)
- ✅ SignatureAuthServiceTest
- ✅ OAuthServiceTest

### Tests de Propriétés (PBT)
- ✅ EncryptionServicePropertyTest (18 tests)
- ✅ SignatureAuthenticationConsistencyPropertyTest
- ✅ SignatureRateLimitingPropertyTest
- ✅ OAuthStateValidationPropertyTest

### Tests d'Intégration
- ✅ SignatureCanvasUITest
- ✅ MultiAuthenticationEquivalencePropertyTest

---

## 📝 Notes de Déploiement

### Prérequis
1. Java 17+
2. MySQL 8.0+
3. Python 3.8+ (pour ML services)
4. Maven 3.6+

### Configuration
1. `oauth.properties` - Credentials Google OAuth
2. `application.properties` - Configuration DB
3. Variables d'environnement pour clés de chiffrement

### Migrations
- V001: Schema de base
- V002: Signature authentication & audit log
- V003: OAuth tokens & user improvements

---

## 🎓 Glossaire

- **DAO** : Data Access Object - Couche d'accès aux données
- **OAuth** : Open Authorization - Protocole d'autorisation
- **CAPTCHA** : Completely Automated Public Turing test
- **AES-GCM** : Advanced Encryption Standard - Galois/Counter Mode
- **Bcrypt** : Fonction de hachage adaptative pour mots de passe
- **SHA-256** : Secure Hash Algorithm 256 bits
- **PBT** : Property-Based Testing

---

**Date de création** : 30 Avril 2026  
**Version** : 1.0  
**Auteur** : E-Sports Development Team
