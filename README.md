# Enterprise Multi-Bank Fraud Detection Risk Monitoring Platform

Enterprise Multi-Bank Fraud Detection and Risk Monitoring Platform is a full-stack fraud analytics system for banks and financial institutions. It combines a Spring Boot backend, React dashboard, PostgreSQL database, and FastAPI machine learning service to analyze external banking transactions in real time.

## Architecture

- Frontend: React, TypeScript, Vite, Tailwind CSS
- Backend: Spring Boot, Spring Security, JWT, PostgreSQL, JPA
- ML service: FastAPI, XGBoost, Scikit-learn, Pandas, NumPy, Joblib, SHAP
- Database: PostgreSQL
- Integration style: frontend -> backend -> ML service -> backend rule engine -> database

## Core Features

- Multi-bank SaaS structure with banks, branches, users, customers, accounts, and beneficiaries
- Role-based access control for Platform Admin, Super Admin, Bank Admin, Branch Manager, Fraud Analyst, Risk Officer, and Auditor
- Secure authentication with JWT access tokens, refresh tokens, MFA/TOTP, account lockout, password history, and forced password changes
- 24-hour temporary password lifecycle for admin-created users
- Audit logs for important security, user, transaction, and fraud investigation actions
- External transaction ingest API for simulating integration with core banking, mobile banking, UPI, ATM, and corporate systems
- Hybrid fraud detection using ML probability plus Spring Boot business rule engine
- Fraud alerts, fraud cases, investigation status, assignment, notes, and risk scoring
- Customer, account, beneficiary trust, notification, and security incident modules
- Daily summary and critical alert email support when SMTP is configured
- Live dashboard polling and ML runtime logs

## Fraud Decision Flow

1. External banking system submits a transaction to Spring Boot.
2. Backend validates bank, branch, account, and transaction context.
3. Backend sends PaySim-compatible transaction features to the FastAPI ML service.
4. ML service returns fraud probability and ML risk score only.
5. Spring Boot applies business rules for account type, beneficiary trust, device, location, channel, time, and behavior.
6. Backend calculates final risk score.
7. Final decision is APPROVE, REVIEW, or BLOCK.
8. Transaction, alerts, fraud cases, notifications, and audit records are saved in PostgreSQL.
9. React dashboard shows live risk monitoring data.

## Run Locally

Start PostgreSQL first and make sure the `fraud_detection` database exists.

ML service:

```powershell
cd ml-service
.\venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --log-level info
```

Backend:

```powershell
cd backend\fraud-detection
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend\bank
npm run dev -- --host 0.0.0.0
```

## QA

Backend:

```powershell
cd backend\fraud-detection
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend\bank
npm run lint
npm run build
```

ML validation:

```powershell
cd ml-service
.\venv\Scripts\python.exe training\validate_model_behavior.py
```
