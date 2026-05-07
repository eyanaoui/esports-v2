# 🧪 Guide de Tests - E-Sports CRUD Application

Ce document explique comment exécuter et comprendre les tests unitaires de l'application.

---

## 📋 Table des Matières

1. [Prérequis](#prérequis)
2. [Exécution des Tests](#exécution-des-tests)
3. [Types de Tests](#types-de-tests)
4. [Structure des Tests](#structure-des-tests)
5. [Interprétation des Résultats](#interprétation-des-résultats)
6. [Ajout de Nouveaux Tests](#ajout-de-nouveaux-tests)

---

## 🔧 Prérequis

- **Java 17** ou supérieur
- **Maven 3.x**
- **JUnit 5** (inclus dans le projet)
- **jqwik** (pour Property-Based Testing, inclus dans le projet)

---

## ▶️ Exécution des Tests

### **1. Exécuter TOUS les tests**

```bash
mvn test
```

**Résultat attendu:**
```
[INFO] Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### **2. Exécuter une suite de tests spécifique**

#### Tests de validation des utilisateurs:
```bash
mvn test -Dtest=UserValidationTest
```

#### Tests de validation des jeux:
```bash
mvn test -Dtest=GameValidationTest
```

#### Tests de validation des guides:
```bash
mvn test -Dtest=GuideValidationTest
```

#### Tests de validation des étapes:
```bash
mvn test -Dtest=GuideStepValidationTest
```

#### Tests de validation des évaluations:
```bash
mvn test -Dtest=GuideRatingValidationTest
```

#### Tests de l'inscription:
```bash
mvn test -Dtest=SignupValidationTest
```

#### Tests de sécurité (BCrypt):
```bash
mvn test -Dtest=PasswordSaltVersionBugConditionTest
mvn test -Dtest=PasswordSaltVersionPreservationTest
```

---

### **3. Exécuter avec rapport détaillé**

```bash
mvn test -X
```

Cette commande affiche tous les détails d'exécution, utile pour le débogage.

---

### **4. Exécuter avec couverture de code (si configuré)**

```bash
mvn clean test jacoco:report
```

Le rapport sera généré dans `target/site/jacoco/index.html`

---

## 🧪 Types de Tests

### **1. Tests Unitaires Classiques**

Tests de validation des règles métier pour chaque formulaire CRUD.

**Exemple:**
```java
@Test
public void testValidEmail() {
    assertTrue(isValidEmail("user@example.com"));
    assertTrue(isValidEmail("test.user@domain.co.uk"));
}

@Test
public void testInvalidEmail() {
    assertFalse(isValidEmail("")); // Empty
    assertFalse(isValidEmail("invalid")); // No @
}
```

**Fichiers:**
- `UserValidationTest.java`
- `GameValidationTest.java`
- `GuideValidationTest.java`
- `GuideStepValidationTest.java`
- `GuideRatingValidationTest.java`
- `SignupValidationTest.java`

---

### **2. Property-Based Testing (PBT)**

Tests avec génération automatique de données aléatoires pour vérifier les propriétés du système.

**Exemple:**
```java
@Property(tries = 5)
void testBugCondition(
    @ForAll @Email String email,
    @ForAll @StringLength(min = 4, max = 20) String password
) {
    // Test avec 5 combinaisons aléatoires d'email/password
    // Vérifie que le hashage BCrypt fonctionne toujours
}
```

**Fichiers:**
- `PasswordSaltVersionBugConditionTest.java` - Vérifie que le bug est corrigé
- `PasswordSaltVersionPreservationTest.java` - Vérifie que le comportement est préservé

**Avantages du PBT:**
- ✅ Teste automatiquement des milliers de cas
- ✅ Trouve des cas limites inattendus
- ✅ Génère des données aléatoires mais reproductibles

---

## 📁 Structure des Tests

```
src/test/java/com/esports/
│
├── validation/                          # Tests de validation
│   ├── UserValidationTest.java          # Validation utilisateurs (8 tests)
│   ├── GameValidationTest.java          # Validation jeux (8 tests)
│   ├── GuideValidationTest.java         # Validation guides (8 tests)
│   ├── GuideStepValidationTest.java     # Validation étapes (10 tests)
│   └── GuideRatingValidationTest.java   # Validation évaluations (6 tests)
│
├── controllers/                         # Tests des contrôleurs
│   └── SignupValidationTest.java        # Validation inscription (6 tests)
│
└── bugfix/                              # Tests de correction de bugs
    ├── PasswordSaltVersionBugConditionTest.java      # Test PBT (1 test)
    └── PasswordSaltVersionPreservationTest.java      # Tests PBT (6 tests)
```

**Total: 53 tests**

---

## 📊 Interprétation des Résultats

### **✅ Succès**

```
[INFO] Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Signification:**
- Tous les tests ont réussi
- Toutes les validations fonctionnent correctement
- Le code est prêt pour la production

---

### **❌ Échec**

```
[ERROR] Tests run: 53, Failures: 1, Errors: 0, Skipped: 0
[ERROR] testValidEmail:25 expected: <true> but was: <false>
```

**Signification:**
- Un test a échoué
- La validation d'email ne fonctionne pas comme prévu
- Il faut corriger le code avant de déployer

**Actions:**
1. Lire le message d'erreur
2. Identifier le test qui a échoué
3. Corriger le code de validation
4. Relancer les tests

---

### **⚠️ Erreur**

```
[ERROR] Tests run: 53, Failures: 0, Errors: 1, Skipped: 0
[ERROR] testValidEmail:25 NullPointerException
```

**Signification:**
- Une erreur s'est produite pendant l'exécution
- Le code a un bug (ex: NullPointerException)
- Il faut corriger le bug avant de continuer

---

## 🆕 Ajout de Nouveaux Tests

### **1. Créer un nouveau fichier de test**

```java
package com.esports.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MonNouveauTest {

    @Test
    public void testMaValidation() {
        // Arrange (Préparer)
        String input = "test";
        
        // Act (Agir)
        boolean result = maMethodeDeValidation(input);
        
        // Assert (Vérifier)
        assertTrue(result);
    }
    
    private boolean maMethodeDeValidation(String input) {
        // Logique de validation
        return input != null && !input.isEmpty();
    }
}
```

---

### **2. Exécuter le nouveau test**

```bash
mvn test -Dtest=MonNouveauTest
```

---

### **3. Bonnes pratiques**

✅ **DO:**
- Tester les cas valides ET invalides
- Tester les cas limites (vide, null, max length)
- Utiliser des noms de tests descriptifs
- Un test = une assertion principale
- Suivre le pattern AAA (Arrange, Act, Assert)

❌ **DON'T:**
- Tester plusieurs choses dans un seul test
- Dépendre de l'ordre d'exécution des tests
- Utiliser des données en dur qui peuvent changer
- Ignorer les tests qui échouent

---

## 📈 Métriques de Qualité

### **Couverture de Code**

Pour générer un rapport de couverture:

```bash
mvn clean test jacoco:report
```

**Objectif:** >80% de couverture pour les classes de validation

---

### **Temps d'Exécution**

Les tests doivent être rapides:
- ✅ Tests unitaires: <100ms par test
- ✅ Tests PBT: <1s par propriété
- ✅ Suite complète: <5s

**Temps actuel:** 1.352s pour 53 tests ✅

---

## 🐛 Débogage des Tests

### **1. Activer les logs détaillés**

```bash
mvn test -X -Dtest=MonTest
```

---

### **2. Exécuter un seul test**

```java
@Test
public void testSpecifique() {
    // Ajouter des System.out.println() pour déboguer
    System.out.println("Debug: input = " + input);
    assertTrue(result);
}
```

---

### **3. Utiliser le débogueur IDE**

1. Ouvrir le fichier de test dans votre IDE
2. Placer un point d'arrêt
3. Clic droit → "Debug Test"

---

## 📚 Ressources

### **Documentation JUnit 5:**
- https://junit.org/junit5/docs/current/user-guide/

### **Documentation jqwik (PBT):**
- https://jqwik.net/docs/current/user-guide.html

### **Bonnes pratiques de tests:**
- https://martinfowler.com/articles/practical-test-pyramid.html

---

## ✅ Checklist avant Commit

Avant de commiter du code, vérifier:

- [ ] Tous les tests passent (`mvn test`)
- [ ] Aucun test n'est ignoré (@Disabled)
- [ ] Les nouveaux tests sont ajoutés pour les nouvelles fonctionnalités
- [ ] La couverture de code est maintenue ou améliorée
- [ ] Les tests sont rapides (<5s pour la suite complète)

---

## 🎯 Résumé

**Commande principale:**
```bash
mvn test
```

**Résultat attendu:**
```
Tests run: 53, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**En cas de problème:**
1. Lire le message d'erreur
2. Identifier le test qui échoue
3. Corriger le code
4. Relancer les tests

**Tous les tests doivent passer avant de déployer en production!** ✅

---

**Dernière mise à jour:** 16 Avril 2026  
**Version:** 1.0.0
