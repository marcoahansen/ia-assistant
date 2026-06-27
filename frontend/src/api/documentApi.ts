import { apiClient } from './client';
import type { DocumentListResponse, DocumentResponse, DocumentStatusResponse } from './types';
import { API_BASE_URL } from '../utils/constants';
import { ApiError } from './types';

export async function listDocuments(): Promise<DocumentListResponse> {
  return apiClient<DocumentListResponse>('/documents');
}

export async function getDocumentStatus(id: string): Promise<DocumentStatusResponse> {
  return apiClient<DocumentStatusResponse>(`/documents/${id}/status`);
}

export async function uploadDocument(
  file: File,
  onProgress?: (percent: number) => void,
): Promise<DocumentResponse> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (event) => {
      if (event.lengthComputable && onProgress) {
        onProgress(Math.round((event.loaded / event.total) * 100));
      }
    });

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(JSON.parse(xhr.responseText));
      } else {
        const error = JSON.parse(xhr.responseText);
        reject(new ApiError(xhr.status, error.message ?? 'Erro no upload'));
      }
    });

    xhr.addEventListener('error', () => {
      reject(new ApiError(0, 'Erro de conexão durante upload'));
    });

    xhr.open('POST', `${API_BASE_URL}/documents/upload`);
    const formData = new FormData();
    formData.append('file', file);
    xhr.send(formData);
  });
}
