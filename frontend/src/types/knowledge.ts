export interface KnowledgeDocument {
  documentId: string
  title: string
  sourceType: string
  sourceUrl: string
  chunkCount: number
  createdAt: string
}

export interface RetrievedContext {
  chunkId: string
  documentId: string
  documentTitle: string
  sourceType: string
  content: string
  score: number
}

export interface IngestKnowledgeDocumentRequest {
  title: string
  content: string
  sourceType?: string
  sourceUrl?: string
}