# Plano de Implementação — Backend (Parte 1)

> **Referência:** [01-backend-architecture.md](../specs/01-backend-architecture.md)  
> **Sprint:** 1  
> **Stack:** Java 21 · Spring Boot 3.x · Spring Data JPA · H2  
> **Objetivo:** API REST funcional para chat (stub), histórico, upload e health check

---

## Visão geral

| Fase | Foco | Entregável |
|---|---|---|
| 0 | Bootstrap | Projeto compila e sobe na porta 8080 |
| 1 | Domínio e persistência | Entidades, repositórios e schema H2 |
| 2 | Storage | Upload local via Port/Adapter |
| 3 | Services | Regras RN-01 a RN-09 implementadas |
| 4 | API HTTP | Controllers, DTOs, mappers e contratos |
| 5 | Infra transversal | Erros globais, CORS, config |
| 6 | Validação | Testes de integração + controller (opcional) |

**Dependência externa:** nenhuma — o backend pode ser desenvolvido e testado isoladamente (Postman/curl/MockMvc).

**Ordem recomendada:** fases 0 → 6 sequencialmente. Fases 3 e 4 podem avançar em paralelo por endpoint, mas Services devem existir antes dos Controllers.

---

## Fase 0 — Bootstrap do projeto

**Duração estimada:** 0,5 dia

### Tarefas

- [ ] Criar projeto Spring Boot 3.x (Java 21) com dependências:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-validation`
  - `h2` (runtime) — banco in-memory da Parte 1
  - `lombok` (opcional, alinhado à spec)
  - `spring-boot-starter-test`
- [ ] Definir pacote base `com.assistant`
- [ ] Criar `Application.java`
- [ ] Configurar `application.yml` com H2 in-memory (`jdbc:h2:mem:assistant`), H2 console, multipart 10 MB, upload-dir, CORS, porta 8080
- [ ] Verificar startup: `./mvnw spring-boot:run` sem erros

### Critério de aceite

- Aplicação sobe em `http://localhost:8080`
- H2 console acessível em `/h2-console` (JDBC URL: `jdbc:h2:mem:assistant`)

---

## Fase 1 — Domínio e persistência

**Duração estimada:** 1 dia

### Tarefas

- [ ] Criar enum `MessageRole` (`USER`, `ASSISTANT`)
- [ ] Criar entidades JPA:
  - `Conversation` — `id`, `title`, `createdAt`, `updatedAt`
  - `Message` — `id`, `conversation` (FK), `role`, `content`, `createdAt`
  - `Document` — `id`, `originalFilename`, `storedFilename`, `contentType`, `sizeBytes`, `uploadedAt`
- [ ] Configurar relacionamento `Conversation` 1:N `Message` com cascade adequado
- [ ] Criar exceções de domínio:
  - `ConversationNotFoundException`
  - `DocumentNotFoundException`
  - `InvalidFileTypeException`
  - `FileSizeExceededException`
- [ ] Criar repositories Spring Data:
  - `ConversationRepository`
  - `MessageRepository` — query de mensagens ordenadas por `createdAt`
  - `DocumentRepository` — query ordenada por `uploadedAt` DESC

### Critério de aceite

- Schema H2 criado automaticamente no startup (`ddl-auto: create-drop`)
- Repositories injetáveis no contexto Spring
- Dados persistidos no H2 durante a execução (perdidos ao reiniciar — esperado na Parte 1)

---

## Fase 2 — Storage (Port/Adapter)

**Duração estimada:** 0,5 dia

### Tarefas

- [ ] Criar interface `FileStoragePort`:
  - `store(MultipartFile file, String storedFilename)`
  - `exists()` / `isAccessible()` para health check
- [ ] Implementar `LocalFileStorageAdapter`:
  - Diretório configurável via `app.storage.upload-dir`
  - Criar diretório se não existir
- [ ] Criar `StorageConfig` com `@ConfigurationProperties` ou `@Value`
- [ ] Sanitização de nome de arquivo (RN-08): remover path traversal (`../`, barras)

### Critério de aceite

- Arquivo salvo em disco com nome UUID + extensão
- Health check consegue detectar storage inacessível

---

## Fase 3 — Services (regras de negócio)

**Duração estimada:** 1,5 dia

Implementar na ordem abaixo — `ChatService` depende de `ConversationRepository`; `DocumentService` depende de `FileStoragePort`.

### 3.1 ChatService (RN-01 a RN-04)

- [ ] `processMessage(ChatMessageRequest)`:
  - Se `conversationId` nulo → criar nova `Conversation` (RN-01)
  - Se informado → buscar ou lançar `ConversationNotFoundException` (RN-05 via ChatService)
  - Persistir mensagem USER (RN-02)
  - Gerar resposta stub ASSISTANT: `"Recebi sua mensagem: {content}"` (RN-03)
  - Atualizar `updatedAt` da conversa (RN-04)
  - Derivar `title` da primeira mensagem (max 100 chars)
- [ ] Retornar `ChatMessageResponse` via mapper

### 3.2 ConversationService (RN-05)

- [ ] `getConversation(UUID id)`:
  - Buscar conversa com mensagens ordenadas
  - Lançar `ConversationNotFoundException` se inexistente
- [ ] Retornar `ConversationResponse` via mapper

### 3.3 DocumentService (RN-06 a RN-08)

- [ ] `uploadDocument(MultipartFile file)`:
  - Validar MIME type permitido (PDF, TXT, DOCX) — RN-06
  - Validar tamanho ≤ 10 MB — RN-07
  - Sanitizar nome original — RN-08
  - Salvar via `FileStoragePort`, persistir metadados
- [ ] `listDocuments()` — listagem ordenada por `uploadedAt` DESC

### 3.4 HealthService (RN-09)

- [ ] `checkHealth()`:
  - Verificar conexão com banco
  - Verificar acesso ao storage
  - Retornar `UP`/`DOWN` por componente
  - HTTP 200 se tudo UP, 503 se algum DOWN

### Critério de aceite

- Toda regra RN-01 a RN-09 coberta em código
- Nenhuma regra de negócio em Controller ou Repository

---

## Fase 4 — API HTTP (Controllers, DTOs, Mappers)

**Duração estimada:** 1 dia

### Tarefas

**DTOs request:**
- [ ] `ChatMessageRequest` — `@NotBlank`, `@Size(max=10000)`, `conversationId` opcional
- [ ] (Upload usa `MultipartFile` diretamente no controller)

**DTOs response:**
- [ ] `ChatMessageResponse`, `ConversationResponse`, `DocumentResponse`, `DocumentListResponse`, `HealthResponse`, `ErrorResponse`

**Mappers:**
- [ ] `MessageMapper`, `ConversationMapper`, `DocumentMapper` — Entity ↔ DTO

**Controllers (delegação 100% ao Service):**
- [ ] `POST /api/chat` → `ChatController`
- [ ] `GET /api/conversations/{id}` → `ConversationController`
- [ ] `POST /api/documents/upload` → `DocumentController` (multipart)
- [ ] `GET /api/documents` → `DocumentController`
- [ ] `GET /api/health` → `HealthController`

### Critério de aceite

| Endpoint | Status esperado |
|---|---|
| POST `/api/chat` (sem conversationId) | 200 + nova conversa |
| POST `/api/chat` (conversationId inválido) | 404 |
| POST `/api/chat` (content vazio) | 400 |
| GET `/api/conversations/{id}` | 200 com histórico |
| POST `/api/documents/upload` (PDF válido) | 201 |
| POST `/api/documents/upload` (tipo inválido) | 400 |
| POST `/api/documents/upload` (> 10 MB) | 413 |
| GET `/api/documents` | 200 com lista |
| GET `/api/health` | 200 ou 503 |

---

## Fase 5 — Infra transversal

**Duração estimada:** 0,5 dia

### Tarefas

- [ ] `@RestControllerAdvice` — mapeamento de exceções para HTTP status conforme spec
- [ ] Formato padrão `ErrorResponse` (timestamp, status, error, message, path)
- [ ] `WebConfig` — CORS para `http://localhost:5173`
- [ ] Garantir datas em ISO-8601 UTC nas respostas JSON

### Critério de aceite

- Erros retornam JSON padronizado, sem stack trace exposto
- Frontend em `:5173` consegue chamar a API sem erro CORS

---

## Fase 6 — Validação (sem testes unitários)

**Duração estimada:** 1 dia

> Conforme decisão do projeto: **sem testes unitários**. Foco em integração e controller.

### Testes de controller (MockMvc + `@MockBean`)

- [ ] `ChatControllerTest` — 200 válido, 400 content vazio
- [ ] `ConversationControllerTest` — 200 e 404
- [ ] `DocumentControllerTest` — 201, 400, 413
- [ ] `HealthControllerTest` — 200 UP, 503 DOWN

### Testes de integração (`@SpringBootTest` + MockMvc + H2)

- [ ] Fluxo chat completo: POST sem ID → GET histórico com 2 mensagens
- [ ] Conversa inexistente → 404
- [ ] Upload PDF válido → 201 + aparece em GET `/api/documents`
- [ ] Upload tipo inválido → 400
- [ ] Health com DB e storage UP

### Validação manual

- [ ] Collection Postman/Insomnia com todos os endpoints
- [ ] Smoke test com curl documentado no README (se existir)

### Critério de aceite

- Todos os cenários da tabela de integração da spec passando
- Cobertura ≥ 80% em fluxos de integração e controller (meta da spec)

---

## Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Validação MIME inconsistente entre browser e backend | Validar extensão **e** content-type no Service |
| Path traversal em nomes de arquivo | Sanitizar no Service antes de chamar storage |
| H2 in-memory perde dados a cada restart | Comportamento esperado na Parte 1; documentar no README |
| Multipart limit não aplicado | Confirmar `spring.servlet.multipart` no yml |

---

## Definition of Done (Sprint 1)

- [ ] Todos os endpoints da spec respondem conforme contrato
- [ ] Regras RN-01 a RN-09 implementadas nos Services
- [ ] Controllers sem lógica de negócio
- [ ] CORS habilitado para frontend
- [ ] Erros padronizados via `@RestControllerAdvice`
- [ ] Persistência via H2 in-memory configurada e funcional
- [ ] Testes de integração e controller passando com H2 (sem unit tests)
- [ ] API testável manualmente via Postman/curl

---

## Próximos passos (fora do escopo Parte 1)

- Integração com LLM/IA real (substituir stub RN-03)
- Banco persistente (ex.: PostgreSQL) se necessário além da Parte 1
- Autenticação/autorização
- OpenAPI/Swagger (`OpenApiConfig` — opcional na spec)
