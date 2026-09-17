# Event Attendance Backend

A Spring Boot + Kotlin backend for:

- attendee registration
- email verification
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

From the project folder run:

```bash
docker compose up -d
```

This starts:

- PostgreSQL: `localhost:5432`
- Mailpit SMTP: `localhost:1025`
- Mailpit inbox UI: `http://localhost:8025`

No real email account is needed for local testing.

## 3. Run Spring Boot

Run:

`EventAttendanceApplication.kt`

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

Example:

```bash
curl -X POST http://localhost:8080/api/v1/registrations \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Hend Sayed\",\"email\":\"hend@test.com\"}"
```

Then open:

`http://localhost:8025`

You will find the verification email in Mailpit.

Copy the 6-digit code.

## 5. Verify email

### POST /api/v1/registrations/verify

```json
{
  "email": "hend@test.com",
  "code": "123456"
}
```

The response contains a unique `qrToken`.

The backend also sends a second email containing the generated QR image.

Open Mailpit again and inspect the QR email.

## 6. Test check-in

Use the `qrToken` from the verification response.

### POST /api/v1/check-in

This endpoint requires organizer/admin authentication.

Username:

`admin`

Password:

`admin123`

Request:

```json
{
  "qrToken": "EVENT-..."
}
```

Using curl:

```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/check-in \
  -H "Content-Type: application/json" \
  -d "{\"qrToken\":\"EVENT-YOUR-TOKEN\"}"
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

## 7. Statistics

### GET /api/v1/admin/stats

Requires:

`admin / admin123`

Example:

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

## Important production notes

This project is ready for local functional testing. Before production:

1. Change `ADMIN_USERNAME` and `ADMIN_PASSWORD`.
2. Use HTTPS.
3. Use a managed PostgreSQL database.
4. Use a real transactional email provider instead of Mailpit.
5. Move secrets to environment variables / secret manager.
6. Consider JWT or another proper authentication mechanism for the organizer app.
7. Add rate limiting for registration, verification, and resend endpoints.
8. Add database migrations (Flyway/Liquibase) instead of relying on `ddl-auto: update`.
9. Add an Event entity if the same backend will manage multiple events.
10. Add audit logging if the client needs detailed attendance history.

## Production email configuration

The local configuration uses Mailpit:

```yaml
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_AUTH=false
MAIL_STARTTLS=false
MAIL_FROM=no-reply@event.local
```

For a real SMTP provider, set the environment variables to that provider's SMTP settings.

Do not commit real SMTP passwords or production credentials to Git.
