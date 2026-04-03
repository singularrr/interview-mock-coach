import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import InterviewView from '@/views/InterviewView.vue'
import SummaryView from '@/views/SummaryView.vue'
import KnowledgeLibraryView from '@/views/KnowledgeLibraryView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/interview', name: 'interview', component: InterviewView },
    { path: '/summary', name: 'summary', component: SummaryView },
    { path: '/knowledge', name: 'knowledge', component: KnowledgeLibraryView },
  ],
})

export default router
