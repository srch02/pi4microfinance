# Pi4MicroFinance - API Santé et Microfinance

## Démarrage Rapide

### 1. Configuration Base de Données
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pidb1
spring.datasource.username=root
spring.datasource.password=
```

### 2. Démarrer le Serveur
```bash
.\mvnw.cmd spring-boot:run
```

### 3. Accéder à l'API
- Swagger UI: http://localhost:8083/pidb/swagger-ui.html
- API: http://localhost:8083/pidb/api/

## API Principales

### Medication Analysis
- **POST** `/api/medication-analysis/analyze/{memberId}` - Analyser une image de médicament
- **GET** `/api/medication-analysis/history/{memberId}` - Historique des analyses
- **GET** `/api/medication-analysis/{analysisId}` - Détails d'une analyse
- **DELETE** `/api/medication-analysis/{analysisId}` - Supprimer une analyse

### Test Simple (Sans dépendances externes)
- **GET** `/api/medication-analysis-test/health` - Vérifier la santé de l'API
- **POST** `/api/medication-analysis-test/test-simple` - Test simple

### Members
- **GET/POST/PUT/DELETE** `/api/members/` - Gestion des membres

### Doctors
- **GET/POST/PUT/DELETE** `/api/doctors/` - Gestion des docteurs

### Consultations
- **GET/POST/PUT/DELETE** `/api/consultations/` - Gestion des consultations

### Products
- **GET/POST/PUT/DELETE** `/api/products/` - Gestion des produits

## Technologies Utilisées

- **Framework**: Spring Boot 4.0.2
- **Base de Données**: MySQL
- **ORM**: Hibernate/JPA
- **API Documentation**: Swagger/OpenAPI
- **Sécurité**: Spring Security
- **Email**: EmailJS
- **Analyse d'Images**: Llama AI (avec fallback)

## Structure du Projet

```
src/main/
├── java/pi/db/piversionbd/
│   ├── PiversionbdApplication.java
│   ├── Controllers/          # Endpoints REST
│   ├── Services/             # Logique métier
│   ├── Repositories/         # Accès données
│   ├── Entities/             # Modèles de données
│   │   ├── groups/           # Entités de groupe
│   │   ├── health/           # Entités santé
│   │   ├── score/            # Entités score
│   │   ├── pre/              # Entités préliminaires
│   │   └── admin/            # Entités admin
│   └── Config/               # Configuration
└── resources/
    └── application.properties # Configuration
```

## Compilation et Déploiement

### Compiler
```bash
.\mvnw.cmd clean compile
```

### Tester
```bash
.\mvnw.cmd test
```

### Créer un JAR
```bash
.\mvnw.cmd clean package
```

## Fonctionnalités Principales

### 1. Analyse de Médicaments
- Upload d'images de médicaments
- Reconnaissance automatique avec Llama AI
- Extraction d'informations complètes
- Fallback automatique en cas d'erreur

### 2. Gestion de Membres
- Enregistrement de patients
- Historique de santé
- Suivi des consultations

### 3. Gestion de Docteurs
- Répertoire de docteurs
- Spécialités
- Partenariats

### 4. Consultations
- Réservation en ligne
- Consultations à distance (Google Meet)
- Consultations sur place (Google Maps)
- Paiement

### 5. Produits Pharmaceutiques
- Catalogue de médicaments
- Recommandations
- Gestion des allergies

## Configuration EmailJS

Ajouter à `application.properties`:
```properties
emailjs.service.id=YOUR_SERVICE_ID
emailjs.template.id=YOUR_TEMPLATE_ID
emailjs.public.key=YOUR_PUBLIC_KEY
emailjs.private.key=YOUR_PRIVATE_KEY
```

## Notes de Développement

- Les images sont stockées dans: `uploads/medications/`
- MySQL doit être en cours d'exécution
- Hibernate crée automatiquement les tables (DDL auto)
- CORS est activé pour tous les domaines

## Support et Bugs

Pour signaler un bug ou demander une feature, consultez les logs de la console ou vérifiez la réponse d'erreur de l'API.

---

**Version**: 2.0.0  
**Date**: 31 Mars 2026
**Status**: Production Ready ✅

