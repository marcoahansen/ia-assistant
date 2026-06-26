import { apiClient } from './client';
import type { ConversationListResponse, ConversationResponse } from './types';

export async function listConversations(): Promise<ConversationListResponse> {
  return apiClient<ConversationListResponse>('/conversations');
}

export async function getConversation(
  id: string,
): Promise<ConversationResponse> {
  return apiClient<ConversationResponse>(`/conversations/${id}`);
}
