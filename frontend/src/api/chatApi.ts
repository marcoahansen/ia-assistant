import { apiClient } from './client';
import type { ChatMessageRequest, ChatMessageResponse } from './types';

export async function sendChatMessage(
  data: ChatMessageRequest,
): Promise<ChatMessageResponse> {
  return apiClient<ChatMessageResponse>('/chat', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}
