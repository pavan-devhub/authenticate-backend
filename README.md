# authenticate-backend

REST API for Authenticate, built with Spring Boot 4 and Java 17. It runs on MySQL on your computer and on PostgreSQL in production. The React frontend lives in the `authenticate-frontend` folder next to this one.

Registering needs a 6-digit code that is emailed through Gmail, and the account is only created once that code is entered. Logging in only needs the email and password.

## Run it locally

You need Java 17+, a running MySQL server and a Gmail account to send the codes from.

1. Copy `.env.example` to `.env` and fill in your MySQL details and your Gmail address.
2. Create a Gmail app password and put it in `.env` as `MAIL_PASSWORD`. Your normal Google password won't work.
   1. Turn on 2-Step Verification for the Google account.
   2. Create an app password at <https://myaccount.google.com/apppasswords>.
   3. Paste the 16 characters without spaces.
3. Start the API from this folder:

   ```bash
   ./mvnw spring-boot:run
   ```

The API starts on <http://localhost:8080>. The `authenticate_db` database is created on first start, and Flyway creates the tables.

If startup fails with `Access denied ... (using password: NO)`, the app didn't find your MySQL password. Check that `.env` exists in this folder.

If the app says it couldn't send the verification email, the log shows why. `535 Username and Password not accepted` means Gmail rejected the app password.

## Deploy to Render

The backend runs on Render as a Docker container with a Render PostgreSQL database. [`render.yaml`](render.yaml) creates both and connects them.

1. Push this folder to its own GitHub repository. `render.yaml` must be at the root.
2. In Render, choose **New > Blueprint** and pick that repository.
3. When Render asks for `MAIL_USERNAME` and `MAIL_PASSWORD`, enter your Gmail address and app password.
4. Apply the Blueprint. Render builds the [`Dockerfile`](Dockerfile), and Flyway creates the tables on first start.
5. In Vercel, set `VITE_API_URL` on the frontend project to the backend's Render address, such as `https://authenticate-backend.onrender.com`. Then redeploy the frontend.

**The web service needs a paid plan.** Render's free plan blocks outbound email on SMTP ports 25, 465 and 587, so Gmail can't send the codes from it. `render.yaml` therefore uses the Starter plan. To stay on the free plan, the codes would have to go through an HTTPS email API instead of Gmail SMTP.

The free PostgreSQL database expires 30 days after it's created unless you upgrade it.

To build and run the image yourself:

```bash
docker build -t authenticate-backend .
```

```bash
docker run -p 8080:8080 -e DATABASE_URL=postgresql://user:password@host:5432/authenticate_db -e MAIL_USERNAME=you@gmail.com -e MAIL_PASSWORD=yourapppassword authenticate-backend
```

## Configuration

Set these in `.env`, as environment variables, or in Render.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | none | PostgreSQL URL such as `postgresql://user:password@host:5432/db`. When set, it replaces the three `DB_` settings. |
| `DB_URL` | `jdbc:mysql://localhost:3306/authenticate_db?...` | JDBC connection URL |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | empty | Database password |
| `MAIL_USERNAME` | empty | Gmail address the codes are sent from |
| `MAIL_PASSWORD` | empty | Gmail app password for that address |
| `PORT` | `8080` | Port the API listens on. Render sets it. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:[*],https://authenticate-frontend.vercel.app` | Comma-separated frontend origins. `[*]` matches any port. |

The code rules are the `app.otp` settings in `application.properties`:

- A code works for 10 minutes.
- 5 wrong codes end the attempt.
- A new code can be requested every 60 seconds. Requesting one doesn't reset the wrong-code count.

## API

Registering takes two calls. The first emails a code and returns a challenge. The second sends the code back with the challenge's `challengeId` and creates the account.

| Method | Path | Body | Success | Errors |
| --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | `name`, `email`, `password` | `202` with a challenge | `400` invalid details, `409` email taken, `503` email not sent |
| `POST` | `/api/auth/register/verify` | `challengeId`, `code` | `201` with the new user | `400` wrong code, `409` email taken, `410` start again |
| `POST` | `/api/auth/otp/resend` | `challengeId` | `200` with the updated challenge | `410` start again, `429` too soon |
| `POST` | `/api/auth/login` | `email`, `password` | `200` with the user and login history | `400` missing fields, `401` wrong credentials |

The response shapes are:

- **Challenge:** `{ "challengeId", "email", "expiresAt", "resendAvailableAt" }`.
- **User:** `{ "id", "name", "email" }`.
- **Login:** `{ "user", "loginAttempts" }`. `loginAttempts` holds the 50 most recent attempts, newest first, as `{ "id", "outcome", "successful", "attemptedAt" }`.

`outcome` is `SUCCESS` or `WRONG_PASSWORD`. History from when logging in needed a code can also contain `WRONG_CODE`. Every attempt on an existing account is recorded. Attempts with an email that has no account aren't recorded.

Errors follow [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457), and `detail` holds the message to show:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "That code is incorrect. You have 4 attempts left.",
  "attemptsLeft": 4
}
```

## Project structure

Requests flow down through the layers: controller, then service, then repository.

```text
src/main/java/com/authenticate/backend
├── config       Beans and settings (CORS, password encoder, code rules, clock, DATABASE_URL)
├── controller   HTTP endpoints. No business logic.
├── dto          Request and response shapes, including input validation
├── entity       JPA entities mapped to database tables
├── exception    Custom exceptions and the global error handler
├── mapper       Entity to DTO conversion
├── repository   Database access through Spring Data JPA
└── service      Business logic (auth, codes, email), with implementations in service/impl

src/main/resources
├── application.properties   Application configuration
└── db/migration
    ├── mysql                Flyway migrations for MySQL (local)
    └── postgresql           The same migrations for PostgreSQL (production)
```

To change the database schema, add the next migration to both `db/migration/mysql` and `db/migration/postgresql`, with the same version number, such as `V6__description.sql`. Don't edit a migration that has already run.

## Tests

```bash
./mvnw test
```

The tests don't need a database or a mail server.
