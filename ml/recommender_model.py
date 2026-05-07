from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import joblib
import mysql.connector
import numpy as np

# connect to DB
conn = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="esports_db"
)
cursor = conn.cursor()
cursor.execute("SELECT id, name, description FROM game WHERE description IS NOT NULL")
rows = cursor.fetchall()
conn.close()

ids          = [row[0] for row in rows]
names        = [row[1] for row in rows]
descriptions = [row[2] for row in rows]

print(f"✅ Loaded {len(names)} games")

# build TF-IDF matrix
vectorizer = TfidfVectorizer()
tfidf_matrix = vectorizer.fit_transform(descriptions)

# save everything
joblib.dump({
    'ids':         ids,
    'names':       names,
    'vectorizer':  vectorizer,
    'tfidf_matrix': tfidf_matrix
}, 'recommender_model.pkl')

print("✅ Recommender model saved!")

# test
def recommend(game_name, top_n=3):
    if game_name not in names:
        return []
    idx = names.index(game_name)
    scores = cosine_similarity(tfidf_matrix[idx], tfidf_matrix).flatten()
    scores[idx] = 0  # exclude itself
    top_indices = np.argsort(scores)[::-1][:top_n]
    return [(names[i], round(float(scores[i]), 2)) for i in top_indices]

# test with first game
print(f"\nGames similar to '{names[0]}':")
for name, score in recommend(names[0]):
    print(f"  → {name} (score: {score})")