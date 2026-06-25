# Assistant Backend

API REST do Assistente Inteligente — Parte 1 (chat stub, upload, histórico, health check).

## Stack

- Java 21
- Spring Boot 3.4.4
- Spring Data JPA
- H2 in-memory
- Maven

## Pré-requisitos

- Java 21+ (`java -version`)
- Maven 3.9+ (`mvn --version`) — ou use o wrapper incluso (`./mvnw`)

## Como executar

```bash
cd backend

# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## H2 Console

Acessível em `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:assistant`
- User: `sa`
- Password: *(vazio)*

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/chat` | Enviar mensagem (stub) |
| GET | `/api/conversations/{id}` | Histórico da conversa |
| POST | `/api/documents/upload` | Upload de arquivo (multipart) |
| GET | `/api/documents` | Listar documentos |
| GET | `/api/health` | Health check |

## Exemplos com curl

```bash
# Chat sem conversationId (cria nova conversa)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"content": "Olá"}'

# Recuperar histórico
curl http://localhost:8080/api/conversations/{id}

# Upload de PDF
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@documento.pdf"

# Listar documentos
curl http://localhost:8080/api/documents

# Health check
curl http://localhost:8080/api/health
```

## Testes

```bash
cd backend
mvn test
```

## Observações

- Banco H2 **in-memory**: dados são perdidos ao reiniciar
- Respostas do chat são **stubadas** (`"Recebi sua mensagem: ..."`)
- Upload aceita apenas PDF, TXT e DOCX (máx. 10 MB)
