# Event Attendance Backend

A Spring Boot + Kotlin backend for:

- attendee registration
- email verification (6-digit code, expires in 2 minutes)
- resend verification code
- unique QR generation
- automatic QR email
- QR check-in
- duplicate check-in protection
- basic attendance statistics
- PostgreSQL persistence
- local email testing with Mailpit
- HTTP Basic protection for organizer/admin endpoints

## Requirements

- IntelliJ IDEA
- JDK 21
- Docker Desktop (for PostgreSQL + Mailpit)
- Internet connection for Gradle to download dependencies

## 1. Open the project

Open this folder in IntelliJ IDEA:

`event-attendance-backend`

Wait for Gradle sync to finish.

Make sure IntelliJ is using **JDK 21** for the Gradle JVM and project SDK.

## 2. Start PostgreSQL and Mailpit

Start Docker Desktop first and wait until it says **Running**. Then, from the project folder:

```bash
docker compose up -d
docker ps
```

`docker ps` must list both containers, and postgres must show `healthy`.

This starts:

- PostgreSQL: `localhost:5433` (mapped to 5432 inside the container)
- Mailpit SMTP: `localhost:1025`
- Mailpit inbox UI: `http://localhost:8025`

PostgreSQL uses port **5433** so it does not clash with another Postgres that may already use 5432. If you change this port, change it in both `docker-compose.yml` and the `DB_URL` default in `application.yml`.

No real email account is needed for local testing.

### Keeping the data safe

| Command | Effect |
|---|---|
| `docker compose stop` | Stops the containers, keeps everything |
| `docker compose up -d` | Starts them again |
| `docker compose down` | Removes the containers, keeps the database volume |
| `docker compose down -v` | Removes the containers **and deletes the database** |

Both services use `restart: unless-stopped`, so they come back automatically when Docker Desktop restarts. Mailpit keeps emails only while its container exists.

## 3. Run Spring Boot

Run:

`EventAttendanceApplication.kt`

The console should end with `Started EventAttendanceApplicationKt`.

The API starts on:

`http://localhost:8080`

## 4. Test registration

### POST /api/v1/registrations

```json
{
  "name": "Hend Sayed",
  "email": "hend@test.com"
}
```

**Windows PowerShell:**

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/registrations -ContentType "application/json" -Body '{"name":"Hend Sayed","email":"hend@test.com"}'
```

**curl (macOS / Linux):**

```bash
curl -X POST http://localhost:8080/api/v1/registrations \
  -H "Content-Type: application/json" \
  -d '{"name":"Hend Sayed","email":"hend@test.com"}'
```

Then open:

`http://localhost:8025`

You will find the verification email in Mailpit. Copy the 6-digit code.

If the email is already registered but **not verified**, registering again sends a new code. If it is already verified, the API returns `409`.

## 5. Verify email

The code expires **2 minutes** after it is sent. The value is set by `app.verification.code-expiration-minutes` (environment variable `VERIFICATION_EXPIRATION_MINUTES`).

### POST /api/v1/registrations/verify

```json
{
  "email": "hend@test.com",
  "code": "123456"
}
```

The response contains a unique `qrToken`.

The backend also sends a second email containing the generated QR image. Open Mailpit again and inspect the QR email.

Possible errors:

| Status | Message | Meaning |
|---|---|---|
| 400 | Invalid verification code. | Wrong code |
| 400 | Verification code has expired. | Older than 2 minutes, request a new one |
| 404 | Registration not found. | Email was never registered |

## 6. Resend verification code

### POST /api/v1/registrations/resend-verification

```json
{
  "email": "hend@test.com"
}
```

A new code is emailed and the old code stops working. Returns `409` if the email is already verified and `404` if it is not registered.

## 7. Test check-in

Use the `qrToken` from the verification response.

### POST /api/v1/check-in

This endpoint requires organizer/admin authentication.

Username: `admin`
Password: `admin123`

Request:

```json
{
  "qrToken": "EVENT-..."
}
```

PowerShell:

```powershell
$auth = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/check-in -Headers @{Authorization=$auth} -ContentType "application/json" -Body '{"qrToken":"EVENT-YOUR-TOKEN"}'
```

curl:

```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/check-in \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"EVENT-YOUR-TOKEN"}'
```

First scan:

```json
{
  "status": "CHECKED_IN",
  "checkedIn": true
}
```

Second scan:

```json
{
  "status": "ALREADY_CHECKED_IN",
  "checkedIn": true
}
```

Invalid QR:

```json
{
  "status": "INVALID_QR",
  "checkedIn": false
}
```

## 8. Statistics

### GET /api/v1/admin/stats

Requires `admin / admin123`.

```bash
curl -u admin:admin123 http://localhost:8080/api/v1/admin/stats
```

## API summary

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/v1/registrations` | Public |
| POST | `/api/v1/registrations/verify` | Public |
| POST | `/api/v1/registrations/resend-verification` | Public |
| POST | `/api/v1/check-in` | Admin |
| GET | `/api/v1/admin/stats` | Admin |

## Using the Android apps

- The phone and the PC must be on the **same Wi-Fi**.
- Run `ipconfig` and use the IPv4 address of the **Wi-Fi adapter** (not the vEthernet / WSL one). In the app set `BASE_URL = "http://<that-ip>:8080/api/v1/"`. It can change after a reboot or on a new network.
- Android emulator: use `http://10.0.2.2:8080/api/v1/`.
- Allow inbound TCP port 8080 in Windows Firewall (admin PowerShell):

  ```powershell
  New-NetFirewallRule -DisplayName "Spring 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
  ```

- Quick test from the phone browser: open `http://<that-ip>:8080/api/v1/admin/stats`. A login prompt means the backend is reachable.
- The app manifest needs `INTERNET` permission and `android:usesCleartextTraffic="true"` while using plain HTTP.
- The scanner app must send the admin Basic login on `/check-in` and `/admin/stats` (an OkHttp interceptor does this).

