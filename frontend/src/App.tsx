import { useEffect, useState } from 'react';
import { ConfigProvider } from 'antd';
import { ThemeProvider, useTheme } from './contexts/ThemeContext';
import { useChatConversation } from './hooks/useChatConversation';
import { useDocumentUpload } from './hooks/useDocumentUpload';
import { listConversations } from './api/conversationApi';
import ChatLayout from './components/layout/ChatLayout';
import MessageList from './components/chat/MessageList';
import ChatInput from './components/chat/ChatInput';
import DocumentUpload from './components/documents/DocumentUpload';
import type { ConversationSummary } from './api/types';

function AppContent() {
  const { config } = useTheme();
  const [uploadOpen, setUploadOpen] = useState(false);
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);

  const {
    messages,
    isLoading: chatLoading,
    error: chatError,
    conversationId,
    sendMessage,
    loadConversation,
    resetConversation,
    clearError: clearChatError,
  } = useChatConversation();

  const {
    documents,
    isUploading,
    progress,
    error: uploadError,
    uploadFile,
    loadDocuments,
    clearError: clearUploadError,
  } = useDocumentUpload();

  const loadConversations = async () => {
    try {
      const data = await listConversations();
      setConversations(data.conversations);
    } catch {
      // Silently fail — sidebar apenas não atualiza
    }
  };

  useEffect(() => {
    loadConversations();
  }, []);

  const handleSendMessage = async (content: string) => {
    await sendMessage(content);
    await loadConversations();
  };

  return (
    <ConfigProvider theme={config}>
      <ChatLayout
        conversations={conversations}
        activeId={conversationId}
        onSelect={loadConversation}
        onNewChat={resetConversation}
      >
        {chatError && (
          <div
            role="alert"
            style={{
              padding: '8px 16px',
              background: 'var(--ant-color-error-bg, #fff1f0)',
              color: 'var(--ant-color-error)',
              fontSize: 13,
              cursor: 'pointer',
            }}
            onClick={clearChatError}
          >
            {chatError} (clique para fechar)
          </div>
        )}

        <MessageList messages={messages} isLoading={chatLoading} />

        <ChatInput
          onSend={handleSendMessage}
          isLoading={chatLoading}
          onUploadClick={() => setUploadOpen(true)}
        />

        <DocumentUpload
          isOpen={uploadOpen}
          onClose={() => setUploadOpen(false)}
          documents={documents}
          isUploading={isUploading}
          progress={progress}
          error={uploadError}
          onUpload={uploadFile}
          onLoadDocuments={loadDocuments}
          onClearError={clearUploadError}
        />
      </ChatLayout>
    </ConfigProvider>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <AppContent />
    </ThemeProvider>
  );
}
