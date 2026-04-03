# interview-mock-coach

复试模拟官（interview-mock-coach）是一个面向研究生复试场景的前后端分离 Web demo。项目支持信息录入、复试题目生成、多轮问答、回答评分、总结与 Markdown 导出。

## 技术栈

- 后端：Java 17, Spring Boot 3, Maven, H2
- 前端：Vue 3, Vite, TypeScript, Element Plus, Pinia, Vue Router
- AI：Mock 模式默认可用，OpenAI 模式保留扩展点

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
- Maven 3.9+（或安装后再使用 `mvn`）
- Node.js 18 LTS / 20 LTS
- npm（随 Node 安装）

## 启动后端

```bash
cd d:\codex_test
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

## 启动前端

```bash
cd d:\codex_test\frontend
copy .env.example .env.local
npm.cmd install
npm.cmd run dev
```

如果你不想复制环境文件，也可以直接在本地创建 `.env.local`，内容如下：

```bash
VITE_API_BASE_URL=http://localhost:8080
```

前端默认地址：`http://localhost:5173`

## mock 模式与真实模式切换

后端配置文件：`src/main/resources/application.yml`

- mock 模式：

```yml
ai:
  mode: mock
```

- 真实 OpenAI 模式：

```yml
ai:
  mode: openai
```

OpenAI 相关配置也在同一个文件中预留了：`base-url`、`api-key`、`model`、`temperature`。

## 主要功能流程

1. 首页填写学校、专业、研究方向、简历要点。
2. 点击“生成模拟面试”。
3. 进入模拟问答页，逐题提交回答并查看评分点评。
4. 点击“生成总结”进入总结页。
5. 在总结页导出 Markdown。

## 常见问题

- `npm.ps1` 被系统拦截：请在 PowerShell 中使用 `npm.cmd`。
- 后端无法启动：确认已安装 JDK 17 和 Maven。
- 页面空白：确认后端已经启动，且前端 `VITE_API_BASE_URL` 指向正确地址。
