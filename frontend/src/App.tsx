import { useState } from 'react';
import { ConfigProvider } from 'antd';
import { ThemeProvider, useTheme } from './contexts/ThemeContext';
import { useChatConversation } from './hooks/useChatConversation';
import { useDocumentUpload } from './hooks/useDocumentUpload';
import ChatLayout from './components/layout/ChatLayout';
import MessageList from './components/chat/MessageList';
import ChatInput from './components/chat/ChatInput';
import DocumentUpload from './components/documents/DocumentUpload';

function AppContent() {
  const { config } = useTheme();
  const [uploadOpen, setUploadOpen] = useState(false);

  const {
    messages,
    isLoading: chatLoading,
    error: chatError,
    sendMessage,
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

  return (
    <ConfigProvider theme={config}>
      <ChatLayout>
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
          onSend={sendMessage}
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
