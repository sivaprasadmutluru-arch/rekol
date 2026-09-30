# Rekol

Rekol is an Android-first personal memory assistant. This repository contains a native Jetpack Compose application, a Spring Boot API, and a React/Vite admin portal.

## Included experience

- Branded splash screen using the supplied Rekol artwork
- Home dashboard and quick capture actions
- Android Sharesheet receiver for text, images and PDFs
- Capture preview and simulated AI analysis flow
- Today journal, Library and Insights screens
- Spring Boot authentication, OTP, memory, and admin endpoints
- React/Vite admin portal with live API metrics and responsive mobile layout
- Local PostgreSQL and MinIO configuration

## Android setup

1. Copy your Firebase file to `app/google-services.json` (it is intentionally excluded from git). Use `app/google-services.json.example` as a shape reference.
2. Open this folder in Android Studio.
2. Allow Gradle sync to complete.
3. Run the `app` configuration on an Android 8 or newer emulator/device.

From PowerShell, build with:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is created under `app\build\outputs\apk\debug`.

## Backend setup

Install Docker Desktop, then run:

```powershell
docker compose up --build
```

- API documentation: http://localhost:8000/docs
- MinIO console: http://localhost:9001
- PostgreSQL: localhost:5432

The Spring Boot API uses PostgreSQL and can be run with `backend-java` or the root Docker Compose file.

```powershell
cd backend-java
mvn spring-boot:run
```

Set `DEV_OTP_BYPASS=true` only for local testing. Keep it false in production.

## Project structure

```text
app/                     Android application
  src/main/java/         Compose screens and navigation
  src/main/res/          Rekol artwork and Android resources
backend/                 FastAPI service
admin-dashboard/         React + Vite operations console
docker-compose.yml       Local infrastructure
```

## Next engineering milestone

Connect object uploads to MinIO and run OCR and enrichment through a worker as the next backend milestone.

## Admin dashboard

The `admin-dashboard` folder contains the Rekol operations console. It includes overview metrics, capture activity, memory source distribution, recent system events, service health, and responsive navigation for Users, AI Processing, Memories, Analytics, Privacy and Settings.

```powershell
cd admin-dashboard
npm install
npm run dev
```
