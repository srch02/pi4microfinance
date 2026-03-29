"""
Entraînement du modèle ML pour détection des conditions exclues / coefficient de risque.
À exécuter après téléchargement des datasets Kaggle:
  - Disease Prediction: kaggle.com/datasets/kaushil268/disease-prediction-using-machine-learning
  - Medical Insurance: kaggle.com/datasets/tejashvi14/medical-insurance-premium-prediction

Usage: python train_model.py [dataset.csv]
Si aucun fichier: génère un modèle de démonstration sur données synthétiques.
"""
import sys
import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import joblib
import os

def create_synthetic_dataset(n=500):
    """Données synthétiques si pas de dataset Kaggle."""
    np.random.seed(42)
    age = np.random.randint(18, 70, n)
    flu_freq = np.random.randint(0, 12, n)
    has_allergies = np.random.choice([0, 1], n, p=[0.7, 0.3])
    profession_risk = np.random.choice([0, 1], n, p=[0.8, 0.2])
    family_history = np.random.choice([0, 1], n, p=[0.6, 0.4])
    # Règles simplifiées pour is_excluded
    is_excluded = (
        (age > 65) | (flu_freq >= 6) | ((has_allergies == 1) & (profession_risk == 1))
    ).astype(int)
    return pd.DataFrame({
        'age': age, 'flu_frequency': flu_freq,
        'has_allergies': has_allergies, 'profession_risk': profession_risk,
        'family_history': family_history, 'is_excluded': is_excluded
    })

def main():
    dataset_path = sys.argv[1] if len(sys.argv) > 1 else None

    if dataset_path and os.path.exists(dataset_path):
        df = pd.read_csv(dataset_path)
        # Adapter les noms de colonnes selon le dataset Kaggle
        col_map = {
            'Age': 'age', 'age': 'age',
            'flu_frequency': 'flu_frequency', 'FluFrequency': 'flu_frequency',
            'has_allergies': 'has_allergies', 'Allergies': 'has_allergies',
            'profession_risk': 'profession_risk', 'ProfessionRisk': 'profession_risk',
            'family_history': 'family_history', 'FamilyHistory': 'family_history',
            'is_excluded': 'is_excluded', 'Excluded': 'is_excluded', 'Disease': 'is_excluded'
        }
        df = df.rename(columns={k: v for k, v in col_map.items() if k in df.columns})
        # Créer is_excluded si absent (ex: from Disease Prediction dataset)
        if 'is_excluded' not in df.columns and 'Disease' in df.columns:
            excluded_diseases = ['Diabetes', 'Cancer', 'Heart disease', 'Kidney disease']
            df['is_excluded'] = df['Disease'].isin(excluded_diseases).astype(int)
        features = [c for c in ['age', 'flu_frequency', 'has_allergies', 'profession_risk', 'family_history'] if c in df.columns]
        if not features:
            features = ['age']  # fallback
        target = 'is_excluded' if 'is_excluded' in df.columns else df.columns[-1]
    else:
        df = create_synthetic_dataset()
        features = ['age', 'flu_frequency', 'has_allergies', 'profession_risk', 'family_history']
        target = 'is_excluded'

    X = df[features].fillna(0)
    y = df[target]

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X_train, y_train)

    acc = model.score(X_test, y_test)
    print(f"Accuracy: {acc:.2%}")

    model_path = os.path.join(os.path.dirname(__file__), 'solidari_health_model.pkl')
    joblib.dump(model, model_path)
    print(f"Modèle sauvegardé: {model_path}")

if __name__ == '__main__':
    main()
