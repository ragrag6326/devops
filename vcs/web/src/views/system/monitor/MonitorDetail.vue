<script setup>
import { ref, computed, onActivated, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { 
  ArrowLeft, Switch, CircleCheck, Warning, 
  Upload, RefreshRight, UploadFilled 
} from '@element-plus/icons-vue'
// 假設您的 API 檔案已包含這些方法，若無請確保在 api/monitor.js 中定義
import { switchTraffic, getCurrentTraffic, healthCheck , restartService } from '@/api/monitor'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const projectName = computed(() => route.params.projectName)
// env 從路由 query 取得，預設 prod
const env = computed(() => route.query.env || 'prod')

const currentUser = ref('');

// 響應式數據結構
const data = ref({
    blueHealth: null,
    greenHealth: null,
    liveTraffic: '',   // BLUE_ACTIVE / GREEN_ACTIVE
    headerTraffic: '', // BLUE_ACTIVE / GREEN_ACTIVE
    loading: false,
    switchMode: '',    // '' (正式) | 'header' (測試)

    // --- 上傳與重啟相關 ---
    uploadDialogVisible: false,
    uploadTarget: '', // 'blue' | 'green'
    uploadFileList: [],
    isDeploying: false
})

// 核心：獲取完整細節狀態
const loadData = async () => {
    data.value.loading = true
    try {
        const [hBlue, hGreen, tLive, tHeader] = await Promise.all([
            healthCheck(env.value, projectName.value, 'blue'),
            healthCheck(env.value, projectName.value, 'green'),
            getCurrentTraffic(env.value, projectName.value, 'live'),
            getCurrentTraffic(env.value, projectName.value, 'header')
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
 * 流量切換
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

        const res = await switchTraffic(env.value, currentUser.value, projectName.value, target.toLowerCase(), data.value.switchMode)
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

/**
 * 🚀 安全重啟邏輯
 * @param target 'blue' | 'green'
 */
const handleSafeRestart = async (target) => {
    const targetUpper = target.toUpperCase();
    // 檢查當前正式流量是否指向該目標
    const isTargetLive = data.value.liveTraffic === `${targetUpper}_ACTIVE`;

    if (isTargetLive) {
        // --- 危險場景：目標正在承載流量 ---
        const alternative = target === 'blue' ? 'green' : 'blue';
        try {
            await ElMessageBox.confirm(
                `⚠️ 警告：[${targetUpper}] 正在承載正式流量！\n\n系統將執行保護程序：\n1. 先切換流量至 [${alternative.toUpperCase()}]\n2. 再重啟 [${targetUpper}]\n\n確定繼續嗎？`,
                '安全重啟保護',
                {
                    confirmButtonText: '執行安全重啟',
                    cancelButtonText: '取消',
                    type: 'warning',
                    customClass: 'glass-confirm'
                }
            );

            // 1. 自動切換流量
            data.value.loading = true;
            const switchRes = await switchTraffic(env.value, currentUser.value, projectName.value, alternative, '');
            if (switchRes.code !== 1) throw new Error("自動切換失敗，終止重啟");
            
            ElMessage.success(`流量已切換至 ${alternative.toUpperCase()}，準備重啟...`);
            await loadData(); // 更新狀態

            // 2. 執行重啟
            await executeRestartAPI(target);

        } catch (e) {
            if (e !== 'cancel') ElMessage.error(e.message || '操作取消');
            data.value.loading = false;
        }
    } else {
        // --- 安全場景：目標閒置 ---
        try {
            await ElMessageBox.confirm(
                `確定重啟 [${targetUpper}] 嗎？\n目前無正式流量，可直接重啟。`,
                '重啟確認',
                { confirmButtonText: '確認重啟', cancelButtonText: '取消', type: 'info', customClass: 'glass-confirm' }
            );
            await executeRestartAPI(target);
        } catch (e) {}
    }
}

// 實際調用重啟 API
const executeRestartAPI = async (target) => {
    data.value.loading = true;
    try {
        // restartService 重啟服務
        const res = await restartService(env.value, currentUser.value, projectName.value, target);
        
        // API 延遲
        await new Promise(r => setTimeout(r, 10000));
        
        if (res.code === 1 ) {
          ElMessage.success(`[${target.toUpperCase()}] 重啟指令已發送 , ${res.data}`);
          ElMessage.info('等待服務初始化，5秒後刷新狀態...');
          setTimeout(() => loadData(), 5000);
        } else {
          ElMessage.error(res.msg || '重啟失敗 (後端返回錯誤)');
        }

        console.log(res.value  , res.code , res.data);

    } catch (e) {
      console.error('RestartAPI 詳細錯誤訊息:', e);
      if (e.code === 'ECONNABORTED') {
        ElMessage.error('請求超時：後端重啟花費太久，前端中斷了連線');
      } else {
          ElMessage.error('重啟請求失敗: ' + (e.message || '未知錯誤'));
      }
    } finally {
        data.value.loading = false;
    }
}

/**
 * 📂 檔案上傳
 */
const openUploadDialog = (target) => {
    data.value.uploadTarget = target;
    data.value.uploadFileList = [];
    data.value.uploadDialogVisible = true;
}

const customUploadRequest = async (param) => {
    data.value.isDeploying = true;
    const formData = new FormData();
    formData.append('file', param.file);
    formData.append('projectName', projectName.value);
    formData.append('target', data.value.uploadTarget);

    try {
        const res = await uploadDeploy(formData);
        if (res.code === 1) {
            ElMessage.success(`檔案上傳並重啟成功`);
            param.onSuccess();
            data.value.uploadDialogVisible = false;
            loadData();
        } else {
            ElMessage.error(res.msg || '上傳失敗');
            param.onError();
        }
    } catch (e) {
        ElMessage.error('上傳過程發生錯誤');
        param.onError();
    } finally {
        data.value.isDeploying = false;
    }
}

onMounted(() => {
    currentUser.value = localStorage.getItem('current_username') || 'Admin';
    loadData()
})
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
            <el-icon><Switch /></el-icon> 流量切換與部屬
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
          <div class="card-title-row">
             <div class="card-title">Blue 環境</div>
             <div class="action-icons">
                 <!-- <el-tooltip content="上傳檔案並部署" placement="top">
                     <el-button circle size="small" :icon="Upload" @click="openUploadDialog('blue')" class="icon-btn" />
                 </el-tooltip> -->
                 <el-tooltip content="重啟服務 (Restart)" placement="top" popper-class="glass-tooltip">
                     <el-button circle type="danger" @click="handleSafeRestart('blue')" class="action-btn btn-restart">
                        <el-icon><RefreshRight /></el-icon>
                     </el-button>
                 </el-tooltip>
             </div>
          </div>

          <div class="env-status">
              <el-tag 
                v-if="data.liveTraffic === 'BLUE_ACTIVE'" 
                size="small" effect="dark" class="status-tag"  
                :class="{ 'dimmed': data.switchMode === 'header' }">
                LIVE
              </el-tag>
              
              <el-tag 
                v-if="data.headerTraffic === 'BLUE_ACTIVE'" 
                size="small" type="warning" effect="dark" class="status-tag" 
                :class="{ 'dimmed': data.switchMode !== 'header' }">
                HEADER
              </el-tag>
          </div>

          <el-button 
            type="primary" 
            class="switch-btn"
            :disabled="(data.switchMode === 'header' ? data.headerTraffic : data.liveTraffic) === 'BLUE_ACTIVE'" 
            @click="execSwitch('BLUE')"
          >切換至藍色</el-button>
        </div>

        <div class="vs-icon">VS</div>

        <div class="env-card green" :class="{ 'active': (data.switchMode === 'header' ? data.headerTraffic : data.liveTraffic) === 'GREEN_ACTIVE' }">
          <div class="card-title-row">
             <div class="card-title">Green 環境</div>
             <div class="action-icons">

                 <!-- <el-tooltip content="上傳檔案並部署" placement="top" popper-class="glass-tooltip">
                     <el-button circle size="small" :icon="Upload" @click="openUploadDialog('green')" class="action-btn btn-upload" />
                 </el-tooltip> -->

                 <el-tooltip content="重啟服務 (Restart)" effect="dark" popper-class="glass-tooltip" placement="top">
                     <el-button circle type="danger" :icon="RefreshRight" @click="handleSafeRestart('green')" class="action-btn btn-restart" />
                 </el-tooltip>
             </div>
          </div>

          <div class="env-status">
              <el-tag 
                v-if="data.liveTraffic === 'GREEN_ACTIVE'" 
                size="small" effect="dark" class="status-tag" 
                :class="{ 'dimmed': data.switchMode === 'header' }">
                LIVE
              </el-tag>
              
              <el-tag 
                v-if="data.headerTraffic === 'GREEN_ACTIVE'" 
                size="small" type="warning" effect="dark" class="status-tag" 
                :class="{ 'dimmed': data.switchMode !== 'header' }">
                HEADER
              </el-tag>
          </div>

          <el-button 
            type="success" 
            class="switch-btn"
            :disabled="(data.switchMode === 'header' ? data.headerTraffic : data.liveTraffic) === 'GREEN_ACTIVE'" 
            @click="execSwitch('GREEN')"
          >切換至綠色</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="data.uploadDialogVisible" title="熱更部署 (Hot Deploy)" width="400px" class="glass-dialog">
         <div class="upload-content">
             <p class="warning-text">
                 <el-icon><Warning /></el-icon> 上傳後將自動覆蓋檔案並重啟 [{{ data.uploadTarget.toUpperCase() }}]
             </p>
             
             <el-upload
                 class="upload-demo"
                 drag
                 action="#" 
                 :http-request="customUploadRequest"
                 :file-list="data.uploadFileList"
                 :limit="1"
                 :disabled="data.isDeploying"
             >
                 <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                 <div class="el-upload__text">拖曳檔案至此或 <em>點擊上傳</em></div>
             </el-upload>
         </div>
     </el-dialog>
  </div>
</template>

<style>
/* 針對 Tooltip 本體 */
.el-popper.is-dark.glass-tooltip {
    /* 背景：深黑透光，使用 RGBA 確保透明度 */
    background: rgba(15, 23, 42, 0.9) !important; 
    backdrop-filter: blur(12px) !important;
    
    /* 邊框：細微亮邊 */
    border: 1px solid rgba(255, 255, 255, 0.2) !important;
    
    /* 文字 */
    color: #e2e8f0 !important;
    font-weight: 600 !important;
    font-size: 13px !important;
    padding: 8px 14px !important;
    border-radius: 8px !important;
    
    /* 陰影：增加立體感 */
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5) !important;
}

/* 針對 Tooltip 的小箭頭 (Arrow) */
.el-popper.is-dark.glass-tooltip .el-popper__arrow::before {
    background: rgba(15, 23, 42, 0.9) !important;
    border: 1px solid rgba(255, 255, 255, 0.2) !important;
    /* 移除 Element Plus 預設的箭頭背景色 */
    right: 0;
}
</style>

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
.switch-layout { display: flex; align-items: center; gap: 40px; }
.vs-icon { font-size: 24px; font-weight: 900; color: rgba(255,255,255,0.1); font-style: italic; }

/* 切換模式顯示優化 */
.right-mode {
    display: flex; align-items: center;
    background: rgba(0, 0, 0, 0.3); padding: 6px 10px;
    border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.1);
}
.mode-label { margin-right: 12px; font-weight: 600; color: #cbd5e1; }

/* 1. 所有 el-radio-button 按鈕的【未選中 / 預設】狀態 */
:deep(.el-radio-button__inner) {
    background: transparent !important;       /* 透明背景 */
    border: 1px solid rgba(255, 255, 255, 0.2) !important; /* 淡邊框 */
    color: #94a3b8 !important;                /* 灰色文字 */
    border-radius: 6px !important;
    padding: 8px 16px;
    font-weight: bold;
    transition: all 0.3s;
    box-shadow: none !important;              /* 移除預設陰影 */
}

/* 選中：正式分流 (藍色系) */
:deep(.el-radio-button__original-radio[value=""]:checked + .el-radio-button__inner) {
    background: rgba(99, 102, 241, 0.2) !important;
    color: #818cf8 !important;
    border: 1px solid #6366f1 !important;
    box-shadow: 0 0 10px rgba(99, 102, 241, 0.3) !important;
}

/* 選中：Header 測試 (黃色系) */
:deep(.el-radio-button__original-radio[value="header"]:checked + .el-radio-button__inner) {
    background: rgba(245, 158, 11, 0.2) !important;
    color: #fbbf24 !important;
    border: 1px solid #f59e0b !important;
    box-shadow: 0 0 15px rgba(245, 158, 11, 0.5) !important;
    transform: scale(1.05);
}

/* 移除 Element Plus 預設的左邊框陰影 (這是導致兩邊框重疊變粗的原因) */
:deep(.el-radio-button:first-child .el-radio-button__inner) {
    border-left: 1px solid rgba(255, 255, 255, 0.2) !important;
}
:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    box-shadow: none; 
}

/* 卡片與標籤 */
.env-card { 
    flex: 1; text-align: center; padding: 30px; border-radius: 20px; 
    background: rgba(0,0,0,0.15); border: 1px solid transparent; 
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); position: relative;
}
.env-card.active { 
    background: rgba(99, 102, 241, 0.08); 
    border-color: rgba(99, 102, 241, 0.4);
    box-shadow: 0 0 25px rgba(99, 102, 241, 0.1);
}

.card-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.card-title { font-size: 18px; font-weight: bold; }

/* 操作按鈕 */
.icon-btn { background: rgba(255,255,255,0.1); border: none; color: #fff; margin-left: 8px; }
.icon-btn:hover { background: rgba(255,255,255,0.3); transform: scale(1.1); }

.switch-btn { width: 40%; margin-top: 25px; }
.env-status { display: flex; justify-content: center; gap: 8px; height: 28px; }

/* 標籤視覺邏輯 */
.status-tag { transition: all 0.3s ease; font-weight: bold; box-shadow: 0 0 10px rgba(0,0,0,0.3); }
.status-tag.dimmed { opacity: 0.2; filter: grayscale(100%); transform: scale(0.9); box-shadow: none; }
.status-tag:not(.dimmed) { transform: scale(1.15); z-index: 2; }

/* Dialog */
.upload-content { text-align: center; }
.warning-text { color: #f59e0b; margin-bottom: 15px; }

/* --- 核心：動作按鈕容器 --- */
.action-icons {
    display: flex;
    gap: 12px; /* 按鈕之間的距離 */
}

/* --- 通用動作按鈕樣式 --- */
.action-btn {
    width: 36px !important;    /* 📏 強制加大按鈕寬度 */
    height: 36px !important;   /* 📏 強制加大按鈕高度 */
    font-size: 18px !important;/* 🔍 這是控制【圖標大小】的關鍵 */
    
    border: 1px solid rgba(255, 255, 255, 0.15) !important;
    background: rgba(255, 255, 255, 0.05) !important;
    color: #cbd5e1 !important;
    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); /* 彈跳過渡效果 */
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 !important;
}

/* --- Hover 通用效果：放大 + 變亮 --- */
.action-btn:hover {
    transform: translateY(-2px) scale(1.1); /* 稍微上浮並放大 */
    color: #fff !important;
    background: rgba(255, 255, 255, 0.15) !important;
    border-color: rgba(255, 255, 255, 0.5) !important;
}

/* --- 🟦 上傳按鈕專屬特效 (藍青色光暈) --- */
.action-btn.btn-upload:hover {
    color: #22d3ee !important; /* Cyan-400 */
    border-color: #22d3ee !important;
    box-shadow: 0 0 15px rgba(34, 211, 238, 0.4), inset 0 0 10px rgba(34, 211, 238, 0.1);
}

/* --- 🟥 重啟按鈕專屬特效 (警示紅光暈) --- */
.action-btn.btn-restart:hover {
    color: #f87171 !important; /* Red-400 */
    border-color: #f87171 !important;
    box-shadow: 0 0 15px rgba(248, 113, 113, 0.4), inset 0 0 10px rgba(248, 113, 113, 0.1);
}

/* 按鈕點擊時的下壓感 */
.action-btn:active {
    transform: scale(0.95);
}

</style>