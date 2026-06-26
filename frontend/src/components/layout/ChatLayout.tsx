import { useState } from 'react';
import { Layout, Switch, Typography } from 'antd';
import { MoonOutlined, SunOutlined, BulbOutlined } from '@ant-design/icons';
import { useTheme } from '../../contexts/ThemeContext';
import type { ReactNode } from 'react';

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

interface ChatLayoutProps {
  children: ReactNode;
}

export default function ChatLayout({ children }: ChatLayoutProps) {
  const { mode, toggle } = useTheme();
  const [collapsed, setCollapsed] = useState(false);

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
          <BulbOutlined style={{ fontSize: 20, color: 'var(--ant-color-primary)' }} />
          <Title level={4} style={{ margin: 0 }}>
            Assistente IA
          </Title>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SunOutlined style={{ color: mode === 'light' ? 'var(--ant-color-primary)' : undefined }} />
          <Switch
            checked={mode === 'dark'}
            onChange={toggle}
            aria-label="Alternar tema claro/escuro"
            checkedChildren={<MoonOutlined />}
            unCheckedChildren={<SunOutlined />}
          />
          <MoonOutlined style={{ color: mode === 'dark' ? 'var(--ant-color-primary)' : undefined }} />
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
          <div
            style={{
              padding: 16,
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
              height: '100%',
            }}
          >
            <Text type="secondary" style={{ textTransform: 'uppercase', fontSize: 12 }}>
              Conversas
            </Text>
            <Text type="secondary" style={{ fontStyle: 'italic', fontSize: 13 }}>
              Em breve...
            </Text>
          </div>
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
    </Layout>
  );
}
