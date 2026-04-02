
# 🌟 SolidarHealth – Full-Stack Micro-Insurance Platform

**Making healthcare affordable, accessible, and equitable for every Tunisian.**

---

## Overview

**SolidarHealth** is a full-stack digital micro-insurance platform designed to make healthcare accessible to everyone. It leverages AI-driven pricing, community risk pooling, automated claims, and advanced security to provide a seamless and secure user experience.

**Key Capabilities:**

* **AI-driven dynamic pricing:** Personalized monthly premiums using machine learning risk scoring.
* **Community risk pooling:** Groups of 5–30 members share healthcare costs collectively.
* **Claims automation:** OCR document processing with fast reimbursements (<24h).
* **Advanced security:** JWT, reCAPTCHA, facial authentication, Hedera blockchain integration.
* **Notifications:** Email verification, password resets, and claim tracking.

---

## Features

### Backend Core Features

| Feature               | Description                                                  |
| --------------------- | ------------------------------------------------------------ |
| User Registration     | CIN verification, medical profile setup, duplicate detection |
| Smart Pricing Engine  | ML-based risk scoring → personalized premiums                |
| Group Management      | Create/manage solidarity groups (5–30 members)               |
| Payment & Pooling     | Automated billing & fund distribution                        |
| Claims Automation     | OCR uploads, AI scoring, auto-approval                       |
| Fraud Detection       | Anomaly detection & automated blacklisting                   |
| reCAPTCHA Protection  | Anti-bot verification on registration and login              |
| JWT Security          | Token-based authentication for API endpoints                 |
| Email Notifications   | Registration confirmation, password reset, claim updates     |
| Facial Authentication | Optional secure face recognition login                       |

### Frontend Features (Angular)

| Feature                | Description                                                     |
| ---------------------- | --------------------------------------------------------------- |
| SPA with Angular       | Reactive UI built with TypeScript & RxJS                        |
| Angular Material UI    | Modern UI components and layouts                                |
| Registration & Login   | Includes reCAPTCHA, email verification, and face authentication |
| Group Management       | View and join solidarity groups                                 |
| Claims Submission      | Upload documents, track status, OCR processing                  |
| Telemedicine           | Video consultations & intelligent doctor matching               |
| Health Challenges      | Preventive programs with rewards                                |
| Analytics Dashboard    | Admin metrics: performance, retention, churn                    |
| Multi-language Support | Arabic & French                                                 |

---

## Tech Stack

### Backend

* **Java 17 + Spring Boot 3** — REST APIs, JPA, Security
* **MySQL 8** — Relational database
* **Redis** — Caching & session management
* **Python 3.10+** — AI / ML for risk scoring, claims scoring, fraud detection
* **Keycloak** — Authentication & SSO
* **Hedera Hashgraph** — Blockchain-based wallet & security
* **Stripe / PayPal** — Payments
* **Twilio** — SMS notifications
* **Tesseract OCR** — Document processing
* **JWT + reCAPTCHA** — Security & anti-bot protection
* **Email Service** — Notifications for onboarding & claims
* **Facial Recognition** — Optional secure login

### Frontend

* **Angular SPA** — TypeScript + RxJS
* **Angular Material** — UI components
* **Integration** — REST API calls to Spring Boot backend
* **Security** — reCAPTCHA, JWT token storage, face authentication

---

## Backend Architecture

**Microservices & 3-tier design**

```
API Gateway (NGINX + JWT)
│
├── Keycloak Auth Service        # User management & SSO
├── User Service                 # Profile management, CIN verification
├── Insurance Service
│      ├─ Risk Engine           # AI scoring for premiums
│      ├─ Claims Processor      # OCR & auto-approval
│      └─ Policy Manager        # Insurance contract management
├── Payment Service
│      ├─ Billing               # Premium billing
│      └─ Reimbursements        # Automated reimbursements
├── Mailing & Notifications Service
│      └─ Emails                # Registration, password reset, claim tracking
├── Facial Authentication Service
│      └─ Secure face login
├── Security Layer
│      ├─ JWT Authentication
│      └─ reCAPTCHA Verification
└── Data Layer
       ├─ MySQL                # Persistent storage
       └─ Redis                # Cache & session
```

### Backend Modules

| Module | Responsibility                                        |
| ------ | ----------------------------------------------------- |
| 1      | Groups & Payments                                     |
| 2      | Claims & Risk Scoring                                 |
| 3      | Telemedicine & Health Programs                        |
| 4      | Analytics & Admin                                     |
| 5      | Pre-Registration, Identity Verification & Facial Auth |
| 6      | Security (JWT + reCAPTCHA)                            |
| 7      | Mailing & Notifications                               |

---

## Contributors

| Name            | GitHub          |
| --------------- | --------------- |
| Sabbagh Yasmine | @Yasminesabbagh |
| Ketata Eya      | —               |
| Chamekh Sarra   | —               |
| Arfaoui Fares   | —               |
| Stiti Nader     | —               |

---

## Academic Context

**Esprit School of Engineering – Tunisia**
**PIDEV – 4INFINI3 | 2025–2026**

Supports UN SDGs:

* 🟢 SDG 3 — Good Health & Well-Being
* 🟡 SDG 8 — Decent Work & Economic Growth
* 🔵 SDG 9 — Industry, Innovation & Infrastructure
* 🔴 SDG 10 — Reduced Inequalities

---

## Getting Started

### Prerequisites

| Tool        | Version |
| ----------- | ------- |
| Java        | 17+     |
| Maven       | 3.8+    |
| Node.js     | 18+     |
| Angular CLI | Latest  |
| MySQL       | 8+      |
| Python      | 3.10+   |

### Run Backend

```bash
git clone https://github.com/Yasminesabbagh/Esprit-PIDEV-4INFINI3-2026-SolidarHealth.git
cd Esprit-PIDEV-4INFINI3-2026-SolidarHealth

cp .env.example .env
# Update DB credentials & JWT secret

./mvnw spring-boot:run
```

* **API:** [http://localhost:8080](http://localhost:8080)
* **Swagger:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Run Frontend

```bash
cd frontend
npm install
ng serve
```

* **Frontend:** [http://localhost:4200](http://localhost:4200)

### Run AI Engine

```bash
cd ai-engine
pip install -r requirements.txt
python scripts/risk_scoring.py
```

---

## Acknowledgment

Special thanks to **Esprit School of Engineering** and our professors for guidance and mentorship throughout this full-stack project.

---
