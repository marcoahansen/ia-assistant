# Especificação de Arquitetura — Backend (Parte 1)

> **Projeto:** Assistente Inteligente Full-Stack  
> **Stack:** Java 21 · Spring Boot 3.x · Spring Web · Spring Data JPA · H2  
> **Escopo:** Sprint 1 — API REST funcional (chat, upload, histórico, health check)  
> **Versão:** 1.0.0  
> **Status:** Draft — Spec-Driven Development (SDD)

---

## 1. Visão Geral

O backend é a **única camada de orquestração** do sistema. Na Parte 1, ele expõe uma API REST que gerencia conversas, mensagens e documentos. Persistência em **H2 in-memory** — sem banco externo. As respostas de chat são **stubadas** (eco ou resposta fixa) — a integração com IA será adicionada em partes futuras.

### 1.1 Objetivos da Parte 1

| Objetivo | Critério de Aceite |
|---|---|
| API REST funcional | Todos os endpoints retornam respostas conforme contrato |
| Isolamento de domínio | Controllers sem regra de negócio; Services testáveis isoladamente |
| Upload de documentos | Aceita PDF, TXT, DOCX com validação de tipo e tamanho |
| Histórico de conversas | Mensagens persistidas no H2 e recuperáveis por `conversationId` |
| Health check | Endpoint monitorável por ferramentas de observabilidade |

### 1.2 Princípios Arquiteturais

1. **Clean Code** — nomes expressivos, funções pequenas, SRP em cada classe.
2. **Isolamento de Domínio** — regras de negócio exclusivamente em Services e entidades de domínio.
3. **Inversão de Dependência** — Controllers dependem de interfaces (ports); implementações injetadas via Spring DI.
4. **Contratos explícitos** — DTOs de request/response separados das entidades JPA.
5. **Fail Fast** — validação na borda (Controller) + validação de negócio no Service.

---

## 2. Estrutura de Pacotes

```
com.assistant
├── Application.java
├── config/
│   ├── WebConfig.java              # CORS, content negotiation
│   ├── StorageConfig.java          # Path de upload, limites
│   └── OpenApiConfig.java          # Swagger/OpenAPI (opcional Parte 1)
├── controller/                     # ADAPTERS IN — apenas HTTP
│   ├── ChatController.java
│   ├── ConversationController.java
│   ├── DocumentController.java
│   └── HealthController.java
├── dto/
│   ├── request/
│   │   ├── ChatMessageRequest.java
│   │   └── DocumentUploadMetadata.java
│   └── response/
│       ├── ChatMessageResponse.java
│       ├── ConversationResponse.java
│       ├── DocumentResponse.java
│       ├── DocumentListResponse.java
│       ├── ErrorResponse.java
│       └── HealthResponse.java
├── domain/                         # CORE — entidades e regras puras
│   ├── model/
│   │   ├── Conversation.java
│   │   ├── Message.java
│   │   ├── MessageRole.java        # USER | ASSISTANT
│   │   └── Document.java
│   └── exception/
│       ├── ConversationNotFoundException.java
│       ├── DocumentNotFoundException.java
│       ├── InvalidFileTypeException.java
│       └── FileSizeExceededException.java
├── service/                        # USE CASES — orquestração e regras de negócio
│   ├── ChatService.java
│   ├── ConversationService.java
│   ├── DocumentService.java
│   └── HealthService.java
├── repository/                     # ADAPTERS OUT — persistência
│   ├── ConversationRepository.java
│   ├── MessageRepository.java
│   └── DocumentRepository.java
├── mapper/                         # Conversão Entity ↔ DTO
│   ├── ConversationMapper.java
│   ├── MessageMapper.java
│   └── DocumentMapper.java
└── storage/                        # ADAPTERS OUT — filesystem
    ├── FileStoragePort.java        # Interface (port)
    └── LocalFileStorageAdapter.java
```

### 2.1 Regras de Camada

| Camada | Responsabilidade | Proibido |
|---|---|---|
| **Controller** | Receber HTTP, validar DTO (`@Valid`), delegar ao Service, mapear status HTTP | Lógica de negócio, acesso direto a Repository, transformações complexas |
| **Service** | Regras de negócio, orquestração, transações (`@Transactional`) | Annotations HTTP, conhecimento de Request/Response DTOs do Controller |
| **Repository** | CRUD e queries de persistência | Regras de negócio |
| **Domain** | Entidades, enums, exceções de domínio | Dependências de framework (Spring, JPA annotations mínimas nas entidades) |
| **Mapper** | Conversão entre camadas | Lógica de negócio |
| **Storage Port/Adapter** | Abstração de armazenamento de arquivos | Lógica de negócio de documentos |

### 2.2 Injeção de Dependência

```java
// Controller — depende apenas de Service (interface implícita via classe concreta na Parte 1)
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService; // injetado pelo Spring

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.processMessage(request));
    }
}

// Service — depende de Repository e Storage Port
@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        // TODA a lógica de negócio vive aqui
    }
}
```

**Garantias de separação via DI:**

- Spring instancia e injeta dependências pelo construtor (`@RequiredArgsConstructor` + `final`).
- Services recebem **interfaces de repositório** (Spring Data) e **ports** (`FileStoragePort`), nunca implementações concretas instanciadas manualmente.
- Controllers são `@WebMvcTest`-testáveis mockando apenas o Service.
- Services são testáveis via testes de integração (`@SpringBootTest`) sem acoplar implementações concretas.

---

## 3. Modelo de Domínio

### 3.1 Entidades

#### Conversation

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Identificador único (PK) |
| `title` | `String` | Título derivado da primeira mensagem (max 100 chars) |
| `createdAt` | `Instant` | Data de criação |
| `updatedAt` | `Instant` | Data da última mensagem |

#### Message

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Identificador único (PK) |
| `conversation` | `Conversation` | FK — conversa pai |
| `role` | `MessageRole` | `USER` ou `ASSISTANT` |
| `content` | `String` | Texto da mensagem (max 10.000 chars) |
| `createdAt` | `Instant` | Timestamp de envio |

#### Document

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Identificador único (PK) |
| `originalFilename` | `String` | Nome original do arquivo |
| `storedFilename` | `String` | Nome no filesystem (UUID + extensão) |
| `contentType` | `String` | MIME type |
| `sizeBytes` | `Long` | Tamanho em bytes |
| `uploadedAt` | `Instant` | Data do upload |

### 3.2 Regras de Negócio (Service Layer)

| Regra | Service | Descrição |
|---|---|---|
| RN-01 | `ChatService` | Se `conversationId` é nulo, criar nova conversa |
| RN-02 | `ChatService` | Persistir mensagem USER antes de gerar resposta |
| RN-03 | `ChatService` | Resposta ASSISTANT é stub: `"Recebi sua mensagem: {content}"` |
| RN-04 | `ChatService` | Atualizar `updatedAt` da conversa a cada mensagem |
| RN-05 | `ConversationService` | Lançar `ConversationNotFoundException` se ID inexistente |
| RN-06 | `DocumentService` | Aceitar apenas MIME types: `application/pdf`, `text/plain`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| RN-07 | `DocumentService` | Tamanho máximo: **10 MB** por arquivo |
| RN-08 | `DocumentService` | Nome de arquivo sanitizado (remover path traversal) |
| RN-09 | `HealthService` | Retornar status `UP` se DB e storage estão acessíveis |

---

## 4. Contratos de API

**Base URL:** `/api`  
**Content-Type padrão:** `application/json` (exceto upload: `multipart/form-data`)  
**Formato de datas:** ISO-8601 UTC (`2026-06-25T14:30:00Z`)  
**Identificadores:** UUID v4

### 4.1 POST `/api/chat`

Envia uma mensagem do usuário e recebe a resposta do assistente.

**Request Body:**

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Olá, como funciona o upload de documentos?"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `conversationId` | `UUID` | Não | Se omitido, cria nova conversa |
| `content` | `String` | Sim | `@NotBlank`, `@Size(max = 10000)` |

**Response `200 OK`:**

```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "userMessage": {
    "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "role": "USER",
    "content": "Olá, como funciona o upload de documentos?",
    "createdAt": "2026-06-25T14:30:00Z"
  },
  "assistantMessage": {
    "id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
    "role": "ASSISTANT",
    "content": "Recebi sua mensagem: Olá, como funciona o upload de documentos?",
    "createdAt": "2026-06-25T14:30:01Z"
  }
}
```

**Response `400 Bad Request`:** validação falhou (campo `content` vazio).

**Response `404 Not Found`:** `conversationId` informado não existe.

---

### 4.2 GET `/api/conversations/{id}`

Retorna o histórico completo de uma conversa.

**Path Parameter:**

| Param | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | ID da conversa |

**Response `200 OK`:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Olá, como funciona o upload...",
  "createdAt": "2026-06-25T14:30:00Z",
  "updatedAt": "2026-06-25T14:35:00Z",
  "messages": [
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "role": "USER",
      "content": "Olá, como funciona o upload de documentos?",
      "createdAt": "2026-06-25T14:30:00Z"
    },
    {
      "id": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
      "role": "ASSISTANT",
      "content": "Recebi sua mensagem: Olá, como funciona o upload de documentos?",
      "createdAt": "2026-06-25T14:30:01Z"
    }
  ]
}
```

**Response `404 Not Found`:**

```json
{
  "timestamp": "2026-06-25T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Conversation not found: 550e8400-e29b-41d4-a716-446655440000",
  "path": "/api/conversations/550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 4.3 POST `/api/documents/upload`

Upload de documento via `multipart/form-data`.

**Request:**

| Part | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `file` | `MultipartFile` | Sim | Arquivo PDF, TXT ou DOCX |

**Response `201 Created`:**

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "originalFilename": "relatorio.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 204800,
  "uploadedAt": "2026-06-25T14:30:00Z"
}
```

**Response `400 Bad Request`:** tipo de arquivo inválido ou arquivo vazio.

**Response `413 Payload Too Large`:** arquivo excede 10 MB.

---

### 4.4 GET `/api/documents`

Lista todos os documentos enviados, ordenados por `uploadedAt` descendente.

**Response `200 OK`:**

```json
{
  "documents": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "originalFilename": "relatorio.pdf",
      "contentType": "application/pdf",
      "sizeBytes": 204800,
      "uploadedAt": "2026-06-25T14:30:00Z"
    },
    {
      "id": "8d0e7780-8536-51ef-a05c-f18gd2g01bf8",
      "originalFilename": "notas.txt",
      "contentType": "text/plain",
      "sizeBytes": 1024,
      "uploadedAt": "2026-06-25T13:00:00Z"
    }
  ],
  "total": 2
}
```

---

### 4.5 GET `/api/health`

Health check para monitoramento.

**Response `200 OK`:**

```json
{
  "status": "UP",
  "timestamp": "2026-06-25T14:30:00Z",
  "components": {
    "database": "UP",
    "storage": "UP"
  }
}
```

**Response `503 Service Unavailable`:**

```json
{
  "status": "DOWN",
  "timestamp": "2026-06-25T14:30:00Z",
  "components": {
    "database": "DOWN",
    "storage": "UP"
  }
}
```

---

## 5. Tratamento de Erros

### 5.1 Handler Global

Implementar `@RestControllerAdvice` com mapeamento centralizado:

| Exceção | HTTP Status | Código |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Validação de DTO |
| `ConversationNotFoundException` | 404 | Conversa inexistente |
| `DocumentNotFoundException` | 404 | Documento inexistente |
| `InvalidFileTypeException` | 400 | Tipo de arquivo não permitido |
| `FileSizeExceededException` | 413 | Arquivo muito grande |
| `Exception` (genérica) | 500 | Erro interno (sem expor stack trace) |

### 5.2 Formato Padrão de Erro

```json
{
  "timestamp": "2026-06-25T14:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "content must not be blank",
  "path": "/api/chat"
}
```

---

## 6. Configuração

### 6.1 application.yml

Banco de dados: **H2 in-memory** (`jdbc:h2:mem:assistant`). Schema gerenciado pelo Hibernate (`ddl-auto: create-drop`).

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:assistant
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

app:
  storage:
    upload-dir: ./uploads
  cors:
    allowed-origins: http://localhost:5173

server:
  port: 8080
```

### 6.2 CORS

Permitir origem do frontend React (`http://localhost:5173`) para métodos `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` com headers `Content-Type` e `Authorization`.

---

## 7. Estratégia de Testes

### 7.1 Stack de Testes

| Ferramenta | Uso |
|---|---|
| **JUnit 5** | Framework de testes |
| **Mockito** | Mocks em testes de controller (`@MockBean`) |
| **Spring Boot Test** | Testes de integração com `@SpringBootTest` |
| **MockMvc** | Testes de Controller sem subir servidor |
| **H2** | Mesmo banco in-memory usado em runtime e nos testes de integração |

### 7.2 Pirâmide de Testes e Cobertura Esperada

```
         ┌─────────────┐
         │  E2E (few)  │  MockMvc — fluxo completo HTTP
         ├─────────────┤
         │ Integration │  @SpringBootTest + H2 — Service + Repository
         └─────────────┘
```

**Meta de cobertura:** ≥ 80% em fluxos de integração e controller.

### 7.3 Testes de Controller — MockMvc

**Arquivo:** `ChatControllerTest.java`

```java
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ChatService chatService;

    @Test
    void shouldReturn200WhenMessageIsValid() throws Exception { /* ... */ }

    @Test
    void shouldReturn400WhenContentIsBlank() throws Exception { /* ... */ }
}
```

**Regra:** Controllers testados **apenas** com `@WebMvcTest` + `@MockBean` do Service. Nunca levantar contexto completo para testar Controller.

### 7.4 Testes de Integração

**Arquivo:** `ChatIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldCreateConversationAndReturnMessages() throws Exception {
        // POST /api/chat sem conversationId → 200
        // GET /api/conversations/{id} → histórico com 2 mensagens
    }
}
```

**Cenários a validar nos testes de integração:**

| Service | Cenários |
|---|---|
| `ChatService` | Nova conversa, conversa existente, conversa não encontrada, resposta stub |
| `ConversationService` | Busca com mensagens ordenadas, conversa inexistente |
| `DocumentService` | Upload válido (PDF/TXT/DOCX), tipo inválido, tamanho excedido, sanitização de nome |
| `HealthService` | Tudo UP, database DOWN, storage DOWN |

### 7.5 Convenções de Nomenclatura

```
should[ExpectedBehavior]When[Condition]
```

Exemplos:
- `shouldReturn404WhenConversationDoesNotExist`
- `shouldRejectUploadWhenFileTypeIsInvalid`
- `shouldCreateNewConversationWhenIdIsNull`

---

## 8. Checklist de Implementação (Sprint 1)

- [ ] Projeto Spring Boot inicializado (Java 21, Spring Boot 3.x)
- [ ] H2 in-memory configurado (`jdbc:h2:mem:assistant`, console em `/h2-console`)
- [ ] Entidades JPA: `Conversation`, `Message`, `Document`
- [ ] Repositories Spring Data
- [ ] Services com regras de negócio (RN-01 a RN-09)
- [ ] Controllers delegando 100% ao Service
- [ ] DTOs de request/response com Bean Validation
- [ ] Mappers Entity ↔ DTO
- [ ] `FileStoragePort` + `LocalFileStorageAdapter`
- [ ] `@RestControllerAdvice` para erros
- [ ] CORS configurado para frontend
- [ ] Testes Controller (MockMvc + MockBean)
- [ ] Testes de integração com H2 (fluxo chat + upload)
- [ ] Cobertura ≥ 80% em fluxos de integração e controller

---

## 9. Decisões Arquiteturais Registradas (ADR)

| # | Decisão | Justificativa |
|---|---|---|
| ADR-01 | Resposta de chat stubada na Parte 1 | IA será integrada em parte futura; foco agora é infraestrutura |
| ADR-02 | H2 in-memory na Parte 1 | Zero setup externo; suficiente para chat, histórico e metadados de upload |
| ADR-03 | Storage local via Port/Adapter | Permite trocar para S3/MinIO sem alterar Service |
| ADR-04 | UUID como PK | Evita colisões; compatível com geração no frontend |
| ADR-05 | DTOs separados de entidades | Evita vazamento de schema JPA na API |
