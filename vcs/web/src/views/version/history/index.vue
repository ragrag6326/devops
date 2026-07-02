<script setup>
import { onMounted, ref , watch , nextTick , onUnmounted, warn , computed } from 'vue'
import { AnsiUp } from 'ansi_up';

import { queryVersionPage , queryVersionById , deleteVersionById  , edit , getLatestSuccessVersion , getNextVersion , checkDeployable , updateJenkinsBuildId} from "@/api/version";
import { triggerJenkinsBuild , getJenkinsConsoleLog , getJenkinsPiplineNumber } from "@/api/jenkins";
import { deploying } from "@/api/deploy";
import {
  getImageVersionByType,
  getRollBackImageVersion,
  renewimage,
  deleteImage
} from "@/api/monitor";
import { getProjectList, getSystemEnv } from "@/api/project";

import { ElMessage , ElMessageBox , ElLoading, sliderContextKey } from 'element-plus'
import axios from 'axios';


const token = ref ('');


// ---------------- 版本數據列表 ----------------
const versionList = ref([])
const loading = ref(false)

// 1. 從本地存儲獲取角色（建議給予預設值防止 null）
const current_role = ref(localStorage.getItem('current_role') || 'GUEST');

// 2. 專案清單（從後端動態取得）
const allProjects = ref([])
const currentServerEnv = ref('')  // 當前機器環境 (dev / prod)

// 3. 由 allProjects 計算出各分類清單
const frontendProjects = computed(() =>
    allProjects.value.filter(p => p.category === 'frontend').map(p => p.name)
)
const backendProjects = computed(() =>
    allProjects.value.filter(p => p.category === 'backend').map(p => p.name)
)
const FRONTEND_IMAGE_PROJECTS = computed(() => frontendProjects.value)
const BACKEND_IMAGE_PROJECTS = computed(() => backendProjects.value)

const projectNameOptions = computed(() =>
    allProjects.value.map(p => ({ label: p.displayName || p.name, value: p.name }))
)

const filteredProjectOptions = computed(() => {
    return projectNameOptions.value.filter(option => {

        // A. 如果是管理員，全看
        if (current_role.value === 'ADMIN') return true;

        // B. 前端使用者判斷
        if (current_role.value === 'FRONTEND_USER') {
            return frontendProjects.value.includes(option.value);
        }

        // C. 後端使用者判斷
        if (current_role.value === 'BACKEND_USER') {
            return backendProjects.value.includes(option.value);
        }

        // D. 其他角色預設看不到任何專案（或根據需求調整）
        return false;
    });
});


// ---------------- 退版相關變數 ----------------
const rollbackDialogVisible = ref(false);
const rollbackLoading = ref(false);
const rollbackData = ref([]); 
const rollbackForm = ref({
    projectName: '', // 新增欄位紀錄目標專案
    type: '',
    version: ''
});

// 1. 開啟退版視窗
const handleOpenRollback = () => {
    rollbackDialogVisible.value = true;
    
    // 初始化：清空資料
    rollbackData.value = [];
    rollbackForm.value.type = '';
    rollbackForm.value.version = '';
    
    // 如果搜尋欄有選專案，預設帶入 (searchForm 需從你的上下文取得)
    if (searchForm.value.name) {
        rollbackForm.value.projectName = searchForm.value.name;
    } else {
        rollbackForm.value.projectName = '';
    }
};

// 2. 監聽：當「專案名稱」改變時，發送 API 取得版本清單
watch(() => rollbackForm.value.projectName, async (newVal) => {
    if (!newVal) return;
    
    rollbackLoading.value = true;
    rollbackForm.value.type = '';    // 切換專案時重置類型
    rollbackForm.value.version = ''; // 切換專案時重置版號
    rollbackData.value = [];         // 清空舊數據

    try {
        // 發送 PATCH 請求
        // liveViewEnv 決定要查哪台機器的可選版本
        const res = await getRollBackImageVersion(liveViewEnv.value, newVal);
        
        if (res.code === 1) {
            rollbackData.value = res.data; // 將回傳的陣列存入
        } else {
            ElMessage.error(res.msg || "取得版本資訊失敗");
        }
    } catch (error) {
        console.error(error);
        ElMessage.error("系統錯誤：無法取得版本清單");
    } finally {
        rollbackLoading.value = false;
    }
});

// 3. 計算屬性：根據選取的 type (prod/backup)，篩選出對應的 versions 陣列
const availableVersions = computed(() => {
    if (!rollbackForm.value.type) return [];
    
    // 從 rollbackData 中找到符合 type 的那一個物件
    const target = rollbackData.value.find(item => item.type === rollbackForm.value.type);
    
    // 回傳 versions 陣列，如果沒找到則回傳空陣列
    return target ? target.versions : [];
});

// 4. 送出退版請求
const submitRollback = async () => {
    // 1. 解構變數
    const { projectName, type, version } = rollbackForm.value;

    // 2. 防呆
    if (!projectName || !type || !version) {
        ElMessage.warning("請完整選擇：專案、環境類型與版號");
        return;
    }
    // 3. 處理使用者名稱 (防止 null 或 undefined)
    const storedUser = localStorage.getItem('current_username');
    const currentUser = storedUser ? storedUser.replace(/['"]+/g, '') : 'Unknown_User';
    
    // 顯示中文名稱用於確認
    const typeLabel = type === 'prod' ? '正式機 (Prod)' : '備援機 (Backup)';

    ElMessageBox.confirm(
        `確定要將 [${projectName}] 的 ${typeLabel} 更新至版本 ${version} 嗎？`,
        '退版確認',
        { confirmButtonText: '確定執行', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
        
        const loading = ElLoading.service({ text: '正在發送更新指令...' });
        
        try {

            // 呼叫更新 API
            const res = await renewimage({
                env: liveViewEnv.value,
                opertaionName : currentUser,
                projectName: projectName,
                nodeType: type, // 'prod' 或 'backup'
                version: version
            });

            if (res.code === 1) {
                ElMessage.success("指令發送成功！");
                rollbackDialogVisible.value = false;
                fetchLiveVersions();
            } else {
                ElMessage.error(res.msg || "更新失敗");
            }
        } catch (error) {
            ElMessage.error("請求發生錯誤");
        } finally {
            loading.close();
        }
    });
};

// -------------------------------------------

// --- 目前機器版本看板 ---
const NODE_SUFFIXES = ['backup', 'prod', 'dev', 'local', 'test', 'blue', 'green']
const NODE_ORDER = { prod: 0, admin: 0, backup: 1, dev: 2, test: 2, local: 3, blue: 4, green: 5 }

const parseImageLine = (line) => {
  const colon = line.lastIndexOf(':')
  if (colon === -1) {
    return { projectName: line, nodeType: null, version: '未知', raw: line, fullString: line, repoEnv: null }
  }
  const raw = line.slice(0, colon)       // e.g. "frontend-admin/form-service-frontend-test"
  const version = line.slice(colon + 1)  // e.g. "1.0.22"

  // 從 repo 前綴提取環境名稱: "backend-admin" → "admin", "frontend-dev" → "dev"
  const slashIdx = raw.indexOf('/')
  const repoPrefix = slashIdx !== -1 ? raw.slice(0, slashIdx) : ''
  const repoEnv = repoPrefix ? repoPrefix.split('-').slice(1).join('-') : null

  // 去除 repo 前綴，只保留 imageName 部分
  const imageWithSuffix = slashIdx !== -1 ? raw.slice(slashIdx + 1) : raw

  let projectName = imageWithSuffix
  let nodeType = null
  for (const suffix of NODE_SUFFIXES) {
    if (imageWithSuffix.endsWith(`-${suffix}`)) {
      projectName = imageWithSuffix.slice(0, -(suffix.length + 1))
      nodeType = suffix
      break
    }
  }
  // image 名稱無後綴時，用 repo 前綴的 env 作為 nodeType (e.g. backend-admin → admin, backend-dev → dev)
  if (!nodeType && repoEnv) {
    nodeType = repoEnv
  }
  return { projectName, nodeType, version, raw, fullString: line, repoEnv }
}

const liveImageLines = ref([])
const liveImageSet = ref(new Set())
const liveLoading = ref(false)
// liveViewEnv: 目前 live board 顯示哪台機器的資料（'prod' | 'dev'），預設由 currentServerEnv 決定
const liveViewEnv = ref('prod')

const groupedLiveProjects = computed(() => {
  const map = {}
  liveImageLines.value.forEach((item) => {
    if (!map[item.projectName]) {
      map[item.projectName] = { projectName: item.projectName, nodes: [] }
    }
    map[item.projectName].nodes.push({
      nodeType: item.nodeType,
      version: item.version,
      raw: item.raw
    })
  })
  return Object.values(map).map((proj) => ({
    ...proj,
    nodes: proj.nodes.slice().sort(
      (a, b) => (NODE_ORDER[a.nodeType] ?? 9) - (NODE_ORDER[b.nodeType] ?? 9)
    )
  }))
})

const nodeTypeLabel = (type) => {
  const map = { prod: '正式', admin: '正式', backup: '備援', dev: '測試', test: '測試', local: '通用', blue: 'Blue', green: 'Green' }
  return map[type] || type || '通用'
}

const chipClass = (type) => {
  if (type === 'prod' || type === 'admin') return 'chip-prod'
  if (type === 'backup') return 'chip-backup'
  if (type === 'dev' || type === 'test') return 'chip-dev'
  if (type === 'local') return 'chip-local'
  return 'chip-none'
}

const fetchLiveVersions = async () => {
  liveLoading.value = true
  try {
    // 傳入 liveViewEnv (prod/dev)，後端腳本會 SSH 到對應機器取容器版本
    const res = await getImageVersionByType(liveViewEnv.value, 'current')
    if (res.code === 1 && Array.isArray(res.data)) {
      liveImageLines.value = res.data.map(parseImageLine)
      liveImageSet.value = new Set(res.data)
    } else {
      ElMessage.warning(res.msg || '取得版本資訊失敗')
      liveImageLines.value = []
    }
  } catch {
    ElMessage.error('無法連線後端，請確認伺服器狀態')
  } finally {
    liveLoading.value = false
  }
}

// 切換 live board 顯示哪個環境（prod | dev），並立即重新整理
const switchLiveViewEnv = async (env) => {
  liveViewEnv.value = env
  await fetchLiveVersions()
}

// --- 移除 Image ---
// FRONTEND_IMAGE_PROJECTS / BACKEND_IMAGE_PROJECTS 已改為由 allProjects 動態計算（見上方 computed）

const removeDialogVisible = ref(false)
const removeCategory = ref('')
const historyImageLines = ref([])
const removeListLoading = ref(false)
const selectedImageVersions = ref([])
const removeSubmitting = ref(false)

const removeNodeTagClass = (nodeType) => {
  if (nodeType === 'prod') return 'rn-prod'
  if (nodeType === 'backup') return 'rn-backup'
  if (nodeType === 'dev') return 'rn-dev'
  if (nodeType === 'local') return 'rn-local'
  return 'rn-dev'
}

const filteredHistoryImages = computed(() => {
  if (!removeCategory.value || historyImageLines.value.length === 0) return []
  const allowed = new Set(
    removeCategory.value === 'frontend' ? FRONTEND_IMAGE_PROJECTS.value : BACKEND_IMAGE_PROJECTS.value
  )
  return historyImageLines.value
    .map((line) => ({ ...parseImageLine(line), isCurrent: liveImageSet.value.has(line) }))
    .filter((item) => allowed.has(item.projectName))
    .sort((a, b) => {
      if (a.projectName !== b.projectName) return a.projectName.localeCompare(b.projectName)
      return (NODE_ORDER[a.nodeType] ?? 9) - (NODE_ORDER[b.nodeType] ?? 9)
    })
})

const handleOpenRemoveImage = () => {
  removeDialogVisible.value = true
  removeCategory.value = ''
  historyImageLines.value = []
  selectedImageVersions.value = []
}

watch(() => removeCategory.value, async (category) => {
  if (!category) return
  selectedImageVersions.value = []
  historyImageLines.value = []
  removeListLoading.value = true
  try {
    const res = await getImageVersionByType(liveViewEnv.value, 'history')
    if (res.code === 1 && Array.isArray(res.data)) {
      historyImageLines.value = res.data
    } else {
      ElMessage.warning(res.msg || '取得歷史版本失敗')
    }
  } catch {
    ElMessage.error('取得歷史版本時發生錯誤')
  } finally {
    removeListLoading.value = false
  }
})

// fullString 本身已包含完整 repo 前綴 (e.g. frontend-admin/form-service-frontend-test:1.0.22)
// 直接使用，無需再重組路徑
const buildRemoveImagePath = (fullString) => fullString

const isDeleteSuccess = (res) => res?.data === '刪除成功'

const toggleImageSelection = (fullString, isCurrent) => {
  if (isCurrent) return
  const idx = selectedImageVersions.value.indexOf(fullString)
  if (idx >= 0) selectedImageVersions.value.splice(idx, 1)
  else selectedImageVersions.value.push(fullString)
}

const submitRemoveImages = async () => {
  if (selectedImageVersions.value.length === 0) {
    ElMessage.warning('請至少勾選一個版號')
    return
  }
  try {
    await ElMessageBox.confirm(
      `確定要永久移除 ${selectedImageVersions.value.length} 個 Image？此操作不可逆，請謹慎確認。`,
      '確認移除 Image',
      { type: 'warning', confirmButtonText: '確認移除', cancelButtonText: '取消', customClass: 'glass-confirm' }
    )
  } catch {
    return
  }

  removeSubmitting.value = true
  let successCount = 0
  const failures = []
  for (const version of selectedImageVersions.value) {
    const imagePath = buildRemoveImagePath(version)
    try {
      const res = await deleteImage(liveViewEnv.value, imagePath)
      if (isDeleteSuccess(res)) successCount++
      else failures.push({ name: version, reason: res?.data || res?.msg || '未知錯誤' })
    } catch {
      failures.push({ name: version, reason: '網路或伺服器錯誤' })
    }
  }
  removeSubmitting.value = false

  if (successCount > 0) ElMessage.success(`成功移除 ${successCount} 個 Image`)
  if (failures.length > 0) {
    failures.forEach(({ name, reason }) => {
      ElMessage.error({ message: `移除失敗：${name}（${reason}）`, duration: 5000 })
    })
    const res = await getImageVersionByType(liveViewEnv.value, 'history').catch(() => null)
    if (res?.code === 1 && Array.isArray(res.data)) historyImageLines.value = res.data
    selectedImageVersions.value = []
  } else {
    removeDialogVisible.value = false
    fetchLiveVersions()
  }
}

// 搜索欄（projectNameOptions 已改為由 allProjects 動態計算）

const projectEnvOptions = [
    { label: "prod", value: "prod" },
    { label: "dev", value: "dev" },
]

const stateOptions = [
  { label: "部屬中", value: 0 },
  { label: "部屬成功", value: 1 },
  { label: "失敗", value: 2 },
  { label: "回滾", value: 3 },
]


// ----------  搜索表單  ---------- 
const searchForm = ref({
    name: "",
    state: "",
    env: "",
})

// ----------  搜索表單清空  ---------- 
const clear = () => {
  searchForm.value = { name: "", state: "",  env: "" }
  search()
}

// ----------------- 分頁展示 --------------------------------- 
const currentPage= ref(1) // 頁碼
const pageSize = ref(10) // 每頁展示紀錄數
const background = ref(true) // 頁碼背景色
const totalPage = ref(0)  // 總紀錄數

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  search()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  search()
}

// ---------------- 版本數據列表 ----------------
const multipleSelection = ref([]);


// --- jenkins 日誌視窗相關變數 ---
const logDialogVisible = ref(false);
const logContent = ref("");
const logLoading = ref(false);
const currentLogTitle = ref("");
const currentLogInfo = ref({
    env: '',
    version: '',
    buildId: '',
    jobName: '',
    url: '' ,
    pipeline_url: ''
});

// --- 輪詢控制變數 ---
let logTimer = null;         // 計時器 ID
const isPolling = ref(false); // 是否正在輪詢中


// 2. 實例化
const ansiUp = new AnsiUp();

// 監聽 dialog 關閉，確保停止輪詢
watch(logDialogVisible, (val) => {
    if (!val) {
        stopPolling();
    }
});

// 組件銷毀時也要確保停止
onUnmounted(() => {
    stopPolling();
});

// 停止輪詢的函數
const stopPolling = () => {
    if (logTimer) {
        clearTimeout(logTimer);
        logTimer = null;
    }
    isPolling.value = false;
};


// --------------------------------- 查詢歷史紀錄 ---------------------------------
const search = async () => {
    try {
        const result = await queryVersionPage (      
            currentPage.value,
            pageSize.value,
            searchForm.value.name,
            searchForm.value.env,
            searchForm.value.state
        )
        console.log('queryMrPage result:', result)

        if (result?.code) {
            versionList.value = result.data.rows || []
            totalPage.value = result.data.total || 0
            
            // 取得所有 projectName 排除重複
            // const names = versionList.value.map(name => name.projectName).filter(name => name && name.trim() !== "") 
            // projectNameOptions.value = [...new Set(names)]

        } else {
            ElMessage.error(result?.msg || "查詢失敗")
        }

    } catch (error) {
        console.error('Error fetching MR data:', error)
        ElMessage.error('查詢 MR 歷史紀錄失敗! 請查看 console log')
    }
}


// ----------------- 版號表單對話框 --------------------------------- 
const formTitle = ref('');
const addDialogVisible = ref(false);  // 對話框默認隱藏
const editDialogVisible = ref(false);  // 對話框默認隱藏
const versionFormRef = ref();          // 表單驗證

// (新增、修改) 版號表單 數據回顯
const versionForm = ref ({ 
    id: "" ,
    name : "" ,
    env: "",
    version: "",
    branch: "",
    remark: "",
    jenkinsBuildId: "" ,
    deployType: ""
})

const deployTypeOptions = [
    { label: "Main (正式機)", value: "prod" },
    { label: "Backup (備援)", value: "backup" },
]

/**
 * 將 UI 選擇的 env (prod|dev) 對應到專案實際使用的 Jenkins env 名稱
 * 例如 form-service-frontend: prod → admin, dev → dev
 */
const resolveEnv = (projectName, uiEnv) => {
    const proj = allProjects.value.find(p => p.name === projectName)
    console.log('[resolveEnv]', projectName, uiEnv, '→ proj:', JSON.stringify(proj))
    if (!proj) return uiEnv
    if (uiEnv === 'prod' && proj.prodEnv) return proj.prodEnv
    if (uiEnv === 'dev'  && proj.devEnv)  return proj.devEnv
    return uiEnv
}

// 監聽表單中 env 變化
watch(() => versionForm.value.env, async (newEnv) => {

    // 防呆：如果沒選專案，先不動作
    if (!versionForm.value.name) return;

    const projectName = versionForm.value.name;

    try {
        // ===============
        // 如選擇的是 Dev 
        // ===============
        if (newEnv === 'dev') {
            const res = await getNextVersion(projectName, resolveEnv(projectName, 'dev'))
            if (res.code === 1) {
                versionForm.value.version = res.data
                versionForm.value.branch = 'develop'
                ElMessage.success(`已自動帶入 dev 部屬成功版本: ${res.data}`)
            }
        }
        // ================
        // 如選擇的是 Prod
        // ================
        else if (newEnv === 'prod') {
            const devLatestRes = await getNextVersion(projectName, resolveEnv(projectName, 'dev'))
            const devVer = devLatestRes.data

            if (!devVer) {
                ElMessage.error("Dev 尚無版本，無法部署 Prod");
                versionForm.value.version = '';
                return;
            }

            // 防呆：檢查 Prod 是否已經跟上這個版本了
            const prodLatestRes = await getNextVersion(projectName, resolveEnv(projectName, 'prod'));
            const prodVer = prodLatestRes.data; // 1.0.20 或 1.0.19

            // 如果 Dev (1.0.20) == Prod (1.0.20)
            // if (prodVer && devVer === prodVer) {
            //     ElMessageBox.alert(
            //         `目前 Prod 已是最新版本 (${prodVer})，與 Dev 同步。\n請先更新 Dev 環境後再執行此操作。`,
            //         '無法更新',
            //         { type: 'warning' }
            //     );
            //     // 清空版號，並建議讓確認按鈕 disable
            //     versionForm.value.version = '';
            //     return;
            // }

            // 通過檢查，自動填入 Dev 的版號 (e.g. 1.0.20)
            versionForm.value.version = prodVer;
            versionForm.value.branch = 'develop'; // Prod 通常固定分支
            ElMessage.success(`已自動帶入 Prod 部屬成功版本: ${prodVer}`);
        }
    } catch (e) {
        console.error(e);
    }
});

// 監聽表單中 env 變化
watch(() => versionForm.value.deployType, async () => {
    
    // 防呆：如果沒選專案，先不動作
    if (!versionForm.value.deployType) return;
    
    const deployType = versionForm.value.deployType;
    const version = versionForm.value.version;

    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');

    // 建立日期字串：2026/01/26
    const dateStr = `${year}/${month}/${day}`;
    
    try {
        // 優先用 DB defaultBranch，無設定則 fallback 到 master
        const proj = allProjects.value.find(p => p.name === versionForm.value.name)
        const defaultBranch = proj?.defaultBranch || 'master'

        // ===============
        // 如選擇的是 backup
        // ===============
        if (deployType === 'backup') {
            ElMessage.success(`備註已自動填入`);
            versionForm.value.remark = `${dateStr} 備援機更新, 版號: ${version}`;
            versionForm.value.branch = defaultBranch;
        }
        // ================
        // 如選擇的是 Prod
        // ================
        else if (deployType === 'prod') {
            ElMessage.success(`備註已自動填入`);
            versionForm.value.remark = `${dateStr} 正式機更新, 版號: ${version}`;
            versionForm.value.branch = defaultBranch;
        }
    } catch (e) {
        console.error(e);
    }
});

// 版號表單驗證規則
const rules = {
    name: [{ required: true, message: "請選擇專案", trigger: "change" }],
    env: [{ required: true, message: "請選擇環境", trigger: "change" }],
    branch: [{ required: true, message: "請輸入分支", trigger: "change" }],
    deployType: [{
        validator: (rule, value, callback) => {
            const needDeploy = versionForm.value.env === 'prod' && frontendProjects.value.includes(versionForm.value.name)
            if (needDeploy && !value) {
                callback(new Error('請選擇部署類型'))
            } else {
                callback()
            }
        },
        trigger: "change"
    }],
    version: [
        { required: true, message: "請輸版號 格式: 1.0.0", trigger: "change" },
        { pattern: /^[\d]{1}\.[\d]+\.[\d]+$/ , message: '請輸入有效的版號 範例: 1.0.0', trigger: 'blur'}
    ]
}

// 版號表單初始化
const InitAddForm = () => {

    // 點擊添加前 清空的表單
    for (let key in versionForm.value ) {
        if (!Array.isArray(versionForm.value[key]) ) {
            versionForm.value[key] = '';
        } else {
            versionForm.value[key] = [];
        }
    }

    formTitle.value = '新增版號';
    addDialogVisible.value = true;
}

// 版號表單初始化
const InitEditForm = () => {

    // 點擊添加前 清空的表單
    for (let key in versionForm.value ) {
        if (!Array.isArray(versionForm.value[key]) ) {
            versionForm.value[key] = '';
        } else {
            versionForm.value[key] = [];
        }
    }

    formTitle.value = '修改版號';
    editDialogVisible.value = true;
}

// 點開(編輯)版號表單 - 數據回顯
const handleEdit = async (row) => {
    InitEditForm();

    const result = await queryVersionById( row.id ) ;
    if ( result.code ) {
        versionForm.value = result.data;
    } else {
        // 處理錯誤情況 (可選)
        console.error('查詢失敗:', result.msg);
    }
}

// 移除操作
const handleDelete = async (row) => {

    ElMessageBox.confirm(`確定是否刪除 專案:${row.projectName} 環境:${row.projectEnv} 版號:${row.version} `, '提示', {
        cancelButtonText: '取消',
        confirmButtonText: '確定刪除',
        type: 'warning', 
        customClass: 'glass-confirm'
    }).then(async () => {
        const result = await deleteVersionById(row.id)
        console.log('handleDelete API回傳結果:', result);
        
        if ( result.code ) {
            ElMessage.success('刪除成功');
            search(); 
        } else {
            ElMessage.error('移除失敗:' + result.msg);
        }
    }).catch(() => {
        ElMessage.info('已取消');
    })
}

// -------------------------------------------------------------------------------
// 批量移除操作 監聽表格勾選事件 (Element Plus 自動傳入 val，即所有選中的行物件)
const handleSelectionChange = (val) => {
    multipleSelection.value = val;
};

// 批量移除操作
const handleBatchDelete = async () => {

    if (multipleSelection.value.length === 0) {
        return;
    }

    // 取出所有 ID 變成陣列，例如：[101, 102, 103]
    const ids = multipleSelection.value.map(item => item.id);
    console.log(ids);

    ElMessageBox.confirm(`確定要刪除這 ${ids.length} 筆資料嗎？此操作無法恢復。`, '警告', {
        confirmButtonText: '確定刪除',
        cancelButtonText: '取消',
        type: 'warning', 
        customClass: 'glass-confirm'
    }).then(async () => {
        try {
            const result = await deleteVersionById(ids)
            console.log('handleDelete API回傳結果:', result);
            
            if ( result.code ) {
                ElMessage.success('批量刪除成功');
                search(); 
                multipleSelection.value = [];
            } else {
                ElMessage.error(result.msg || '刪除失敗');
            }
        } catch (error){
            console.error('刪除錯誤:', error);
            ElMessage.error('系統發生錯誤');
        }
    }).catch(() => {
        ElMessage.info('已取消');
    })
}


// 輔助函數：將 Jenkins 回傳的絕對路徑轉為 Proxy 相對路徑
const getQueueApiUrl = (absoluteUrl) => {
    // 假設後端回傳的是 http://192.168.1.35:8088/queue/item/319/
    // 需要把它變成 /jenkins-proxy/queue/item/319/api/json
    // 使用正則表達式去掉 http://IP:PORT 部分
    const relativePath = absoluteUrl.replace(/^http:\/\/[^/]+/, '');
    // 確保路徑乾淨並加上 api/json
    return `/jenkins-proxy${relativePath.replace(/\/$/, '')}/api/json`;
};

// 輔助函數：延遲 (Sleep)
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

// 確認 版號表單 ( 新增 、 修改 )
const submitVersionAddandEdit = async () => {

    // 表單驗證
    if (!versionFormRef.value) return;

    versionFormRef.value.validate( async (valid) => { // valid 表示是否校驗通過 : true 通過、false 未通過
        if (valid) {

            let result ;
            
            if ( versionForm.value.id ) { 
                // 修改
                result = await edit(versionForm.value);
                console.log('API回傳結果:', result);
                
                if ( result.code ) {
                    editDialogVisible.value = false;
                    search();
                    ElMessage.success('修改成功');
                } else {
                    ElMessage.error('修改失敗:' + result.msg);
                }
            } else { 
                ElMessageBox.confirm(`此操作將進行環境更新 是否繼續? `, '提示', {
                    cancelButtonText: '取消',
                    confirmButtonText: '確定',
                    type: 'warning', // 跳出的樣式 
                    customClass: 'glass-confirm' // 自定義樣式類名
                }).then(async () => {
                    // 1. 開啟全螢幕 Loading，防止使用者重複點擊
                    const loadingInstance = ElLoading.service({
                        lock: true,
                        text: '正在進行版本檢查與部署請求...',
                        background: 'rgba(0, 0, 0, 0.7)',
                    });
                    let recordId = null;

                    try{
                        // 解構賦值
                        const { name: projectName, env: projectEnv, version , deployType } = versionForm.value;
                        // 將 UI env (prod|dev) 對應到此專案實際的 Jenkins env 名稱
                        // 例如 form-service: prod → admin
                        const actualEnv = resolveEnv(projectName, projectEnv)

                        // ==============================
                        // 步驟 1: 檢查是否可部署 (Check)
                        // =============================
                        const checkResult = await checkDeployable(projectName, actualEnv, version);
                        console.log('checkDeployable API回傳結果:', checkResult);

                        if (!checkResult.code) {
                            throw new Error(checkResult.msg || "版號未通過檢查，請確認版本規則");
                        }

                        // ====================================
                        // 步驟 2: 標記部署狀態 (Mark Deploying)
                        // ====================================

                        // 構建 DTO (Data Transfer Object)
                        const deployParams = {
                            projectName,
                            projectEnv: actualEnv,
                            version,
                            nodeType : deployType,
                            user: 'Web-UI',
                        };
                        const deployResult = await deploying(deployParams);
                        console.log('deploying API回傳結果:', deployResult);
                        
                        if (!deployResult.code) {
                            throw new Error(deployResult.msg || "無法標記部署狀態");
                        }
                        recordId = deployResult.data;
                        console.log('部署紀錄 ID:', recordId);

                        versionForm.value.id = recordId;
                        
                        const editParams = {
                            id : recordId,
                            projectName,
                            projectEnv,
                            version,
                            remark: versionForm.value.remark
                        };


                        const editResult = await edit(editParams);
                        
                        console.log(editResult);
                        
                        ElMessage.success("通過版號檢查並標記部署狀態");

                        // ==========================================
                        // 步驟 3: 觸發 Jenkins (Trigger Jenkins)
                        // ==========================================
                        let jenkinsResult;
                        let jenkinsEnv = actualEnv;
                        let type;
                        const proj = allProjects.value.find(p => p.name === projectName)
                        const hasCustomEnv = proj && proj.prodEnv  // 有自訂 env 的專案跳過 prod-backup 邏輯

                        // 依 env + deployType 選對應欄位（jenkins_job_name_prod/backup/dev）
                        const resolveJenkinsJob = (p, env, dType) => {
                            if (!p) return { jobName: null, token: null }
                            if (env === 'dev')      return { jobName: p.jenkinsJobNameDev    || null, token: p.jenkinsTokenDev    || null }
                            if (dType === 'backup') return { jobName: p.jenkinsJobNameBackup || null, token: p.jenkinsTokenBackup || p.jenkinsTokenProd || null }
                            return                         { jobName: p.jenkinsJobNameProd   || null, token: p.jenkinsTokenProd   || null }
                        }
                        const { jobName: jobNameOverride, token: tokenOverride } = resolveJenkinsJob(proj, projectEnv, deployType)

                        // 判斷：如果專案名稱在前端清單中，呼叫前端 API；否則呼叫後端 API
                        if (frontendProjects.value.includes(projectName)) {
                            type = 'frontend'

                            // 有明確 job name 時直接用，否則才用 env 後綴區分 prod-backup
                            if (!jobNameOverride && !hasCustomEnv) {
                                if (projectEnv === 'prod' && deployType === 'backup') {
                                    jenkinsEnv = 'prod-backup'
                                }
                                if (projectEnv === 'prod' && deployType === 'prod') {
                                    jenkinsEnv = 'prod'
                                }
                            }

                            console.log(`[部署資訊] 專案:${projectName} | 類型:${type} | 環境:${jenkinsEnv} | Job:${jobNameOverride || `${type}-${jenkinsEnv}`}`)
                            jenkinsResult = await triggerJenkinsBuild(projectName, jenkinsEnv, versionForm.value.branch, type, jobNameOverride, tokenOverride)

                        } else {
                            type = 'backend'
                            console.log(`[部署資訊] 專案:${projectName} | 類型:${type} | 環境:${actualEnv} | Job:${jobNameOverride || `${type}-${actualEnv}`}`)
                            jenkinsResult = await triggerJenkinsBuild(projectName, actualEnv, versionForm.value.branch, type, jobNameOverride, tokenOverride)
                        }

                        if (jenkinsResult.status !== 201) {
                            throw new Error(jenkinsResult.msg || "Jenkins 觸發失敗");
                        }
                        
                        // 更新 Loading 文字
                        loadingInstance.setText('請求發送成功，等待 Jenkins 排程...');
                        //ElMessage.success('Jenkins 部署請求發送成功！');

                        // ==========================================
                        // 步驟 4: 輪詢 Queue 直到拿到 Build Number
                        // ==========================================
                        const queueLocation = jenkinsResult.headers['location'];
                        console.log('Queue URL:', queueLocation);

                        if (!queueLocation) throw new Error("Jenkins 未回傳 Queue 位置");

                        let buildNumber = null;
                        let attempts = 0;
                        const maxAttempts = 30; // 最大嘗試次數 (例如 30次 * 2秒 = 60秒)


                        // 開始輪詢
                        while (!buildNumber && attempts < maxAttempts) {
                            attempts++;
                            loadingInstance.setText(`等待 Jenkins 執行中... (${attempts}/${maxAttempts})`);
                            
                            try {
                                const queueApiUrl = getQueueApiUrl(queueLocation);
                                // 帶上 Auth Header
                                const qRes = await axios.get(queueApiUrl, {
                                    auth: { username: "admin", password: "11a7af399de1d45513f9eb13e394ebe1f9" } 
                                });

                                if (qRes.data.executable && qRes.data.executable.number) {
                                    buildNumber = qRes.data.executable.number;
                                } else if (qRes.data.cancelled) {
                                    throw new Error("部署任務在 Jenkins 佇列中被取消");
                                } else {
                                    // 還沒開始，等待 2 秒再試
                                    await sleep(2000);
                                }
                            } catch (qErr) {
                                console.warn("查詢 Queue 失敗，稍後重試", qErr);
                                await sleep(2000); // 失敗也等待一下
                            }
                        }

                        if (!buildNumber) {
                            throw new Error("等待 Jenkins 建置超時，請稍後檢查 Jenkins 狀態");
                        }

                        // ========================
                        // 成功拿到 Build Number
                        // ========================
                        console.log(`Jenkins 建置開始！Build ID: ${buildNumber}`);
                        ElMessage.success(`部署成功啟動！Jenkins Build #${buildNumber}`);
                        
                        
                        const BuildIdParams = {
                            id: recordId,
                            jenkinsBuildId: buildNumber,
                        };

                        const updateResult = await updateJenkinsBuildId(BuildIdParams);
                        console.log('JenkinsBuildId更新結果:', updateResult);

                        addDialogVisible.value = false;
                        search()

                    } catch (error) {
                        console.error('部署流程發生錯誤:', error);

                        if (recordId) {
                            try {
                                const deleteDeployRecordId = await deleteVersionById(recordId);
                                console.log('部署紀錄 ID ' + deleteDeployRecordId + ' 已刪除 (回滾)');
                            } catch (deleteError) {
                                console.error('回滾刪除失敗:', deleteError);
                            }
                        }

                        ElMessage.error(error.message || "系統發生未預期錯誤，請稍後再試");
                        search()
                    } finally {
                        // 無論成功或失敗，最後要關閉 Loading
                        loadingInstance.close();
                    }
                }).catch(() => {
                    ElMessage.info('已取消');
                })
            }
            
        } else {
            ElMessage.error("表單驗證未通過 .... 請重新確認")
        }
    })
}

// --- 查看日誌的函數 ---
const handleViewLog = async (row) => {


    // 1. 設定 Dialog 標題
    currentLogTitle.value = `專案: ${row.projectName} 環境: ${row.projectEnv} 版號: ${row.version}`;

    const buildId = row.jenkinsBuildId || row.jenkins_build_id;
    let jobName = "";
    let pipelineName = "";
    let safePipelineUrl = 'Not yet generate pipeline_url , try again later when job finished';
    let pipeline_link = ""


    const rowProj = allProjects.value.find(p => p.name === row.projectName)
    const typePrefix = frontendProjects.value.includes(row.projectName) ? 'frontend' : 'backend'

    if (row.projectEnv === 'dev') {
        // dev job：優先用 jenkinsJobNameDev，否則 fallback {type}-dev
        jobName = rowProj?.jenkinsJobNameDev || `${typePrefix}-dev`
    } else {
        // prod job：依 nodeType 選對應欄位
        if (row.nodeType === 'backup') {
            jobName = rowProj?.jenkinsJobNameBackup || `${typePrefix}-prod-backup`
        } else {
            jobName = rowProj?.jenkinsJobNameProd || `${typePrefix}-prod`
        }
        // pipeline：優先用 DB jenkinsPipelineName，否則用 {type}-pipeline
        pipelineName = rowProj?.jenkinsPipelineName || `${typePrefix}-pipeline`
        pipeline_link = await getJenkinsPiplineNumber(pipelineName, jobName, buildId)
    }

    if (pipeline_link && pipeline_link.url) {
        safePipelineUrl = pipeline_link.url + 'pipeline-overview/';
    }
    const console_url = `http://192.168.1.35:8088/job/${jobName}/${buildId}/console`


    
    // 1. 設定顯示資訊
    currentLogInfo.value = {
        env: row.projectEnv || 'Unknown Env', // 環境
        version: row.version || 'Unknown Ver', // 版本
        buildId: row.jenkinsBuildId || row.jenkins_build_id || '-', // Build ID
        jobName: jobName || 'Unknown Job' ,// 專案/Job 名稱
        url: console_url || 'Unknown console_url' ,
        pipeline_url: safePipelineUrl
    };

    //console.log(jobName);
    
    if (!buildId) {
        ElMessage.warning("尚未生成 Jenkins Build ID，請稍後再試或是確認部署狀態");
        return;
    }

    if (!jobName) {
        ElMessage.error("找不到專案名稱 (Job Name)，無法跳轉");
        return;
    }

    openLogWindow(jobName, buildId);
}

// 取得 jenkins log
const openLogWindow = async (env, buildNumber) => {
    logDialogVisible.value = true;
    logContent.value = "正在讀取日誌...";
    stopPolling(); // 防止重複開啟

    isPolling.value = true;

    await pullLogRecursive(env, buildNumber, 0);
}

// --- 核心：遞迴讀取日誌 ---
// startOffset: Jenkins API 支援從某個 byte 開始讀取 (增量讀取)，
// 如果您的 API 封裝不支援 start 參數，傳 0 每次讀全部也可以 (但日誌大時會變慢)
const pullLogRecursive = async (env, buildNumber, startOffset = 0) => {
    // 1. 如果視窗關閉了，就停止執行
    if (!logDialogVisible.value || !isPolling.value) return;

    try {
        // 呼叫 API (假設 getJenkinsConsoleLog 支援第三個參數 start)
        // 如果您的 API 不支援 start，就只傳 env, buildNumber，但每次都會拿全部
        const res = await getJenkinsConsoleLog(env, buildNumber);
        
        const rawLog = res.data; // 這次拿到的文字片段
        
        // --- 判斷是否還有新資料 ---
        // Jenkins 通常會在 Header 回傳 X-More-Data: true 代表還在跑
        // 或是我們簡單判斷：如果這次拿到的 rawLog 為空，且 build 狀態還沒結束，就繼續等
        const hasMoreData = res.headers && res.headers['x-more-data'] === 'true';
        
        // 或是透過日誌內容暴力判斷是否結束 (Jenkins 標準結尾)
        const isFinished = rawLog.includes('Finished: SUCCESS') || rawLog.includes('Finished: FAILURE') || rawLog.includes('Finished: ABORTED');

        // --- 處理畫面顯示 ---
        if (rawLog) {
            // 轉換顏色
            const htmlFragment = ansiUp.ansi_to_html(rawLog);

            // 如果是增量讀取 (Offset > 0)，我們要用「追加」的方式
            // 如果是全量讀取 (每次都拿全部)，則是「覆蓋」
            if (startOffset > 0) {
                 logContent.value += htmlFragment;
            } else {
                 // 如果您的 API 每次都回傳整包，這裡直接覆蓋
                 // 注意：每次覆蓋畫面會閃爍，建議後端支援 start 參數
                 logContent.value = htmlFragment;
            }

            // 捲動到底部
            nextTick(() => {
                const terminal = document.getElementById('terminal-content');
                if (terminal) {
                    terminal.scrollTop = terminal.scrollHeight;
                }
            });
        }

        // --- 決定下一動作 ---
        if (isFinished) {
            // A. 已結束：停止輪詢
            stopPolling();
            // 補上最後的提示 (可選)
            logContent.value += '<br/><span style="color:#aaa">---日誌結束---</span>';
        } else {
            // B. 未結束：計算新的 offset 並繼續輪詢
            
            // Jenkins Header 會回傳 X-Text-Size 告訴你目前總大小，下次從這裡開始抓
            let nextOffset = 0;
            if (res.headers && res.headers['x-text-size']) {
                nextOffset = parseInt(res.headers['x-text-size'], 10);
            } else {
                // 如果沒有 header，簡單做法是全量重抓 (offset 維持 0)
                // 或者自己計算長度 (不精準，建議用 API Header)
                nextOffset = 0; 
            }

            // 設置計時器，2秒後再抓一次
            logTimer = setTimeout(() => {
                pullLogRecursive(env, buildNumber, nextOffset);
            }, 2000); 
        }

    } catch (error) {
        console.error("Log Polling Error:", error);
        
        // 遇到錯誤 (例如 404 剛開始還沒生成 Log)，不要馬上死掉，可以 retry 幾次
        // 這裡簡單做：如果還在開啟狀態，就休息 3 秒再試一次 (可能是網路波動)
        if (logDialogVisible.value) {
             logTimer = setTimeout(() => {
                pullLogRecursive(env, buildNumber, 0); // 失敗重試通常從頭抓比較保險
            }, 3000);
        }
    }
}


    // try {
    //     // 直接從前端發送請求
    //     const res = await getJenkinsConsoleLog(env, buildNumber);
        
    //     // 轉換 ANSI 編碼為 HTML
    //     const rawLog = res.data;
    //     // 將 ANSI 轉為 HTML，並將換行符 \n 轉為 HTML 換行 (如果不是用 <pre> 標籤的話需要)
    //     const htmlLog = ansiUp.ansi_to_html(rawLog);

    //     logContent.value = htmlLog;

    //     // 使用 nextTick 確保 DOM 已經更新完 HTML 內容後再捲動
    //     nextTick(() => {
    //         const terminal = document.getElementById('terminal-content');
    //         if (terminal) {
    //             terminal.scrollTop = terminal.scrollHeight;
    //         }
    //     });

    //     // 自動捲動到底部
    //     // setTimeout(() => {
    //     //     const terminal = document.getElementById('terminal-content');
    //     //     if (terminal) terminal.scrollTop = terminal.scrollHeight;
    //     // }, 100);

    // } catch (error) {
    //     console.error(error);
    //     if (error.response && error.response.status === 404) {
    //         logContent.value = '<span style="color: #ff5f56;">找不到該 Build ID 的日誌，可能已被刪除或尚未開始。</span>';
    //     } else {
    //         logContent.value = '<span style="color: #ff5f56;">讀取失敗，請確認 Jenkins 狀態。</span>';
    //     }
    // }
//}


const nodeTypeText = {
  prod: '正式機',
  backup: '備援機',
  dev: '測試機' ,
  loacl: '地端測試'
};

// ------------------------------------------------------------------------------------------- 
// 節點狀態
const customStyles = {
  prod: {
    bg: '#f0f9eb',     // 淺綠背景
    text: '#67c23a',   // 深綠文字
    border: '#e1f3d8'  // 邊框顏色
  },
  backup: {
    bg: '#fdf6ec',     // 淺橘背景
    text: '#e6a23c',   // 深橘文字
    border: '#faecd8'
  },
  dev: {
    bg: '#ecf5ff',     // 淺藍背景
    text: '#409eff',   // 深藍文字
    border: '#d9ecff'
  },
 loacl: {
    bg: '#ecf5ff',     // 淺藍背景
    text: '#409eff',   // 深藍文字
    border: '#d9ecff'
  },
};


// -------------------------------------------------------------------------------------------
// 獲取 token
const getToken = () => {
    token.value = localStorage.getItem('jwt_token')
}

// -------------------------------------------------------------------------------------------
// 初始化：從後端取得專案清單 & 當前環境
const initProjectsAndEnv = async () => {
    try {
        const [projectRes, envRes] = await Promise.all([
            getProjectList(),
            getSystemEnv()
        ])
        if (projectRes.code === 1) {
            allProjects.value = projectRes.data || []
        }
        if (envRes.code === 1) {
            currentServerEnv.value = envRes.data?.env || 'dev'
            // live board 預設顯示本機環境
            liveViewEnv.value = currentServerEnv.value
            // 若搜尋欄環境未設定，預設帶入當前機器環境
            if (!searchForm.value.env) {
                searchForm.value.env = currentServerEnv.value
            }
        }
    } catch (e) {
        console.error('初始化專案/環境失敗', e)
    }
}

// -------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------

onMounted (async () => {
    getToken();
    await initProjectsAndEnv();
    search();
    fetchLiveVersions();
})


</script>

<template>
    <div class="version-history-page">
    <div class="page-header">
        <div>
            <div style="display:flex; align-items:center; gap:10px;">
                <h1 class="page-title-main">版本歷史查詢</h1>
                <span
                    :style="{
                        padding: '3px 12px',
                        borderRadius: '12px',
                        fontSize: '13px',
                        fontWeight: 600,
                        background: liveViewEnv === 'prod' ? '#fff1f0' : '#f0f9eb',
                        color: liveViewEnv === 'prod' ? '#cf1322' : '#389e0d',
                        border: `1px solid ${liveViewEnv === 'prod' ? '#ffa39e' : '#b7eb8f'}`
                    }"
                >
                    {{ liveViewEnv === 'prod' ? '🔴 正式環境' : '🟢 測試環境' }}
                </span>
            </div>
            <p class="page-subtitle">查詢各專案部署紀錄，並即時掌握後端容器 image 版本（4 服務 · 5 節點）</p>
        </div>
        <div style="display:flex; gap:8px; align-items:center;">
            <button class="refresh-live-btn" :disabled="liveLoading" @click="fetchLiveVersions">
                <svg :class="{ 'spin-icon': liveLoading }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                    <polyline points="23 4 23 10 17 10" />
                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                </svg>
                重新整理
            </button>
        </div>
    </div>

    <div class="live-version-board">
        <div class="lvb-header">
            <div class="lvb-title-wrap">
                <span class="live-dot"></span>
                <span class="lvb-title">目前機器版本</span>
                <span class="lvb-source">backend image</span>
                <span v-if="groupedLiveProjects.length" class="lvb-count">{{ groupedLiveProjects.length }} 專案</span>
                <!-- 環境切換 tab -->
                <div style="display:inline-flex; margin-left:14px; border:1px solid #e0e0e0; border-radius:6px; overflow:hidden; font-size:12px;">
                    <button
                        @click="switchLiveViewEnv('prod')"
                        :style="{
                            padding:'3px 12px', border:'none', cursor:'pointer',
                            background: liveViewEnv === 'prod' ? '#cf1322' : '#f5f5f5',
                            color: liveViewEnv === 'prod' ? '#fff' : '#666',
                            fontWeight: liveViewEnv === 'prod' ? 600 : 400
                        }"
                    >正式機</button>
                    <button
                        @click="switchLiveViewEnv('dev')"
                        :style="{
                            padding:'3px 12px', border:'none', cursor:'pointer',
                            background: liveViewEnv === 'dev' ? '#389e0d' : '#f5f5f5',
                            color: liveViewEnv === 'dev' ? '#fff' : '#666',
                            fontWeight: liveViewEnv === 'dev' ? 600 : 400
                        }"
                    >測試機</button>
                </div>
            </div>
            <div class="lvb-actions">
                <button class="lvb-action-btn rollback-btn" @click="handleOpenRollback">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                        <polyline points="1 4 1 10 7 10" />
                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                    </svg>
                    退版
                </button>
                <button class="lvb-action-btn remove-btn" title="移除 Image" @click="handleOpenRemoveImage">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="3 6 5 6 21 6" />
                        <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                        <path d="M10 11v6" />
                        <path d="M14 11v6" />
                        <path d="M9 6V4h6v2" />
                    </svg>
                    <span>移除 Image</span>
                </button>
            </div>
        </div>

        <div v-if="liveLoading" class="lvb-loading">
            <span class="lvb-spinner"></span>
            讀取中...
        </div>
        <div v-else-if="!groupedLiveProjects.length" class="lvb-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            尚無版本資料
        </div>
        <div v-else class="lvb-grid">
            <div v-for="proj in groupedLiveProjects" :key="proj.projectName" class="lvb-project-card">
                <div class="lvb-proj-name">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                    </svg>
                    {{ proj.projectName }}
                </div>
                <div class="lvb-nodes">
                    <span
                        v-for="node in proj.nodes"
                        :key="node.raw"
                        class="lvb-node-chip"
                        :class="chipClass(node.nodeType)"
                    >
                        <span class="chip-node-label">{{ nodeTypeLabel(node.nodeType) }}</span>
                        <span class="chip-divider"></span>
                        <span class="chip-version">{{ node.version }}</span>
                    </span>
                </div>
            </div>
        </div>
    </div>

    <!-- 搜索欄 -->
    <div id="container">
        <!-- {{ versionList }} -->
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
            
            <el-form-item label="專案名稱">
                <el-select v-model="searchForm.name" placeholder="全部" clearable style="width:120px">
                    <el-option  v-for="name in projectNameOptions" :key="name.value" :label="name.label" :value="name.value" />
                    <!-- projectNameOptions 為 computed，由後端 /api/project/list 動態取得 -->
                </el-select>
            </el-form-item>
            
            <el-form-item label="環境">
                <el-select v-model="searchForm.env" placeholder="全部" clearable style="width:120px">
                    <el-option  v-for="env in projectEnvOptions" :key="env.value" :label="env.label" :value="env.value" />
                </el-select>
            </el-form-item>

            <el-form-item label="狀態">
                <el-select v-model="searchForm.state" placeholder="全部" clearable style="width:120px">
                    <el-option  v-for="state in stateOptions" :key="state.value" :label="state.label" :value="state.value" />
                </el-select>
            </el-form-item>

            <el-form-item>
                <el-button el-button type="primary" @click="search">查詢</el-button>
                <el-button el-button type="info" @click="clear">清空</el-button>
            </el-form-item>
        </el-form>

        <el-button el-button type="primary" @click="InitAddForm">新增版號</el-button>
        <el-button el-button type="danger" :disabled="multipleSelection.length === 0" @click="handleBatchDelete"> 批量刪除</el-button>

    </div>

    
    <!-- 數據表格顯示 -->
    <div class="table-container">
        
        <el-table :data="versionList" border style="width:100%" table-layout="auto" v-loading="loading" @selection-change="handleSelectionChange">
                
                <el-table-column type="selection" width="50" align="center" />
                <el-table-column prop="id" label="編號" min-width="60"/>
                <el-table-column prop="projectName" label="專案名稱" min-width="110" show-overflow-tooltip />
                <el-table-column prop="projectEnv" label="環境" min-width="80" />
          
                <el-table-column prop="nodeType" label="節點" min-width="90">
                    <template #default="scope">
                        <el-tag 
                        :color="customStyles[scope.row.nodeType]?.bg" 
                        :style="{ color: customStyles[scope.row.nodeType]?.text, borderColor: customStyles[scope.row.nodeType]?.border }"
                        effect="light"
                        >
                        {{ nodeTypeText[scope.row.nodeType] || '測試機' }}
                        </el-tag>
                    </template>
                </el-table-column>

                <el-table-column prop="version" label="版本" min-width="90"/>
                <el-table-column prop="state" label="狀態" min-width="90">
                    <template #default="scope">
                        <el-tag :type="scope.row.state === 1 ? 'success' : scope.row.state === 2 ? 'danger' : 'warning' ">
                            {{ stateOptions.find(s => s.value === scope.row.state)?.label || "未知" }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="remark" label="備註" min-width="120" show-overflow-tooltip />
                <el-table-column prop="createdTime" label="建立時間" min-width="160" show-overflow-tooltip />

                <el-table-column label="操作" min-width="180" show-overflow-tooltip>
                    <template #default="scope">
                        <el-button type="primary"  @click="handleEdit(scope.row)"> <el-icon><EditPen /></el-icon>  &nbsp; 編輯</el-button>
                        <el-button type="danger"  @click="handleDelete(scope.row)"><el-icon><Delete /></el-icon> &nbsp; 刪除</el-button>
                    </template>
                </el-table-column>

                <el-table-column label="查看jenkins操作日誌" min-width="120" align="center">
                    <template #default="scope">
                        <el-tooltip content="查看建置日誌" placement="top">
                            
                            <el-button circle type="info" plain :disabled="!scope.row.jenkinsBuildId" @click="handleViewLog(scope.row)">
                                <el-icon ><Document /></el-icon>
                            </el-button>
                        </el-tooltip>
                    </template>
                </el-table-column>

        </el-table>
        <br>
    </div>

    <!-- 分頁 -->
    <div class="page-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 30, 40, 50, 60]"
            :background="background"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalPage"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            class="custom-pagination"
        />
    </div>

    <!-- 退版 dialog -->
    <el-dialog v-model="rollbackDialogVisible" title="遠端環境退版 (Rollback)" width="500px">
        
        <el-form :model="rollbackForm" label-width="100px" v-loading="rollbackLoading">
            
            <el-form-item label="專案名稱" required>
                <el-select 
                    v-model="rollbackForm.projectName" 
                    placeholder="請選擇專案" 
                    style="width: 50%" 
                    filterable
                >
                    <el-option 
                        v-for="item in filteredProjectOptions" 
                        :key="item.value" 
                        :label="item.label" 
                        :value="item.value" 
                    />
                </el-select>
            </el-form-item>

            <el-form-item label="環境類型" required v-if="rollbackData.length > 0">
                <el-radio-group v-model="rollbackForm.type" @change="rollbackForm.version = ''">
                    <el-radio 
                        v-for="item in rollbackData" 
                        :key="item.type" 
                        :label="item.type" 
                        border
                    >
                        {{ 
                            item.type == 'prod' ? '正式機 (Prod)' : 
                            item.type == 'backup' ? '備援機 (Backup)' : 
                            item.type == 'dev' ? '測試機 (dev)' :
                            item.type == 'local' ? '地端測試 (local)' : '未知'
                        }}
                    </el-radio>
                </el-radio-group>
            </el-form-item>

            <el-form-item label="選擇版號" required v-if="rollbackForm.type">
                <el-select v-model="rollbackForm.version" placeholder="請選擇版本" style="width: 100%">
                    <el-option 
                        v-for="ver in availableVersions" 
                        :key="ver" 
                        :label="ver" 
                        :value="ver" 
                    />
                </el-select>
            </el-form-item>

            <div v-if="rollbackForm.projectName && rollbackData.length === 0 && !rollbackLoading" style="color: #909399; margin-left: 100px;">
                此專案目前查無可退版資訊
            </div>

        </el-form>

        <template #footer>
            <el-button @click="rollbackDialogVisible = false">取消</el-button>
            <el-button 
                type="danger" 
                @click="submitRollback" 
                :disabled="!rollbackForm.version"
            >
                執行退版
            </el-button>
        </template>
    </el-dialog>

    <!-- 移除 Image dialog -->
    <el-dialog v-model="removeDialogVisible" title="移除 Image" width="560px" class="remove-image-dialog">
        <div class="ri-steps">
            <span class="ri-step active">1. 選擇類別</span>
            <span class="ri-step-arrow">→</span>
            <span class="ri-step" :class="{ active: !!removeCategory }">2. 勾選版本</span>
            <span class="ri-step-arrow">→</span>
            <span class="ri-step" :class="{ active: selectedImageVersions.length > 0 }">3. 確認移除</span>
        </div>

        <div class="ri-category-row">
            <button
                type="button"
                class="ri-cat-btn"
                :class="{ selected: removeCategory === 'frontend' }"
                @click="removeCategory = 'frontend'"
            >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                <span>前端 Image</span>
                <small>frontend-prod</small>
            </button>
            <button
                type="button"
                class="ri-cat-btn"
                :class="{ selected: removeCategory === 'backend' }"
                @click="removeCategory = 'backend'"
            >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/><line x1="6" y1="6" x2="6.01" y2="6"/><line x1="6" y1="18" x2="6.01" y2="18"/></svg>
                <span>後端 Image</span>
                <small>backend-prod</small>
            </button>
        </div>

        <div v-if="removeCategory" class="ri-list-wrap">
            <div class="ri-list-header">
                <span>歷史 Image 清單</span>
                <span v-if="selectedImageVersions.length" class="ri-sel-count">已選 {{ selectedImageVersions.length }} 項</span>
            </div>

            <div v-if="removeListLoading" class="ri-loading">
                <span class="ri-spinner"></span>
                讀取歷史版本中...
            </div>
            <div v-else-if="!filteredHistoryImages.length" class="ri-empty">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/></svg>
                此類別尚無可移除的歷史 Image
            </div>
            <div v-else class="ri-list">
                <label
                    v-for="item in filteredHistoryImages"
                    :key="item.fullString"
                    class="ri-item"
                    :class="{ 'is-current': item.isCurrent, 'is-selected': selectedImageVersions.includes(item.fullString) }"
                    @click.prevent="toggleImageSelection(item.fullString, item.isCurrent)"
                >
                    <input
                        type="checkbox"
                        class="ri-checkbox"
                        :value="item.fullString"
                        :checked="selectedImageVersions.includes(item.fullString)"
                        :disabled="item.isCurrent"
                        @click.stop
                        @change="toggleImageSelection(item.fullString, item.isCurrent)"
                    />
                    <div class="ri-item-body">
                        <div class="ri-item-top">
                            <span class="ri-proj">{{ item.projectName }}</span>
                            <span class="ri-node-tag" :class="removeNodeTagClass(item.nodeType)">
                                {{ nodeTypeLabel(item.nodeType) }}
                            </span>
                            <span v-if="item.isCurrent" class="ri-in-use-badge">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                                使用中
                            </span>
                        </div>
                        <code class="ri-ver">{{ item.fullString }}</code>
                    </div>
                </label>
            </div>

            <div v-if="selectedImageVersions.length" class="ri-warning">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                <span>將永久刪除 <strong>{{ selectedImageVersions.length }}</strong> 個 Image，此操作<strong>無法復原</strong></span>
            </div>
        </div>

        <template #footer>
            <el-button @click="removeDialogVisible = false">取消</el-button>
            <el-button
                type="danger"
                :loading="removeSubmitting"
                :disabled="!selectedImageVersions.length"
                @click="submitRemoveImages"
            >
                確認移除
            </el-button>
        </template>
    </el-dialog>


    <!-- 新增版號(add) dialog -->
     
    <el-dialog v-model="addDialogVisible" :title="formTitle" width="600px" class="custom-edit-dialog">

        <el-form :model="versionForm" :rules="rules" ref="versionFormRef" label-width="90px">
            <!-- {{ versionForm }} -->
            <el-form-item label="專案" prop="name">
                <el-select v-model="versionForm.name" style="width:50%">
                    <!-- <el-option v-for="n in projectNameOptions" :key="n.value" :label="n.label" :value="n.value" /> -->
                    <el-option 
                        v-for="item in filteredProjectOptions" 
                        :key="item.value" 
                        :label="item.label" 
                        :value="item.value" 
                    />
                </el-select>
            </el-form-item>

            <el-form-item label="環境" prop="env">
                <el-select v-model="versionForm.env" style="width:50%">
                    <el-option v-for="e in projectEnvOptions" :key="e.value" :label="e.label" :value="e.value" />
                </el-select>
            </el-form-item>

            <el-form-item label="部署類型" prop="deployType" v-if="versionForm.env === 'prod' && frontendProjects.includes(versionForm.name)">
                <el-select v-model="versionForm.deployType" placeholder="請選擇類型" style="width:50%">
                    <el-option v-for="item in deployTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>

            <el-form-item label="版號" prop="version">
                <el-input v-model="versionForm.version" placeholder="" style="width:50%"/>
            </el-form-item>

            <el-form-item label="分支" prop="branch">
                <el-input v-model="versionForm.branch" placeholder="develop" style="width:50%"/>
            </el-form-item>

            <el-form-item label="備註">
                <el-input type="textarea" v-model="versionForm.remark" />
            </el-form-item>

        </el-form>

        <template #footer>
            <el-button @click="addDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitVersionAddandEdit()">確認</el-button>
        </template>
    </el-dialog>

    <!-- 修改版號(edit) dialog -->
    <el-dialog v-model="editDialogVisible" :title="formTitle" width="600px" class="custom-edit-dialog">

        <el-form :model="versionForm" :rules="rules" ref="versionFormRef" label-width="90px">

            <el-form-item label="備註">
                <el-input type="textarea" v-model="versionForm.remark" />
            </el-form-item>

        </el-form>

        <template #footer>
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitVersionAddandEdit()">確認</el-button>
        </template>
    </el-dialog>


    <el-dialog 
        v-model="logDialogVisible" 
        :title="'建置日誌: ' + currentLogTitle" 
        width="900px" 
        class="terminal-dialog"
        destroy-on-close
        top="5vh"
    >
        <div class="log-info-bar">
            <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="專案環境">
                    <el-tag type="success" size="small">{{ currentLogInfo.env }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="版本號">
                    <el-tag type="primary" size="small">{{ currentLogInfo.version }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Jenkins Build ID">
                    <span style="font-family: monospace; font-weight: bold;">
                        #{{ currentLogInfo.buildId }}
                    </span>
                </el-descriptions-item>

                <el-descriptions-item label="console 連結" >
                    <el-link :href="currentLogInfo.url" target="_blank" :underline="false">
                        <el-tag type="primary" size="small">{{ currentLogInfo.url }}</el-tag>
                    </el-link>
                </el-descriptions-item>

                <el-descriptions-item label="pipeline 連結" v-if="currentLogInfo.env == 'prod'" >
                    <el-link :href="currentLogInfo.pipeline_url" target="_blank" :underline="false">
                        <el-tag type="primary" size="small">{{ currentLogInfo.pipeline_url }}</el-tag>
                    </el-link>
                </el-descriptions-item>

            </el-descriptions>
        </div>

        <div class="terminal-window" v-loading="logLoading" element-loading-background="rgba(0, 0, 0, 0.8)">
            <div class="terminal-header">
                <!-- <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span> -->
                <pre  id="terminal-content" class="terminal-body" v-html="logContent"></pre>
            </div>
            <!-- <pre id="terminal-content" class="terminal-body">{{ logContent }}</pre> -->
        </div>
    </el-dialog>



    </div>
</template>

<style scoped>
.version-history-page {
  padding: 0 4px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title-main {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 4px;
  letter-spacing: -0.02em;
}

.page-subtitle {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

.refresh-live-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 16px;
  border-radius: 9px;
  border: 1px solid var(--border-color);
  background: var(--panel);
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.refresh-live-btn svg {
  width: 15px;
  height: 15px;
}

.refresh-live-btn:hover:not(:disabled) {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-muted);
}

.refresh-live-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spin-icon {
  animation: spin-live 1s linear infinite;
}

.live-version-board {
  background: var(--panel);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 16px 20px 20px;
  margin-bottom: 24px;
  box-shadow: var(--shadow);
}

.lvb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 8px;
  flex-wrap: wrap;
}

.lvb-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.lvb-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.lvb-source {
  font-size: 12px;
  color: var(--muted);
  padding: 2px 8px;
  background: var(--panel-alt);
  border: 1px solid var(--border-color);
  border-radius: 99px;
}

.lvb-count {
  font-size: 12px;
  color: var(--muted);
  margin-left: 4px;
}

.lvb-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lvb-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 13px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--panel-alt);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.18s ease;
}

.lvb-action-btn svg {
  width: 14px;
  height: 14px;
}

.rollback-btn {
  color: #d97706;
  border-color: color-mix(in srgb, #f59e0b 30%, transparent);
  background: color-mix(in srgb, #f59e0b 8%, transparent);
}

.rollback-btn:hover {
  background: color-mix(in srgb, #f59e0b 14%, transparent);
  border-color: #f59e0b;
}

.remove-btn {
  color: var(--danger);
  border-color: color-mix(in srgb, var(--danger) 30%, transparent);
  background: color-mix(in srgb, var(--danger) 8%, transparent);
}

.remove-btn:hover {
  background: color-mix(in srgb, var(--danger) 14%, transparent);
  border-color: var(--danger);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--danger) 20%, transparent);
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--success) 25%, transparent);
  animation: pulse-glow 2s ease-in-out infinite;
}

.lvb-loading,
.lvb-empty {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
  padding: 8px 0;
}

.lvb-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-color);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin-live 0.75s linear infinite;
}

.lvb-empty svg {
  width: 18px;
  height: 18px;
  opacity: 0.55;
}

.lvb-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.lvb-project-card {
  background: var(--panel-alt);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px 14px;
  flex: 0 0 auto;
  min-width: 180px;
  max-width: 280px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.lvb-project-card:hover {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-muted);
}

.lvb-proj-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 10px;
}

.lvb-proj-name svg {
  width: 14px;
  height: 14px;
  color: var(--muted);
}

.lvb-nodes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.lvb-node-chip {
  display: inline-flex;
  align-items: center;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  border: 1px solid transparent;
}

.chip-node-label {
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 600;
}

.chip-divider {
  width: 1px;
  align-self: stretch;
  background: currentColor;
  opacity: 0.2;
}

.chip-version {
  padding: 4px 9px;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.chip-prod {
  background: color-mix(in srgb, #10b981 10%, transparent);
  color: #059669;
  border-color: color-mix(in srgb, #10b981 30%, transparent);
}

.chip-backup {
  background: color-mix(in srgb, #f59e0b 10%, transparent);
  color: #b45309;
  border-color: color-mix(in srgb, #f59e0b 30%, transparent);
}

.chip-dev,
.chip-none {
  background: color-mix(in srgb, #818cf8 10%, transparent);
  color: #4f46e5;
  border-color: color-mix(in srgb, #818cf8 30%, transparent);
}

.chip-local {
  background: color-mix(in srgb, #94a3b8 10%, transparent);
  color: #64748b;
  border-color: color-mix(in srgb, #94a3b8 30%, transparent);
}

html[data-theme='dark'] .chip-prod { color: #34d399; }
html[data-theme='dark'] .chip-backup { color: #fbbf24; }
html[data-theme='dark'] .chip-dev,
html[data-theme='dark'] .chip-none { color: #a5b4fc; }
html[data-theme='dark'] .chip-local { color: #94a3b8; }
html[data-theme='dark'] .rollback-btn { color: #fbbf24; }

@keyframes spin-live {
  to { transform: rotate(360deg); }
}

@keyframes pulse-glow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

.ri-steps {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.ri-step {
  font-size: 12px;
  font-weight: 500;
  color: var(--muted);
  padding: 4px 10px;
  border-radius: 99px;
  background: var(--panel-alt);
  border: 1px solid var(--border-color);
}

.ri-step.active {
  color: var(--brand);
  background: var(--brand-muted);
  border-color: color-mix(in srgb, var(--brand) 30%, transparent);
}

.ri-step-arrow { color: var(--muted); font-size: 16px; }

.ri-category-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}

.ri-cat-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 18px 16px;
  border-radius: 12px;
  border: 2px solid var(--border-color);
  background: var(--panel-alt);
  color: var(--muted);
  cursor: pointer;
  font-family: inherit;
}

.ri-cat-btn svg { width: 24px; height: 24px; }
.ri-cat-btn span { font-size: 15px; font-weight: 600; color: var(--text); }
.ri-cat-btn small { font-size: 11px; color: var(--muted); }
.ri-cat-btn.selected {
  border-color: var(--brand);
  background: var(--brand-muted);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--brand) 12%, transparent);
}
.ri-cat-btn.selected span { color: var(--brand); }

.ri-list-wrap { border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; }
.ri-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--panel-alt);
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}
.ri-sel-count { font-size: 12px; font-weight: 400; color: var(--muted); }
.ri-loading, .ri-empty {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 20px;
  font-size: 13px;
  color: var(--muted);
}
.ri-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-color);
  border-top-color: var(--brand);
  border-radius: 50%;
  animation: spin-live 0.75s linear infinite;
}
.ri-list { max-height: 320px; overflow-y: auto; }
.ri-list::-webkit-scrollbar { width: 5px; }
.ri-list::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 99px; }
.ri-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color);
  user-select: none;
}
.ri-item:last-child { border-bottom: none; }
.ri-item:hover:not(.is-current) { background: var(--panel-alt); }
.ri-item.is-current { opacity: 0.55; cursor: not-allowed; }
.ri-item.is-selected { background: var(--brand-muted); }
.ri-checkbox { margin-top: 3px; accent-color: var(--brand); }
.ri-item-body { flex: 1; min-width: 0; }
.ri-item-top { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; flex-wrap: wrap; }
.ri-proj { font-size: 13px; font-weight: 600; color: var(--text); }
.ri-node-tag { font-size: 11px; font-weight: 600; padding: 2px 7px; border-radius: 99px; border: 1px solid transparent; }
.rn-prod { background: color-mix(in srgb, #10b981 12%, transparent); color: #059669; border-color: color-mix(in srgb, #10b981 30%, transparent); }
.rn-backup { background: color-mix(in srgb, #f59e0b 12%, transparent); color: #b45309; border-color: color-mix(in srgb, #f59e0b 30%, transparent); }
.rn-dev { background: color-mix(in srgb, #818cf8 12%, transparent); color: #4f46e5; border-color: color-mix(in srgb, #818cf8 30%, transparent); }
.rn-local { background: color-mix(in srgb, #94a3b8 12%, transparent); color: #64748b; border-color: color-mix(in srgb, #94a3b8 30%, transparent); }
.ri-in-use-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: var(--success);
  background: color-mix(in srgb, var(--success) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--success) 30%, transparent);
  padding: 2px 8px;
  border-radius: 99px;
}
.ri-in-use-badge svg { width: 11px; height: 11px; }
.ri-ver {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px;
  color: var(--muted);
  word-break: break-all;
}
.ri-item.is-selected .ri-ver { color: var(--brand); }
.ri-warning {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: color-mix(in srgb, var(--danger) 8%, var(--panel));
  border-top: 1px solid color-mix(in srgb, var(--danger) 25%, transparent);
  font-size: 13px;
  color: var(--danger);
}
.ri-warning svg { width: 16px; height: 16px; flex-shrink: 0; }

.container{
    margin: 10px 0px;
}

/* =============================== */
/*    表格組件樣式 (Table)          */
/* =============================== */
/* template 中使用了 .table-container 包裹 el-table */
.table-container {
    border-radius: 15px;
    padding: 0;
    overflow-x: auto;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
    margin-top: 20px;
    max-width: 1500px;
    width: 95%;
    margin-left: auto;
    margin-right: auto;
}

/* 核心：覆蓋 Element Plus 表格的 CSS 變量，採用低飽和度色彩 */
:deep(.table-container .el-table) {
    font-size: 13px;
    line-height: 1.6;
    min-width: 1200px;
}

:deep(.el-table) {
    /* 讓表格體透明，這樣它會透出 .table-container 的深色背景 */
    --el-table-bg-color: var(--table-bg-color) !important;
    --el-table-tr-bg-color: var(--table-bg-color) !important;
    
    /* 表頭：使用一個更深的暗色，以提供視覺上的層次感（如您提供的範例圖所示）*/
    --el-table-header-bg-color: var(--table-bg-color) !important;
    
    /* 邊框顏色：保持極淡，融入背景 */
    --el-table-border-color: var(--table-border-color) !important;
    --el-table-border: 1px solid var(--el-table-border-color) !important;
    
    /* 文字顏色：使用標準淺灰色（非螢光色） */
    /* --el-table-text-color: #d2cbe1 !important; */
    --el-table-text-color: var(--table-text-color) !important;
    --el-table-header-text-color: var(--table-header-text-color) !important; /* 表頭文字稍亮 */
    
    /* Hover 高亮：微小的白色透明度 */
    --el-table-row-hover-bg-color: var(--table-hover-bg) !important;
    
    background-color: transparent !important;
}

/* 強制表格的內容區塊也是透明的 */
:deep(.el-table__inner-wrapper) {
    background-color: transparent !important;
}

/* 讓表格單元格的背景透明並調整邊框 */
:deep(.el-table th.el-table__cell),
:deep(.el-table td.el-table__cell) {
    background-color: var(--table-bg-color) !important;
    border-bottom: 1px solid var(--table-border-color) !important;
    padding: 10px 12px !important;
}

/* 調整空數據提示文字的顏色 */
:deep(.el-table__empty-text) {
    color: var(--table-header-text-color) !important;
    font-size: 15px;
}

/* 去除表格底部的白色線條（Element Plus 默認邊框） */
:deep(.el-table::before) {
    height: 0 !important;
}

/* =============================== */
/* 按鈕樣式加強 (如果全局沒生效) */
/* =============================== */

/* 主按鈕 (新增/編輯) */
:deep(.el-button--primary) {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    border: none;
    box-shadow: 0 2px 6px rgba(99, 102, 241, 0.4);
    color: white;
}
:deep(.el-button--primary:hover) {
    opacity: 0.9;
    transform: translateY(-1px);
}

/* 危險按鈕 (刪除) */
:deep(.el-button--danger) {
    background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
    border: none;
    box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}
:deep(.el-button--danger:hover) {
    opacity: 0.9;
    transform: translateY(-1px);
}

/* 表格內的小按鈕調整 */
:deep(.el-table .el-button) {
    padding: 6px 12px;
    height: auto;
    font-size: 12px;
}


/* =============================== */
/* 分頁組件樣式 (Pagination) */
/* =============================== */

.page-container {
    padding: 16px 0;
    display: flex;
    justify-content: center;
}

/* 核心：設定分頁組件的文字和背景基調 */
:deep(.custom-pagination) {
    /* 總體文字顏色 */
    --el-text-color-regular: #cbd5e1; /* slate-300 */
    /* 分頁背景透明 (讓它透出主內容區的背景) */
    --el-pagination-bg-color: transparent !important;
    padding: 15px 0; /* 增加上下間距 */

}

/* 針對 Prev/Next 按鈕和頁碼數字的背景/邊框/文字調整 */
:deep(.custom-pagination .btn-prev),
:deep(.custom-pagination .btn-next),
:deep(.custom-pagination .el-pager li),
:deep(.custom-pagination .el-select .el-input__wrapper) /* 調整 Size 選擇器的外觀 */
{
    /* 讓按鈕背景為極深藍 (slate-900)，比卡片背景 #1e293b 更深，以增加層次感 */
    background-color: #0f172a !important; 
    
    /* 邊框使用極淡的白色，達到科技感邊緣效果 */
    border: 1px solid rgba(255, 255, 255, 0.15); 
    color: #cbd5e1 !important;
    border-radius: 6px; /* 圓角調整 */
    transition: all 0.3s;
}

/* 活躍/選中頁碼的樣式 (核心高亮) */
:deep(.custom-pagination .el-pager li.is-active) {
    /* 漸層紫色高亮 */
    background: linear-gradient(45deg, #6366f1 0%, #8b5cf6 100%) !important; 
    border-color: #8b5cf6 !important; /* 邊框顏色與漸層呼應 */
    color: white !important;
    font-weight: bold;
    transform: scale(1.05); /* 輕微放大效果 */
} 

/* 頁碼懸停 (Hover) 效果 */
:deep(.custom-pagination .el-pager li:hover:not(.is-active)) {
    background-color: #1e293b !important; /* slate-800 hover */
    border-color: #681656; /* 懸停時邊框變成主題紫色 */
    color: #8b5cf6 !important;
}

/* 調整總數文字的顏色 */
:deep(.custom-pagination .el-pagination__total) {
    color: #94a3b8; /* slate-400 */
}

/* 調整跳頁輸入框的邊框 */
:deep(.custom-pagination .el-input.el-pagination__editor.is-in-pagination .el-input__wrapper) {
    background-color: #0f172a !important; 
    border: 1px solid rgba(255, 255, 255, 0.15) !important;
    box-shadow: none !important;
}


.glass-confirm .el-button--primary {
    /* 確保按鈕使用您定義的漸層和發光效果 */
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
    border: none !important;
    color: #fff !important; /* 文字顏色 */
    box-shadow: 0 4px 10px rgba(99, 102, 241, 0.4);
    font-weight: 600 !important;
    transition: all 0.3s ease;
}

.glass-confirm .el-button--primary:hover {
    opacity: 0.9;
    transform: translateY(-1px);
}


/* 2. ❌ 取消按鈕 (Cancel Button) - 玻璃透明/淡白色邊框 */
.glass-confirm .el-button--default,
.glass-confirm .el-button--info {
    /* 移除預設的白色背景 */
    background: rgba(255, 255, 255, 0.1) !important; 
    
    /* 增加透明邊框和柔和文字顏色 */
    border: 1px solid rgba(255, 255, 255, 0.2) !important;
    color: var(--text-main, #f1f5f9) !important; 
    font-weight: 500 !important;
}

.glass-confirm .el-button--default:hover,
.glass-confirm .el-button--info:hover {
    /* 懸停時背景輕微加亮 */
    background: rgba(255, 255, 255, 0.2) !important;
    border-color: rgba(255, 255, 255, 0.3) !important;
}

/* 3. 調整警告圖標顏色 (選填，使警告色更柔和) */
.glass-confirm .el-icon-warning {
    color: #fbbf24 !important; /* 柔和的黃色/琥珀色 */
}
</style>

<style>
/* 注意：這裡沒有 scoped
   因為 Dialog 被掛載到 body，scoped 樣式無法觸及
*/

/* 1. 針對這個 Dialog 內的 "確認" 按鈕 (Primary) */
.custom-edit-dialog .el-dialog__footer .el-button--primary {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); /* 紫色系漸層 */
    border: none;
    box-shadow: 0 2px 6px rgba(99, 102, 241, 0.4);
    color: white;
    transition: all 0.3s;
}

.custom-edit-dialog .el-dialog__footer .el-button--primary:hover {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.5);
}

/* 2. 針對這個 Dialog 內的 "取消" 按鈕 (Default) */
/* 排除 primary 和 danger，剩下的就是普通按鈕 */
.custom-edit-dialog .el-dialog__footer .el-button:not(.el-button--primary):not(.el-button--danger) {
    background-color: #f3f4f6; /* 淡淡的灰色背景 */
    border: 1px solid #e5e7eb;
    color: #4b5563;
}

.custom-edit-dialog .el-dialog__footer .el-button:not(.el-button--primary):not(.el-button--danger):hover {
    background-color: #e5e7eb;
    color: #111827;
    border-color: #d1d5db;
}
</style>

<style>
/* 終端機 Dialog 樣式 (放在 scoped 外或 global) */
.terminal-dialog .el-dialog__body {
    padding: 0 !important;
    background-color: #1e1e1e;
}
.terminal-dialog .el-dialog__header {
    background-color: #2d2d2d;
    margin-right: 0;
    padding-bottom: 15px;
}
.terminal-dialog .el-dialog__title {
    color: #ccc;
    font-family: monospace;
}
</style>

<style scoped>
.log-info-bar {
    margin-bottom: 15px;
}

/* 終端機視窗本體 */
.terminal-window {
    background-color: #1e1e1e; /* VSCode 深色背景 */
    color: #d4d4d4;             /* 淺灰文字 */
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    border-radius: 0 0 4px 4px;
    overflow: hidden;
}

.terminal-window {
    background-color: #1e1e1e;
    border-radius: 6px;
    box-shadow: 0 4px 10px rgba(0,0,0,0.3);
    overflow: hidden;
    font-family: 'Consolas', 'Monaco', monospace;
}

/* 模擬 Mac 視窗紅黃綠燈 */
.terminal-header {
    background-color: #252526;
    padding: 8px 15px;
    display: flex;
    gap: 8px;
    border-bottom: 1px solid #333;
}
.dot { width: 12px; height: 12px; border-radius: 50%; }
.dot.red { background-color: #ff5f56; }
.dot.yellow { background-color: #ffbd2e; }
.dot.green { background-color: #27c93f; }

/* Log 內容區 */
.terminal-body {
    background-color: #1e1e1e; /* 深色背景 */
    color: #f0f0f0;            /* 預設文字顏色 (白色/淺灰) */
    padding: 15px;
    margin: 0;
    white-space: pre-wrap;     /* 保留換行格式 */
    word-break: break-all;
    max-height: 500px;
    overflow-y: auto;
    font-family: 'Consolas', 'Monaco', monospace; /* 等寬字體 */
    font-size: 13px;
    line-height: 1.5;
}

/* 自定義捲軸 */
.terminal-body::-webkit-scrollbar {
    width: 10px;
}
.terminal-body::-webkit-scrollbar-track {
    background: #1e1e1e; 
}
.terminal-body::-webkit-scrollbar-thumb {
    background: #444; 
    border-radius: 5px;
}
.terminal-body::-webkit-scrollbar-thumb:hover {
    background: #555; 
}
</style>