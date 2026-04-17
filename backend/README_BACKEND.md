# Seichou-Logos (成長の軌跡) - Backend Architecture

## 1. Overview
This backend is designed using **Java 17** and **Spring Boot 3**, following Japanese enterprise development standards. It integrates with LLM APIs to perform cognitive reframing of user emotions.

## 2. Directory Structure
- `backend/sql/`: Database DDL scripts (PostgreSQL).
- `backend/src/main/java/com/seichou/logos/entity/`: JPA Entities mapping to PostgreSQL tables.
- `backend/src/main/java/com/seichou/logos/dto/`: Data Transfer Objects for API requests/responses.
- `backend/src/main/java/com/seichou/logos/service/`: Core business logic, including the AI Reframing Engine.

## 3. Key Features
- **Emotion Reframing**: Converts "negative" logs into "growth assets" using AI.
- **Quantification**: Assigns numerical values to psychological growth (e.g., Self-Awareness +10).
- **PostgreSQL JSONB**: Efficiently stores unstructured AI insights and dynamic growth metrics.

## 4. Tech Stack
- Java 17 / Spring Boot 3
- Spring Data JPA
- PostgreSQL (with JSONB support)
- Lombok
- Swagger/OpenAPI (SpringDoc)
- AI Integration: Spring AI (recommended) or custom HTTP client.

---
*Note: This environment is currently configured for Node.js/React. The Java files provided are architectural blueprints and require a Java runtime for execution.*
