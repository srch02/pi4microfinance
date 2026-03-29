# Service ML - Solidari-Health / pi4microfinance

API Flask pour l'évaluation du risque médical et la détection des conditions exclues.

## Setup

```bash
cd ml-service
pip install -r requirements.txt
```

## Entraînement du modèle

1. **Option A** : Téléchargez un dataset Kaggle (ex: [Disease Prediction](https://www.kaggle.com/datasets/kaushil268/disease-prediction-using-machine-learning))
2. **Option B** : Lancer sans dataset (génère des données synthétiques)

```bash
python train_model.py
# ou avec dataset
python train_model.py datasets/disease_prediction.csv
```

## Démarrer l'API

```bash
python flask_api.py
```

Écoute sur `http://localhost:5000`

## Endpoints

- `POST /predict` – Prédiction du risque (appelé par Spring Boot)
- `GET /health` – Santé de l'API

## Intégration Spring Boot

Dans `application.properties`:
```properties
ml.api.enabled=true
ml.api.url=http://localhost:5000
```
