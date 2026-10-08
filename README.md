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

## 8. Attendance report (PDF / Excel)

Both endpoints require `admin / admin123`.

### GET /api/v1/admin/report/summary

Dashboard numbers as JSON:

```json
{
  "registered": 500,
  "verified": 470,
  "checkedIn": 382,
  "noShows": 118,
  "attendanceRate": 81.3
}
```

- `noShows` = registered - checkedIn
- `attendanceRate` = checkedIn / verified x 100

### GET /api/v1/admin/report/export

| Param | Values | Default |
|---|---|---|
| `format` | `pdf`, `excel` (or `xlsx`) | required |
| `includeAttendees` | `true`, `false` | `true` |

The report contains the summary above plus (when `includeAttendees=true`) an attendee
table with: name, email, verified, status (`Checked In` / `No Show`) and check-in time.
Attendees are sorted by name. Excel output has a `Summary` sheet and an `Attendees` sheet.

```bash
curl -u admin:admin123 -OJ "http://localhost:8080/api/v1/admin/report/export?format=pdf"
curl -u admin:admin123 -OJ "http://localhost:8080/api/v1/admin/report/export?format=excel"
curl -u admin:admin123 -OJ "http://localhost:8080/api/v1/admin/report/export?format=pdf&includeAttendees=false"
```

Optional settings: `REPORT_EVENT_NAME` (title, default `Event`) and `REPORT_TIMEZONE`
(check-in times, default `UTC`, e.g. `Africa/Cairo`).

Note: the PDF uses the built-in Helvetica font, which does not render Arabic or other
non-Latin names. Excel handles them fine.

## API summary

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/v1/registrations` | Public |
| POST | `/api/v1/registrations/verify` | Public |
| POST | `/api/v1/registrations/resend-verification` | Public |
| POST | `/api/v1/check-in` | Admin |
| GET | `/api/v1/admin/stats` | Admin |
| GET | `/api/v1/admin/report/summary` | Admin |
| GET | `/api/v1/admin/report/export?format=pdf\|excel` | Admin |


## 📄 License

EventCheck is proprietary software.

The source code is publicly available for portfolio, educational, and
evaluation purposes only. Copying, modifying, redistributing, or commercially
using the source code is not permitted without prior written permission.

See the [LICENSE](LICENSE) file for the full license terms.

Copyright © 2026 Hend Sayed. All rights reserved.
