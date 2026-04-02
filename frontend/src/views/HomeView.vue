<template>
  <div class="page-shell">
    <div class="page-container">
      <PageHeader
        title="复试模拟官"
        subtitle="面向研究生复试场景的模拟面试系统，先生成题目，再进行回答和总结。"
      >
        <template #extra>
          <el-tag type="primary" effect="plain">MVP Demo</el-tag>
        </template>
      </PageHeader>

      <el-card shadow="never">
        <el-form label-position="top" :model="profile" :rules="rules" ref="formRef">
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="学校" prop="school">
                <el-input v-model="profile.school" placeholder="请输入学校" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="专业" prop="major">
                <el-input v-model="profile.major" placeholder="请输入专业" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="研究方向" prop="researchDirection">
                <el-input v-model="profile.researchDirection" placeholder="请输入研究方向" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="简历要点" prop="resumePoints">
                <el-input
                  v-model="profile.resumePoints"
                  type="textarea"
                  :rows="5"
                  resize="none"
                  placeholder="请输入个人简历要点，多行文本更适合复试场景"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="action-row">
            <ExampleFillButton @fill="fillExample" />
            <el-button type="primary" :loading="loading" @click="generateInterviewHandler">生成模拟面试</el-button>
            <el-button @click="resetFormHandler">清空</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ExampleFillButton from '@/components/ExampleFillButton.vue'
import { useInterviewStore } from '@/store/interview'

const router = useRouter()
const interviewStore = useInterviewStore()
const profile = interviewStore.profile
const loading = interviewStore.loading
const formRef = ref<FormInstance>()

const rules = reactive<FormRules>({
  school: [{ required: true, message: '请输入学校', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  researchDirection: [{ required: true, message: '请输入研究方向', trigger: 'blur' }],
  resumePoints: [{ required: true, message: '请输入简历要点', trigger: 'blur' }],
})

function fillExample() {
  interviewStore.fillExampleProfile()
  ElMessage.success('已填充示例数据')
}

function resetFormHandler() {
  profile.school = ''
  profile.major = ''
  profile.researchDirection = ''
  profile.resumePoints = ''
}

async function generateInterviewHandler() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  try {
    await interviewStore.createInterview(8)
    await router.push('/interview')
  } catch {
    ElMessage.error('生成模拟面试失败，请检查后端是否启动')
  }
}
</script>
