# Gmail Tracker (FinTracker)

A Spring Boot backend application designed to track financial transactions (income and expenses) from emails. It automatically categorizes spending and generates monthly financial summaries.

## Features
- **Transaction Processing**: Parses and stores transaction details (amount, type, category).
- **Monthly Summaries**: Automatically generates and saves aggregated reports (total spent, total received, transaction count).
- **Category Analytics**: Breaks down monthly expenditures by category.
- **Scheduled Jobs**: Uses cron jobs to calculate summaries at the end of every month.

## Tech Stack
- Java 17+
- Spring Boot (Data JPA, Web, Scheduling)
- Maven
- MySQL / H2 (Depending on environment configuration)

## API Documentation
Please refer to `api_doc.md` in the root directory for a comprehensive list of all available REST endpoints.
