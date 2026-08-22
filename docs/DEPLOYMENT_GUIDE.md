# OneStop Career Advisor — Production Deployment & Operations Guide

This guide provides engineering standards, configuration blueprints, and step-by-step instructions for deploying the OneStop Career Advisor platform to production environments.

---

## 1. System Deployment Architecture

```text
                                Internet (HTTPS:443)
                                         │
                                   Reverse Proxy
                              (Nginx / Cloudflare CDN)
                                         │
                   ┌─────────────────────┴─────────────────────┐
                   │                                           │
         Frontend Web Server                           Backend API Server
      Next.js 16 (Node.js 18+)                       Spring Boot 3.3.4 (Java 17)
         Port: 3000 (Internal)                         Port: 8080 (Internal)
                   │                                           │
                   └─────────────────────┬─────────────────────┘
                                         │
                                   Database Cluster
                                MySQL 8.0 (Port: 3306)
                              Persistent Storage Volume
```

---

## 2. Prerequisites & System Requirements

| Component | Minimum Version | Recommended Specs |
| :--- | :--- | :--- |
| **Java Runtime (JRE/JDK)** | OpenJDK 17 LTS | 2 vCPU, 2 GB RAM |
| **Node.js Runtime** | Node.js 18.x LTS or 20.x LTS | 1 vCPU, 1 GB RAM |
| **Relational Database** | MySQL 8.0+ / AWS RDS MySQL | 2 vCPU, 4 GB RAM, SSD |
| **Reverse Proxy** | Nginx 1.22+ or Traefik / Caddy | SSL Termination, HTTP/2 |

---

## 3. Environment Variables Specification

### 3.1 Backend Environment Variables

| Variable | Description | Production Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `PORT` | Backend HTTP port | `8080` |
| `DB_HOST` | MySQL hostname or endpoint | `mysql-db.internal.net` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | Database schema name | `career_advisor` |
| `DB_USERNAME` | Database user | `app_user` |
| `DB_PASSWORD` | Database password | *`<SECURE_DB_PASSWORD>`* |
| `JWT_SECRET` | 256-bit cryptographic key | *`<CRYPTOGRAPHIC_256_BIT_SECRET>`* |
| `JWT_EXPIRATION_MS` | Token validity duration | `86400000` (24 Hours) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `https://careeradvisor.dev,https://www.careeradvisor.dev` |
| `ADMIN_EMAIL` | Production Admin Email | `admin@careeradvisor.dev` |
| `ADMIN_PASSWORD` | Initial Admin Password (one-time) | *`<SECURE_ADMIN_PASS>`* |
| `MAX_FILE_SIZE` | Max resume upload file size | `5MB` |
| `MAX_REQUEST_SIZE` | Max multipart payload size | `10MB` |

### 3.2 Frontend Environment Variables

| Variable | Description | Production Example |
| :--- | :--- | :--- |
| `NEXT_PUBLIC_API_URL` | Public URL for backend REST API | `https://api.careeradvisor.dev` |
| `NODE_ENV` | Node environment | `production` |

---

## 4. Step-by-Step Production Deployment

### Step 1: Database Setup
1. Create database schema:
   ```sql
   CREATE DATABASE career_advisor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Create dedicated application user:
   ```sql
   CREATE USER 'app_user'@'%' IDENTIFIED BY 'STRONG_PASSWORD_HERE';
   GRANT ALL PRIVILEGES ON career_advisor.* TO 'app_user'@'%';
   FLUSH PRIVILEGES;
   ```

### Step 2: Build & Deploy Backend
1. Navigate to backend directory:
   ```bash
   cd backend
   ```
2. Package executable jar:
   ```bash
   ./mvnw clean package -DskipTests
   ```
3. Run with production environment variables:
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export DB_HOST=mysql-db.internal.net
   export DB_NAME=career_advisor
   export DB_USERNAME=app_user
   export DB_PASSWORD=STRONG_PASSWORD_HERE
   export JWT_SECRET=YOUR_SECURE_256_BIT_KEY_HERE
   export CORS_ALLOWED_ORIGINS=https://careeradvisor.dev

   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```

### Step 3: Build & Deploy Frontend
1. Navigate to frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm ci
   ```
3. Build optimized production assets:
   ```bash
   export NEXT_PUBLIC_API_URL=https://api.careeradvisor.dev
   npm run build
   ```
4. Start Next.js production server:
   ```bash
   npm run start -- -p 3000
   ```

---

## 5. Reverse Proxy / Nginx Configuration Blueprint

```nginx
# HTTPS Server Configuration
server {
    listen 443 ssl http2;
    server_name careeradvisor.dev www.careeradvisor.dev;

    ssl_certificate /etc/letsencrypt/live/careeradvisor.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/careeradvisor.dev/privkey.pem;

    # Security Headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Frontend Next.js Proxy
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API Proxy
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Multipart Upload Sizing
        client_max_body_size 10M;
    }
}
```

---

## 6. Health Checks & Verification

1. **Backend Liveness**:
   ```bash
   curl -I https://api.careeradvisor.dev/api/auth/health
   # HTTP/1.1 200 OK
   ```
2. **Admin Governance Liveness**:
   ```bash
   curl -H "Authorization: Bearer <ADMIN_JWT>" https://api.careeradvisor.dev/api/admin/health
   # {"status":"UP","service":"Admin Governance & Metrics Service","role":"ROLE_ADMIN"}
   ```

---

## 7. Troubleshooting & FAQ

- **Issue: `403 Forbidden` on `/api/admin/**`**:
  - *Cause*: Request token does not possess `ROLE_ADMIN` authority or token is expired.
  - *Fix*: Authenticate with administrator credentials (`app.admin.email`).
- **Issue: `413 Payload Too Large` on Resume Upload**:
  - *Cause*: Nginx or Spring Boot multipart size limit exceeded.
  - *Fix*: Verify `client_max_body_size 10M;` in Nginx and `MAX_FILE_SIZE=5MB` in backend.
