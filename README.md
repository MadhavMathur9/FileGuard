# FileGuard - Self-Hosted File Monitoring System

FileGuard is a powerful, self-hosted file monitoring system designed to keep an active watch over your critical directories. It automatically hashes file contents in real-time, builds a rich PostgreSQL index of your filesystem, and exposes all this data through a beautiful, glassmorphism-styled React dashboard.

Whether you want to hunt down pesky duplicate files eating up your storage, enforce automated folder policies (notify, quarantine, hardlink, delete), or get real-time alerts when important files are tampered with, FileGuard acts as your personal filesystem sentry.

## Architecture
FileGuard handles high file throughput seamlessly through a specialized hashing pipeline:
1. **Watcher**: Monitors folders recursively for `WatchEvents`.
2. **Debounce + Stability**: Groups concurrent file events together and ensures files are fully written (no modifications in 2 seconds) before hashing.
3. **Hash Worker Pool**: Hashes files in constant memory via streaming (no OOMs for 10 GB files). Backpressure on the worker queue protects overall memory.
4. **Analyzer & Policy**: Detects duplicates or file changes (tampering). Applies configured policies (NOTIFY, DELETE, QUARANTINE, HARDLINK) to duplicates automatically.
5. **REST API & SSE**: Streams real-time events (`duplicate.found`, `action.applied`, `alert.raised`) over Server-Sent Events straight to the React dashboard.

## Key Features
- **Duplicate Detection**: Finds duplicates by content (SHA-256), ignoring names. Efficient 3-tier indexing (Size -> Partial Hash -> Full Hash).
- **Quarantine and Restore**: Duplicates can be automatically placed into Quarantine and easily restored via a single click on the dashboard.
- **Integrity Baseline and Alerts**: Takes a snapshot of a folder. Any untracked modifications, additions, or deletions trigger an Integrity Alert in real time.
- **Startup Reconciliation**: Re-synchronizes offline filesystem modifications with the Postgres index on startup.
- **Modern Dashboard**: Visually stunning dashboard in React + Vite + TypeScript.

## Quick Start (Local Setup)

### 1. Requirements
- **Java 21**
- **Maven**
- **Node.js (18+)**
- **PostgreSQL (15+)**
- **Docker & Docker Compose** (Optional for backing services)

### 2. Setup
Spin up the PostgreSQL database:
```bash
docker-compose up -d
```

Start the Backend (Port 8080):
```bash
cd backend
mvn spring-boot:run
```

Start the Frontend (Port 5173):
```bash
cd frontend
npm install
npm run dev
```

### 3. Usage
- Go to `http://localhost:5173`.
- Go to the **Folders** page and add a directory to monitor.
- Files will be actively digested and monitored for Duplicates or Integrity Changes!

## Deployment (Railway / Cloud)

FileGuard is built to be easily deployed on cloud platforms like [Railway](https://railway.app/). 

Because the code is already configured to accept cloud environment variables, deployment is straightforward:
1. **Provision a Database**: Add a PostgreSQL database plugin to your Railway project.
2. **Deploy Backend**: Link this repository and deploy the `backend` folder. Railway will automatically inject the `DATABASE_URL` or standard PostgreSQL variables which the app will pick up.
3. **Deploy Frontend**: Deploy the `frontend` folder. Depending on your setup, you can either serve it statically or configure it to point its API calls to your deployed backend URL.

## Technologies
- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, HikariCP, PostgreSQL
- **Frontend:** React 18, Vite, TypeScript, React Router, TanStack Query, Vanilla CSS (Glassmorphism + Dark Mode)
