import { useEffect, useCallback, useRef } from 'react';
import { Modal, Upload, Progress, List, Alert, Button, Typography } from 'antd';
import { InboxOutlined, ReloadOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd';
import type { DocumentResponse } from '../../api/types';
import { ALLOWED_EXTENSIONS, MAX_FILE_SIZE } from '../../utils/constants';

const { Dragger } = Upload;
const { Text } = Typography;

interface DocumentUploadProps {
  isOpen: boolean;
  onClose: () => void;
  documents: DocumentResponse[];
  isUploading: boolean;
  progress: number;
  error: string | null;
  onUpload: (file: File) => Promise<void>;
  onLoadDocuments: () => Promise<void>;
  onClearError: () => void;
}

export default function DocumentUpload({
  isOpen,
  onClose,
  documents,
  isUploading,
  progress,
  error,
  onUpload,
  onLoadDocuments,
  onClearError,
}: DocumentUploadProps) {
  const hasLoaded = useRef(false);

  useEffect(() => {
    if (isOpen && !hasLoaded.current) {
      hasLoaded.current = true;
      onLoadDocuments();
    }
    if (!isOpen) {
      hasLoaded.current = false;
      onClearError();
    }
  }, [isOpen, onLoadDocuments, onClearError]);

  const handleFile = useCallback(
    (file: File) => {
      onUpload(file);
      return false;
    },
    [onUpload],
  );

  return (
    <Modal
      title="Upload de documentos"
      open={isOpen}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={520}
    >
      {error && (
        <Alert
          message={error}
          type="error"
          closable
          onClose={onClearError}
          role="alert"
          style={{ marginBottom: 16 }}
        />
      )}

      <Dragger
        accept={ALLOWED_EXTENSIONS.join(',')}
        beforeUpload={handleFile as any}
        showUploadList={false}
        disabled={isUploading}
        aria-label="Área de upload de documentos"
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">
          Clique ou arraste arquivos aqui
        </p>
        <p className="ant-upload-hint">
          PDF, TXT ou DOCX — máximo 10 MB
        </p>
      </Dragger>

      {isUploading && (
        <div style={{ marginTop: 16 }}>
          <Text type="secondary">Enviando...</Text>
          <Progress percent={progress} status="active" />
        </div>
      )}

      <div
        style={{
          marginTop: 24,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Text strong>Documentos enviados</Text>
        <Button
          size="small"
          icon={<ReloadOutlined />}
          onClick={onLoadDocuments}
          aria-label="Atualizar lista de documentos"
        />
      </div>

      <List
        style={{ marginTop: 8 }}
        locale={{ emptyText: 'Nenhum documento enviado' }}
        dataSource={documents}
        renderItem={(doc) => (
          <List.Item>
            <List.Item.Meta
              title={doc.originalFilename}
              description={`${(doc.sizeBytes / 1024).toFixed(1)} KB`}
            />
          </List.Item>
        )}
      />
    </Modal>
  );
}
