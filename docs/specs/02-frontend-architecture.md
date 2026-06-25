# Especificação de Arquitetura — Frontend (Parte 1)

> **Projeto:** Assistente Inteligente Full-Stack  
> **Stack:** React 18 · TypeScript · Vite · Vitest · React Testing Library  
> **Escopo:** Sprint 3 — Interface de chat funcional com upload de documentos  
> **Versão:** 1.0.0  
> **Status:** Draft — Spec-Driven Development (SDD)

---

## 1. Visão Geral

O frontend é uma **SPA (Single Page Application)** que fornece a interface de chat e upload de documentos. Comunica com o backend exclusivamente via HTTP (REST). Na Parte 1, não há WebSocket — cada mensagem é enviada via POST e o histórico carregado via GET.

### 1.1 Objetivos da Parte 1

| Objetivo | Critério de Aceite |
|---|---|
| Interface de chat funcional | Enviar mensagens, visualizar histórico, scroll automático |
| Indicador de typing/loading | Feedback visual enquanto aguarda resposta da API |
| Upload de documentos | Drag-and-drop com barra de progresso |
| Separação Hook/UI | Lógica de estado isolada em `useChatConversation`; componentes são presentacionais |
| Acessibilidade | Navegação por teclado, ARIA, HTML semântico desde a concepção |

### 1.2 Princípios Arquiteturais

1. **Separação de Responsabilidades** — Hooks gerenciam estado e efeitos colaterais; componentes renderizam UI.
2. **Unidirecionalidade de Dados** — Estado flui do hook para componentes via props; eventos fluem via callbacks.
3. **Composição sobre Herança** — Componentes pequenos e compostos.
4. **Acessibilidade First** — Requisitos A11y definidos na spec, não adicionados depois.
5. **Separação testável** — Hooks isolados da UI facilitam testes de integração e mocks de props nos componentes.

---

## 2. Estrutura de Diretórios

```
src/
├── main.tsx                        # Entry point
├── App.tsx                         # Root: conversationId, theme, routing
├── App.css                         # Estilos globais / CSS variables (theme)
├── api/
│   ├── client.ts                   # Axios/fetch wrapper com base URL
│   ├── chatApi.ts                  # POST /api/chat
│   ├── conversationApi.ts          # GET /api/conversations/{id}
│   ├── documentApi.ts              # POST upload, GET list
│   └── types.ts                    # Tipos espelhando DTOs do backend
├── hooks/
│   ├── useChatConversation.ts      # Estado e lógica do chat
│   └── useDocumentUpload.ts        # Estado e lógica de upload
├── components/
│   ├── layout/
│   │   └── ChatLayout.tsx          # Sidebar + área principal
│   ├── chat/
│   │   ├── MessageList.tsx
│   │   ├── MessageBubble.tsx
│   │   └── ChatInput.tsx
│   ├── documents/
│   │   └── DocumentUpload.tsx
│   └── ui/                         # Primitivos reutilizáveis (Button, Spinner, etc.)
│       ├── Button.tsx
│       ├── Spinner.tsx
│       └── ProgressBar.tsx
├── contexts/
│   └── ThemeContext.tsx            # Dark/light theme
├── utils/
│   ├── formatTimestamp.ts
│   └── constants.ts                # API_BASE_URL, MAX_FILE_SIZE, ALLOWED_TYPES
└── test/
    ├── setup.ts                    # Vitest setup (RTL, matchers)
    ├── mocks/
    │   ├── handlers.ts             # MSW handlers
    │   └── server.ts               # MSW server
    └── factories/
        └── messageFactory.ts       # Dados de teste
```

---

## 3. Hierarquia de Componentes

```
<App>
├── <ThemeProvider>
│   └── <ChatLayout>
│       ├── [Sidebar]                    # Lista de conversas (placeholder Parte 1)
│       │   └── conversationId selector
│       └── [Main Area]
│           ├── <MessageList>
│           │   └── <MessageBubble /> × N
│           ├── [Typing Indicator]       # Condicional: isLoading
│           ├── <ChatInput>
│           │   ├── <textarea>
│           │   ├── <Button send>
│           │   └── <DocumentUpload trigger>
│           └── <DocumentUpload modal/panel>
```

### 3.1 Diagrama de Fluxo de Dados

```
┌──────────────────────────────────────────────────────────────┐
│                        useChatConversation                    │
│  State: messages, conversationId, isLoading, error           │
│  Actions: sendMessage, loadConversation, clearError          │
└──────────────┬───────────────────────────────┬───────────────┘
               │ props                         │ callbacks
               ▼                               ▲
┌──────────────────────────┐    events    ┌────┴─────────────┐
│      <MessageList />      │◄────────────│   <ChatInput />   │
│      <MessageBubble />    │             │  <DocumentUpload> │
└──────────────────────────┘             └──────────────────┘
               │
               ▼
         chatApi.ts  ──HTTP──►  Backend /api/*
```

---

## 4. Especificação de Componentes

### 4.1 `<App />` — Root Component

**Responsabilidade:** Gerenciar `conversationId` global, theme e composição top-level.

| Prop / State | Tipo | Descrição |
|---|---|---|
| `conversationId` | `string \| null` | ID da conversa ativa |
| `theme` | `'light' \| 'dark'` | Tema visual via `ThemeContext` |

**Comportamento:**
- Inicializa com `conversationId = null` (nova conversa).
- Provê `ThemeProvider` envolvendo toda a árvore.
- Renderiza `<ChatLayout />` como filho direto.

**Acessibilidade:**
- `<html lang="pt-BR">` definido em `index.html`.
- Skip link: `<a href="#main-chat" className="sr-only">Ir para o chat</a>`.

---

### 4.2 `<ChatLayout />` — Layout Responsivo

**Responsabilidade:** Estrutura visual com sidebar de conversas e área principal de chat.

**Props:**

| Prop | Tipo | Descrição |
|---|---|---|
| `conversationId` | `string \| null` | Conversa ativa |
| `onConversationChange` | `(id: string) => void` | Callback de troca de conversa |
| `children` | `ReactNode` | Área principal (MessageList + ChatInput) |

**Layout:**

```
┌─────────────────────────────────────────────┐
│  Header (logo + theme toggle)               │
├──────────┬──────────────────────────────────┤
│ Sidebar  │  Main Chat Area                  │
│ (240px)  │  ┌────────────────────────────┐  │
│          │  │ MessageList (flex-grow)    │  │
│ Convs    │  ├────────────────────────────┤  │
│ list     │  │ ChatInput (fixed bottom)   │  │
│          │  └────────────────────────────┘  │
└──────────┴──────────────────────────────────┘
```

**Acessibilidade:**
- `<aside aria-label="Lista de conversas">` para sidebar.
- `<main id="main-chat" role="main" aria-label="Área de chat">` para área principal.
- Layout responsivo: sidebar colapsável em viewport < 768px com botão hamburger acessível.

---

### 4.3 `<MessageList />` — Lista de Mensagens

**Responsabilidade:** Renderizar mensagens com scroll automático para a última.

**Props:**

| Prop | Tipo | Descrição |
|---|---|---|
| `messages` | `Message[]` | Array de mensagens da conversa |
| `isLoading` | `boolean` | Exibir indicador de typing |

**Comportamento:**
- Scroll automático para o final quando `messages` muda ou `isLoading` torna-se `true`.
- Renderiza `<MessageBubble />` para cada mensagem.
- Exibe indicador de typing (três dots animados) quando `isLoading === true`.

**Acessibilidade:**
- Container: `<ul role="log" aria-live="polite" aria-label="Histórico de mensagens" aria-relevant="additions">`.
- Cada mensagem: `<li>` dentro do `<ul>`.
- Indicador de typing: `<span aria-label="Assistente está digitando">`.
- Anúncio para screen readers quando nova mensagem chega (via `aria-live`).

---

### 4.4 `<MessageBubble />` — Bolha de Mensagem

**Responsabilidade:** Exibir uma mensagem individual com Markdown renderizado e timestamp.

**Props:**

| Prop | Tipo | Descrição |
|---|---|---|
| `message` | `Message` | Objeto `{ id, role, content, createdAt }` |
| `isOwn` | `boolean` | Derivado de `role === 'USER'` |

**Comportamento:**
- Renderiza `content` como Markdown (biblioteca: `react-markdown`).
- Alinhamento: USER à direita, ASSISTANT à esquerda.
- Exibe timestamp formatado (ex: "14:30").

**Acessibilidade:**
- `<article aria-label="{role === 'USER' ? 'Sua mensagem' : 'Resposta do assistente'}">`.
- Timestamp: `<time dateTime={createdAt} aria-label="Enviada às {formattedTime}">`.
- Markdown: garantir que headings gerados não quebrem hierarquia (configurar `react-markdown` para renderizar headings como `<p>` ou `<strong>`).

**Estilos:**
- USER: fundo primário, texto claro, border-radius assimétrico (top-right menor).
- ASSISTANT: fundo neutro, borda sutil, border-radius assimétrico (top-left menor).

---

### 4.5 `<ChatInput />` — Entrada de Mensagem

**Responsabilidade:** Textarea com envio por Enter, botão de envio, trigger de upload e indicador de loading.

**Props:**

| Prop | Tipo | Descrição |
|---|---|---|
| `onSend` | `(content: string) => void` | Callback de envio |
| `isLoading` | `boolean` | Desabilita input durante loading |
| `onUploadClick` | `() => void` | Abre painel/modal de upload |
| `disabled` | `boolean` | Desabilita interação |

**Comportamento:**
- `Enter` (sem Shift) envia mensagem.
- `Shift + Enter` insere quebra de linha.
- Botão de envio desabilitado se textarea vazio ou `isLoading`.
- Textarea limpa após envio bem-sucedido.
- Foco retorna ao textarea após envio.

**Acessibilidade:**
- `<form role="form" aria-label="Enviar mensagem">`.
- Textarea: `<textarea aria-label="Digite sua mensagem" aria-describedby="input-hint" rows={1}>`.
- Hint: `<span id="input-hint" className="sr-only">Pressione Enter para enviar, Shift+Enter para nova linha</span>`.
- Botão envio: `<button type="submit" aria-label="Enviar mensagem" disabled={...}>`.
- Botão upload: `<button type="button" aria-label="Anexar documento">`.
- Durante loading: textarea recebe `aria-disabled="true"`.

**Navegação por teclado:**
- `Tab` navega entre textarea → botão upload → botão envio.
- `Escape` limpa textarea (se contém texto).

---

### 4.6 `<DocumentUpload />` — Upload de Arquivos

**Responsabilidade:** Drag-and-drop de arquivos com barra de progresso.

**Props:**

| Prop | Tipo | Descrição |
|---|---|---|
| `onUploadComplete` | `(document: Document) => void` | Callback pós-upload |
| `isOpen` | `boolean` | Controla visibilidade do painel |
| `onClose` | `() => void` | Fecha o painel |

**Comportamento:**
- Zona de drop aceita arrastar arquivos ou clique para selecionar.
- Tipos aceitos: `.pdf`, `.txt`, `.docx` (validação client-side antes do envio).
- Tamanho máximo: 10 MB (validação client-side).
- Barra de progresso durante upload (`XMLHttpRequest` ou Axios `onUploadProgress`).
- Exibe erro amigável para tipo/tamanho inválido.
- Lista documentos já enviados (GET `/api/documents`) ao abrir.

**Acessibilidade:**
- Drop zone: `<div role="region" aria-label="Área de upload de documentos">`.
- Input file oculto visualmente: `<input type="file" accept=".pdf,.txt,.docx" aria-label="Selecionar arquivo" className="sr-only">`.
- Botão visível: `<button aria-label="Selecionar arquivo para upload">`.
- Durante drag over: `aria-dropeffect="copy"`.
- Progress bar: `<progress value={percent} max={100} aria-label="Progresso do upload: {percent}%">`.
- Erros: `<div role="alert" aria-live="assertive">`.
- Lista de documentos: `<ul aria-label="Documentos enviados">`.

**Navegação por teclado:**
- `Enter` / `Space` no botão abre seletor de arquivos.
- `Escape` fecha o painel de upload.

---

## 5. Hook — `useChatConversation`

### 5.1 Contrato

```typescript
interface UseChatConversationReturn {
  // State (read-only para componentes)
  messages: Message[];
  conversationId: string | null;
  isLoading: boolean;
  error: string | null;

  // Actions
  sendMessage: (content: string) => Promise<void>;
  loadConversation: (id: string) => Promise<void>;
  clearError: () => void;
  resetConversation: () => void;
}
```

### 5.2 Tipos

```typescript
interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string; // ISO-8601
}

interface ChatMessageResponse {
  conversationId: string;
  userMessage: Message;
  assistantMessage: Message;
}

interface ConversationResponse {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: Message[];
}
```

### 5.3 Lógica Interna

```
sendMessage(content):
  1. Validar content (trim, não vazio) — senão, return
  2. setIsLoading(true), clearError()
  3. POST /api/chat { conversationId, content }
  4. On success:
     - Atualizar conversationId (se era null)
     - Append userMessage + assistantMessage ao messages[]
  5. On error:
     - setError(mensagem amigável)
  6. setIsLoading(false)

loadConversation(id):
  1. setIsLoading(true), clearError()
  2. GET /api/conversations/{id}
  3. On success: setMessages(data.messages), setConversationId(id)
  4. On error: setError(...)
  5. setIsLoading(false)

resetConversation():
  1. setMessages([]), setConversationId(null), clearError()
```

### 5.4 Regras de Separação Hook vs UI

| Responsabilidade | Hook | Componente |
|---|---|---|
| Chamar API | ✅ | ❌ |
| Gerenciar loading/error state | ✅ | ❌ |
| Validar input de negócio | ✅ | ❌ |
| Renderizar UI | ❌ | ✅ |
| Disparar callbacks de UI (onSend) | ❌ | ✅ (delega ao hook) |
| Scroll automático | ❌ | ✅ (`MessageList` via ref/effect) |
| Renderizar Markdown | ❌ | ✅ (`MessageBubble`) |
| Formatação de timestamp | ❌ | ✅ (util `formatTimestamp`) |

**Regra absoluta:** Nenhum componente importa de `api/` diretamente. Toda comunicação HTTP passa pelo hook.

---

## 6. Camada de API (Frontend)

### 6.1 client.ts

```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export async function apiClient<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new ApiError(response.status, error.message ?? 'Erro desconhecido');
  }

  return response.json();
}
```

### 6.2 Endpoints Consumidos

| Função | Método | Endpoint | Usado por |
|---|---|---|---|
| `sendChatMessage()` | POST | `/chat` | `useChatConversation` |
| `getConversation()` | GET | `/conversations/{id}` | `useChatConversation` |
| `uploadDocument()` | POST | `/documents/upload` | `useDocumentUpload` |
| `listDocuments()` | GET | `/documents` | `useDocumentUpload` |

---

## 7. Requisitos de Acessibilidade (A11y)

### 7.1 Checklist Global

| Requisito | Implementação |
|---|---|
| Idioma da página | `<html lang="pt-BR">` |
| Skip navigation | Link "Ir para o conteúdo principal" visível no focus |
| Landmarks | `<header>`, `<aside>`, `<main>`, `<footer>` |
| Focus visible | Outline visível em todos os elementos interativos (`:focus-visible`) |
| Contraste | WCAG AA — mínimo 4.5:1 para texto normal |
| Reduced motion | `@media (prefers-reduced-motion: reduce)` desabilita animações |
| Tamanho de toque | Mínimo 44×44px para botões em mobile |

### 7.2 ARIA por Componente

| Componente | Atributos ARIA |
|---|---|
| `MessageList` | `role="log"`, `aria-live="polite"`, `aria-relevant="additions"` |
| `MessageBubble` | `aria-label` descritivo por role |
| `ChatInput` | `aria-label`, `aria-describedby`, `aria-disabled` |
| `DocumentUpload` | `role="region"`, `role="alert"` para erros, `aria-dropeffect` |
| Typing indicator | `aria-label="Assistente está digitando"`, `aria-live="polite"` |
| Theme toggle | `aria-label="Alternar tema claro/escuro"`, `aria-pressed` |

### 7.3 Navegação por Teclado

| Contexto | Tecla | Ação |
|---|---|---|
| ChatInput | `Enter` | Enviar mensagem |
| ChatInput | `Shift+Enter` | Nova linha |
| ChatInput | `Escape` | Limpar textarea |
| DocumentUpload | `Enter`/`Space` | Abrir seletor de arquivos |
| DocumentUpload | `Escape` | Fechar painel |
| Global | `Tab` | Navegação sequencial entre interativos |
| Sidebar | `Arrow Up/Down` | Navegar entre conversas (futuro) |

### 7.4 Screen Reader

- Novas mensagens anunciadas via `aria-live="polite"` no `MessageList`.
- Erros anunciados via `role="alert"` (live region assertive).
- Upload progress anunciado a cada 25% via `aria-valuenow`.

---

## 8. Estratégia de Testes

### 8.1 Stack de Testes

| Ferramenta | Uso |
|---|---|
| **Vitest** | Runner de testes (compatível com Vite) |
| **React Testing Library** | Renderização e interação com componentes |
| **MSW (Mock Service Worker)** | Mock de API HTTP nos testes |
| **@testing-library/user-event** | Simulação de interações realistas |
| **@testing-library/jest-dom** | Matchers customizados (`toBeInTheDocument`, etc.) |

### 8.2 Pirâmide de Testes

```
         ┌──────────────────┐
         │  Integration     │  Hook + Componentes juntos (MSW)
         ├──────────────────┤
         │  Component       │  Render + props + a11y
         └──────────────────┘
```

**Meta de cobertura:** ≥ 75% em fluxos de integração e componentes.

### 8.3 Testes de Integração Hook + UI

**Arquivo:** `ChatFlow.integration.test.tsx`

Foco principal: validar que o hook e os componentes funcionam **juntos**.

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from '../App';

describe('Chat Flow Integration', () => {
  it('should send a message and display response in the chat', async () => {
    const user = userEvent.setup();
    render(<App />);

    const textarea = screen.getByRole('textbox', { name: /digite sua mensagem/i });
    await user.type(textarea, 'Olá, assistente');
    await user.click(screen.getByRole('button', { name: /enviar/i }));

    await waitFor(() => {
      expect(screen.getByText('Olá, assistente')).toBeInTheDocument();
      expect(screen.getByText(/Recebi sua mensagem/)).toBeInTheDocument();
    });
  });

  it('should show typing indicator while waiting for response', async () => { /* ... */ });
  it('should display error message when API fails', async () => { /* ... */ });
  it('should auto-scroll to latest message', async () => { /* ... */ });
});
```

### 8.4 Testes de Componente

| Componente | Cenários |
|---|---|
| `MessageBubble` | Render USER vs ASSISTANT, Markdown, timestamp formatado |
| `MessageList` | Lista vazia, múltiplas mensagens, typing indicator visível |
| `ChatInput` | Enter envia, Shift+Enter nova linha, disabled quando loading |
| `DocumentUpload` | Drag-and-drop, validação de tipo, progress bar, erro de tamanho |
| `ChatLayout` | Landmarks semânticos presentes, sidebar colapsável |

### 8.5 Testes de Acessibilidade

```typescript
import { axe, toHaveNoViolations } from 'jest-axe';
expect.extend(toHaveNoViolations);

it('should have no a11y violations on chat page', async () => {
  const { container } = render(<App />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

**Casos a11y obrigatórios:**

| Teste | Verificação |
|---|---|
| Landmarks | `main`, `aside`, `header` presentes |
| ARIA live | `MessageList` possui `aria-live="polite"` |
| Keyboard | ChatInput enviável via Enter |
| Labels | Todos os inputs/buttons possuem `aria-label` ou `<label>` |
| Focus | Tab order lógico entre interativos |

### 8.6 MSW Handlers (Mock)

```typescript
// test/mocks/handlers.ts
export const handlers = [
  http.post('/api/chat', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      conversationId: '550e8400-e29b-41d4-a716-446655440000',
      userMessage: { id: '1', role: 'USER', content: body.content, createdAt: new Date().toISOString() },
      assistantMessage: { id: '2', role: 'ASSISTANT', content: `Recebi sua mensagem: ${body.content}`, createdAt: new Date().toISOString() },
    });
  }),

  http.get('/api/conversations/:id', ({ params }) => {
    return HttpResponse.json({ id: params.id, title: 'Test', messages: [], createdAt: '', updatedAt: '' });
  }),

  http.post('/api/documents/upload', () => {
    return HttpResponse.json({ id: '1', originalFilename: 'test.pdf', contentType: 'application/pdf', sizeBytes: 1024, uploadedAt: new Date().toISOString() }, { status: 201 });
  }),

  http.get('/api/documents', () => {
    return HttpResponse.json({ documents: [], total: 0 });
  }),
];
```

### 8.7 Convenções de Nomenclatura

```
should [expected behavior] when [condition]
```

Exemplos:
- `should display typing indicator while waiting for response`
- `should reject file upload when type is invalid`
- `should have no a11y violations on chat page`

---

## 9. Theming

### 9.1 CSS Variables

```css
:root {
  --color-bg-primary: #ffffff;
  --color-bg-secondary: #f5f5f5;
  --color-text-primary: #1a1a1a;
  --color-text-secondary: #666666;
  --color-accent: #4f46e5;
  --color-user-bubble: #4f46e5;
  --color-assistant-bubble: #f0f0f0;
  --color-border: #e0e0e0;
  --color-error: #dc2626;
}

[data-theme="dark"] {
  --color-bg-primary: #1a1a2e;
  --color-bg-secondary: #16213e;
  --color-text-primary: #eaeaea;
  --color-text-secondary: #a0a0a0;
  --color-accent: #818cf8;
  --color-user-bubble: #4338ca;
  --color-assistant-bubble: #2a2a4a;
  --color-border: #333355;
  --color-error: #f87171;
}
```

### 9.2 ThemeContext

- Persiste preferência em `localStorage`.
- Respeita `prefers-color-scheme` do OS como default.
- Toggle acessível com `aria-pressed`.

---

## 10. Configuração

### 10.1 Variáveis de Ambiente

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### 10.2 vite.config.ts (test)

```typescript
/// <reference types="vitest" />
export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
});
```

---

## 11. Checklist de Implementação (Sprint 3)

- [ ] Projeto Vite + React + TypeScript inicializado
- [ ] Camada `api/` com client e tipos espelhando backend
- [ ] Hook `useChatConversation` com toda lógica de chat
- [ ] Hook `useDocumentUpload` com lógica de upload
- [ ] Componentes: App, ChatLayout, MessageList, MessageBubble, ChatInput, DocumentUpload
- [ ] ThemeContext com dark/light mode
- [ ] Acessibilidade: landmarks, ARIA, keyboard navigation
- [ ] MSW configurado para testes
- [ ] Testes de integração hook + UI
- [ ] Testes de acessibilidade (jest-axe)
- [ ] Testes de componentes individuais
- [ ] Cobertura ≥ 75% em fluxos de integração e componentes

---

## 12. Decisões Arquiteturais Registradas (ADR)

| # | Decisão | Justificativa |
|---|---|---|
| ADR-01 | Hooks centralizam toda lógica de estado | Separação estrita hook/UI; componentes testáveis com props |
| ADR-02 | fetch nativo (sem Axios) | Menos dependências; suficiente para Parte 1 |
| ADR-03 | MSW para mocks de API | Testes realistas sem backend; reutilizável em Storybook |
| ADR-04 | react-markdown para renderização | Seguro contra XSS; extensível |
| ADR-05 | CSS variables para theming | Simples, performático, sem dependência de CSS-in-JS |
| ADR-06 | A11y na spec, não afterthought | Requisitos definidos antes da implementação |
