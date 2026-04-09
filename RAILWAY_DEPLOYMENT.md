# NexBuy — Railway Deployment Guide

## Architecture on Railway

```
Railway Project: nexbuy
├── Service 1: nexbuy-backend   (Spring Boot JAR — Dockerfile)
├── Service 2: nexbuy-frontend  (Angular + Nginx — frontend/Dockerfile)
└── Service 3: nexbuy-db        (MySQL 8 — Railway Plugin)
```

---

## Step 1 — Push Code to GitHub

Make sure your code is on GitHub before deploying to Railway.

```bash
git init                          # if not already a git repo
git add .
git commit -m "chore: ready for Railway deployment"
git remote add origin https://github.com/YOUR_USERNAME/nexbuy.git
git push -u origin main
```

---

## Step 2 — Create Railway Project

1. Go to https://railway.app and sign in
2. Click **New Project**
3. Select **Deploy from GitHub repo**
4. Select your `nexbuy` repository
5. Railway will auto-detect the root `Dockerfile` → this becomes the **Backend service**

---

## Step 3 — Add MySQL Database

1. Inside your Railway project, click **+ New Service**
2. Select **Database → MySQL**
3. Railway provisions MySQL 8 automatically
4. Click the MySQL service → **Variables** tab
5. Copy the value of `MYSQL_URL` — you will need it in Step 5

---

## Step 4 — Add Frontend Service

1. Click **+ New Service → GitHub Repo**
2. Select the same `nexbuy` repo
3. Click the service → **Settings**
4. Set **Root Directory** to `frontend`
5. Railway will use `frontend/Dockerfile` automatically

---

## Step 5 — Set Backend Environment Variables

Click the **Backend service → Variables** tab and add ALL of these:

```
DB_URL                          = jdbc:mysql://<MYSQL_HOST>:<MYSQL_PORT>/railway
DB_USERNAME                     = root
DB_PASSWORD                     = <MYSQL_PASSWORD from Railway MySQL service>
SPRING_JPA_HIBERNATE_DDL_AUTO   = validate
PORT                            = 8080
JWT_SECRET                      = e2UoDB0G0TuNGsLKl1g0AdKEhAEFtqFeS9a0VqvBesSnRS74eG/FF4RbkaxEq2FYIJ7cjXb3Ctwso6+wTnaIJg==
SPRING_MAIL_HOST                = smtp.gmail.com
SPRING_MAIL_PORT                = 587
SPRING_MAIL_USERNAME            = nexbuyaipowerd@gmail.com
SPRING_MAIL_PASSWORD            = ezzcugfgolmysvdx
APP_FRONTEND_URL                = https://<YOUR_FRONTEND_RAILWAY_URL>
APP_CORS_ALLOWED_ORIGINS        = https://<YOUR_FRONTEND_RAILWAY_URL>
APP_PAYMENT_RAZORPAY_KEY_ID     = rzp_test_SNRI6LG8i5r4Hk
APP_PAYMENT_RAZORPAY_KEY_SECRET = qLtyhfefl4lsj1QmhHA8V72v
APP_PAYMENT_RAZORPAY_BASE_URL   = https://api.razorpay.com
APP_PAYMENT_RAZORPAY_WEBHOOK_SECRET = <from Razorpay dashboard>
REDIS_HOST                      = localhost
REDIS_PORT                      = 6379
MANAGEMENT_HEALTH_REDIS_ENABLED = false
APP_UPLOAD_DIR                  = /var/nexbuy/uploads
APP_AI_OPENAI_API_KEY           = <your OpenAI key or leave blank>
```

> To get DB_URL values: click MySQL service → Connect tab → copy host, port, password

---

## Step 6 — Set Frontend Environment Variable

Click the **Frontend service → Variables** tab and add:

```
RAILWAY_ENVIRONMENT = production
```

Then update `frontend/src/assets/runtime-config.js` with your backend URL:

```js
window.__NEXBUY_CONFIG__ = {
  apiBase: 'https://<YOUR_BACKEND_RAILWAY_URL>'
};
```

Commit and push this change — Railway will auto-redeploy.

---

## Step 7 — Run Database Schema

After the backend deploys for the first time:

1. Go to Railway MySQL service → **Query** tab
2. Paste and run the contents of `db/schema.sql`
3. Optionally run `db/seed.sql` for sample data

OR connect via MySQL client:
```bash
mysql -h <MYSQL_HOST> -P <MYSQL_PORT> -u root -p<MYSQL_PASSWORD> railway < db/schema.sql
```

---

## Step 8 — Generate Railway URLs

1. Backend service → **Settings → Networking → Generate Domain**
   → e.g. `nexbuy-backend.up.railway.app`

2. Frontend service → **Settings → Networking → Generate Domain**
   → e.g. `nexbuy-frontend.up.railway.app`

3. Go back to Backend service → Variables → update:
   ```
   APP_FRONTEND_URL         = https://nexbuy-frontend.up.railway.app
   APP_CORS_ALLOWED_ORIGINS = https://nexbuy-frontend.up.railway.app
   ```

4. Update `frontend/src/assets/runtime-config.js`:
   ```js
   window.__NEXBUY_CONFIG__ = {
     apiBase: 'https://nexbuy-backend.up.railway.app'
   };
   ```
   Commit and push.

---

## Step 9 — Verify Deployment

Check these URLs after deployment:

| Check | URL | Expected |
|---|---|---|
| Backend health | `https://nexbuy-backend.up.railway.app/actuator/health` | `{"status":"UP"}` |
| Product API | `https://nexbuy-backend.up.railway.app/products` | JSON product list |
| Frontend | `https://nexbuy-frontend.up.railway.app` | NexBuy homepage |
| Admin login | `https://nexbuy-frontend.up.railway.app/auth/login` | Login page |

---

## Deployment Checklist

- [ ] Code pushed to GitHub
- [ ] Railway project created
- [ ] MySQL service added
- [ ] Backend service created with root Dockerfile
- [ ] Frontend service created with `frontend/` root directory
- [ ] All backend environment variables set
- [ ] `runtime-config.js` updated with backend Railway URL
- [ ] `db/schema.sql` executed on Railway MySQL
- [ ] Backend health check returns `UP`
- [ ] Frontend loads in browser
- [ ] Login works
- [ ] Products load

---

## Estimated Cost on Railway

| Resource | Plan | Cost |
|---|---|---|
| Backend service | Hobby | ~$5/month |
| Frontend service | Hobby | ~$5/month |
| MySQL database | Hobby | ~$5/month |
| **Total** | | **~$15/month** |

Free trial: Railway gives $5 free credit to start.

---

*NexBuy Railway Deployment Guide — April 10, 2026*
