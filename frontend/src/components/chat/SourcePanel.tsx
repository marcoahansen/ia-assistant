import { Collapse, Typography, Tag } from 'antd';
import type { SourceDTO } from '../../api/types';

const { Text, Paragraph } = Typography;

interface SourcePanelProps {
  sources: SourceDTO[];
}

function scoreColor(score: number): string {
  if (score >= 0.8) return 'green';
  if (score >= 0.6) return 'orange';
  return 'red';
}

export default function SourcePanel({ sources }: SourcePanelProps) {
  if (!sources || sources.length === 0) return null;

  const items = sources.map((src, i) => ({
    key: src.chunkId,
    label: (
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Text strong>{src.documentName}</Text>
        <Tag color={scoreColor(src.similarityScore)}>
          {(src.similarityScore * 100).toFixed(0)}%
        </Tag>
      </div>
    ),
    children: (
      <div>
        <Text type="secondary" style={{ fontSize: 12 }}>
          Chunk: {src.chunkId.slice(0, 8)}&hellip;
        </Text>
        <Paragraph
          style={{ marginTop: 4, marginBottom: 0, fontSize: 13 }}
          ellipsis={{ rows: 4, expandable: true, symbol: 'Mostrar mais' }}
        >
          {src.chunkContent}
        </Paragraph>
      </div>
    ),
  }));

  return (
    <div style={{ marginTop: 8 }}>
      <Collapse
        ghost
        size="small"
        items={[
          {
            key: 'sources',
            label: (
              <Text type="secondary" style={{ fontSize: 12 }}>
                Fontes ({sources.length})
              </Text>
            ),
            children: <Collapse ghost size="small" items={items} />,
          },
        ]}
      />
    </div>
  );
}
