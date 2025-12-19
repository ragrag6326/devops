<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'
import { healthCheck, getCurrentTraffic } from '@/api/monitor'
import { ElMessage } from 'element-plus'

const router = useRouter()
const projectNames = ['tv', 'go-api', 'go-nuxt']
const projects = ref([])
const loading = ref(false)

const goToDetail = (name) => {
  router.push(`/system/monitor/${name}`)
}

// 修正：手動刷新直接調用請求函數
const refreshAll = () => {
  fetchAllStatus()
  ElMessage.success('監控數據已更新')
}

const fetchAllStatus = async () => {
  loading.value = true
  try {
    const projectPromises = projectNames.map(async (name) => {
      // 併發請求：藍機、綠機、正式分流
      const [blueHealth, greenHealth, trafficRes] = await Promise.all([
        healthCheck(name, 'blue'),
        healthCheck(name, 'green'),
        getCurrentTraffic(name, 'live')
      ])

      return {
        name: name,
        blueStatus: blueHealth.data,   // 200 或其他
        greenStatus: greenHealth.data, // 200 或其他
        activeTraffic: trafficRes.data // "BLUE_ACTIVE" 或 "GREEN_ACTIVE"
      }
    })

    projects.value = await Promise.all(projectPromises)
  } catch (error) {
    console.error('監控數據抓取失敗:', error)
    ElMessage.error('部分服務數據獲取失敗')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAllStatus()
})
</script>

<template>
  <div class="overview-container">
    <div class="page-header">
      <h2 class="title">服務監控中心</h2>
      <el-button type="primary" plain size="small" :icon="Refresh" @click="refreshAll">
        手動刷新
      </el-button>
    </div>

    <div class="project-grid" v-loading="loading">
      <div 
        v-for="proj in projects" 
        :key="proj.name" 
        class="glass-card project-card"
        @click="goToDetail(proj.name)"
      >
        <div class="card-header">
          <span class="proj-name">{{ proj.name }}</span>
          <div class="status-group">
            <el-tooltip :content="'Blue Node: ' + proj.blueStatus">
              <div class="node-dot" :class="proj.blueStatus === 200 ? 'online' : 'offline'">B</div>
            </el-tooltip>
            <el-tooltip :content="'Green Node: ' + proj.greenStatus">
              <div class="node-dot" :class="proj.greenStatus === 200 ? 'online' : 'offline'">G</div>
            </el-tooltip>
          </div>
        </div>
        
        <div class="card-body">
          <div class="info-item">
            <span class="label">當前流量指向</span>
            <el-tag :type="proj.activeTraffic === 'BLUE_ACTIVE' ? 'primary' : 'success'" size="small" effect="dark">
              {{ proj.activeTraffic === 'BLUE_ACTIVE' ? 'BLUE' : 'GREEN' }}
            </el-tag>
          </div>
          
          <div class="info-item">
            <span class="label">節點狀態摘要</span>
            <span class="value-summary">
              <span :class="proj.blueStatus === 200 ? 'text-ok' : 'text-err'">B</span>
              <span class="sep">/</span>
              <span :class="proj.greenStatus === 200 ? 'text-ok' : 'text-err'">G</span>
            </span>
          </div>
        </div>

        <div class="card-footer">
          <span>進入控制面板</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overview-container { padding: 10px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.title { 
  font-size: 22px; 
  font-weight: 600; 
  background: linear-gradient(to right, #fff, #94a3b8); 
  -webkit-background-clip: text; 
  -webkit-text-fill-color: transparent; 
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

/* --- 玻璃卡片 --- */
.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.glass-card:hover {
  transform: translateY(-5px);
  border-color: var(--primary-color);
  background: rgba(99, 102, 241, 0.1);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

/* --- 卡片頭部與雙燈 --- */
.card-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 20px; 
}
.proj-name { font-weight: 700; font-size: 17px; color: #fff; letter-spacing: 0.5px; }

.status-group { display: flex; gap: 8px; }

.node-dot {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  position: relative;
}

.node-dot.online { 
  background: #4ade80; 
  box-shadow: 0 0 8px rgba(74, 222, 128, 0.5); 
}
.node-dot.offline { 
  background: #f87171; 
  box-shadow: 0 0 8px rgba(248, 113, 113, 0.5); 
  animation: flash 1.5s infinite;
}

@keyframes flash {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* --- 卡片內容 --- */
.info-item { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 12px; 
  font-size: 14px; 
}
.label { color: var(--text-sub); }

.value-summary { font-weight: bold; font-size: 13px; }
.text-ok { color: #4ade80; }
.text-err { color: #f87171; }
.sep { margin: 0 4px; color: rgba(255,255,255,0.1); }

.card-footer {
  margin-top: 10px; 
  padding-top: 15px;
  border-top: 1px solid rgba(255,255,255,0.05);
  display: flex; 
  justify-content: space-between;
  font-size: 12px; 
  color: var(--text-sub);
}
</style>