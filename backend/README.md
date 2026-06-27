# Assistant Backend

API REST do Assistente Inteligente — Chat com RAG, upload de documentos e histórico de conversas.

## Stack

- Java 21
- Spring Boot 3.4.4
- Spring Data JPA / Hibernate
- PostgreSQL + pgvector
- Maven

## Pré-requisitos

- Java 21+ (`java -version`)
- Maven 3.9+ (`mvn --version`) — ou use o wrapper incluso (`./mvnw`)
- Docker Desktop (para PostgreSQL + Ollama)

## Como executar

```bash
cd backend
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## Perfis

O backend usa perfis Spring Boot para alternar entre provedores de IA:

| Profile | Embedding | LLM | Arquivo |
|---------|-----------|-----|---------|
| `local` | Ollama (`nomic-embed-text`) | Ollama (`llama3.2:3b`) | `application-local.yml` |
| `groq` | Ollama (`nomic-embed-text`) | Groq (`llama-3.3-70b-versatile`) | `application-groq.yml` |
| *(default)* | OpenAI (`text-embedding-3-small`) | OpenAI (`gpt-4o-mini`) | `application.yml` |

### Perfil local (Ollama)

```powershell
# 1. Sobe os containers (raiz do projeto)
docker compose up -d

# 2. Baixa os modelos
docker compose exec ollama ollama pull nomic-embed-text
docker compose exec ollama ollama pull llama3.2:3b

# 3. Ativa o profile e executa
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
./mvnw.cmd spring-boot:run
```

### Perfil groq (LLM via API)

```powershell
# 1. Sobe os containers
docker compose up -d

# 2. Baixa o modelo de embedding
docker compose exec ollama ollama pull nomic-embed-text

# 3. Configura .env com sua chave GROQ_API_KEY
copy .env.example .env
# Edite .env e preencha GROQ_API_KEY

# 4. Ativa o profile e executa
$env:SPRING_PROFILES_ACTIVE="groq"
./mvnw.cmd spring-boot:run
```

> O embedding sempre usa Ollama nos perfis `local` e `groq`. Apenas o LLM alterna entre Ollama, Groq e OpenAI.

## pgvector

O banco PostgreSQL usa a extensão `vector` para armazenar e buscar embeddings.  
A extensão é ativada automaticamente via [`scripts/init-pgvector.sql`](../scripts/init-pgvector.sql) na primeira inicialização do container.

Caso o volume já exista, ative manualmente:

```powershell
docker exec pgvector psql -U postgres -d assistant -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/chat` | Enviar mensagem |
| GET | `/api/conversations` | Listar conversas |
| GET | `/api/conversations/{id}` | Histórico da conversa |
| POST | `/api/documents/upload` | Upload de arquivo (multipart) |
| GET | `/api/documents` | Listar documentos |
| GET | `/api/documents/{id}/status` | Status do documento |
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

- Upload aceita apenas PDF, TXT e DOCX (máx. 10 MB)
- Embedding local depende do Ollama rodando em segundo plano
- A extensão `vector` do PostgreSQL é obrigatória para o RAG funcionar
