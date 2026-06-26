import { Typography } from 'antd';
import Markdown from 'react-markdown';
import type { MessageDetail } from '../../api/types';
import { formatTimestamp } from '../../utils/formatTimestamp';

const { Text } = Typography;

interface MessageBubbleProps {
  message: MessageDetail;
}

export default function MessageBubble({ message }: MessageBubbleProps) {
  const isOwn = message.role === 'USER';

  return (
    <article
      aria-label={isOwn ? 'Sua mensagem' : 'Resposta do assistente'}
      style={{
        display: 'flex',
        justifyContent: isOwn ? 'flex-end' : 'flex-start',
        marginBottom: 12,
      }}
    >
      <div
        style={{
          maxWidth: '70%',
          padding: '10px 14px',
          borderRadius: isOwn ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
          background: isOwn ? 'var(--ant-color-primary)' : 'var(--ant-color-bg-elevated)',
          color: isOwn ? '#fff' : 'var(--ant-color-text)',
          border: isOwn ? 'none' : '1px solid var(--ant-color-border)',
        }}
      >
        <div style={{ wordBreak: 'break-word' }}>
          <Markdown>{message.content}</Markdown>
        </div>
        <div style={{ marginTop: 4 }}>
          <Text
            type="secondary"
            style={{
              fontSize: 11,
              color: isOwn ? 'rgba(255,255,255,0.75)' : undefined,
            }}
          >
            <time dateTime={message.createdAt}>
              {formatTimestamp(message.createdAt)}
            </time>
          </Text>
        </div>
      </div>
    </article>
  );
}
