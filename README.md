# Graduate Interview Mock Coach（复试模拟官）

An AI-powered mock interview system for graduate school admission preparation.

一个面向研究生复试准备场景的 AI 模拟面试系统，支持信息录入、复试问题生成、多轮问答、回答评分与总结建议，适合作为课程设计、个人项目展示或复试相关 Demo。

---

## 1. Project Overview

Graduate Interview Mock Coach is a lightweight AI-assisted web application designed for graduate school interview preparation.  
Users can input their target university, major, research direction, and resume highlights, then the system generates likely interview questions and provides interactive mock interview practice, answer evaluation, and final feedback.

“复试模拟官” 是一个轻量级 AI Web 应用，面向研究生复试场景，帮助用户进行模拟问答训练。系统根据用户填写的院校、专业、研究方向和个人简历要点，生成常见复试问题，并对用户回答进行分析、评分和总结。

---

## 2. Core Features

- Candidate profile input  
  输入目标院校、专业、研究方向、简历要点

- AI-based interview question generation  
  根据用户背景生成复试常见问题

- Multi-round mock interview  
  支持一题一答的模拟问答流程

- Answer evaluation and scoring  
  对回答进行评分、优点分析、不足分析和改进建议

- Follow-up question hints  
  识别容易被老师继续追问的点

- Interview summary report  
  输出本次模拟训练的总结与薄弱项分析

- Markdown export for notes  
  支持将总结结果导出为 Markdown 面试笔记

---

## 3. Target Scenario

This project is designed for:

- Graduate school re-exam/interview preparation
- Personal AI application demos
- LLM-based educational assistant practice
- Portfolio projects for software engineering / AI application development

适用场景包括：

- 考研复试模拟训练
- AI 应用类课程设计或实验项目
- 大模型应用开发练手项目
- 个人简历与 GitHub 项目展示

---

## 4. Tech Stack

### Frontend
- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router

### Backend
- Java 17
- Spring Boot 3
- Maven

### AI Layer
- Mock interview service for local demo
- Optional OpenAI-based interview service integration

### Storage
- H2 / in-memory storage for MVP
- Can be extended to MySQL later

---

## 5. Project Structure

```text
interview-mock-coach/
├── backend/                 # Spring Boot backend
├── frontend/                # Vue frontend
├── docs/                    # project docs
├── prompts/                 # AI prompt templates
├── README.md
├── .gitignore
└── AGENTS.md
