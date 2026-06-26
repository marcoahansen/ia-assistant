import type { MessageDetail } from '../../api/types';

export function buildMessage(
  overrides: Partial<MessageDetail> = {},
): MessageDetail {
  return {
    id: crypto.randomUUID(),
    role: 'USER',
    content: 'Mensagem de teste',
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

export function buildAssistantMessage(
  overrides: Partial<MessageDetail> = {},
): MessageDetail {
  return buildMessage({ role: 'ASSISTANT', content: 'Resposta de teste', ...overrides });
}
