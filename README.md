# Projeto IA

Assistente inteligente com backend em Java/Spring Boot e frontend web.  
O backend expõe uma API REST para chat, upload de documentos e histórico de conversas. O frontend consome essa API e exibe a interface para o usuário.

- **Backend** — API REST em Java 21 + Spring Boot.  
  → [`backend/README.md`](backend/README.md)

- **Frontend** — Aplicação cliente em React + TypeScript (Vite).  
  → [`frontend/README.md`](frontend/README.md)

## Infraestrutura

O projeto utiliza [Docker Compose](docker-compose.yml) para subir os serviços necessários:

| Serviço | Imagem | Função |
|---------|--------|--------|
| `ollama` | [ollama/ollama](https://hub.docker.com/r/ollama/ollama) | Modelos de IA locais |
| `pgvector` | [pgvector/pgvector:pg16](https://hub.docker.com/r/pgvector/pgvector) | PostgreSQL com extensão vector |
| `n8n` | [n8nio/n8n](https://hub.docker.com/r/n8nio/n8n) | Automação de workflows |

> A extensão `vector` do PostgreSQL é ativada automaticamente na primeira inicialização via [`scripts/init-pgvector.sql`](scripts/init-pgvector.sql).  
> Se o volume do `pgvector` já existir, execute manualmente:  
> `docker exec pgvector psql -U postgres -d assistant -c "CREATE EXTENSION IF NOT EXISTS vector;"`

## Perfis de execução

O backend possui três perfis Spring Boot para diferentes provedores de IA:

| Profile | Embedding | LLM | Descrição |
|---------|-----------|-----|-----------|
| `local` | Ollama (`nomic-embed-text`) | Ollama (`llama3.2:3b`) | Modelos locais gratuitos |
| `groq` | Ollama (`nomic-embed-text`) | Groq (`llama-3.3-70b-versatile`) | LLM via API externa rápida |
| *(default)* | OpenAI (`text-embedding-3-small`) | OpenAI (`gpt-4o-mini`) | API paga da OpenAI |

### Setup rápido — perfil local

```powershell
# 1. Sobe os containers
docker compose up -d

# 2. Baixa os modelos
docker compose exec ollama ollama pull nomic-embed-text
docker compose exec ollama ollama pull llama3.2:3b

# 3. Configura ambiente
copy backend\.env.example backend\.env

# 4. Executa o backend
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
./mvnw.cmd spring-boot:run
```

> Também é possível usar o script [`scripts/setup-ollama.ps1`](scripts/setup-ollama.ps1) para automatizar os passos 1 e 2.

### Setup rápido — perfil groq

```powershell
# 1. Sobe os containers
docker compose up -d

# 2. Baixa o modelo de embedding
docker compose exec ollama ollama pull nomic-embed-text

# 3. Configura ambiente
copy backend\.env.example backend\.env
# Edite backend\.env e preencha GROQ_API_KEY com sua chave

# 4. Executa o backend
cd backend
$env:SPRING_PROFILES_ACTIVE="groq"
./mvnw.cmd spring-boot:run
```
