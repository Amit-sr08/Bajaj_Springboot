# Bajaj Finserv Health | Qualifier 1 | JAVA

Production-ready Spring Boot app that:

- On startup, calls the generateWebhook API to get a unique webhook URL and a JWT accessToken.
- Selects the correct SQL (odd/even based on regNo) from resources and submits it.
- Sends the final SQL query to the webhook URL with the JWT in the Authorization header.

## Tech
- Java 17
- Spring Boot 3 (WebClient)
- Apache PDFBox (optional) to auto-extract SQL from PDFs
- Maven (Spring Boot plugin for fat JAR)

## Configure your details
Edit `src/main/resources/application.yml`:

```yaml
app:
  candidate:
    name: "Amit SR"          # CHANGE
    regNo: "PES1UG22CS075"         # CHANGE
    email: "amitsr882004@gmail.com" # CHANGE
  sql:
    preferPdf: true
    oddPdfPath: "/absolute/path/to/SQL Question 1 JAVA .pdf"
    evenPdfPath: "/absolute/path/to/SQL Qwestion  2 JAVA .pdf"
```

## SQL answers
- For odd last-two regNo digits → Question 1: put your final SQL in `src/main/resources/sql/odd.sql`
- For even last-two regNo digits → Question 2: put your final SQL in `src/main/resources/sql/even.sql`

Keep it as a single statement. Avoid trailing semicolon if the server disallows it.

Placeholders are already present; replace them with your real solution.

## How it works (flow)
1. App starts (`ApplicationRunner`).
2. POST `https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA` with your `name, regNo, email`.
3. Response parsed for:
   - `webhook` (URL to submit to). If missing, it falls back to `/hiring/testWebhook/JAVA`.
   - `accessToken` (JWT for Authorization header).
4. App computes odd/even from last two digits of `regNo`, reads SQL from:
   - PDFs (if `preferPdf: true` and file exists), extracting the final `SELECT`/`WITH ... SELECT` block; otherwise
   - `src/main/resources/sql/odd.sql` or `src/main/resources/sql/even.sql`
5. App POSTs to the webhook URL with header `Authorization: <accessToken>` and body:
   ```json
   { "finalQuery": "YOUR_SQL_QUERY_HERE" }
   ```
6. Logs server response.

No controller/endpoints are exposed; everything runs at startup.

## Build
```bash
mvn -q -DskipTests package
```
Produces a runnable fat JAR at:
```
target/bfh-qualifier1-java-1.0.0.jar
```

## Run
```bash
java -jar target/bfh-qualifier1-java-1.0.0.jar
```

## Notes / Expectations
- Authorization header uses the token as provided by the API (no implicit Bearer prefix).
- If the API returns an absolute webhook URL, the app will submit to it directly.
- If webhook is not returned, the app falls back to `https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA`.

## Submission checklist (what to upload)
- Public GitHub repo containing:
  - Source code
  - Final JAR in `target/` or a `release`
  - RAW downloadable link to the JAR (e.g., GitHub Releases asset link)
- Share the public JAR link

Example GitHub link format:
```
https://github.com/your-username/your-repo.git
```

## Project structure
```
.
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── bajaj
│   │   │           └── bfh
│   │   │               ├── Application.java
│   │   │               ├── config
│   │   │               │   └── AppProperties.java
│   │   │               ├── http
│   │   │               │   ├── HttpProperties.java
│   │   │               │   └── WebClientConfig.java
│   │   │               ├── model
│   │   │               │   ├── GenerateWebhookRequest.java
│   │   │               │   ├── GenerateWebhookResponse.java
│   │   │               │   └── SubmitSolutionRequest.java
│   │   │               └── service
│   │   │                   └── QualifierRunner.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── sql
│   │           ├── even.sql
│   │           └── odd.sql
└── target
```

## Troubleshooting
- If you see "SQL could not be resolved", ensure `odd.sql`/`even.sql` exist and contain non-empty content.
- If you see token/webhook missing, check API connectivity and your candidate details in `application.yml`.


