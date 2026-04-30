# DWP Debt Collection Management System (DCMS)

Greenfield debt collection management platform for the UK Department for Work and Pensions.

## Prerequisites

- Docker Desktop 4.x
- Docker Compose v2.x
- Git
- Java 21 (for local backend development without Docker)
- Node.js 20 (for local frontend development without Docker)

## Getting Started

1. Clone the repository
   ```bash
   git clone https://github.com/nctct/DWP-system-prototype.git
   cd DWP-system-prototype
   ```

2. Copy `.env.example` to `.env`
   ```bash
   cp .env.example .env
   ```

3. Fill in any required values in `.env` (defaults work for local development)

4. Start the full stack
   ```bash
   docker compose -f infrastructure/docker-compose.yml up
   ```

## Accessing the Application

| Service    | URL                                        |
|------------|--------------------------------------------|
| Frontend   | http://localhost:8080                      |
| API        | http://localhost:8081/api                  |
| Keycloak   | http://localhost:9090                      |
| Health (live)  | http://localhost:8081/health/live      |
| Health (ready) | http://localhost:8081/health/ready     |

## Default Credentials

| Account         | Username      | Password  | Role          |
|-----------------|---------------|-----------|---------------|
| Admin user      | admin         | admin123  | DCMS_ADMIN    |
| Case worker     | caseworker1   | test123   | CASE_WORKER   |
| Keycloak admin  | admin         | admin     | (Keycloak UI) |

## Stopping the Application

```bash
docker compose -f infrastructure/docker-compose.yml down
```

## Resetting the Database

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```

## Development

### Local Git hooks (recommended)
```bash
powershell -File scripts/install-git-hooks.ps1
```

This installs:
- `pre-commit` AGENTS sync when `.claude/agents` or `.claude/skills` are staged
- Codex context checkpoint hooks that log to `.codex/session-YYYY-MM-DD.jsonl`

Optional environment knobs:
- `CODEX_COMPACT_FILE_THRESHOLD` (default `25`)
- `CODEX_COMPACT_LINE_THRESHOLD` (default `800`)

### Backend only (without Docker)
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend only (without Docker)
```bash
cd frontend
npm install
npm run dev
```

### Running tests
```bash
# Backend
cd backend && ./mvnw test

# Frontend
cd frontend && npm test
```

### Building Docker images locally
```bash
docker build -t ufstpit-dev-docker.repo.netcompany.com/dcms_v0/dcms_v0-api:$(git rev-parse --short HEAD) ./backend
docker build -t ufstpit-dev-docker.repo.netcompany.com/dcms_v0/dcms_v0-frontend:$(git rev-parse --short HEAD) ./frontend
```

## Architecture

- **Backend**: Java 21 + Spring Boot 3.4.x monolith (Maven)
- **Frontend**: React 18 + TypeScript + GOV.UK Design System (Vite)
- **Database**: PostgreSQL 16 (Flyway migrations run on startup)
- **Auth**: Keycloak 24 (OAuth2/OIDC)
- **Local cluster**: k3d (see `docs/runbooks/local-setup.md`)
- **Dev cluster**: AKS on Azure

## Project Structure

```
DWP-system-prototype/
├── backend/          Spring Boot monolith
├── frontend/         React + TypeScript frontend
├── infrastructure/   Docker Compose, Keycloak config, Helm chart
├── docs/             Architecture decisions, runbooks
└── .github/          GitHub Actions workflows
```
