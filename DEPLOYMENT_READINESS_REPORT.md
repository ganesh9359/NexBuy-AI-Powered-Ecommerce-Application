# NexBuy — Deployment Readiness Report

**Date:** April 10, 2026  
**Assessed By:** Amazon Q (Senior Engineer Review)  
**Project:** NexBuy E-Commerce Platform  
**Stack:** Spring Boot 3.5.11 · Angular 17 · MySQL 8 · Java 21

---

## Overall Verdict

```
╔══════════════════════════════════════════════════════════╗
║         NOT READY FOR PRODUCTION — YET                   ║
║                                                          ║
║  Backend Build   : ✅ SUCCESS                            ║
║  Frontend Build  : ✅ SUCCESS                            ║
║  Tests           : ✅ 68/68 PASS                         ║
║  Blockers        : ❌ 5 CRITICAL issues must be fixed    ║
║  Warnings        : ⚠️  6 items to address before go-live ║
╚══════════════════════════════════════════════════════════╝
```

The application **builds and tests successfully**, but has **5 critical security/configuration
issues** that MUST be resolved before going live.

---

## 1. Build Status

| Artifact | Command | Status | Output |
|---|---|---|---|
| Backend JAR | `mvnw clean package -DskipTests` | ✅ SUCCESS | `nexbuy-0.0.1-SNAPSHOT.jar` |
| Frontend Bundle | `ng build --configuration production` | ✅ SUCCESS | `dist/frontend/` (916 KB) |
| Unit Tests | `mvnw test` | ✅ SUCCESS | 68/68 passed |

---

## 2. ❌ Critical Blockers (Must Fix Before Deploy)

---

### BLOCKER 1 — Hardcoded Database Password in Source Code
**File:** `src/main/resources/application.properties`  
**Line:** `spring.datasource.password=${DB_PASSWORD:ganesh}`

The default fallback value `ganesh` is a real password hardcoded in source code.
If this file is committed to Git, the database password is publicly exposed.

**Fix:**
```properties
# Remove the default fallback — force it to be set via environment variable
spring.datasource.password=${DB_PASSWORD}
```
Also add `application.properties` to `.gitignore` or use a secrets manager.

---

### BLOCKER 2 — Hardcoded Razorpay Live Keys in Source Code
**File:** `src/main/resources/application.properties`

```properties
app.payment.razorpay.key-id=${APP_PAYMENT_RAZORPAY_KEY_ID:rzp_test_SNRI6LG8i5r4Hk}
app.payment.razorpay.key-secret=${APP_PAYMENT_RAZORPAY_KEY_SECRET:qLtyhfefl4lsj1QmhHA8V72v}
```

Real Razorpay test keys are hardcoded as fallback defaults. These must never be in source code.

**Fix:**
```properties
app.payment.razorpay.key-id=${APP_PAYMENT_RAZORPAY_KEY_ID}
app.payment.razorpay.key-secret=${APP_PAYMENT_RAZORPAY_KEY_SECRET}
```
Set the actual values only via environment variables or a secrets manager (AWS Secrets Manager, etc.).

---

### BLOCKER 3 — Hardcoded Gmail App Password in Source Code
**File:** `src/main/resources/application.properties`

```properties
spring.mail.username=${SPRING_MAIL_USERNAME:nexbuyaipowerd@gmail.com}
spring.mail.password=${SPRING_MAIL_PASSWORD:ezzcugfgolmysvdx}
```

A real Gmail app password is hardcoded. This is a serious credential leak.

**Fix:**
```properties
spring.mail.username=${SPRING_MAIL_USERNAME}
spring.mail.password=${SPRING_MAIL_PASSWORD}
```

---

### BLOCKER 4 — Weak Default JWT Secret
**File:** `src/main/resources/application.properties`

```properties
security.jwt.secret=${JWT_SECRET:replace_me_with_a_secure_32_char_secret}
```

The fallback value `replace_me_with_a_secure_32_char_secret` is a weak, publicly known string.
If `JWT_SECRET` env var is not set in production, all JWTs will be signed with this weak key —
making the entire auth system bypassable.

**Fix:**
```properties
security.jwt.secret=${JWT_SECRET}
```
Generate a strong secret: `openssl rand -base64 64`

---

### BLOCKER 5 — `spring.jpa.hibernate.ddl-auto=update` in Production
**File:** `src/main/resources/application.properties`

```properties
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
```

The default `update` mode allows Hibernate to auto-modify your production database schema.
This can cause **irreversible data loss** in production.

**Fix:**
```properties
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}
```
Use `validate` in production and manage schema changes with proper migration scripts (Flyway/Liquibase).

---

## 3. ⚠️ Warnings (Fix Before or Shortly After Go-Live)

| # | Area | Issue | Priority |
|---|---|---|---|
| W1 | `pom.xml` | `spring-boot-devtools` included — must be excluded in production builds | High |
| W2 | `pom.xml` | Version is `0.0.1-SNAPSHOT` — change to a release version e.g. `1.0.0` | Medium |
| W3 | File Uploads | `APP_UPLOAD_DIR` uses local filesystem — not suitable for multi-instance or cloud deployment | High |
| W4 | CORS | `APP_CORS_ALLOWED_ORIGINS` defaults to `http://localhost:4200` — must be set to production domain | High |
| W5 | Frontend | `app.component.scss` is 25.98 KB — exceeds recommended component style budget | Low |
| W6 | Razorpay | Switch from test keys (`rzp_test_*`) to live keys (`rzp_live_*`) for real payments | High |

---

### Fix W1 — Remove DevTools from Production
**File:** `pom.xml`

`spring-boot-devtools` auto-restarts the app and disables caching. It should never run in production.
It is marked `optional=true` which helps, but explicitly exclude it in the production profile.

---

### Fix W3 — File Upload Storage
Currently product images are stored on the local filesystem (`uploads/products/`).
This will not work if you deploy to multiple instances or a cloud platform.

**Recommended:** Use AWS S3, Cloudinary, or any object storage.
The `CloudStorageService` class already exists in the project — wire it up.

---

### Fix W4 — CORS for Production
Before deploying, set:
```bash
APP_CORS_ALLOWED_ORIGINS=https://your-production-domain.com
APP_FRONTEND_URL=https://your-production-domain.com
```

---

## 4. Pre-Deployment Checklist

### Backend
- [ ] Remove all hardcoded credential fallbacks from `application.properties`
- [ ] Set `JWT_SECRET` to a strong 64-char random value via environment variable
- [ ] Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` in production
- [ ] Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` via environment variables
- [ ] Set `APP_CORS_ALLOWED_ORIGINS` to production domain
- [ ] Set `APP_FRONTEND_URL` to production domain
- [ ] Switch Razorpay to live keys (`rzp_live_*`)
- [ ] Set `APP_PAYMENT_RAZORPAY_WEBHOOK_SECRET` for payment webhook verification
- [ ] Set `APP_AI_OPENAI_API_KEY` if AI features are needed
- [ ] Configure `REDIS_HOST` and `REDIS_PORT` for production Redis instance
- [ ] Set `APP_UPLOAD_DIR` to a persistent volume path or migrate to S3
- [ ] Run `mvnw clean package -DskipTests` to produce the final JAR
- [ ] Run database schema via `db/schema.sql` on the production database

### Frontend
- [ ] Set `window.__NEXBUY_CONFIG__.apiBase` in `assets/runtime-config.js` to production API URL
- [ ] Run `ng build --configuration production`
- [ ] Deploy `dist/frontend/` to a static host (Nginx, S3+CloudFront, Netlify, Vercel)
- [ ] Configure Nginx/server to redirect all routes to `index.html` (Angular SPA routing)

### Infrastructure
- [ ] MySQL 8 database provisioned and accessible
- [ ] Redis instance provisioned (or disable Redis health check if not using caching)
- [ ] SSL/TLS certificate configured (HTTPS required for production)
- [ ] Domain DNS configured
- [ ] Firewall rules — only expose port 443 (HTTPS) publicly

---

## 5. Recommended Deployment Architecture

```
Internet
    │
    ▼
[ Nginx / Load Balancer ]  ← SSL termination, serves Angular static files
    │
    ├──► /api/*  ──► [ Spring Boot JAR :8080 ]
    │                        │
    │                   [ MySQL 8 ]
    │                   [ Redis ]
    │
    └──► /*      ──► [ Angular dist/frontend/ ]
```

---

## 5. Environment Variables Required for Production

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://db-host:3306/nexbuy_db` |
| `DB_USERNAME` | DB username | `nexbuy_user` |
| `DB_PASSWORD` | DB password | *(strong random password)* |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | *(openssl rand -base64 64)* |
| `PORT` | Server port | `8080` |
| `APP_FRONTEND_URL` | Frontend URL for redirects | `https://nexbuy.com` |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `https://nexbuy.com` |
| `SPRING_MAIL_USERNAME` | Gmail address | `your@gmail.com` |
| `SPRING_MAIL_PASSWORD` | Gmail app password | *(from Google account)* |
| `APP_PAYMENT_RAZORPAY_KEY_ID` | Razorpay live key ID | `rzp_live_xxxxx` |
| `APP_PAYMENT_RAZORPAY_KEY_SECRET` | Razorpay live secret | *(from Razorpay dashboard)* |
| `APP_PAYMENT_RAZORPAY_WEBHOOK_SECRET` | Razorpay webhook secret | *(from Razorpay dashboard)* |
| `REDIS_HOST` | Redis hostname | `redis-host` |
| `REDIS_PORT` | Redis port | `6379` |
| `APP_UPLOAD_DIR` | Upload directory path | `/var/nexbuy/uploads` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate DDL mode | `validate` |
| `APP_AI_OPENAI_API_KEY` | OpenAI API key (optional) | `sk-...` |

---

## 6. Summary

| Category | Status |
|---|---|
| Backend compiles | ✅ |
| Frontend compiles | ✅ |
| All tests pass | ✅ |
| No hardcoded secrets | ❌ Fix 3 credential leaks |
| JWT security | ❌ Fix weak default secret |
| Database safety | ❌ Fix DDL auto mode |
| File storage | ⚠️ Local only — not cloud-ready |
| CORS configured | ⚠️ Needs production domain |
| Razorpay live keys | ⚠️ Still on test keys |
| DevTools excluded | ⚠️ Should be excluded in prod |

**Fix the 5 blockers → then the application is ready to deploy.**

The fixes are all configuration changes — no code changes needed.
Estimated time to fix all blockers: **30 minutes**.

---

*Report generated by Amazon Q — April 10, 2026*
