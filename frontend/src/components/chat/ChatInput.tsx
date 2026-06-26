import { useState, useRef, useCallback, type ChangeEvent } from 'react';
import { Input, Button } from 'antd';
import { SendOutlined, PaperClipOutlined } from '@ant-design/icons';

const { TextArea } = Input;

interface ChatInputProps {
  onSend: (content: string) => void;
  isLoading: boolean;
  onUploadClick: () => void;
  disabled?: boolean;
}

export default function ChatInput({
  onSend,
  isLoading,
  onUploadClick,
  disabled,
}: ChatInputProps) {
  const [text, setText] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = useCallback(() => {
    const trimmed = text.trim();
    if (!trimmed || isLoading || disabled) return;
    onSend(trimmed);
    setText('');
    textareaRef.current?.focus();
  }, [text, isLoading, disabled, onSend]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
    if (e.key === 'Escape') {
      setText('');
    }
  };

  return (
    <form
      role="form"
      aria-label="Enviar mensagem"
      onSubmit={(e) => {
        e.preventDefault();
        handleSend();
      }}
      style={{
        display: 'flex',
        gap: 8,
        padding: '12px 16px',
        borderTop: '1px solid var(--ant-color-border)',
        background: 'var(--ant-color-bg-container)',
      }}
    >
      <Button
        type="text"
        icon={<PaperClipOutlined />}
        aria-label="Anexar documento"
        onClick={onUploadClick}
        disabled={disabled}
      />
      <TextArea
        ref={textareaRef as React.Ref<any>}
        value={text}
        onChange={(e: ChangeEvent<HTMLTextAreaElement>) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Digite sua mensagem..."
        aria-label="Digite sua mensagem"
        aria-describedby="input-hint"
        disabled={isLoading || disabled}
        autoSize={{ minRows: 1, maxRows: 4 }}
        style={{ flex: 1, resize: 'none' }}
      />
      <span id="input-hint" style={{ display: 'none' }}>
        Pressione Enter para enviar, Shift+Enter para nova linha
      </span>
      <Button
        type="primary"
        icon={<SendOutlined />}
        htmlType="submit"
        aria-label="Enviar mensagem"
        loading={isLoading}
        disabled={!text.trim() || disabled}
      />
    </form>
  );
}
