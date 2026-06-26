import { useState, useCallback } from 'react';
import { Layout, Switch, Typography, List, Drawer, Button } from 'antd';
import {
  MoonOutlined,
  SunOutlined,
  BulbOutlined,
  MessageOutlined,
  PlusOutlined,
  MenuOutlined,
} from '@ant-design/icons';
import { useTheme } from '../../contexts/ThemeContext';
import type { ConversationSummary } from '../../api/types';
import { formatTimestamp } from '../../utils/formatTimestamp';
import type { ReactNode } from 'react';

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

interface ChatLayoutProps {
  children: ReactNode;
  conversations: ConversationSummary[];
  activeId: string | null;
  onSelect: (id: string) => void;
  onNewChat: () => void;
}

export default function ChatLayout({
  children,
  conversations,
  activeId,
  onSelect,
  onNewChat,
}: ChatLayoutProps) {
  const { mode, toggle } = useTheme();
  const [collapsed, setCollapsed] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const handleSelect = useCallback(
    (id: string) => {
      onSelect(id);
      setDrawerOpen(false);
    },
    [onSelect],
  );

  const handleNewChat = useCallback(() => {
    onNewChat();
    setDrawerOpen(false);
  }, [onNewChat]);

  const sidebarContent = (
    <div
      style={{
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        height: '100%',
      }}
    >
      <Button
        type="dashed"
        icon={<PlusOutlined />}
        block
        onClick={handleNewChat}
        aria-label="Nova conversa"
      >
        Nova conversa
      </Button>

      <Text
        type="secondary"
        style={{
          textTransform: 'uppercase',
          fontSize: 12,
          marginTop: 8,
        }}
      >
        Conversas
      </Text>

      <List
        dataSource={conversations}
        locale={{ emptyText: 'Nenhuma conversa' }}
        renderItem={(item) => (
          <List.Item
            key={item.id}
            onClick={() => handleSelect(item.id)}
            style={{
              cursor: 'pointer',
              padding: '8px 12px',
              borderRadius: 6,
              background:
                item.id === activeId
                  ? 'var(--ant-color-primary-bg)'
                  : 'transparent',
              border:
                item.id === activeId
                  ? '1px solid var(--ant-color-primary-border)'
                  : '1px solid transparent',
              transition: 'all 0.2s',
            }}
          >
            <List.Item.Meta
              avatar={
                <MessageOutlined
                  style={{
                    color: 'var(--ant-color-text-secondary)',
                    fontSize: 16,
                  }}
                />
              }
              title={
                <Text
                  style={{
                    fontSize: 13,
                    fontWeight: item.id === activeId ? 600 : 400,
                    color: 'var(--ant-color-text)',
                  }}
                  ellipsis
                >
                  {item.title || 'Sem título'}
                </Text>
              }
              description={
                <Text
                  type="secondary"
                  style={{ fontSize: 11 }}
                >
                  {formatTimestamp(item.updatedAt)}
                </Text>
              }
            />
          </List.Item>
        )}
      />
    </div>
  );

  return (
    <Layout style={{ height: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
          background: 'var(--ant-color-bg-container)',
          borderBottom: '1px solid var(--ant-color-border)',
          height: 56,
          lineHeight: '56px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {collapsed && (
            <Button
              type="text"
              icon={<MenuOutlined />}
              onClick={() => setDrawerOpen(true)}
              aria-label="Abrir lista de conversas"
              style={{ fontSize: 18 }}
            />
          )}
          <BulbOutlined
            style={{ fontSize: 20, color: 'var(--ant-color-primary)' }}
          />
          <Title level={4} style={{ margin: 0 }}>
            Assistente IA
          </Title>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SunOutlined
            style={{
              color: mode === 'light' ? 'var(--ant-color-primary)' : undefined,
            }}
          />
          <Switch
            checked={mode === 'dark'}
            onChange={toggle}
            aria-label="Alternar tema claro/escuro"
            checkedChildren={<MoonOutlined />}
            unCheckedChildren={<SunOutlined />}
          />
          <MoonOutlined
            style={{
              color: mode === 'dark' ? 'var(--ant-color-primary)' : undefined,
            }}
          />
        </div>
      </Header>

      <Layout>
        <Sider
          width={240}
          breakpoint="md"
          collapsedWidth={0}
          onCollapse={setCollapsed}
          trigger={null}
          style={{
            background: 'var(--ant-color-bg-container)',
            borderRight: '1px solid var(--ant-color-border)',
          }}
        >
          {sidebarContent}
        </Sider>

        <Content
          id="main-chat"
          role="main"
          aria-label="Área de chat"
          style={{
            display: 'flex',
            flexDirection: 'column',
            background: 'var(--ant-color-bg-layout)',
            position: 'relative',
          }}
        >
          {children}
        </Content>
      </Layout>

      <Drawer
        title="Conversas"
        placement="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={280}
        styles={{ body: { padding: 0 } }}
      >
        {sidebarContent}
      </Drawer>
    </Layout>
  );
}
