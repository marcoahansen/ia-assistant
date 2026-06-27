import { useState, useCallback } from 'react';
import type { MessageDetail, ChatMessageRequest, SourceDTO } from '../api/types';
import { sendChatMessage } from '../api/chatApi';
import { getConversation } from '../api/conversationApi';

export interface UseChatConversationReturn {
  messages: MessageDetail[];
  sourcesMap: Record<string, SourceDTO[]>;
  conversationId: string | null;
  isLoading: boolean;
  error: string | null;
  sendMessage: (content: string) => Promise<void>;
  loadConversation: (id: string) => Promise<void>;
  clearError: () => void;
  resetConversation: () => void;
}

export function useChatConversation(): UseChatConversationReturn {
  const [messages, setMessages] = useState<MessageDetail[]>([]);
  const [sourcesMap, setSourcesMap] = useState<Record<string, SourceDTO[]>>({});
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => setError(null), []);

  const sendMessage = useCallback(
    async (content: string) => {
      const trimmed = content.trim();
      if (!trimmed) return;

      setIsLoading(true);
      clearError();

      try {
        const data: ChatMessageRequest = {
          conversationId,
          content: trimmed,
        };

        const response = await sendChatMessage(data);

        setConversationId(response.conversationId);
        setMessages((prev) => [
          ...prev,
          response.userMessage,
          response.assistantMessage,
        ]);

        if (response.sources && response.sources.length > 0) {
          setSourcesMap((prev) => ({
            ...prev,
            [response.assistantMessage.id]: response.sources!,
          }));
        }
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : 'Erro ao enviar mensagem';
        setError(message);
      } finally {
        setIsLoading(false);
      }
    },
    [conversationId, clearError],
  );

  const loadConversation = useCallback(async (id: string) => {
    setIsLoading(true);
    clearError();

    try {
      const response = await getConversation(id);
      setMessages(response.messages);
      setConversationId(response.id);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Erro ao carregar conversa';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [clearError]);

  const resetConversation = useCallback(() => {
    setMessages([]);
    setSourcesMap({});
    setConversationId(null);
    clearError();
  }, [clearError]);

  return {
    messages,
    sourcesMap,
    conversationId,
    isLoading,
    error,
    sendMessage,
    loadConversation,
    clearError,
    resetConversation,
  };
}
