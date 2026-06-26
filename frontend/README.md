# Frontend — Projeto IA

Interface web do Assistente Inteligente, construída com React + TypeScript e Vite.

## Stack

- React 18
- TypeScript
- Vite 6
- Ant Design 5
- react-markdown

## Pré-requisitos

- Node.js 18+
- npm (ou yarn)

## Como executar

```bash
cd frontend
npm install
npm run dev
```

A aplicação sobe em `http://localhost:5173` (porta padrão do Vite).

## Build

```bash
cd frontend
npm run build
```

O build gera os arquivos estáticos na pasta `dist/`.

## Estrutura

```
src/
├── api/        — Chamadas HTTP para o backend
├── components/ — Componentes React
├── contexts/   — Contextos React
├── hooks/      — Hooks customizados
└── utils/      — Utilitários
```

## Observações

- A aplicação depende do backend rodando em `http://localhost:8080`.
- Configure a URL do backend pela variável de ambiente `VITE_API_URL` (arquivo `.env`).
