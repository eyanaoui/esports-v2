from flask import Flask, request, jsonify
import joblib
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

app = Flask(__name__)

# load models
sentiment_model  = joblib.load('sentiment_model.pkl')
difficulty_model = joblib.load('difficulty_model.pkl')
recommender      = joblib.load('recommender_model.pkl')

@app.route('/predict/sentiment', methods=['POST'])
def predict_sentiment():
    data = request.get_json()
    comment = data.get('comment', '')
    if not comment:
        return jsonify({'error': 'No comment provided'}), 400
    prediction = sentiment_model.predict([comment])[0]
    confidence = max(sentiment_model.predict_proba([comment])[0])
    return jsonify({
        'comment':    comment,
        'sentiment':  prediction,
        'confidence': round(float(confidence), 2)
    })

@app.route('/predict/difficulty', methods=['POST'])
def predict_difficulty():
    data = request.get_json()
    description = data.get('description', '')
    if not description:
        return jsonify({'error': 'No description provided'}), 400
    prediction = difficulty_model.predict([description])[0]
    confidence = max(difficulty_model.predict_proba([description])[0])
    return jsonify({
        'difficulty': prediction,
        'confidence': round(float(confidence), 2)
    })

@app.route('/predict/recommend', methods=['POST'])
def recommend_games():
    data = request.get_json()
    game_name = data.get('game_name', '')
    if not game_name:
        return jsonify({'error': 'No game name provided'}), 400

    names        = recommender['names']
    tfidf_matrix = recommender['tfidf_matrix']
    ids          = recommender['ids']

    if game_name not in names:
        return jsonify({'recommendations': []})

    idx    = names.index(game_name)
    scores = cosine_similarity(tfidf_matrix[idx], tfidf_matrix).flatten()
    scores[idx] = 0
    top_indices  = np.argsort(scores)[::-1][:3]

    recommendations = [
        {
            'id':    ids[i],
            'name':  names[i],
            'score': round(float(scores[i]), 2)
        }
        for i in top_indices if scores[i] > 0
    ]

    return jsonify({'recommendations': recommendations})

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok'})

if __name__ == '__main__':
    app.run(port=5000, debug=True)