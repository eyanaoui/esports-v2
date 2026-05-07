from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
import joblib
import mysql.connector

# connect to DB
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="esports_db"
)
cursor = conn.cursor()
cursor.execute("SELECT description, difficulty FROM guide WHERE description IS NOT NULL AND difficulty IS NOT NULL")
rows = cursor.fetchall()
conn.close()

descriptions = [row[0] for row in rows]
labels       = [row[1] for row in rows]

# extra training data
extra = [
    ("beginner friendly simple basic easy steps starter guide",           "Easy"),
    ("no experience needed straightforward quick simple tutorial",         "Easy"),
    ("basic introduction simple concepts easy to follow beginner",         "Easy"),
    ("advanced complex mechanics deep strategy high skill required",       "Hard"),
    ("expert level difficult challenging mastery precision needed",        "Hard"),
    ("hard to learn complex combos advanced techniques pro level",         "Hard"),
    ("intermediate some experience needed moderate difficulty balanced",   "Medium"),
    ("moderate challenge requires practice some knowledge needed",        "Medium"),
    ("average difficulty not too easy not too hard some skill needed",    "Medium"),
]

for text, label in extra:
    descriptions.append(text)
    labels.append(label)

print(f"✅ Total samples: {len(descriptions)}")
print(f"   Easy: {labels.count('Easy')}, Medium: {labels.count('Medium')}, Hard: {labels.count('Hard')}")

model = Pipeline([
    ('tfidf', TfidfVectorizer()),
    ('clf',   MultinomialNB())
])

model.fit(descriptions, labels)
print("✅ Model trained!")

joblib.dump(model, 'difficulty_model.pkl')
print("✅ Model saved!")

# test
tests = [
    "A simple beginner guide to get started quickly",
    "Advanced complex mechanics requiring high skill and precision",
    "Some experience needed moderate challenge balanced gameplay",
]
for t in tests:
    print(f"  '{t[:50]}...' → {model.predict([t])[0]}")