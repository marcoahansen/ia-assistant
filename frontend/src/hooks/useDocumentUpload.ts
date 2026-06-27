import { useState, useCallback, useRef } from 'react';
import type { DocumentResponse, DocumentStatus } from '../api/types';
import { listDocuments, uploadDocument, getDocumentStatus } from '../api/documentApi';
import { MAX_FILE_SIZE, ALLOWED_TYPES } from '../utils/constants';

export interface DocumentStatusInfo {
  status: DocumentStatus;
  chunksCount: number | null;
}

export interface UseDocumentUploadReturn {
  documents: DocumentResponse[];
  documentStatuses: Record<string, DocumentStatusInfo>;
  isUploading: boolean;
  progress: number;
  error: string | null;
  uploadFile: (file: File) => Promise<void>;
  loadDocuments: () => Promise<void>;
  clearError: () => void;
}

const POLL_INTERVAL = 5000;

export function useDocumentUpload(): UseDocumentUploadReturn {
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [documentStatuses, setDocumentStatuses] = useState<Record<string, DocumentStatusInfo>>({});
  const [isUploading, setIsUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const pollingRef = useRef<Record<string, ReturnType<typeof setInterval>>>({});

  const clearError = useCallback(() => setError(null), []);

  const startPolling = useCallback((docId: string) => {
    if (pollingRef.current[docId]) return;

    pollingRef.current[docId] = setInterval(async () => {
      try {
        const statusResp = await getDocumentStatus(docId);
        setDocumentStatuses((prev) => ({
          ...prev,
          [docId]: {
            status: statusResp.status,
            chunksCount: statusResp.chunksCount,
          },
        }));

        if (statusResp.status === 'COMPLETED' || statusResp.status === 'FAILED') {
          clearInterval(pollingRef.current[docId]);
          delete pollingRef.current[docId];
          loadDocuments();
        }
      } catch {
        clearInterval(pollingRef.current[docId]);
        delete pollingRef.current[docId];
      }
    }, POLL_INTERVAL);
  }, []);

  const loadDocuments = useCallback(async () => {
    try {
      const response = await listDocuments();
      setDocuments(response.documents);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Erro ao carregar documentos';
      setError(message);
    }
  }, []);

  const validateFile = useCallback((file: File): string | null => {
    if (!ALLOWED_TYPES.includes(file.type as never)) {
      return 'Tipo de arquivo não suportado. Aceitamos: PDF, TXT, DOCX.';
    }
    if (file.size > MAX_FILE_SIZE) {
      return 'Arquivo muito grande. Tamanho máximo: 10 MB.';
    }
    return null;
  }, []);

  const uploadFile = useCallback(
    async (file: File) => {
      const validationError = validateFile(file);
      if (validationError) {
        setError(validationError);
        return;
      }

      setIsUploading(true);
      setProgress(0);
      clearError();

      try {
        const doc = await uploadDocument(file, (percent) => {
          setProgress(percent);
        });
        setDocuments((prev) => [...prev, doc]);
        startPolling(doc.id);
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : 'Erro ao fazer upload';
        setError(message);
      } finally {
        setIsUploading(false);
      }
    },
    [validateFile, clearError, startPolling],
  );

  return {
    documents,
    documentStatuses,
    isUploading,
    progress,
    error,
    uploadFile,
    loadDocuments,
    clearError,
  };
}
