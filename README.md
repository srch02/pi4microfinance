🌟 SolidarHealth – Full-Stack Micro-Insurance Platform
Making healthcare affordable, accessible, and equitable for every Tunisian.

Overview
SolidarHealthis a full-stack digital micro-insurance platform that makes healthcare accessible to everyone.

It combines:

AI-driven dynamic pricing: Personalized monthly premiums based on machine learning risk scoring. Community risk pooling: Groups of 5–30 members sharing healthcare costs collectively. Claims automation: OCR processing with fast reimbursements (<24 hours). Advanced security: JWT, reCAPTCHA, facial authentication, Hedera blockchain. Email & notifications: Verification, password reset, and claim tracking.

Features
Backend Core Features
Feature	Description
User Registration	CIN verification, medical profile setup, duplicate detection
Smart Pricing Engine	ML-based risk scoring → personalized premiums
Group Management	Create/manage solidarity groups (5–30 members)
Payment & Pooling	Automated billing & fund distribution
Claims Automation	OCR uploads, AI scoring, auto-approval
Fraud Detection	Anomaly detection & automated blacklisting
reCAPTCHA Protection	Anti-bot verification on registration and login
JWT Security	Protect API endpoints with token-based authentication
Email Notifications	Registration confirmation, password reset, claim updates
Facial Authentication	Optional face recognition login
Frontend Features (Angular)
Feature	Description
SPA with Angular	Reactive UI with TypeScript & RxJS
Angular Material UI	Modern components and layouts
Registration & Login	With reCAPTCHA, email verification, and face authentication
Group Management	View and join solidarity groups
Claims Submission	Upload documents, track status, OCR processing
Telemedicine	Video consultations & intelligent doctor matching
Health Challenges	Preventive programs with rewards
Analytics Dashboard	Admin metrics: performance, retention, churn
Multi-language Support	Arabic & French
Tech Stack
Backend
Java 17 + Spring Boot 3 (REST, JPA, Security)
MySQL 8 — Relational database
Redis — Caching & session management
Python 3.10+ — AI / ML for risk scoring, claims scoring, fraud detection
Keycloak — Authentication & SSO
Hedera Hashgraph — Blockchain-based wallet & security
Stripe / PayPal — Payments
Twilio — SMS notifications
Tesseract OCR — Document processing
JWT + reCAPTCHA — Security & anti-bot protection
Mailing — Email notifications for onboarding and claims
Facial Recognition — Optional secure login
Frontend
Angular SPA — TypeScript + RxJS
Angular Material — UI components
Integration — Calls backend REST APIs (Spring Boot)
Security — reCAPTCHA, JWT token storage, Face Authentication
Backend Architecture
Microservices & 3-tier design (Backend Only)

API Gateway (NGINX + JWT)
│
├── Keycloak Auth Service
│      └─ Gestion des utilisateurs et SSO
│
├── User Service
│      └─ Gestion des profils, inscription, vérification CIN
│
├── Insurance Service
│      ├─ Risk Engine          → Scoring IA pour primes
│      ├─ Claims Processor    → Traitement OCR, approbation automatique
│      └─ Policy Manager      → Gestion des contrats d’assurance
│
├── Payment Service
│      ├─ Billing             → Facturation des primes
│      └─ Reimbursements      → Remboursements automatisés
│
├── Mailing & Notifications Service
│      └─ Emails (confirmation, reset password, suivi réclamation)
│
├── Facial Authentication Service
│      └─ Authentification faciale sécurisée
│
├── Security Layer
│      ├─ JWT Authentication   → Protection des endpoints REST
│      └─ reCAPTCHA Verification → Anti-bot
│
└── Data Layer
       ├─ MySQL   → Stockage persistant
       └─ Redis   → Cache et sessions
Backend Modules

Module	Responsibility
1	Groups & Payments
2	Claims & Risk Scoring
3	Telemedicine & Health Programs
4	Analytics & Admin
5	Pre-Registration, Identity Verification & Facial Auth
6	Security (JWT + reCAPTCHA)
7	Mailing & Notifications
Contributors
Name	GitHub
Sabbagh Yasmine	@Yasminesabbagh
Ketata Eya	—
Chamekh Sarra	—
Arfaoui Fares	—
Stiti Nader	—
Academic Context
Esprit School of Engineering – Tunisia
PIDEV – 4INFINI3 | 2025–2026
Supports UN SDGs:

🟢 SDG 3 — Good Health & Well-Being
🟡 SDG 8 — Decent Work & Economic Growth
🔵 SDG 9 — Industry, Innovation & Infrastructure
🔴 SDG 10 — Reduced Inequalities
Getting Started
Prerequisites
Tool	Version
Java	17+
Maven	3.8+
Node.js	18+
Angular CLI	Latest
MySQL	8+
Python	3.10+
Run Backend
git clone https://github.com/Yasminesabbagh/Esprit-PIDEV-4INFINI3-2026-SolidarHealth.git
cd Esprit-PIDEV-4INFINI3-2026-SolidarHealth

cp .env.example .env
# Update DB credentials & JWT secret

./mvnw spring-boot:run
API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui.html
Run Frontend
cd frontend
npm install
ng serve
Frontend: http://localhost:4200
Run AI Engine
cd ai-engine
pip install -r requirements.txt
python scripts/risk_scoring.py
Acknowledgment
Special thanks to Esprit School of Engineering and our professors for guidance and mentorship throughout this full-stack project.
