# System Docs — Parte 2: Pipeline RAG, Orquestração n8n e Interface com Fontes

> **Projeto:** Assistente Inteligente Full-Stack  
> **Stack:** Java 21 · Spring Boot 3.4.4 · PostgreSQL + pgvector · React 18 · TypeScript · Vite · Ant Design 5 · n8n  
> **Escopo:** Parte 2 — Pipeline de ingestão de documentos, retrieval aumentado por contexto, orquestração assíncrona com n8n e interface React com rastreabilidade de fontes  
> **Versão:** 1.0.0  
> **Status:** Draft — Spec-Driven Development (SDD)

---

## Índice

1. [Diagrama de Sequência Textual — Ingestão](#1-diagrama-de-sequência-textual--ingestão)
2. [Diagrama de Sequência Textual — Retrieval](#2-diagrama-de-sequência-textual--retrieval)
3. [Contratos de Interface — Backend](#3-contratos-de-interface--backend)
4. [Contratos de API HTTP](#4-contratos-de-api-http)
5. [Novos Tipos TypeScript — Frontend](#5-novos-tipos-typescript--frontend)
6. [Roteiro de Validação Manual por Camada](#6-roteiro-de-validação-manual-por-camada)
7. [Mapa de Violações Arquiteturais](#7-mapa-de-violações-arquiteturais)
8. [Template do AGENTS.md — Parte 2](#8-template-do-agentsmd--parte-2)

---

## 1. Diagrama de Sequência Textual — Ingestão

```
┌──────────┐   ┌──────────────┐   ┌───────────────────┐   ┌────────────────┐   ┌──────────────────────┐   ┌────────────┐
│  Client   │   │  Document    │   │  DocumentIngestion│   │  Embedding     │   │  DocumentChunk      │   │  n8n       │
│  (React)  │   │  Controller  │   │  Service          │   │  Service       │   │  Repository          │   │  Webhook   │
└─────┬─────┘   └──────┬───────┘   └────────┬──────────┘   └───────┬────────┘   └──────────┬───────────┘   └─────┬──────┘
      │                │                     │                     │                       │                     │
      │ POST /upload   │                     │                     │                       │                     │
      │ multipart      │                     │                     │                       │                     │
      │───────┬───────►│                     │                     │                       │                     │
      │        │       │                     │                     │                       │                     │
      │        │       │ 1. Salva arquivo    │                     │                       │                     │
      │        │       │    (storage)        │                     │                       │                     │
      │        │       │    + metadados no DB│                     │                       │                     │
      │        │       │───────┬─────────────►│                     │                       │                     │
      │        │       │        │              │                     │                       │                     │
      │        │       │        │ 2. Inicia    │                     │                       │                     │
      │        │       │        │    ingestão  │                     │                       │                     │
      │        │       │        │    assíncrona│                     │                       │                     │
      │        │       │        │◄─────────────│                     │                       │                     │
      │        │       │        │              │                     │                       │                     │
      │        │       │ 201 Created          │                     │                       │                     │
      │        │       │ { id, status:        │                     │                       │                     │
      │        │       │   PROCESSING }       │                     │                       │                     │
      │◄───────┼───────│                      │                     │                       │                     │
      │        │       │                      │                     │                       │                     │
      │        │       │    ─ ─ ─ ingestão é assíncrona ─ ─ ─ ─ ─►│                       │                     │
      │        │       │                      │                     │                       │                     │
      │        │       │                      │ 3. parse(file)      │                       │                     │
      │        │       │                      │    → texto bruto    │                       │                     │
      │        │       │                      │───────┬────────────►│                       │                     │
      │        │       │                      │        │             │                       │                     │
      │        │       │                      │        │ 4. chunk(text)                      │                     │
      │        │       │                      │        │    → lista de chunks                │                     │
      │        │       │                      │        │◄────────────│                       │                     │
      │        │       │                      │        │             │                       │                     │
      │        │       │                      │        │             │                       │                     │
      │        │       │                      │  5. Para cada chunk: │                       │                     │
      │        │       │                      │    embed(chunkText)  │                       │                     │
      │        │       │                      │───────────┬─────────►│                       │                     │
      │        │       │                      │            │          │                       │                     │
      │        │       │                      │            │  6. List<Float> embedding        │                     │
      │        │       │                      │◄───────────┼──────────│                       │                     │
      │        │       │                      │            │          │                       │                     │
      │        │       │                      │  7. save(chunk, embedding)                    │                     │
      │        │       │                      │────────────┼─────────────────────────────────►│                     │
      │        │       │                      │            │          │                       │                     │
      │        │       │                      │  8. status = COMPLETED                        │                     │
      │        │       │                      │  + update Document.status                     │                     │
      │        │       │                      │───────┬────│──────────│───────────────────────►│                     │
      │        │       │                      │        │    │          │                       │                     │
      │        │       │                      │        │    │          │  9. POST /webhook     │                     │
      │        │       │                      │        │    │          │     { documentId,     │                     │
      │        │       │                      │        │    │          │       status,         │                     │
      │        │       │                      │        │    │          │       chunksCount }   │                     │
      │        │       │                      │        │    │          │──────────────────────►│                     │
      │        │       │                      │        │    │          │                       │                     │
      │        │       │                      │        │    │          │  (Fire & Forget —    │                     │
      │        │       │                      │        │    │          │   sem await nem retry)│                     │
```

### Notas do Diagrama de Ingestão

- O upload síncrono (passos 1-2) retorna `201 Created` imediatamente com `status: PROCESSING`.
- A ingestão completa (passos 3-9) roda em thread separada (`@Async` ou `TaskExecutor`).
- O webhook ao n8n (passo 9) é fire-and-forget: o backend não aguarda resposta, não interpreta retorno, não faz retry.
- Se a ingestão falha, `Document.status` vai para `FAILED` e o n8n é notificado com `status: FAILED`.

---

## 2. Diagrama de Sequência Textual — Retrieval

```
┌──────────┐   ┌──────────────┐   ┌──────────────┐   ┌────────────────┐   ┌──────────────────────┐   ┌─────────┐
│  Client   │   │  Chat        │   │  RagService  │   │  Embedding     │   │  DocumentChunk      │   │  LLM    │
│  (React)  │   │  Controller  │   │  (orquestrador)            │   │  Repository          │   │         │
└─────┬─────┘   └──────┬───────┘   └──────┬────────┘   └───────┬────────┘   └──────────┬───────────┘   └────┬────┘
      │                │                   │                    │                       │                    │
      │ POST /chat     │                   │                    │                       │                    │
      │ { content }    │                   │                    │                       │                    │
      │───────┬───────►│                   │                    │                       │                    │
      │        │       │                   │                    │                       │                    │
      │        │       │ 1. processMessage │                    │                       │                    │
      │        │       │    (request)      │                    │                       │                    │
      │        │       │──────────────────►│                    │                       │                    │
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 2. embed(         │                       │                    │
      │        │       │                   │      question)    │                       │                    │
      │        │       │                   │──────────────────►│                       │                    │
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 3. List<Float>     │                       │                    │
      │        │       │                   │◄───────────────────│                       │                    │
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 4. findSimilar(    │                       │                    │
      │        │       │                   │      embedding,    │                       │                    │
      │        │       │                   │      TOP_K,        │                       │                    │
      │        │       │                   │      MIN_SIMILARITY│                       │                    │
      │        │       │                   │    )               │                       │                    │
      │        │       │                   │────────────────────────────────────────────►│                    │
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 5. List<ChunkResult>                       │                    │
      │        │       │                   │◄────────────────────────────────────────────│                    │
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 6. buildPrompt     │                       │                    │
      │        │       │                   │    (question,      │                       │                    │
      │        │       │                   │     contextChunks) │                       │                    │
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 7. callLLM(prompt) │                       │                    │
      │        │       │                   │──────────────────────────────────────────────────────────────►│
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 8. LLM response    │                       │                    │
      │        │       │                   │◄──────────────────────────────────────────────────────────────│
      │        │       │                   │                    │                       │                    │
      │        │       │                   │ 9. Monta resposta  │                       │                    │
      │        │       │                   │    com sources     │                       │                    │
      │        │       │                   │    (chunks usados) │                       │                    │
      │        │       │ 10. ChatMessageResponse               │                       │                    │
      │        │       │     { content, sources: [...] }       │                       │                    │
      │        │       │◄───────────────────│                    │                       │                    │
      │        │       │                   │                    │                       │                    │
      │ 200 OK │       │                   │                    │                       │                    │
      │◄───────┼───────│                   │                    │                       │                    │
```

### Notas do Diagrama de Retrieval

- O `RagService` **nunca** acessa repositório, banco ou serviço de embedding diretamente — delega tudo.
- Se `TOP_K = 0` ou não há chunks similares, o RagService envia o prompt sem contexto (fallback gracefully).
- O array `sources` na resposta contém os chunks recuperados com score de similaridade, documento de origem e texto do chunk.
- A chamada ao LLM (passo 7) é feita via interface abstrata (port), não diretamente.

---

## 3. Contratos de Interface — Backend

### 3.1 `DocumentIngestionService`

**Pacote:** `com.assistant.service.ingestion`  
**Responsabilidade:** Orquestrar o pipeline de ingestão: parsing → chunking → embedding → persistência → notificação.

| Método | Entrada | Saída | Exceções |
|---|---|---|---|
| `ingestDocument(UUID documentId)` | `documentId: UUID` — ID do documento recém-salvo | `void` — processo assíncrono | `DocumentNotFoundException` (404) se documentId não existir; `IngestionFailedException` (500) se parsing/chunking falhar |
| `parseFile(Path storedFilePath, String contentType)` | `storedFilePath: Path` — caminho físico do arquivo; `contentType: String` — MIME type | `String` — texto bruto extraído | `UnsupportedFileTypeException` (400) se tipo não suportado para parsing |
| `chunkText(String text)` | `text: String` — texto bruto extraído | `List<String>` — lista de chunks | Nenhuma (retorna lista vazia se texto vazio) |

**Regras:**
- `ingestDocument` é disparado por `@Async` e executa em thread separada.
- O método delega parsing → chunking → embedding externamente (`EmbeddingService`) e persistência externamente (`DocumentChunkRepository`).
- Ao final (sucesso ou falha), atualiza `Document.status` e dispara notificação ao n8n (fire-and-forget).
- `parseFile` deve usar estratégia de parsing por tipo de arquivo (PDF → Apache PDFBox, TXT → leitura direta, DOCX → Apache POI).
- `chunkText` implementa chunking por parágrafos com sobreposição configurável (tamanho do chunk e overlap são parâmetros de configuração).

**Dependências injetadas:**
- `DocumentRepository` — para buscar documento e atualizar status
- `EmbeddingService` — para gerar embeddings de cada chunk
- `DocumentChunkRepository` — para persistir chunks com vetores
- `N8nWebhookNotifier` — para notificar n8n ao final
- `TaskExecutor` — para execução assíncrona

---

### 3.2 `EmbeddingService`

**Pacote:** `com.assistant.service.embedding`  
**Responsabilidade:** Gerar vetores de embedding a partir de texto. **Não conhece domínio, entidades ou persistência.**

| Método | Entrada | Saída | Exceções |
|---|---|---|---|
| `embed(String text)` | `text: String` — texto a ser embedado | `List<Float>` — vetor de embedding | `EmbeddingServiceException` (500) se API de embedding falhar ou retornar resposta inválida |

**Regras (R2 — agnóstico de domínio):**
- O método recebe apenas `String` e retorna apenas `List<Float>`.
- Não pode importar ou referenciar `Document`, `Chunk`, `Conversation` ou qualquer entidade de domínio.
- Não pode acessar banco de dados, repositórios ou services de domínio.
- A implementação concreta pode chamar API externa (OpenAI, Hugging Face, Ollama) ou usar modelo local.

**Dependências injetadas:**
- Propriedades de configuração do modelo de embedding (URL, API key, modelo, etc.)

---

### 3.3 `RagService`

**Pacote:** `com.assistant.service.rag`  
**Responsabilidade:** Orquestrar retrieval augmented generation. **Não acessa banco nem chama embedding diretamente (R3).**

| Método | Entrada | Saída | Exceções |
|---|---|---|---|
| `enrichPrompt(String question, String conversationHistory)` | `question: String` — pergunta do usuário; `conversationHistory: String` — histórico formatado da conversa | `RagContext` — contexto enriquecido com chunks similares e prompt montado | `RagServiceException` (500) se embedding ou retrieval falhar |
| `buildPrompt(String question, String conversationHistory, List<ChunkResult> relevantChunks)` | `question`, `conversationHistory`, `relevantChunks` — lista de chunks relevantes | `String` — prompt completo com contexto | Nenhuma |
| `callLlm(String prompt)` | `prompt: String` — prompt montado | `String` — resposta do LLM | `LlmServiceException` (500) se API do LLM falhar |

**Classe auxiliar `RagContext`:**

```java
public class RagContext {
    private String enrichedPrompt;       // Prompt montado com contexto
    private List<ChunkResult> sources;   // Chunks usados como fonte
}
```

**Classe auxiliar `ChunkResult`:**

```java
public class ChunkResult {
    private UUID chunkId;
    private String chunkContent;
    private String documentId;
    private String documentName;
    private Double similarityScore;
}
```

**Regras (R3 — delegação pura):**
- `enrichPrompt` delega ao `EmbeddingService.embed(question)` e ao `DocumentChunkRepository.findSimilar(embedding, topK, minSimilarity)`.
- `RagService` **não** injeta `DocumentChunkRepository` nem chama JPA — usa uma interface `ChunkRetrievalPort`.
- Se `TOP_K = 0`, pula retrieval e monta prompt sem contexto.

**Dependências injetadas:**
- `EmbeddingService` — para gerar embedding da pergunta
- `ChunkRetrievalPort` — interface para retrieval (implementada por `DocumentChunkRepository`)
- `LlmPort` — interface para chamada ao LLM (implementada por `LlmService` ou adaptador REST)
- `@Value("${app.rag.top-k}")` — `int`, default 5
- `@Value("${app.rag.min-similarity}")` — `double`, default 0.7

---

### 3.4 `DocumentChunkRepository`

**Pacote:** `com.assistant.repository`  
**Responsabilidade:** Persistir e recuperar chunks de documento com busca por similaridade vetorial (pgvector).

**Pai:** `PagingAndSortingRepository<DocumentChunk, UUID>`

| Método | Entrada | Saída |
|---|---|---|
| `saveAll(List<DocumentChunk> chunks)` | `chunks: List<DocumentChunk>` | `List<DocumentChunk>` |
| `findByDocumentId(UUID documentId)` | `documentId: UUID` | `List<DocumentChunk>` |
| `findSimilar(List<Float> embedding, int topK, double minSimilarity)` | `embedding: List<Float>`, `topK: int`, `minSimilarity: double` | `List<ChunkResultProjection>` |

**Query de similaridade (pgvector):**

```java
@Query(value = """
    SELECT dc.id as chunkId, dc.content as chunkContent,
           d.id as documentId, d.original_filename as documentName,
           (1 - (dc.embedding <=> CAST(:embedding AS vector))) as similarityScore
    FROM document_chunks dc
    JOIN documents d ON d.id = dc.document_id
    WHERE (1 - (dc.embedding <=> CAST(:embedding AS vector))) >= :minSimilarity
    ORDER BY similarityScore DESC
    LIMIT :topK
    """, nativeQuery = true)
List<ChunkResultProjection> findSimilar(@Param("embedding") String embeddingStr,
                                         @Param("topK") int topK,
                                         @Param("minSimilarity") double minSimilarity);
```

**Nota:** A projeção `ChunkResultProjection` implementa `ChunkResult` e converte os campos nativos para os tipos Java. O parâmetro `embedding` é convertido para string no formato pgvector (`[0.1,0.2,...]`).

**Entidade `DocumentChunk`:**

| Campo | Tipo | Coluna | Descrição |
|---|---|---|---|
| `id` | `UUID` | `id` | PK |
| `document` | `Document` | `document_id` (FK) | Documento de origem |
| `content` | `String` (TEXT) | `content` | Texto do chunk |
| `chunkIndex` | `Integer` | `chunk_index` | Ordem do chunk no documento |
| `embedding` | `vector` | `embedding` | Vetor pgvector (dimensão configurável) |
| `createdAt` | `Instant` | `created_at` | Timestamp de criação |

---

### 3.5 Enum `DocumentStatus`

**Pacote:** `com.assistant.domain.model`

```java
public enum DocumentStatus {
    PENDING,       // Aguardando processamento (upload concluído)
    PROCESSING,    // Ingestão em andamento
    COMPLETED,     // Ingestão concluída com sucesso
    FAILED         // Ingestão falhou
}
```

---

### 3.6 Entidade `Document` — Campos Adicionados

| Campo | Tipo | Descrição |
|---|---|---|
| `status` | `DocumentStatus` | Estado da ingestão (default `PENDING`) |
| `chunksCount` | `Integer` | Número de chunks gerados (nullable; preenchido após ingestão) |

---

### 3.7 Interface `N8nWebhookNotifier`

**Pacote:** `com.assistant.service.notification`

```java
public interface N8nWebhookNotifier {
    void notifyIngestionComplete(UUID documentId, DocumentStatus status, int chunksCount);
}
```

**Implementação:** RestTemplate/WebClient configurado com URL de variável de ambiente `N8N_WEBHOOK_URL`.

**Payload do POST:**

```json
{
  "event": "document.ingestion.completed",
  "documentId": "uuid",
  "status": "COMPLETED",
  "chunksCount": 42,
  "timestamp": "2026-06-26T10:00:00Z"
}
```

---

### 3.8 Novas Exceções

| Exceção | HTTP Status | Quando ocorre |
|---|---|---|
| `IngestionFailedException` | 500 | Falha no pipeline de ingestão (parsing, chunking, embedding) |
| `UnsupportedFileTypeException` | 400 | Tipo de arquivo não suportado para parsing |
| `EmbeddingServiceException` | 500 | API de embedding indisponível ou retorna erro |
| `RagServiceException` | 500 | Falha no retrieval ou na chamada ao LLM |
| `LlmServiceException` | 500 | API do LLM indisponível |
| `IngestionInProgressException` | 409 | Tentativa de reingestão de documento em estado PROCESSING |

---

## 4. Contratos de API HTTP

**Base URL:** `/api`  
**Content-Type padrão:** `application/json` (exceto upload: `multipart/form-data`)  
**Formato de datas:** ISO-8601 UTC

### 4.1 `POST /api/documents/upload`

Endpoint existente, **modificado** para iniciar ingestão assíncrona automaticamente.

| Detalhe | Valor |
|---|---|
| Método | `POST` |
| Rota | `/api/documents/upload` |
| Content-Type | `multipart/form-data` |
| Request Part | `file: MultipartFile` (PDF, TXT, DOCX) |

**Response `201 Created`:**

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "originalFilename": "relatorio.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 204800,
  "status": "PROCESSING",
  "uploadedAt": "2026-06-26T10:00:00Z"
}
```

**Status Codes:**

| Status | Semântica |
|---|---|
| `201 Created` | Upload aceito e ingestão iniciada assincronamente |
| `400 Bad Request` | Tipo de arquivo inválido ou arquivo vazio |
| `413 Payload Too Large` | Arquivo excede 10 MB |
| `500 Internal Server Error` | Falha irrecuperável no servidor |

---

### 4.2 `GET /api/documents/{id}/status`

**Novo endpoint.** Consulta o estado atual da ingestão de um documento.

| Detalhe | Valor |
|---|---|
| Método | `GET` |
| Rota | `/api/documents/{id}/status` |
| Path Parameter | `id: UUID` — ID do documento |

**Response `200 OK`:**

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "originalFilename": "relatorio.pdf",
  "status": "COMPLETED",
  "chunksCount": 42,
  "uploadedAt": "2026-06-26T10:00:00Z",
  "completedAt": "2026-06-26T10:00:05Z"
}
```

**Status Codes:**

| Status | Semântica |
|---|---|
| `200 OK` | Documento encontrado, status retornado |
| `404 Not Found` | Documento não existe |
| `500 Internal Server Error` | Falha irrecuperável no servidor |

**Estados possíveis de `status`:**

| Estado | Significado |
|---|---|
| `PENDING` | Upload concluído, ingestão ainda não iniciou |
| `PROCESSING` | Ingestão em andamento |
| `COMPLETED` | Ingestão concluída com sucesso |
| `FAILED` | Ingestão falhou |

---

### 4.3 `POST /api/chat`

Endpoint existente, **modificado** para incluir campo `sources` na resposta.

| Detalhe | Valor |
|---|---|
| Método | `POST` |
| Rota | `/api/chat` |
| Content-Type | `application/json` |

**Request Body:**

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Qual é o prazo de entrega do relatório?"
}
```

**Response `200 OK`** (estrutura modificada):

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "userMessage": {
    "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "role": "USER",
    "content": "Qual é o prazo de entrega do relatório?",
    "createdAt": "2026-06-26T10:00:00Z"
  },
  "assistantMessage": {
    "id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
    "role": "ASSISTANT",
    "content": "Conforme o documento 'relatorio.pdf', o prazo de entrega é 30/06/2026.",
    "createdAt": "2026-06-26T10:00:01Z"
  },
  "sources": [
    {
      "chunkId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "chunkContent": "O prazo de entrega do relatório final é 30 de junho de 2026...",
      "documentId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "documentName": "relatorio.pdf",
      "similarityScore": 0.92
    }
  ]
}
```

**Status Codes:**

| Status | Semântica |
|---|---|
| `200 OK` | Mensagem processada com resposta |
| `400 Bad Request` | Campo `content` vazio ou inválido |
| `404 Not Found` | `conversationId` informado não existe |
| `500 Internal Server Error` | Falha no RagService, LLM ou embedding |

---

### 4.4 `GET /api/health`

**Endpoint existente, mantido com adição do componente `embedding`.**

**Response `200 OK`:**

```json
{
  "status": "UP",
  "timestamp": "2026-06-26T10:00:00Z",
  "components": {
    "database": "UP",
    "storage": "UP",
    "embedding": "UP"
  }
}
```

**Nota:** O componente `embedding` é adicionado ao health check. O `HealthService` tenta uma chamada leve ao `EmbeddingService` (ex: `embed("health")`) para verificar disponibilidade.

**Response `503 Service Unavailable`** (qualquer componente DOWN):

```json
{
  "status": "DOWN",
  "timestamp": "2026-06-26T10:00:00Z",
  "components": {
    "database": "UP",
    "storage": "UP",
    "embedding": "DOWN"
  }
}
```

**Status Codes:**

| Status | Semântica |
|---|---|
| `200 OK` | Todos os componentes UP |
| `503 Service Unavailable` | Um ou mais componentes DOWN |

---

### 4.5 Webhook de Saída (Backend → n8n)

**Disparado por:** `N8nWebhookNotifier.notifyIngestionComplete()`  
**Método:** `POST`  
**URL:** Configurada via variável de ambiente `N8N_WEBHOOK_URL`  
**Fire-and-Forget:** O backend não aguarda resposta, não interpreta retorno, não faz retry.

**Payload:**

```json
{
  "event": "document.ingestion.completed",
  "documentId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "status": "COMPLETED",
  "chunksCount": 42,
  "timestamp": "2026-06-26T10:00:05Z"
}
```

**Regras (R4 — desacoplamento do n8n):**
- Se o n8n estiver indisponível, o backend **não falha** — apenas loga o erro.
- O payload é fixo; o n8n não pode demandar mudanças no backend para acomodar workflows.
- A URL do webhook é configurada via `application.yml` ou variável de ambiente, nunca hardcoded.

---

### 4.6 Tabela Resumo de Endpoints

| Método | Rota | Descrição | Status Codes |
|---|---|---|---|
| `POST` | `/api/documents/upload` | Upload + início da ingestão | 201, 400, 413, 500 |
| `GET` | `/api/documents/{id}/status` | Status da ingestão | 200, 404, 500 |
| `GET` | `/api/documents` | Listar documentos | 200 |
| `POST` | `/api/chat` | Chat com RAG (sources incluso) | 200, 400, 404, 500 |
| `GET` | `/api/conversations` | Listar conversas | 200 |
| `GET` | `/api/conversations/{id}` | Histórico da conversa | 200, 404 |
| `GET` | `/api/health` | Health check (+ embedding) | 200, 503 |

---

## 5. Novos Tipos TypeScript — Frontend

### 5.1 Tipo `Source` (objeto de fonte individual)

```typescript
export interface Source {
  chunkId: string;
  chunkContent: string;
  documentId: string;
  documentName: string;
  similarityScore: number;
}
```

### 5.2 `ChatMessageResponse` (modificado)

```typescript
export interface ChatMessageResponse {
  conversationId: string;
  userMessage: MessageDetail;
  assistantMessage: MessageDetail;
  sources: Source[];
}
```

### 5.3 `DocumentResponse` (modificado — adiciona status e chunksCount)

```typescript
export interface DocumentResponse {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  chunksCount?: number;
  uploadedAt: string;
}
```

### 5.4 `DocumentStatusResponse` (novo — resposta de GET /status)

```typescript
export interface DocumentStatusResponse {
  id: string;
  originalFilename: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  chunksCount?: number;
  uploadedAt: string;
  completedAt?: string;
}
```

### 5.5 Estado do Hook `useChatConversation` (evoluído)

```typescript
export interface UseChatConversationReturn {
  messages: MessageDetail[];
  sourcesMap: Record<string, Source[]>;  // NOVO: associa messageId do assistente → sources
  conversationId: string | null;
  isLoading: boolean;
  error: string | null;
  sendMessage: (content: string) => Promise<void>;
  loadConversation: (id: string) => Promise<void>;
  clearError: () => void;
  resetConversation: () => void;
}
```

**Nota:** O `sourcesMap` é um dicionário indexado pelo `messageId` da mensagem do assistente. Isso permite que o `<SourcePanel />` exiba as fontes corretas para cada resposta sem misturar fontes entre mensagens.

### 5.6 Props do Componente `<SourcePanel />`

```typescript
export interface SourcePanelProps {
  sources: Source[];
  isOpen: boolean;
  onToggle: () => void;
}
```

### 5.7 Props do Componente `<DocumentUpload />` (modificado)

```typescript
export interface DocumentUploadProps {
  onUploadComplete: (document: DocumentResponse) => void;
  documents: DocumentResponse[];       // NOVO: estado vindo do hook
  onRefresh: () => Promise<void>;       // NOVO: recarregar lista
  isOpen: boolean;
  onClose: () => void;
}
```

### 5.8 Interface do Hook `useDocumentUpload` (evoluído)

```typescript
export interface UseDocumentUploadReturn {
  documents: DocumentResponse[];
  isUploading: boolean;
  progress: number;
  error: string | null;
  uploadFile: (file: File) => Promise<void>;
  loadDocuments: () => Promise<void>;
  pollDocumentStatus: (id: string) => Promise<DocumentStatusResponse>;  // NOVO
  clearError: () => void;
}
```

### 5.9 Tipo para o Health Check

```typescript
export interface HealthResponse {
  status: 'UP' | 'DOWN' | string;
  timestamp: string;
  components: {
    database: 'UP' | 'DOWN';
    storage: 'UP' | 'DOWN';
    embedding?: 'UP' | 'DOWN';        // NOVO
  };
}
```

---

## 6. Roteiro de Validação Manual por Camada

### Etapa 1 — Backend Isolado (sem RAG, sem frontend, sem n8n)

**Ferramenta:** Postman / Insomnia / curl + logs do terminal

| Critério de Aceitação | Como Validar Manualmente | Falha se |
|---|---|---|
| Upload retorna 201 com `status: PROCESSING` | `POST /api/documents/upload` com arquivo PDF válido. Verificar response body contém `"status": "PROCESSING"` | Retornar status diferente de PROCESSING |
| `GET /documents/{id}/status` retorna estado correto | Após upload, chamar `GET /api/documents/{id}/status` repetidamente a cada 2s. Observar transição `PENDING → PROCESSING → COMPLETED` | Transição de estado inválida ou status incorreto |
| `GET /documents/{id}/status` retorna 404 para ID inexistente | Chamar endpoint com UUID aleatório. Verificar `404 Not Found` + `ErrorResponse` | Retornar 200 ou mensagem sem formato estruturado |
| `EmbeddingService` funciona isoladamente | Chamar endpoint de health e verificar `components.embedding: "UP"`. Opcional: testar via console H2 se dados persistiram | Health retorna embedding DOWN ou erro |
| RagService monta contexto com chunks | Enviar mensagem via `POST /api/chat` com pergunta relacionada a documento já ingerido. Verificar `sources` no response | sources vazio quando deveria conter chunks |
| RagService fallback sem contexto | Enviar pergunta sem documentos ingeridos. Verificar `sources: []` e resposta do LLM sem contexto | Retornar erro ou sources null |
| Erros estruturados seguem contrato `ErrorResponse` | Causar erros intencionais (tipo inválido, conversationId inexistente). Verificar formato JSON com `timestamp`, `status`, `error`, `message`, `path` | Mensagem de erro em formato livre ou status HTTP incorreto |
| `TOP_K` e `MIN_SIMILARITY` lidos de `application.yml` | Alterar `app.rag.top-k` para 1 no `application.yml`, reiniciar backend. Verificar que apenas 1 chunk é retornado em `sources` | Ainda retorna número anterior de chunks |

---

### Etapa 2 — Pipeline RAG (integração entre serviços)

**Ferramenta:** Postman + console do backend (logs) + consulta direta ao PostgreSQL (pgAdmin / psql)

| Critério de Aceitação | Como Validar Manualmente | Falha se |
|---|---|---|
| Fluxo completo: upload → parsing → chunking → embedding → persistência no pgvector | Fazer upload de PDF de 3 páginas. Aguardar 10s. Consultar tabela `document_chunks` no PostgreSQL via psql/pgAdmin. Verificar registros com `chunk_index` sequencial e `embedding` preenchido | Chunks não persistidos ou vetores com valor null |
| Webhook ao n8n é disparado após ingestão | Observar logs do backend: `"N8n webhook notified successfully"`. Se n8n estiver rodando, verificar se o webhook receiver capturou o payload | Nenhuma linha de log de notificação |
| Webhook é fire-and-forget (n8n offline não quebra ingestão) | Parar n8n. Fazer upload de documento. Verificar que ingestão completa (status COMPLETED) mesmo com log de erro `"Failed to notify n8n"` | Ingestão não completa ou status fica PROCESSING |
| Chunking respeita tamanho e overlap configurados | Configurar `app.rag.chunk.max-size: 100` e `overlap: 20`. Upload de documento. Verificar `length(chunk.content)` no banco respeita limite | Chunks com tamanho muito acima do configurado |
| Parsing funciona para PDF, TXT e DOCX | Submeter cada tipo. Verificar `DocumentChunk.content` populado com texto legível no banco | Parsing retorna vazio ou erro |
| Documento em PROCESSING não pode ser reingestionado | Fazer upload, aguardar status PROCESSING (antes de COMPLETED). Chamar `POST /upload` novamente com mesmo arquivo → verificar 409 Conflict | Segunda chamada retorna 200 ou 201 |

---

### Etapa 3 — Frontend + Backend (sem n8n)

**Ferramenta:** Navegador (Chrome DevTools) + backend rodando

| Critério de Aceitação | Como Validar Manualmente | Falha se |
|---|---|---|
| Upload mostra badge PROCESSING imediatamente | Abrir modal de upload, enviar arquivo. Observar badge/ tag ao lado do documento na lista | Badge mostra COMPLETED ou nenhum feedback |
| Status evolui PROCESSING → COMPLETED automaticamente | Manter modal de upload aberto. Observar badge mudar de azul (PROCESSING) para verde (COMPLETED) em até 30s | Badge não atualiza ou fica PROCESSING para sempre |
| Painel de fontes aparece na resposta do assistente | Enviar pergunta sobre documento ingerido. Na bolha de resposta do assistente, clicar em "Ver fontes". Verificar painel expande com chunks e scores | Painel não abre ou não aparecem fontes |
| Fontes vazias não exibem botão "Ver fontes" | Enviar pergunta sem relação com documentos. Verificar que não há botão "Ver fontes" na bolha do assistente | Botão aparece mesmo sem fontes |
| Erro da API exibe notificação amigável | Parar backend. Enviar mensagem. Verificar `message.error()` ou notificação com texto legível (não raw JSON) | Exibe stack trace ou JSON cru |
| Drag-and-drop com progresso | Arrastar PDF para área de upload. Verificar barra de progresso visível durante envio | Progresso não aparece ou upload falha silenciosamente |
| Health check visível na interface | Implementar badge de status no header. Verificar ícone verde/vermelho conforme `GET /api/health` | Badge não reflete estado real |

---

### Etapa 4 — n8n + Backend (validação de contrato)

**Ferramenta:** n8n editor UI + backend + Postman

| Critério de Aceitação | Como Validar Manualmente | Falha se |
|---|---|---|
| Webhook POST chega ao n8n com payload correto | Configurar n8n com "Webhook" node + "Wait" node. Fazer upload de documento. Verificar no n8n execution log o payload JSON completo | Payload chega com campos faltando ou tipos incorretos |
| Backend não falha se n8n está offline | Parar n8n (ou usar URL inválida em `N8N_WEBHOOK_URL`). Upload de documento. Verificar ingestão completa e log `"Failed to notify n8n"` | Ingestão falha ou backend lança exceção |
| n8n consegue consultar status do documento | No workflow n8n, após receber webhook, adicionar "HTTP Request" node `GET /api/documents/{id}/status`. Executar. Verificar resposta 200 com dados | Backend precisa de mudança para acomodar n8n |
| Múltiplas ingestões simultâneas funcionam | Disparar 3 uploads em sequência rápida (via Postman ou curl). Aguardar todos completarem. Verificar que cada documento tem seus próprios chunks no banco | Chunks de documentos diferentes misturados ou algum documento fica PROCESSING indefinidamente |

---

## 7. Mapa de Violações Arquiteturais

| # | Violação | Sintoma no Código | Por que é Problemático | Como Corrigir |
|---|---|---|---|---|
| 1 | **Serviço com dupla responsabilidade (R1)** | `DocumentIngestionService` também faz retrieval, ou `RagService` também faz ingestão | Quebra o princípio da separação de fases RAG. Dificulta teste, manutenção e escalabilidade independente. Um bug na ingestão pode afetar retrieval e vice-versa. | Extrair RetrievalService com responsabilidade única. Manter `DocumentIngestionService` focado apenas em ingestão (parsing → chunking → embedding → persistir). |
| 2 | **EmbeddingService acoplado a entidade de domínio (R2)** | `EmbeddingService.embed(Document document)` ou método que recebe/acessa `Document`, `Chunk`, `Conversation` | Viola o princípio de agnosticismo de domínio. Impede reúso do `EmbeddingService` em outros contextos. Cria dependência circular se embedding precisar de entidade que depende de embedding. | Assinar método como `embed(String text): List<Float>`. Toda conversão texto → entidade deve ocorrer antes ou depois, nunca dentro do `EmbeddingService`. |
| 3 | **RagService acessando banco de dados diretamente (R3)** | `RagService` possui `@Autowired DocumentChunkRepository` ou faz queries JPA/JPQL | Viola a delegação pura. O `RagService` vira um "deus service" que faz orquestração E acesso a dados. Dificulta testar lógica de retrieval isoladamente. | `RagService` deve receber uma interface `ChunkRetrievalPort` com método `findSimilar(List<Float>, int, double)`. A implementação concreta fica no `DocumentChunkRepository`. |
| 4 | **n8n exigindo mudanças no backend (R4)** | Workflow n8n espera campo específico que não existe; ou backend precisa adicionar endpoint porque n8n "precisa" | Acoplamento invertido: o orquestrador externo dita o contrato da API. Perde-se o benefício do desacoplamento. n8n vira dependência crítica e mudanças no n8n quebram o backend. | O contrato do backend é fixo e autossuficiente. O n8n deve adaptar seus workflows ao contrato existente, não o contrário. Mudanças no payload do webhook exigem bump de versão do contrato. |
| 5 | **Hook controlando estado de UI (R5)** | `useChatConversation` retorna `isSourcePanelOpen: boolean` ou hook gerencia visibilidade de componentes | Vazamento de responsabilidade. A camada lógica (hook) passa a saber de detalhes de apresentação. Quebra a separação hook/UI. Dificulta reúso do hook em diferentes layouts. | Estado `isOpen` do `<SourcePanel />` deve ser gerenciado via `useState` dentro do componente pai visual (ex: `App.tsx` ou `ChatLayout.tsx`), nunca no hook. |
| 6 | **Shape de resposta definido primeiro no componente (R6)** | `MessageBubble.tsx` acessa `message.sources` antes do tipo `ChatMessageResponse` ser atualizado | Viola o princípio "contratos antes do código". O tipo TypeScript deve ser atualizado primeiro, depois o componente. Caso contrário, erros de compilação ou undefined em runtime. | Fluxo correto: 1) atualizar DTO Java `ChatMessageResponse` 2) atualizar `ChatMessageResponse` em `frontend/src/api/types.ts` 3) atualizar hook `useChatConversation` 4) atualizar componentes visuais. |
| 7 | **Erro semântico de HTTP Status (R7)** | 200 retornado para documento não encontrado, ou 500 para validação de entrada | Viola o contrato de erros. Dificulta debugging, monitoramento e consumo da API. Clientes (frontend, n8n) não conseguem diferenciar tipos de erro. | 400 para entrada inválida, 404 para recurso inexistente, 409 para conflito de estado (ex: reingestão), 500 para falha irrecuperável. Mapeamento centralizado no `GlobalExceptionHandler`. |
| 8 | **TOP_K e MIN_SIMILARITY hardcoded (R8)** | `RagService.java` contém `private static final int TOP_K = 5;` ou constante similar | Impede ajuste sem recompilar. Diferentes ambientes (dev, staging, prod) podem precisar de valores diferentes. Viola externalização de configuração. | Mover para `application.yml` como `app.rag.top-k` e `app.rag.min-similarity`. Injetar via `@Value` no `RagService`. |
| 9 | **Ingestão síncrona bloqueando HTTP response** | `DocumentController.uploadDocument()` chama `ingestDocument()` diretamente sem `@Async` | Usuário fica bloqueado até a ingestão completa (potencialmente minutos). Timeout HTTP. Experiência do usuário degradada. | `uploadDocument()` retorna imediatamente com status PROCESSING. `ingestDocument()` é marcado com `@Async` e executa em thread separada. |
| 10 | **EmbeddingService persistindo vetores** | `EmbeddingService` salva embedding no banco de dados | Viola o princípio de agnosticismo de domínio (R2). EmbeddingService não pode conhecer persistência. | EmbeddingService retorna `List<Float>`. Quem persiste é o `DocumentChunkRepository`, chamado pelo `DocumentIngestionService`. |

---

## 8. Template do AGENTS.md — Parte 2

```markdown
# AGENTS.md — Parte 2: Pipeline RAG, n8n e Interface com Fontes

## Módulo A — DocumentIngestionService

**Escopo:**
Serviço responsável por orquestrar o pipeline de ingestão de documentos:
parsing (PDF/TXT/DOCX) → chunking com overlap configurável → delegação ao
EmbeddingService → persistência via DocumentChunkRepository → notificação
ao n8n via webhook fire-and-forget.

**Prompt base:**
Implementar `DocumentIngestionService` com método `ingestDocument(UUID documentId)`
marcado como `@Async`. O método deve:
1. Buscar `Document` pelo ID (lançar `DocumentNotFoundException` se não existir)
2. Atualizar status para `PROCESSING`
3. Fazer parsing do arquivo baseado no `contentType`
4. Dividir em chunks com tamanho e overlap configuráveis
5. Para cada chunk, chamar `EmbeddingService.embed(text)` e persistir
   `DocumentChunk` com o vetor retornado
6. Atualizar status para `COMPLETED` (ou `FAILED` em caso de erro)
7. Disparar notificação ao n8n via `N8nWebhookNotifier`

**Verificação manual:**
- Upload retorna 201 com status PROCESSING — confirmar via Postman
- Após ingestão completa, `GET /documents/{id}/status` retorna COMPLETED
- Observar log: `"N8n webhook notified successfully"`

**Premissas arquiteturais comunicadas:**
- R1: Ingestão e Retrieval são pipelines separados (classes, pacotes e responsabilidades distintas)
- R2: EmbeddingService é agnóstico de domínio (String → List<Float>)
- R4: Backend dispara webhook sem depender de resposta do n8n

---

## Módulo B — EmbeddingService

**Escopo:**
Serviço puro que encapsula a chamada à API de embedding (OpenAI, Hugging Face,
Ollama ou similar). Recebe `String`, retorna `List<Float>`. Sem conhecimento
de entidades de domínio ou persistência.

**Prompt base:**
Criar `EmbeddingService` com método público `List<Float> embed(String text)`.
A implementação usa RestTemplate/WebClient para chamar API externa configurada
via application.yml (url, api-key, model, dimensions). Incluir tratamento de
timeout, retry configurável e `EmbeddingServiceException` para erros.

**Verificação manual:**
- Health check (`GET /api/health`) exibe `components.embedding: "UP"` se embedding funcional
- Causar falha na API de embedding (desligar serviço) → health retorna `embedding: "DOWN"`

**Premissas arquiteturais:**
- R2: Agnóstico de domínio — violação se referenciar Document, Chunk, Conversation
- R8: URL, api-key, modelo e dimensão são configurações externas

---

## Módulo C — RagService

**Escopo:**
Orquestrador puro do retrieval augmentado. Não acessa banco de dados nem chama
embedding diretamente — delega para `EmbeddingService` (via port) e
`DocumentChunkRepository` (via `ChunkRetrievalPort`). Monta o prompt com
contexto e chama o LLM.

**Prompt base:**
Implementar `RagService` com método `RagContext enrichPrompt(String question,
String conversationHistory)`. Internamente deve:
1. Chamar `EmbeddingService.embed(question)`
2. Chamar `ChunkRetrievalPort.findSimilar(embedding, topK, minSimilarity)`
3. Montar prompt completo com contexto dos chunks
4. Retornar `RagContext` com prompt e `List<ChunkResult>`

**Verificação manual:**
- Enviar pergunta no chat → response contém `sources` com chunks relevantes
- Se nenhum documento existe, `sources: []` e resposta sem contexto
- Alterar `app.rag.top-k` para 1 em application.yml → apenas 1 chunk retornado

**Premissas arquiteturais:**
- R3: Delegação pura — não acessa banco nem chama embedding diretamente
- R8: TOP_K e MIN_SIMILARITY são externalizados

---

## Módulo D — DocumentChunkRepository

**Escopo:**
Repository JPA com native query pgvector para busca por similaridade.
Persiste `DocumentChunk` com campo `embedding` do tipo pgvector.

**Prompt base:**
Criar entidade JPA `DocumentChunk` com campos (id UUID, document FK,
content TEXT, chunk_index INT, embedding vector(1536), created_at TIMESTAMP).
Implementar query nativa pgvector: `ORDER BY embedding <=> :embedding`
com filtro `>= :minSimilarity` e `LIMIT :topK`.
Usar projeção `ChunkResultProjection` que implementa `ChunkResult`.

**Verificação manual:**
- Após upload de PDF, consultar `document_chunks` no PostgreSQL via psql
- Verificar `embedding` preenchido (não nulo) com array de floats
- Verificar `chunk_index` sequencial e `content` com texto legível

**Premissas arquiteturais:**
- R3: `RagService` não acessa este repositório diretamente — usa `ChunkRetrievalPort`

---

## Módulo E — ChatMessageResponse com Sources

**Escopo:**
Evolução do DTO de resposta do chat para incluir array de sources.
Atualização do tipo TypeScript correspondente.

**Prompt base:**
Adicionar campo `List<SourceDTO> sources` ao `ChatMessageResponse`.
Criar `SourceDTO` com campos (chunkId, chunkContent, documentId, documentName,
similarityScore). Atualizar `ChatService.processMessage()` para integrar
`RagService` e preencher sources na resposta.
No frontend, atualizar interface `ChatMessageResponse` em types.ts.

**Verificação manual:**
- Enviar pergunta via POST /api/chat → response contém `sources` array
- Enviar pergunta sem documentos ingeridos → `sources: []` (nunca null)
- Verificar no DevTools Network tab que o payload respeita o contrato

**Premissas arquiteturais:**
- R6: Contratos antes do código (types primeiro, componentes depois)

---

## Módulo F — Componente SourcePanel

**Escopo:**
Componente React colapsável que exibe as fontes (chunks) usadas para gerar
a resposta do assistente.

**Prompt base:**
Criar `<SourcePanel>` com props `{ sources: Source[]; isOpen: boolean;
onToggle: () => void }`. Exibir lista de chunks com:
- Nome do documento de origem
- Score de similaridade (formatado como porcentagem)
- Texto do chunk (truncado com "Expandir" link)
- Botão "Ver fontes" no `<MessageBubble>` do assistente

**Verificação manual:**
- Enviar pergunta → clicar "Ver fontes" → painel expande com chunks e scores
- Enviar pergunta sem fontes → botão "Ver fontes" não aparece
- Verificar que fechar/abrir o painel não recarrega a página nem faz nova requisição

**Premissas arquiteturais:**
- R5: Separação visual/lógica — hook não gerencia estado de UI

---

## Módulo G — DocumentStatus API + Frontend Polling

**Escopo:**
Endpoint GET /api/documents/{id}/status e polling no frontend para
acompanhar progresso da ingestão.

**Prompt base:**
Criar `GET /api/documents/{id}/status` retornando `DocumentStatusResponse`
com status, chunksCount, completedAt. No frontend, estender
`UseDocumentUploadReturn` com método `pollDocumentStatus(id)` que faz
polling a cada 2s até status != PROCESSING.
Exibir badge de status no `DocumentUpload` (antd Tag: PROCESSING = azul,
COMPLETED = verde, FAILED = vermelho).

**Verificação manual:**
- Upload de documento → badge PROCESSING azul → após ~5s badge COMPLETED verde
- Chamar `GET /api/documents/{id}/status` com UUID inválido → 404
- Modal de upload exibe status de cada documento na lista

**Premissas arquiteturais:**
- R7: Erros como contrato — status codes HTTP corretos
- R4: Frontend faz polling para status; n8n recebe notificação passiva

---

## Módulo H — N8nWebhookNotifier

**Escopo:**
Notificador HTTP que dispara evento de ingestão completa para o n8n.
Fire-and-forget: sem aguardar resposta, sem retry, sem dependência.

**Prompt base:**
Criar interface `N8nWebhookNotifier` com método
`void notifyIngestionComplete(UUID documentId, DocumentStatus status,
int chunksCount)`. Implementar com RestTemplate configurado com
timeout curto (2s). URL lida de `app.n8n.webhook-url`. Logar erro
se n8n estiver indisponível sem interromper fluxo.

**Verificação manual:**
- Upload de documento com n8n rodando → log: `"N8n webhook notified successfully"`
- Upload com n8n parado → log: `"Failed to notify n8n"`, mas ingestão conclui (status COMPLETED)
- No n8n, verificar execution com payload JSON completo

**Premissas arquiteturais:**
- R4: Desacoplamento do n8n — backend não depende de resposta do n8n
- R4: n8n não dita contrato do backend
```

---

## Apêndice A — application.yml (Parte 2 — adições)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/assistant
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true

app:
  storage:
    upload-dir: ./uploads
  cors:
    allowed-origins: http://localhost:5173

  rag:
    top-k: 5
    min-similarity: 0.7
    chunk:
      max-size: 512
      overlap: 64

  embedding:
    provider: openai
    url: https://api.openai.com/v1/embeddings
    api-key: ${EMBEDDING_API_KEY}
    model: text-embedding-3-small
    dimensions: 1536

  n8n:
    webhook-url: ${N8N_WEBHOOK_URL}

server:
  port: 8080
```

---

## Apêndice B — Dependências Maven (adições ao pom.xml)

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.4.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

---

## Apêndice C — Script SQL para pgvector

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(1536),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

---

## Apêndice D — Seqüência de Implementação Sugerida

| Ordem | Módulo | Depende de | Como Verificar |
|---|---|---|---|
| 1 | Infraestrutura: PostgreSQL + pgvector + dependências Maven | N/A | `mvn compile` bem-sucedido |
| 2 | Entidade `DocumentChunk` + `DocumentStatus` + migração `Document` | #1 | Subir app, verificar tabelas criadas no PostgreSQL |
| 3 | `DocumentChunkRepository` com query pgvector | #2 | Inserir chunk manualmente no banco, chamar query via psql |
| 4 | `EmbeddingService` (agnóstico de domínio) | #1 | `GET /api/health` → `embedding: "UP"` |
| 5 | `DocumentIngestionService` (parsing → chunking → embed → persistir) | #2, #3, #4 | Upload PDF → consultar `document_chunks` populado |
| 6 | `N8nWebhookNotifier` | #1 | Upload → log `"N8n webhook notified"` |
| 7 | `ChunkRetrievalPort` + `RagService` | #3, #4 | `POST /api/chat` → response com `sources` |
| 8 | `LlmPort` + integração LLM | #7 | Chat retorna resposta contextualizada |
| 9 | `ChatMessageResponse.sources` + `GET /documents/{id}/status` | #5, #8 | Testar via Postman |
| 10 | Tipos TypeScript atualizados | #9 | `tsc --noEmit` compila sem erros |
| 11 | Hook `useChatConversation` evoluído + `useDocumentUpload` polling | #10 | Upload via UI → badge evolve PROCESSING → COMPLETED |
| 12 | Componente `<SourcePanel />` + badges de status no upload | #11 | Navegador: chat com fontes visíveis |
| 13 | Validação Etapa 4 (n8n + backend) | #6 | Webhook recebido no n8n editor |
| 14 | Atualizar `AGENTS.md` com templates dos módulos | Todos | Documento revisado |
