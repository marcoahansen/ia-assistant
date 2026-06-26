import { apiClient } from './client';
import type { ConversationResponse } from './types';

export async function getConversation(
  id: string,
): Promise<ConversationResponse> {
  return apiClient<ConversationResponse>(`/conversations/${id}`);
}
