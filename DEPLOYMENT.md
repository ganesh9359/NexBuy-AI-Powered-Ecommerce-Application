# Cloud Deployment Notes

## Backend

Set these environment variables in cloud instead of editing source:

- `PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `APP_FRONTEND_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `APP_UPLOAD_DIR`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_SSL_ENABLED`

## Frontend

The Angular app now reads `assets/runtime-config.js` at startup. In each environment, set:

```js
window.__NEXBUY_CONFIG__ = {
  apiBase: 'https://api.your-domain.com'
};
```

That lets the same frontend build point to different backend URLs without rebuilding.

## Uploads

`APP_UPLOAD_DIR` is configurable, but uploads still use local filesystem storage. For cloud:

- use a persistent disk/volume for single-instance deployment, or
- replace local file storage with object storage such as S3 before scaling to multiple instances.

## Local Defaults

If no runtime frontend config is provided:

- frontend on `http://localhost:4200` will call `http://localhost:8080`
- non-local deployments will default to same-origin API calls
