import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const followUpText = ref('')

  function setFollowUpText(text: string) {
    followUpText.value = text
  }

  function clearFollowUpText() {
    followUpText.value = ''
  }

  return {
    followUpText,
    setFollowUpText,
    clearFollowUpText,
  }
})
