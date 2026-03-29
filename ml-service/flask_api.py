"""
API Flask pour l'évaluation du risque médical.
Appelée par Spring Boot (RiskAssessmentMLService).

Démarrage: python flask_api.py
Écoute sur http://localhost:5000
"""
from flask import Flask, request, jsonify
import joblib
import numpy as np
import os

app = Flask(__name__)

MODEL_PATH = os.path.join(os.path.dirname(__file__), 'solidari_health_model.pkl')
model = None
if os.path.exists(MODEL_PATH):
    model = joblib.load(MODEL_PATH)

# Règles strictes de rejet (priorité absolue sur le modèle)
EXCLUDED_KEYWORDS = [
    'diabete', 'diabetes', 'diabetic', 'cancer', 'insuffisance renale',
    'cirrhose', 'infarctus', 'lupus', 'sclerose en plaques', 'sclérose',
    'dialyse', 'dialysis', 'chemotherapy', 'chimiotherapie', 'insuline', 'insulin'
]

def normalize(s):
    if s is None:
        return ""
    return str(s).lower().strip()

def calculate_risk_coefficient(data):
    """Formule de coefficient de risque (alignée avec PreRegistrationServiceImpl)."""
    coeff = 1.0
    age = int(data.get('age', 0) or 0)
    flu_freq = int(data.get('flu_frequency', 0) or 0)
    has_allergies = bool(data.get('has_allergies'))
    profession_risk = int(data.get('profession_risk', 0) or 0)
    family_history = bool(data.get('family_history'))

    if age > 50:
        coeff += 0.15
    if flu_freq >= 3:
        coeff += 0.70
    elif flu_freq > 0:
        coeff += 0.20
    if has_allergies:
        coeff += 0.20
    if profession_risk == 1:
        coeff += 0.30
    if family_history:
        coeff += 0.10

    return round(min(coeff, 3.0), 2)

@app.route('/predict', methods=['POST'])
def predict():
    data = request.get_json() or {}
    ocr_text = normalize(data.get('ocr_text', ''))

    # 1. Vérification prioritaire par mots-clés (OCR)
    for keyword in EXCLUDED_KEYWORDS:
        if keyword in ocr_text:
            return jsonify({
                'is_excluded': True,
                'reason': f'Condition exclue détectée: {keyword}',
                'risk_coefficient': None,
                'confidence': 0.99
            })

    # 2. Prédiction ML pour is_excluded (si modèle disponible)
    features = [
        int(data.get('age', 0) or 0),
        int(data.get('flu_frequency', 0) or 0),
        1 if data.get('has_allergies') else 0,
        int(data.get('profession_risk', 0) or 0),
        1 if data.get('family_history') else 0
    ]

    if model is not None:
        X = np.array([features])
        prediction = model.predict(X)[0]
        proba = model.predict_proba(X)[0]
        is_excluded = bool(prediction)
        confidence = float(max(proba))
    else:
        # Fallback sans modèle: utiliser la formule de risque uniquement
        is_excluded = False
        confidence = 0.7

    risk_coeff = calculate_risk_coefficient(data)

    return jsonify({
        'is_excluded': is_excluded,
        'reason': None,
        'risk_coefficient': risk_coeff,
        'confidence': confidence
    })

@app.route('/health')
def health():
    return jsonify({'status': 'ok', 'model_loaded': model is not None})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
