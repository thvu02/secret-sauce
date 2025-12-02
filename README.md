# SplittyDupe

Receipt-splitting application with OCR parsing, user authentication, and payment tracking. Built with Spring Boot (Java 17) and React + TypeScript.

## Quick Start

### Local Development

**Prerequisites:**
- Java 17
- Node.js 20+
- gcloud CLI
- Google Cloud credentials

**Backend:**
```bash
# Authenticate with GCP
gcloud auth application-default login

./mvnw clean install          # Build project
./mvnw spring-boot:run        # Run locally
./mvnw test                   # Run tests
# Backend runs on http://localhost:8080
```

**Frontend:**
```bash
cd react-app
npm install
npm run dev
# Frontend runs on http://localhost:5173
```

### Production Deployment

**Full deployment to GCP:**
```bash
./deploy.sh
```

## Project Structure

```
├── src/main/java/org/splittydupe/startup/
│   ├── controller/          # REST API endpoints
│   ├── service/             # Business logic layer
│   ├── repository/          # Data access layer
│   ├── model/               # Domain entities
│   ├── dto/                 # API request/response objects
│   └── config/              # Spring configuration
├── react-app/
│   ├── src/
│   │   ├── components/      # Reusable UI components
│   │   ├── pages/           # Page-level components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── services/        # API service layer
│   │   ├── types/           # TypeScript interfaces
│   │   └── utils/           # Utility functions
│   ├── Dockerfile           # Frontend containerization
│   └── nginx.conf           # Nginx configuration
├── Dockerfile               # Backend containerization
├── cloudbuild.yaml          # CI/CD pipeline
├── deploy.sh                # Deployment script
```

## Architecture

### Tech Stack

**Backend:**
- Spring Boot 3.1.5
- Java 17
- Google Cloud Firestore
- Google Cloud Document AI
- Spring Security + JWT
- Spring Mail (SMTP)

**Frontend:**
- React 19
- TypeScript 5.9
- Vite 7
- Tailwind CSS 4
- React Router 7

### Cloud Services

- **Cloud Run** - Containerized backend and frontend
- **Firestore** - NoSQL database for receipts, users, tokens
- **Document AI** - OCR receipt parsing
- **Secret Manager** - JWT secret, email credentials
- **Container Registry** - Docker images
- **Cloud Build** - CI/CD pipeline