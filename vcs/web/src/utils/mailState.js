// 共享的信箱未讀數字（relax.vue 寫入，layout.vue 讀取）
import { ref } from 'vue'
export const mailUnreadCount = ref(0)
