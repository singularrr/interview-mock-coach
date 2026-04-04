# interview-mock-coach

复试模拟官（interview-mock-coach）是一个面向研究生复试场景的前后端分离 Web demo。当前版本支持两条路线：

- 默认 `mock` 模式：无需 OpenAI Key、无需 PostgreSQL，启动即可演示
- 标准 RAG 模式：Spring AI + OpenAI ChatClient + OpenAI Embeddings + pgvector

## 项目定位

- 首页录入学校、专业、研究方向和简历要点
- 自动生成复试题目
- 模拟一题一答、评分与点评
- 生成总结并导出 Markdown
- 支持导入知识库资料并按上下文检索增强

## 技术栈

- 后端：Java 17, Spring Boot 3.4.x, Maven, JPA, H2, PostgreSQL, Spring AI
- 前端：Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router
- AI：Mock 模式 + OpenAI ChatClient 模式
- RAG：Spring AI Embeddings + pgvector，知识检索支持关键词兜底

## 目录结构

```text
interview-mock-coach/
├─ src/                      # Spring Boot backend
├─ frontend/                 # Vue frontend
├─ pom.xml
├─ README.md
└─ .gitignore
```

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18 LTS / 20 LTS
- npm（随 Node 安装）
- 如果使用 RAG 标准模式，还需要：
  - PostgreSQL 14+
  - OpenAI API Key

## 后端启动

### 1. 默认 mock 模式

```bash
cd d:\codex_test
mvn spring-boot:run
```

默认会以 `mock` 模式启动：
- 不需要 `DEEPSEEK_API_KEY`
- 不需要 PostgreSQL
- 所有题目生成、评分和总结都会使用规则引擎兜底

### 2. 标准 RAG 模式

```bash
cd d:\codex_test
mvn spring-boot:run -Dspring-boot.run.profiles=rag
```

启动前请准备：
- PostgreSQL 数据库 `interview_mock_coach`
- 环境变量 `DEEPSEEK_API_KEY`

## 前端启动

```bash
cd d:\codex_test\frontend
copy .env.example .env.local
npm.cmd install
npm.cmd run dev
```

前端默认地址：`http://localhost:5173`

如果你不想复制环境文件，也可以直接创建 `.env.local`：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## 页面说明

- 首页：填写学校、专业、研究方向、简历要点，生成模拟面试
- 模拟页：逐题作答、提交回答、查看点评
- 总结页：查看总评分、薄弱项、高频题型、改进建议，并导出 Markdown
- 知识库管理页：导入文本类资料并进行检索

## 知识库管理页

页面地址：`/knowledge`

支持功能：
- 导入知识文档
- 查看已导入文档列表
- 输入关键词检索相关上下文
- 自动结合当前候选人信息做辅助检索

支持的导入格式：`.txt`、`.md`、`.json`、`.csv`、`.pdf`

PDF 会在后端自动提取文本并切分入库，不需要你手动转成文本。

### 导入示例

```json
{
  "title": "某校计算机学院复试培养方案",
  "sourceType": "OFFICIAL",
  "sourceUrl": "https://example.com",
  "content": "这里放文档正文，后端会自动切分并入库"
}
```

## AI 与 ChatClient 配置说明
### 1. 以前没有接入 ChatClient 的时候是怎么工作的

最开始项目里是通过 `MockInterviewAiService` 做规则推理的：
- 根据学校、专业、研究方向和简历要点生成题目
- 根据回答长度、结构、关键词和报考方向做评分
- 根据题型和回答内容生成追问和总结

也就是说，**没有接入 ChatClient 时，默认就是 mock / 规则引擎推理**，不是调用真实大模型。

### 2. 现在 ChatClient 是怎么接入的

当前 `ai.mode=openai` 时，会使用 Spring AI 的 `ChatClient`：
- 先把候选人信息、检索到的知识库上下文拼成 prompt
- 再让模型输出结构化 JSON
- 后端把 JSON 解析回 DTO
- 如果 ChatClient 不可用、OpenAI Key 缺失、或者模型返回格式不对，系统会自动回退到 mock 规则引擎，保证 demo 不会挂

相关实现文件：
- `src/main/java/com/example/interviewmockcoach/service/ai/OpenAiInterviewAiService.java`
- `src/main/java/com/example/interviewmockcoach/service/ai/MockInterviewAiService.java`

### 3. ChatClient 相关依赖

`pom.xml` 里已经加入：
- `spring-ai-starter-model-openai`
- `spring-ai-starter-vector-store-pgvector`

### 4. OpenAI / Spring AI 配置项

#### `application.yml`

```yml
ai:
  mode: mock
  openai:
    base-url: https://api.deepseek.com
    api-key: ""
    model: deepseek-chat
    temperature: 0.3
rag:
  enabled: false
```

#### `application-rag.yml`

```yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/interview_mock_coach
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY:}
      chat:
        options:
          model: deepseek-chat
      embedding:
        options:
          model: text-embedding-3-small
    vectorstore:
      pgvector:
        initialize-schema: true
        dimensions: 1536
rag:
  enabled: true
```

### 5. 各配置项的作用

- `ai.mode`
  - `mock`：默认规则引擎
  - `openai`：启用 `ChatClient`
- `ai.openai.base-url`
  - OpenAI API 地址，默认 `https://api.deepseek.com`
- `ai.openai.api-key`
  - OpenAI Key
  - 这个是你在当前项目里自己填写的运行配置
- `ai.openai.model`
  - OpenAI 聊天模型名
- `ai.openai.temperature`
  - 生成随机性
- `rag.enabled`
  - `false`：使用关键词检索兜底
  - `true`：启用 pgvector + Spring AI 向量检索
- `spring.ai.openai.chat.options.model`
  - Spring AI ChatClient 使用的聊天模型
- `spring.ai.openai.embedding.options.model`
  - Embedding 模型
- `spring.ai.vectorstore.pgvector.initialize-schema`
  - 是否自动初始化 pgvector 相关表结构
- `spring.ai.vectorstore.pgvector.dimensions`
  - 向量维度，和 embedding 模型保持一致

## RAG 模式运行步骤

1. 启动 PostgreSQL
2. 创建数据库 `interview_mock_coach`
3. 设置环境变量 `DEEPSEEK_API_KEY`
4. 启动后端时带上 `rag` profile
5. 打开 `/knowledge` 导入文档
6. 再进入首页生成模拟面试

## 主要功能流程

1. 首页填写学校、专业、研究方向、简历要点。
2. 点击“生成模拟面试”。
3. 进入模拟问答页，逐题提交回答并查看评分点评。
4. 点击“生成总结”进入总结页。
5. 在总结页导出 Markdown。
6. 如果先导入了知识文档，题目生成和回答分析会参考检索到的上下文。

## 常见问题

- `npm.ps1` 被系统拦截：请在 PowerShell 中使用 `npm.cmd`
- 后端无法启动：确认已安装 JDK 17 和 Maven
- RAG 模式启动失败：确认 PostgreSQL 已启动、数据库已创建、`DEEPSEEK_API_KEY` 已配置
- 页面空白：确认后端已经启动，且前端 `VITE_API_BASE_URL` 指向正确地址
- 知识库检索没有结果：先确认已经导入过资料，并尝试更具体的关键词


