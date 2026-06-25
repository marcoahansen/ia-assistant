# Plano de Implementação — Frontend (Parte 1)

> **Referência:** [02-frontend-architecture.md](../specs/02-frontend-architecture.md)  
> **Sprint:** 3  
> **Stack:** React 18 · TypeScript · Vite · **Ant Design 5**  
> **Objetivo:** SPA de chat funcional com upload de documentos

---

## Visão geral

| Fase | Foco | Entregável |
|---|---|---|---|
| 0 | Bootstrap | Projeto Vite + Ant Design sobe em `:5173` |
| 1 | Fundação | API client, tipos, utils, ConfigProvider setup |
| 2 | Hooks | Lógica de chat e upload isolada da UI |
| 3 | ~~UI primitivos~~ | Ant Design já provê Button, Spin, Progress |
| 4 | Chat UI | Layout, mensagens, input (antd Layout, Input, Button) |
| 5 | Upload | DocumentUpload com antd Upload.Dragger + Modal |
| 6 | Theming | Dark/light mode via antd ConfigProvider + algorithm |
| 7 | Validação | Testes manuais E2E com backend |

**Dependência externa:** backend rodando em `http://localhost:8080/api` a partir da Fase 2.

**Ordem recomendada:** fases 0 → 7 sequencialmente. Fases 4 e 5 podem ser paralelizadas após Fase 2.

---

## Fase 0 — Bootstrap do projeto

**Duração estimada:** 0,5 dia

### Tarefas

- [ ] Criar projeto Vite + React + TypeScript
- [ ] Criar estrutura de pastas conforme spec (`api/`, `hooks/`, `components/`, `contexts/`, `utils/`)
- [ ] Configurar `.env` com `VITE_API_BASE_URL=http://localhost:8080/api`
- [ ] Instalar dependências:
  - `antd` + `@ant-design/icons` (design system)
  - `react-markdown` (renderização segura)
- [ ] Remover `ui/` da estrutura (antd substitui Button, Spin, Progress)
- [ ] Verificar: `npm run dev` abre em `http://localhost:5173`

### Critério de aceite

- App renderiza placeholder sem erros no console
- Hot reload funcional

---

## Fase 1 — Fundação (API, tipos, utils)

**Duração estimada:** 0,5 dia

### Tarefas

**Tipos (`api/types.ts`):**
- [ ] `Message`, `MessageRole`, `ChatMessageResponse`, `ConversationResponse`
- [ ] `Document`, `DocumentListResponse`
- [ ] `ApiError` class

**Client HTTP (`api/client.ts`):**
- [ ] `apiClient<T>()` com fetch nativo (ADR-02 — sem Axios)
- [ ] Tratamento de erros HTTP → `ApiError`

**Endpoints:**
- [ ] `chatApi.ts` — `sendChatMessage()`
- [ ] `conversationApi.ts` — `getConversation(id)`
- [ ] `documentApi.ts` — `uploadDocument()` (com progress via XMLHttpRequest), `listDocuments()`

**Utils:**
- [ ] `constants.ts` — `MAX_FILE_SIZE` (10 MB), `ALLOWED_TYPES`
- [ ] `formatTimestamp.ts` — ex: "14:30"

**ConfigProvider (App.tsx):**
- [ ] Configurar `ConfigProvider` do antd com `theme.token` (cores primárias, bordas)
- [ ] Reset mínimo de estilos via `App.css`

### Critério de aceite

- Funções de API tipadas e importáveis pelos hooks
- Nenhum componente importa `api/` diretamente (regra da spec)

---

## Fase 2 — Hooks (lógica de estado)

**Duração estimada:** 1 dia

### 2.1 useChatConversation

- [ ] State: `messages`, `conversationId`, `isLoading`, `error`
- [ ] `sendMessage(content)`:
  - Validar trim/não vazio
  - POST `/api/chat`
  - Atualizar `conversationId` se era null
  - Append `userMessage` + `assistantMessage`
- [ ] `loadConversation(id)` — GET `/api/conversations/{id}`
- [ ] `clearError()`, `resetConversation()`

### 2.2 useDocumentUpload

- [ ] State: `isUploading`, `progress`, `error`, `documents`
- [ ] `uploadFile(file)` — validação client-side (tipo, tamanho) + POST multipart
- [ ] `loadDocuments()` — GET `/api/documents`
- [ ] `clearError()`

### Critério de aceite

- Hooks exportam contrato da spec (seção 5.1)
- Fluxos testáveis manualmente contra o backend

---

## Fase 3 — ~~Componentes UI primitivos~~ (PULADA)

**Ant Design já fornece:** `Button`, `Spin`, `Progress`, `Upload`, `Modal`, `Layout`, `Input`, `Alert`, `List`, `Switch`, `Typography`.

Nenhum componente `ui/` custom é necessário. Basta importar do antd diretamente nos componentes de domínio.

---

## Fase 4 — Chat UI

**Duração estimada:** 1,5 dia

Implementar na ordem de composição (de baixo para cima):

### 4.1 MessageBubble

- [ ] Props: `message`, `isOwn`
- [ ] Render Markdown via `react-markdown`
- [ ] Alinhamento USER/ASSISTANT, timestamp formatado

### 4.2 MessageList

- [ ] Props: `messages`, `isLoading`
- [ ] Scroll automático ao final (ref + useEffect)
- [ ] Typing indicator (três dots) quando `isLoading`

### 4.3 ChatInput

- [ ] Props: `onSend`, `isLoading`, `onUploadClick`, `disabled`
- [ ] Usar `antd Input.TextArea` com `onPressEnter`
- [ ] Enter envia, Shift+Enter nova linha, Escape limpa
- [ ] Limpar textarea após envio
- [ ] Botão de envio: `antd Button`
- [ ] Botão de anexar: `antd Button` com ícone Upload

### 4.4 ChatLayout

- [ ] `antd Layout`, `Layout.Header`, `Layout.Sider`, `Layout.Content`
- [ ] Header (logo + theme toggle com `antd Switch`)
- [ ] Sidebar placeholder (lista de conversas — Parte 1)
- [ ] Área principal de chat
- [ ] Responsivo: Sider colapsável com `breakpoint="md"`

### 4.5 App (composição)

- [ ] Gerenciar `conversationId` global
- [ ] Conectar hooks → componentes via props/callbacks

### Critério de aceite

- Fluxo completo: digitar → enviar → ver mensagem user + resposta stub
- Typing indicator visível durante loading
- Scroll automático para última mensagem

---

## Fase 5 — Upload de documentos

**Duração estimada:** 1 dia

### Tarefas

- [ ] `DocumentUpload.tsx`:
  - `antd Modal` controlado por `isOpen` / `onClose`
  - `antd Upload.Dragger` para drag-and-drop + clique
  - Validação client-side: `.pdf`, `.txt`, `.docx`, max 10 MB
  - `antd Progress` durante upload
  - Lista de documentos ao abrir (via `useDocumentUpload`)
  - Exibição de erros amigável com `antd Alert`
- [ ] Integrar trigger no `ChatInput` (botão anexar)
- [ ] Callback `onUploadComplete` após sucesso

### Critério de aceite

- Upload de PDF válido exibe progresso e atualiza lista
- Tipo/tamanho inválido exibe erro amigável sem chamar API
- Escape fecha o painel

---

## Fase 6 — Theming

**Duração estimada:** 0,5 dia

### Tarefas

- [ ] `ThemeContext.tsx`:
  - State `light` | `dark`
  - Persistir em `localStorage`
  - Default: `prefers-color-scheme`
  - Retornar `algorithm` e `token` para `ConfigProvider`
- [ ] Configurar `ConfigProvider` no `<App />` com `theme={{ algorithm, token }}`
- [ ] Integrar `antd Switch` no header do `ChatLayout` para toggle

### Critério de aceite

- Tema persiste após reload
- antd componentes respeitam dark/light mode

---

## Fase 7 — Validação manual (E2E local)

**Duração estimada:** 0,5 dia

**Pré-requisito:** backend Sprint 1 concluído (CORS e contratos de API).

### Checklist

- [ ] Backend rodando + frontend em `:5173`
- [ ] Chat end-to-end: enviar mensagem e receber resposta stub
- [ ] Typing indicator durante loading
- [ ] Scroll automático para última mensagem
- [ ] Upload end-to-end: PDF válido, progresso e listagem
- [ ] Upload rejeitado: tipo ou tamanho inválido
- [ ] CORS sem erros no console
- [ ] Dark/light mode funcional

### Critério de aceite

- Fluxos principais validados manualmente com backend real

---

## Integração com backend

| Momento | O que validar |
|---|---|
| Após Fase 1 | Endpoints do backend via Postman/curl |
| Após Fase 2 | Hook `sendMessage` contra backend real |
| Após Fase 5 | Upload multipart + listagem |
| Fase 7 | Fluxo completo local (backend + frontend) |

---

## Riscos e mitigações

| Risco | Mitigação |
|---|---|
| CORS bloqueando fetch | Confirmar backend Fase 5 antes de debug no frontend |
| Upload sem progresso com fetch | Usar `XMLHttpRequest` para `onprogress` (spec) |
| Sidebar placeholder confunde UX | Documentar como "futuro" na UI |

---

## Definition of Done (Sprint 3)

- [ ] Chat funcional: enviar, histórico, loading, scroll automático
- [ ] Upload funcional: drag-and-drop, progresso, validação, listagem
- [ ] Hooks isolam toda lógica; componentes presentacionais
- [ ] Dark/light mode persistente via antd ConfigProvider
- [ ] a11y básica herdada do antd + customizações
- [ ] Integração local com backend validada manualmente

---

## Próximos passos (fora do escopo Parte 1)

- Sidebar com lista real de conversas
- WebSocket para respostas em streaming
- Roteamento (React Router) por `conversationId`
