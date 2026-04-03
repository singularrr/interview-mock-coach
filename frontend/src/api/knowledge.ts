import { get, post } from './http'
import type { ApiResponse } from '@/types/api'
import type { KnowledgeDocument, RetrievedContext, IngestKnowledgeDocumentRequest } from '@/types/knowledge'

export async function ingestKnowledgeDocument(request: IngestKnowledgeDocumentRequest) {
  return post<ApiResponse<KnowledgeDocument>, IngestKnowledgeDocumentRequest>('/api/knowledge-documents/ingest', request)
}

export async function listKnowledgeDocuments() {
  return get<ApiResponse<KnowledgeDocument[]>>('/api/knowledge-documents')
}

export async function searchKnowledgeDocuments(params: { query: string; profileContext?: string; topK?: number }) {
  const searchParams = new URLSearchParams()
  searchParams.set('query', params.query)
  if (params.profileContext) {
    searchParams.set('profileContext', params.profileContext)
  }
  searchParams.set('topK', String(params.topK ?? 4))
  return get<ApiResponse<RetrievedContext[]>>(`/api/knowledge-documents/search?${searchParams.toString()}`)
}