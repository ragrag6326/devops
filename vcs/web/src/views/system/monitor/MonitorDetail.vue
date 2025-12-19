<script setup>
import { ref, computed, onActivated, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Switch, CircleCheck, Warning } from '@element-plus/icons-vue'
import { switchTraffic, getCurrentTraffic, healthCheck } from '@/api/monitor'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const projectName = computed(() => route.params.projectName)

// 響應式數據結構
const data = ref({
    blueHealth: null,
    greenHealth: null,
    liveTraffic: '',   // 正式流量: BLUE_ACTIVE / GREEN_ACTIVE
    headerTraffic: '', // Header流量: BLUE_ACTIVE / GREEN_ACTIVE
    loading: false,
    switchMode: ''     // 預設切換模式 (空字串為正式, 'header' 為測試)
})

// 核心：獲取完整細節狀態
const loadData = async () => {
    data.value.loading = true
    try {
        const [hBlue, hGreen, tLive, tHeader] = await Promise.all([
            healthCheck(projectName.value, 'blue'),
            healthCheck(projectName.value, 'green'),
            getCurrentTraffic(projectName.value, 'live'),
            getCurrentTraffic(projectName.value, 'header')
        ])
        data.value.blueHealth = hBlue.data
        data.value.greenHealth = hGreen.data
        data.value.liveTraffic = tLive.data
        data.value.headerTraffic = tHeader.data
    } catch (err) {
        ElMessage.error('數據加載失敗，請檢查網路或 API')
    } finally {
        data.value.loading = false
    }
}

/**
 * 執行切換
 * @param target 'BLUE' | 'GREEN'
 */
const execSwitch = async (target) => {
    const isHeader = data.value.switchMode === 'header';
    const modeText = isHeader ? '【測試 Header】' : '【正式分流】';
    const targetText = target === 'BLUE' ? '藍色環境' : '綠色環境';
    
    try {
        await ElMessageBox.confirm(
            `確定執行 ${modeText} 切換嗎？\n專案：${projectName.value}\n目標：${targetText}`,
            '流量切換安全驗證',
            { 
                customClass: 'glass-confirm', 
                confirmButtonText: '確認執行',
                cancelButtonText: '取消',
                type: 'warning' 
            }
        )

        const res = await switchTraffic(projectName.value, target.toLowerCase(), data.value.switchMode)
        if (res.code === 1) {
            ElMessage.success(`操作完成: ${res.data}`)
            loadData() // 刷新視圖
        } else {
            ElMessage.error(res.msg || '切換失敗')
        }
    } catch (e) {
        // 使用者取消
    }
}

onMounted(loadData)
onActivated(loadData)
</script>

<template>
  <div class="detail-container" v-loading="data.loading">
    <div class="nav-header">
      <el-button link @click="$router.push('/system/monitor')" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回列表
      </el-button>
      <div class="project-title-tag">
        正在控制：<span>{{ projectName }}</span>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="glass-panel status-box">
          <h3>Blue Node 健康度</h3>
          <div class="health-indicator">
            <div class="pulse-circle" :class="data.blueHealth === 200 ? 'safe' : 'danger'"></div>
            <div class="text">
              <p class="code">HTTP {{ data.blueHealth || '--' }}</p>
              <p class="msg">{{ data.blueHealth === 200 ? '藍色節點正常' : '節點連線異常' }}</p>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="glass-panel status-box">
          <h3>Green Node 健康度</h3>
          <div class="health-indicator">
            <div class="pulse-circle" :class="data.greenHealth === 200 ? 'safe' : 'danger'"></div>
            <div class="text">
              <p class="code">HTTP {{ data.greenHealth || '--' }}</p>
              <p class="msg">{{ data.greenHealth === 200 ? '綠色節點正常' : '節點連線異常' }}</p>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="glass-panel traffic-box">
          <div class="box-title">
            <span class="dot live"></span> 正式分流狀態 (Live)
          </div>
          <div class="traffic-content">
            <div class="active-env" :class="data.liveTraffic === 'BLUE_ACTIVE' ? 'blue' : 'green'">
                {{ data.liveTraffic === 'BLUE_ACTIVE' ? 'BLUE ACTIVE' : 'GREEN ACTIVE' }}
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="glass-panel traffic-box">
          <div class="box-title">
            <span class="dot header"></span> 測試流量狀態 (Header)
          </div>
          <div class="traffic-content">
            <div class="active-env" :class="data.headerTraffic === 'BLUE_ACTIVE' ? 'blue' : 'green'">
                {{ data.headerTraffic === 'BLUE_ACTIVE' ? 'BLUE (HEADER)' : 'GREEN (HEADER)' }}
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="glass-panel control-console">
      <div class="console-header">
        <div class="left">
            <el-icon><Switch /></el-icon> 流量切換控制台
        </div>
        <div class="right-mode">
            <span class="mode-label">切換模式：</span>
            <el-radio-group v-model="data.switchMode" size="small">
                <el-radio-button label="">正式分流</el-radio-button>
                <el-radio-button label="header">Header 測試</el-radio-button>
            </el-radio-group>
        </div>
      </div>

      <div class="switch-layout">
        <div class="env-card blue" :class="{ 'active': (data.switchMode === 'header' ? data.headerTraffic : data.liveTraffic) === 'BLUE_ACTIVE' }">
          <div class="card-title">Blue 環境</div>
          <div class="env-status">
              <el-tag v-if="data.liveTraffic === 'BLUE_ACTIVE'" size="small" effect="dark">LIVE</el-tag>
              <el-tag v-if="data.headerTraffic === 'BLUE_ACTIVE'" size="small" type="warning" effect="dark">HEADER</el-tag>
          </div>
          <el-button 
            type="primary" 
            :disabled="(data.switchMode === 'header' ? data.headerTraffic : data.liveTraffic) === 'BLUE_ACTIVE'" 
            @click="execSwitch('BLUE')"
          >切換至藍色</el-button>
        </div>

        <div class="vs-icon">VS</div>

        <div class="env-card green" :class="{ 'active': (data.switchMode === 'header' ? data.headerTraffic : data.liveTraffic) === 'GREEN_ACTIVE' }">
          <div class="card-title">Green 環境</div>
          <div class="env-status">
              <el-tag v-if="data.liveTraffic === 'GREEN_ACTIVE'" size="small" effect="dark">LIVE</el-tag>
              <el-tag v-if="data.headerTraffic === 'GREEN_ACTIVE'" size="small" type="warning" effect="dark">HEADER</el-tag>
          </div>
          <el-button 
            type="success" 
            :disabled="(data.switchMode === 'header' ? data.headerTraffic : data.liveTraffic) === 'GREEN_ACTIVE'" 
            @click="execSwitch('GREEN')"
          >切換至綠色</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-container { display: flex; flex-direction: column; gap: 20px; }
.nav-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px; }
.project-title-tag { background: rgba(99, 102, 241, 0.1); padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(99, 102, 241, 0.2); }
.project-title-tag span { font-weight: bold; color: var(--primary-color); }

.glass-panel { background: var(--glass-bg); border: 1px solid var(--glass-border); border-radius: 20px; padding: 24px; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2); }

/* 狀態盒與健康度 */
.status-box h3, .traffic-box .box-title { font-size: 14px; color: var(--text-sub); margin-bottom: 20px; display: flex; align-items: center; }
.health-indicator { display: flex; align-items: center; gap: 20px; }
.pulse-circle { width: 40px; height: 40px; border-radius: 50%; transition: all 0.3s; }
.pulse-circle.safe { background: #4ade80; box-shadow: 0 0 20px rgba(74, 222, 128, 0.4); animation: breathe 2s infinite; }
.pulse-circle.danger { background: #f87171; box-shadow: 0 0 20px rgba(248, 113, 113, 0.4); }

@keyframes breathe { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }

.active-env { font-size: 20px; font-weight: bold; text-shadow: 0 0 10px rgba(255,255,255,0.1); }
.active-env.blue { color: #60a5fa; }
.active-env.green { color: #4ade80; }

.traffic-box .dot { width: 8px; height: 8px; border-radius: 50%; margin-right: 10px; }
.dot.live { background: #6366f1; box-shadow: 0 0 8px #6366f1; }
.dot.header { background: #fbbf24; box-shadow: 0 0 8px #fbbf24; }

/* 控制台 */
.console-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.console-header .left { display: flex; align-items: center; gap: 10px; font-weight: 600; }
.mode-label { font-size: 13px; color: var(--text-sub); margin-right: 10px; }

.switch-layout { display: flex; align-items: center; gap: 40px; }
.vs-icon { font-size: 24px; font-weight: 900; color: rgba(255,255,255,0.1); font-style: italic; }

.env-card { 
    flex: 1; text-align: center; padding: 35px; border-radius: 20px; 
    background: rgba(0,0,0,0.15); border: 1px solid transparent; 
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
}
.env-card.active { 
    background: rgba(99, 102, 241, 0.08); 
    border-color: rgba(99, 102, 241, 0.4);
    box-shadow: 0 0 25px rgba(99, 102, 241, 0.1);
}
.card-title { font-size: 18px; font-weight: bold; margin-bottom: 15px; }
.env-status { display: flex; justify-content: center; gap: 5px; height: 24px; margin-bottom: 20px; }

/* 修改 Radio Button 玻璃擬態樣式 */
:deep(.el-radio-button__inner) {
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    color: var(--text-sub) !important;
}
:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: var(--primary-color) !important;
    color: white !important;
}
</style>