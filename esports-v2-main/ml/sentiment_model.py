import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
import joblib
import mysql.connector

# connect to DB and fetch comments
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="esports_db"
)

cursor = conn.cursor()
cursor.execute("SELECT comment, rating_value FROM guide_rating WHERE comment IS NOT NULL")
rows = cursor.fetchall()
conn.close()

# prepare data from DB
comments = [row[0] for row in rows]
labels   = ["positive" if row[1] >= 3 else "negative" for row in rows]

# add extra training data to balance the model
extra_comments = [
    "amazing guide very helpful",        "positive",
    "best guide i have ever seen",       "positive",
    "very clear and easy to follow",     "positive",
    "great explanation loved it",        "positive",
    "super useful thanks a lot",         "positive",
    "perfect guide highly recommend",    "positive",
    "good tips learned a lot",           "positive",
    "excellent well written guide",      "positive",
    "this is awesome very detailed",     "positive",
    "love this guide so much",           "positive",
    "terrible guide waste of time",      "negative",
    "worst guide ever useless",          "negative",
    "bad explanation did not help",      "negative",
    "horrible guide very confusing",     "negative",
    "completely wrong information",      "negative",
    "this guide is garbage",             "negative",
    "do not follow this guide",          "negative",
    "very bad poorly written",           "negative",
    "misleading and unhelpful",          "negative",
    "absolute trash guide",              "negative",
]

extra_texts  = extra_comments[0::2]
extra_labels = extra_comments[1::2]

comments += extra_texts
labels   += extra_labels

print(f"✅ Total training samples: {len(comments)}")
print(f"   Positive: {labels.count('positive')}, Negative: {labels.count('negative')}")

# train model
model = Pipeline([
    ('tfidf', TfidfVectorizer()),
    ('clf',   MultinomialNB())
])

model.fit(comments, labels)
print("✅ Model trained!")

# save model
joblib.dump(model, 'sentiment_model.pkl')
print("✅ Model saved!")

# quick test
test_comments = [
    "This guide is amazing and very helpful!",
    "This guide is terrible and useless",
    "pretty good guide overall",
    "worst guide ever",
    "not bad could be better",
    "i hate this guide"
]
predictions = model.predict(test_comments)
for comment, pred in zip(test_comments, predictions):
    emoji = "✅" if pred == "positive" else "❌"
    print(f"  {emoji} '{comment}' → {pred}")