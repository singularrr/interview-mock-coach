<template>
  <div class="page-shell">
    <div class="page-container">
      <PageHeader
        title="知识库管理"
        subtitle="导入复试资料、查看文档列表，并检索与当前候选人背景相关的上下文。"
      >
        <template #extra>
          <div class="action-row">
            <el-button @click="goHome">返回首页</el-button>
            <el-button plain @click="goInterview">去模拟面试</el-button>
            <el-button type="primary" plain :loading="loadingDocuments" @click="loadDocuments">刷新列表</el-button>
          </div>
        </template>
      </PageHeader>

      <el-alert
        class="section-gap"
        title="当前页面支持导入文本型知识资料，系统会自动切分并进入检索流程。"
        type="info"
        show-icon
        :closable="false"
      />

      <el-row :gutter="16" class="section-gap">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <div class="card-title" style="margin-bottom: 0;">
                <strong>导入知识文档</strong>
                <el-tag type="success" effect="plain">上传资料</el-tag>
              </div>
            </template>

            <el-form label-position="top">
              <el-form-item label="标题">
                <el-input v-model="ingestForm.title" placeholder="例如：某校计算机学院复试培养方案" />
              </el-form-item>

              <el-row :gutter="12">
                <el-col :xs="24" :md="12">
                  <el-form-item label="来源类型">
                    <el-select v-model="ingestForm.sourceType" placeholder="请选择来源类型">
                      <el-option label="官方资料" value="OFFICIAL" />
                      <el-option label="复试攻略" value="GUIDE" />
                      <el-option label="导师介绍" value="PROFESSOR" />
                      <el-option label="个人笔记" value="NOTE" />
                      <el-option label="其他" value="GENERAL" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="来源链接">
                    <el-input v-model="ingestForm.sourceUrl" placeholder="可选，填写文档来源链接" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="文件导入">
                <div class="stack" style="width: 100%; gap: 8px;">
                  <input ref="fileInputRef" type="file" accept=".txt,.md,.json,.csv" @change="handleFileChange" />
                  <div class="muted-text">支持导入文本类文件，PDF 请先转成 txt/md 后再导入。</div>
                </div>
              </el-form-item>

              <el-form-item label="文档内容">
                <el-input
                  v-model="ingestForm.content"
                  :rows="12"
                  placeholder="将文档正文粘贴到这里，或者通过文件导入填充"
                  resize="none"
                  type="textarea"
                />
              </el-form-item>

              <div class="action-row">
                <el-button type="primary" :loading="ingesting" @click="handleIngest">导入知识文档</el-button>
                <el-button @click="fillExample">填充示例资料</el-button>
                <el-button @click="clearIngestForm">清空</el-button>
              </div>
            </el-form>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <div class="card-title" style="margin-bottom: 0;">
                <strong>知识检索</strong>
                <el-tag type="warning" effect="plain">RAG Search</el-tag>
              </div>
            </template>

            <el-form label-position="top">
              <el-form-item label="检索关键词">
                <el-input v-model="searchForm.query" placeholder="例如：培养方案、导师方向、复试高频题" />
              </el-form-item>
              <el-form-item label="候选人上下文">
                <el-input
                  v-model="searchForm.profileContext"
                  :rows="5"
                  placeholder="可留空，默认会使用当前会话里的学校/专业/研究方向"
                  resize="none"
                  type="textarea"
                />
              </el-form-item>
              <el-row :gutter="12">
                <el-col :xs="24" :md="12">
                  <el-form-item label="返回条数">
                    <el-input-number v-model="searchForm.topK" :max="10" :min="1" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="当前候选人">
                    <el-input :model-value="currentProfileSummary" readonly />
                  </el-form-item>
                </el-col>
              </el-row>

              <div class="action-row">
                <el-button type="primary" :loading="searching" @click="handleSearch">检索资料</el-button>
                <el-button @click="fillSearchFromProfile">使用当前候选人信息</el-button>
                <el-button @click="clearSearchResult">清空结果</el-button>
              </div>
            </el-form>

            <el-divider />

            <el-empty v-if="!searchResults.length" description="输入关键词后点击检索" />
            <div v-else class="stack">
              <el-card v-for="item in searchResults" :key="item.chunkId" shadow="never">
                <template #header>
                  <div class="card-title" style="margin-bottom: 0;">
                    <strong>{{ item.documentTitle || item.documentId }}</strong>
                    <el-space wrap>
                      <el-tag size="small" type="info">{{ item.sourceType }}</el-tag>
                      <el-tag size="small" type="success">得分 {{ item.score.toFixed(2) }}</el-tag>
                    </el-space>
                  </div>
                </template>
                <div class="muted-text" style="line-height: 1.8; white-space: pre-wrap;">{{ item.content }}</div>
              </el-card>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="section-gap" shadow="never">
        <template #header>
          <div class="card-title" style="margin-bottom: 0;">
            <strong>已导入知识文档</strong>
            <el-tag type="primary" effect="plain">{{ documents.length }} 条</el-tag>
          </div>
        </template>

        <el-empty v-if="!documents.length" description="当前还没有导入任何知识文档" />

        <el-table v-else :data="documents" stripe>
          <el-table-column label="标题" prop="title" min-width="220" />
          <el-table-column label="来源类型" prop="sourceType" width="140" />
          <el-table-column label="来源链接" prop="sourceUrl" min-width="220" />
          <el-table-column label="切分块数" prop="chunkCount" width="100" align="center" />
          <el-table-column label="导入时间" prop="createdAt" min-width="180" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { useInterviewStore } from '@/store/interview'
import {
  ingestKnowledgeDocument,
  listKnowledgeDocuments,
  searchKnowledgeDocuments,
} from '@/api/knowledge'
import type { KnowledgeDocument, RetrievedContext } from '@/types/knowledge'

const router = useRouter()
const interviewStore = useInterviewStore()
const fileInputRef = ref<HTMLInputElement | null>(null)
const ingesting = ref(false)
const searching = ref(false)
const loadingDocuments = ref(false)
const documents = ref<KnowledgeDocument[]>([])
const searchResults = ref<RetrievedContext[]>([])

const ingestForm = reactive({
  title: '',
  sourceType: 'OFFICIAL',
  sourceUrl: '',
  content: '',
})

const searchForm = reactive({
  query: '',
  profileContext: '',
  topK: 4,
})

const currentProfileSummary = computed(() => {
  const profile = interviewStore.profile
  if (!profile.school && !profile.major && !profile.researchDirection) {
    return '暂无候选人信息'
  }
  return [profile.school, profile.major, profile.researchDirection].filter(Boolean).join(' / ')
})

function goHome() {
  router.push('/')
}

function goInterview() {
  router.push('/interview')
}

function fillExample() {
  ingestForm.title = '某校计算机学院复试培养方案'
  ingestForm.sourceType = 'OFFICIAL'
  ingestForm.sourceUrl = 'https://example.com/graduate-admission'
  ingestForm.content = [
    '1. 复试重点考察专业基础、科研潜力和表达能力。',
    '2. 推荐结合本科课程、项目经历和研究兴趣进行准备。',
    '3. 常见追问包括：为什么选择本校、本专业、研究方向和未来规划。',
    '4. 建议关注学院官网、导师主页和培养方案中的关键信息。',
  ].join('\n')
  ElMessage.success('已填充示例资料')
}

function fillSearchFromProfile() {
  const profile = interviewStore.profile
  searchForm.profileContext = [profile.school, profile.major, profile.researchDirection, profile.resumePoints]
    .filter(Boolean)
    .join(' ')
  ElMessage.success('已使用当前候选人信息')
}

function clearIngestForm() {
  ingestForm.title = ''
  ingestForm.sourceType = 'OFFICIAL'
  ingestForm.sourceUrl = ''
  ingestForm.content = ''
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function clearSearchResult() {
  searchResults.value = []
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  const text = await file.text()
  ingestForm.content = text
  if (!ingestForm.title) {
    ingestForm.title = file.name.replace(/\.[^.]+$/, '')
  }
  ElMessage.success(`已读取文件：${file.name}`)
}

async function loadDocuments() {
  loadingDocuments.value = true
  try {
    const response = await listKnowledgeDocuments()
    documents.value = response.data ?? []
  } catch {
    ElMessage.error('加载知识文档失败')
  } finally {
    loadingDocuments.value = false
  }
}

async function handleIngest() {
  if (!ingestForm.title.trim() || !ingestForm.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  ingesting.value = true
  try {
    await ingestKnowledgeDocument({
      title: ingestForm.title.trim(),
      sourceType: ingestForm.sourceType,
      sourceUrl: ingestForm.sourceUrl.trim() || undefined,
      content: ingestForm.content,
    })
    ElMessage.success('知识文档已导入')
    await loadDocuments()
  } catch {
    ElMessage.error('导入知识文档失败')
  } finally {
    ingesting.value = false
  }
}

async function handleSearch() {
  if (!searchForm.query.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  searching.value = true
  try {
    const response = await searchKnowledgeDocuments({
      query: searchForm.query.trim(),
      profileContext: searchForm.profileContext.trim() || undefined,
      topK: searchForm.topK,
    })
    searchResults.value = response.data ?? []
    if (!searchResults.value.length) {
      ElMessage.info('没有检索到匹配结果')
    }
  } catch {
    ElMessage.error('检索资料失败')
  } finally {
    searching.value = false
  }
}

onMounted(async () => {
  fillSearchFromProfile()
  await loadDocuments()
})
</script>