export interface MessageDetail {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
}

export interface ChatMessageRequest {
  conversationId?: string | null;
  content: string;
}

export interface SourceDTO {
  chunkId: string;
  chunkContent: string;
  documentId: string;
  documentName: string;
  similarityScore: number;
}

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface ChatMessageResponse {
  conversationId: string;
  userMessage: MessageDetail;
  assistantMessage: MessageDetail;
  sources?: SourceDTO[];
}

export interface DocumentStatusResponse {
  id: string;
  originalFilename: string;
  status: DocumentStatus;
  chunksCount: number | null;
  uploadedAt: string;
}

export interface ConversationSummary {
  id: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationListResponse {
  conversations: ConversationSummary[];
  total: number;
}

export interface ConversationResponse {
  id: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
  messages: MessageDetail[];
}

export interface DocumentResponse {
  id: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  status: DocumentStatus;
  chunksCount: number | null;
  uploadedAt: string;
}

export interface DocumentListResponse {
  documents: DocumentResponse[];
  total: number;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
