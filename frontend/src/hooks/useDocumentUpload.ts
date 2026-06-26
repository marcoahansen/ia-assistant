import { useState, useCallback } from 'react';
import type { DocumentResponse } from '../api/types';
import { listDocuments, uploadDocument } from '../api/documentApi';
import { MAX_FILE_SIZE, ALLOWED_TYPES } from '../utils/constants';

export interface UseDocumentUploadReturn {
  documents: DocumentResponse[];
  isUploading: boolean;
  progress: number;
  error: string | null;
  uploadFile: (file: File) => Promise<void>;
  loadDocuments: () => Promise<void>;
  clearError: () => void;
}

export function useDocumentUpload(): UseDocumentUploadReturn {
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => setError(null), []);

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
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : 'Erro ao fazer upload';
        setError(message);
      } finally {
        setIsUploading(false);
      }
    },
    [validateFile, clearError],
  );

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

  return {
    documents,
    isUploading,
    progress,
    error,
    uploadFile,
    loadDocuments,
    clearError,
  };
}
