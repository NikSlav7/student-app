# Student Results Analysis — Resource Server

Backend resource service for a student results analysis platform. The service stores academic profiles and uploaded results, processes marks from PDF documents, calculates averages, and exposes protected REST endpoints for the frontend.

This repository is the resource server. The broader application also includes a separate [authentication server](https://github.com/NikSlav7/students-app-auth-server) and [React frontend](https://github.com/NikSlav7/students-app-front-end).

## Features

- Student profiles, academic years, periods, subjects, marks, and absences
- Protected REST endpoints for profile and academic-result data
- JWT bearer-token validation through a servlet filter
- PDF table extraction using Apache PDFBox and Tabula
- Parsing of integer and decimal grading formats
- Calculation and retrieval of subject averages
- Upload history for imported results
- Password-reset support through email
- Language detection for English, Estonian, and Russian result files
- Development and production Spring configuration profiles
- Dockerfile for containerised packaging

## How it works

1. The client sends an authenticated request with a bearer token.
2. `TokenFilter` validates the token and places the user identity in Spring Security's context.
3. Resource services load the corresponding student profile.
4. Uploaded result PDFs are extracted as tables and converted into domain objects.
5. Marks are associated with subjects, periods, and academic years.
6. The API returns structured data, including averages and absence statistics.

## Architecture

```text
src/main/java/com/example/ResourceServer/
├── dao/              # Data-access operations
├── dictionary/       # Language detection and translations
├── domains/          # Academic domain objects
├── dto/              # Request and response transfer objects
├── exceptions/       # API exceptions and global handling
├── mail/             # Password-reset email support
├── pdf/              # PDF extraction and mark processing
├── repositories/     # Spring persistence repositories
├── rest/             # REST resource services
├── rowMappers/       # JDBC row mapping
├── schedule/         # Scheduled maintenance tasks
└── security/         # JWT authentication and CORS handling
```

## Technology stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA and JDBC
- PostgreSQL
- Apache PDFBox
- Tabula
- Spring Mail
- Maven
- Docker

## Running locally

### Prerequisites

- JDK 17
- PostgreSQL
- Maven, or use the included Maven wrapper
- SMTP credentials if password-reset emails are required

### Configuration

The project contains separate configuration files for development and production. Configure the local database, authentication-server connection, mail server, and token settings through environment-specific configuration.

Keep passwords, signing keys, database credentials, and mail credentials outside version control.

### Start the service

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

### Build and test

```bash
./mvnw test
./mvnw package
```

### Build the Docker image

```bash
docker build -t student-results-resource-server .
```

Run the container only after supplying the required database and application configuration.

## API areas

The resource server includes endpoints for:

- `/api/profile` — profile and aggregated mark data
- academic years and periods — academic structure and result history
- absences — absence statistics
- uploads — imported result files
- password reset — token and email-based reset flow

For example, the profile service provides an endpoint that returns mark averages grouped by academic year and period:

```http
GET /api/profile/get-all-mark-averages
Authorization: Bearer <token>
```

## Testing

The repository contains tests for application startup and mark-processing behaviour. Run the full test suite with:

```bash
./mvnw test
```

Some tests require a configured database or test environment; use the test configuration supplied in `src/test/resources`.

## Engineering highlights

- Implemented a multi-service design with a dedicated authentication server and resource server.
- Converted semi-structured PDF tables into persistent academic data.
- Modelled relationships among profiles, academic years, periods, subjects, marks, and uploads.
- Added transaction boundaries around result processing and persistence.
- Added consistent exception types and API-level error handling.
- Supported multilingual input formats and decimal grading conventions.

## CV summary

> Developed a Spring Boot resource server for analysing student results, implementing JWT-protected REST APIs, PostgreSQL persistence, PDF table extraction, grade-average calculation, multilingual file parsing, password-reset emails, and Docker packaging.
