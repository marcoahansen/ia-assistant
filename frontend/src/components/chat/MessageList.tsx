import { useEffect, useRef } from 'react';
import { Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';
import type { MessageDetail, SourceDTO } from '../../api/types';
import MessageBubble from './MessageBubble';

interface MessageListProps {
  messages: MessageDetail[];
  sourcesMap: Record<string, SourceDTO[]>;
  isLoading: boolean;
}

export default function MessageList({ messages, sourcesMap, isLoading }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  return (
    <div
      role="log"
      aria-live="polite"
      aria-label="Histórico de mensagens"
      aria-relevant="additions"
      style={{
        flex: 1,
        overflowY: 'auto',
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {messages.length === 0 && !isLoading && (
        <div
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--ant-color-text-tertiary)',
            fontStyle: 'italic',
          }}
        >
          Envie uma mensagem para começar.
        </div>
      )}

      {messages.map((msg) => (
        <MessageBubble key={msg.id} message={msg} sources={sourcesMap[msg.id]} />
      ))}

      {isLoading && (
        <div
          aria-label="Assistente está digitando"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-start',
            paddingLeft: 16,
            marginBottom: 12,
          }}
        >
          <Spin indicator={<LoadingOutlined spin />} size="small" />
          <span style={{ marginLeft: 8, color: 'var(--ant-color-text-tertiary)' }}>
            Digitando...
          </span>
        </div>
      )}

      <div ref={bottomRef} />
    </div>
  );
}
