<script setup>
import { ref, onMounted, watch } from 'vue'
import * as yaml from 'js-yaml'
import { Plus, Edit, Delete, Setting, Refresh, ArrowDown, Connection, Tools } from '@element-plus/icons-vue'
import { getProjectListAll, addProject, updateProject, deleteProject, initProject } from '@/api/project'
import { getProjectConfig, saveProjectConfig, checkConfigSync, syncConfigToRemote, copyConfigFrom } from '@/api/config'
import { getDockerCompose, saveDockerCompose } from '@/api/dockerCompose'
import { healthCheck as monitorHealthCheck } from '@/api/monitor'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

// ── 表格 ──────────────────────────────────────────────────────────────────
const tableData = ref([])
const loading   = ref(false)

// ── 新增/編輯 Dialog ──────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle   = ref('')
const isEdit        = ref(false)
const dialogTab     = ref('basic')

const emptyForm = () => ({
  id: null, name: '', displayName: '', description: '',
  gitlabProjectId: null, category: 'backend', isActive: 1, sortOrder: 99,
  scriptName: '', imageKeyword: '', hasProd: 1, hasDev: 0,
  prodEnv: '', devEnv: '',
  jenkinsJobNameProd: '', jenkinsJobNameBackup: '', jenkinsJobNameDev: '',
  jenkinsTokenProd: '', jenkinsTokenBackup: '', jenkinsTokenDev: '',
  jenkinsPipelineName: '', defaultBranch: '',
  prodSshEnv: '', devSshEnv: '',
})
const form    = ref(emptyForm())
const formRef = ref(null)
const rules = {
  name:        [{ required: true, message: '請輸入系統名稱', trigger: 'blur' }],
  displayName: [{ required: true, message: '請輸入顯示名稱', trigger: 'blur' }],
  category:    [{ required: true, message: '請選擇分類',     trigger: 'change' }],
}

// ── Config Drawer ──────────────────────────────────────────────────────────
const configDrawerVisible = ref(false)
const configLoading       = ref(false)
const configSaving        = ref(false)
const configProjectName   = ref('')
const configTab           = ref('prod')

const PROD_FIELDS = [
  { key: 'PROD_BLUE_CONTAINERS',   label: 'Blue 容器',         hint: '多個以空格分隔，如 go_nuxt go_nuxt2' },
  { key: 'PROD_GREEN_CONTAINERS',  label: 'Green/Backup 容器', hint: '多個以空格分隔，無則留空' },
  { key: 'PROD_BLUE_CHECK_PORTS',  label: 'Blue 健檢 Port',    hint: '多個以空格分隔，如 8091 8094' },
  { key: 'PROD_GREEN_CHECK_PORTS', label: 'Green 健檢 Port',   hint: '多個以空格分隔，無則留空' },
  { key: 'PROD_NGINX_CONF',        label: 'nginx conf 路徑',   hint: '如 /etc/nginx/conf.d/tv/nginx-tv.conf' },
  { key: 'PROD_LIVE_UPSTREAM',     label: 'Live Upstream',     hint: 'nginx 正式流量 upstream' },
  { key: 'PROD_HEADER_UPSTREAM',   label: 'Header Upstream',   hint: 'nginx 測試 header upstream' },
  { key: 'PROD_TRAFFIC_BLUE_PORT', label: 'Blue 流量 Port',    hint: 'switch_traffic 用' },
  { key: 'PROD_DEPLOY_BASE',       label: 'Deploy 目錄',       hint: 'docker-compose.yml 所在目錄' },
  { key: 'PROD_SWITCH_SCRIPT',     label: 'Switch Script',     hint: '留空用 ${PROD_DEPLOY_BASE}/script/switch_traffic.sh' },
  { key: 'PROD_HEALTH_HOST',       label: 'Health Host',       hint: '如 www.tkbtv.com.tw' },
  { key: 'PROD_HEALTH_SCHEME',     label: 'Health Scheme',     type: 'select', options: ['http', 'https'] },
  { key: 'PROD_HEALTH_PATH',       label: 'Health Path',       hint: '如 /front/toHeader.action' },
  { key: 'PROD_IMAGE_REPO',        label: 'Image Repo',        hint: '如 backend-prod / frontend-prod' },
]
const DEV_FIELDS = [
  { key: 'DEV_BLUE_CONTAINERS',   label: 'Blue 容器',         hint: '多個以空格分隔' },
  { key: 'DEV_GREEN_CONTAINERS',  label: 'Green/Backup 容器', hint: '無則留空' },
  { key: 'DEV_BLUE_CHECK_PORTS',  label: 'Blue 健檢 Port',    hint: '多個以空格分隔' },
  { key: 'DEV_GREEN_CHECK_PORTS', label: 'Green 健檢 Port',   hint: '無則留空' },
  { key: 'DEV_NGINX_CONF',        label: 'nginx conf 路徑',   hint: '無流量切換則留空' },
  { key: 'DEV_LIVE_UPSTREAM',     label: 'Live Upstream',     hint: '無流量切換則留空' },
  { key: 'DEV_HEADER_UPSTREAM',   label: 'Header Upstream',   hint: '無流量切換則留空' },
  { key: 'DEV_TRAFFIC_BLUE_PORT', label: 'Blue 流量 Port',    hint: '無流量切換則留空' },
  { key: 'DEV_DEPLOY_BASE',       label: 'Deploy 目錄',       hint: 'docker-compose.yml 所在目錄' },
  { key: 'DEV_SWITCH_SCRIPT',     label: 'Switch Script',     hint: '留空用預設路徑' },
  { key: 'DEV_HEALTH_HOST',       label: 'Health Host',       hint: '' },
  { key: 'DEV_HEALTH_SCHEME',     label: 'Health Scheme',     type: 'select', options: ['http', 'https'] },
  { key: 'DEV_HEALTH_PATH',       label: 'Health Path',       hint: '' },
  { key: 'DEV_IMAGE_REPO',        label: 'Image Repo',        hint: '如 backend-dev / frontend-dev' },
]
const SHARED_FIELDS = [
  { key: 'IMAGE_KEYWORD', label: 'Image Keyword', hint: 'Docker image grep 關鍵字，留空使用系統名稱' },
  { key: 'BLUE_ENV_KEY',  label: 'Blue Env Key',  hint: '通常 BLUE_VERSION' },
  { key: 'GREEN_ENV_KEY', label: 'Green Env Key', hint: '通常 GREEN_VERSION' },
]
const configForm = ref({ prod: {}, dev: {}, shared: {} })

// ── 健康檢查 ──────────────────────────────────────────────────────────────
const healthChecking = ref({ prod: false, dev: false })
// { prod: { blue: {ok,code}, green: {ok,code} }, dev: {...} }
const healthResult   = ref({ prod: null, dev: null })

const handleHealthCheck = async (env) => {
  healthChecking.value[env] = true
  healthResult.value[env]   = null
  const project = configProjectName.value
  const parse = (r) => {
    if (r.status === 'rejected') return { ok: false, msg: '請求異常' }
    const code = r.value?.data ?? r.value?.code
    const ok   = typeof code === 'number' ? code >= 200 && code < 400 : false
    return { ok, msg: typeof code === 'number' ? String(code) : '—' }
  }
  try {
    if (env === 'prod') {
      const [blueRes, greenRes] = await Promise.allSettled([
        monitorHealthCheck(env, project, 'blue'),
        monitorHealthCheck(env, project, 'green'),
      ])
      healthResult.value[env] = { blue: parse(blueRes), green: parse(greenRes) }
    } else {
      const [blueRes] = await Promise.allSettled([monitorHealthCheck(env, project, 'blue')])
      healthResult.value[env] = { blue: parse(blueRes), green: null }
    }
  } catch {
    healthResult.value[env] = { blue: { ok: false, msg: '請求異常' }, green: null }
  } finally {
    healthChecking.value[env] = false
  }
}

// ── 載入 ──────────────────────────────────────────────────────────────────
const fetchList = async () => {
  loading.value = true
  try {
    const res = await getProjectListAll()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

// ── 新增/編輯 ──────────────────────────────────────────────────────────────
const handleAdd = () => {
  isEdit.value = false; dialogTitle.value = '新增專案'; dialogTab.value = 'basic'
  form.value = emptyForm(); dialogVisible.value = true
}
const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '編輯專案'; dialogTab.value = 'basic'
  form.value = { ...row }; dialogVisible.value = true
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(
    `確定刪除「${row.displayName || row.name}」？`, '刪除確認',
    { type: 'warning', confirmButtonText: '確定刪除', cancelButtonText: '取消' }
  )
  try {
    await deleteProject(row.id); ElMessage.success('刪除成功'); fetchList()
  } catch { ElMessage.error('刪除失敗') }
}
const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (isEdit.value) {
      await updateProject(form.value); ElMessage.success('修改成功')
    } else {
      await addProject(form.value); ElMessage.success('新增成功')
    }
    dialogVisible.value = false; fetchList()
  } catch { ElMessage.error(isEdit.value ? '修改失敗' : '新增失敗') }
}

// ── 初始化 ─────────────────────────────────────────────────────────────────
// ── 初始化 — 確認 dialog ─────────────────────────────────────────────────
const initConfirmVisible = ref(false)
const initConfirmChecked = ref(false)
const initPendingRow     = ref(null)

const initDialogVisible = ref(false)
const initLoading       = ref(false)
const initResult        = ref(null)  // { config, prod, dev }
const initProjectName   = ref('')

/** 第一步：開啟確認 dialog */
const handleInitConfirm = (row) => {
  initPendingRow.value    = row
  initConfirmChecked.value = false
  initConfirmVisible.value = true
}

/** 第二步：確認後執行初始化 */
const handleInit = async () => {
  const row = initPendingRow.value
  initConfirmVisible.value = false
  initProjectName.value    = row.displayName || row.name
  initResult.value         = null
  initDialogVisible.value  = true
  initLoading.value        = true
  try {
    const res = await initProject(row.name)
    if (res.code === 1) {
      initResult.value = res.data
    } else {
      initResult.value = { config: '❌ ' + (res.msg || '初始化失敗'), prod: '', dev: '' }
    }
  } catch (e) {
    initResult.value = { config: '❌ ' + (e?.message || '請求失敗'), prod: '', dev: '' }
  } finally {
    initLoading.value = false
  }
}

// ── 列操作 dropdown ───────────────────────────────────────────────────────
const handleRowAction = (cmd, row) => {
  if (cmd === 'edit')    handleEdit(row)
  if (cmd === 'delete')  handleDelete(row)
  if (cmd === 'init')    handleInitConfirm(row)
  if (cmd === 'compose') handleComposeOpen(row)
}

// ── Config Copy ──────────────────────────────────────────────────────────
const copyDialogVisible = ref(false)
const copySourceProject = ref('')
const copyCopying       = ref(false)

const handleCopyOpen = () => {
  copySourceProject.value = ''
  copyDialogVisible.value = true
}

const handleCopyConfirm = async () => {
  if (!copySourceProject.value) { ElMessage.warning('請選擇來源專案'); return }
  copyCopying.value = true
  try {
    const res = await copyConfigFrom(configProjectName.value, copySourceProject.value)
    if (res.code === 1) {
      // 直接用回傳的 DTO 刷新表單，不需再發一次 GET
      configForm.value = { prod: res.data.prod || {}, dev: res.data.dev || {}, shared: res.data.shared || {} }
      ElMessage.success(`已從「${copySourceProject.value}」複製 config.sh`)
      copyDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '複製失敗')
    }
  } catch { ElMessage.error('複製失敗') }
  finally { copyCopying.value = false }
}

// ── Config Sync ───────────────────────────────────────────────────────────
const syncDialogVisible = ref(false)
const syncChecking      = ref(false)
const syncSyncing       = ref(false)
const syncCheckResult   = ref(null)   // { errors, warnings, canSync }
const syncFinalResult   = ref(null)   // { syncResult, warnings }

const handleSync = async () => {
  syncCheckResult.value = null
  syncFinalResult.value = null
  syncChecking.value = true
  syncDialogVisible.value = true
  try {
    const res = await checkConfigSync(configProjectName.value, configForm.value)
    if (res.code === 1) {
      syncCheckResult.value = res.data
    } else {
      syncCheckResult.value = { errors: [res.msg || '驗證失敗'], warnings: [], canSync: false }
    }
  } catch (e) {
    syncCheckResult.value = { errors: [e?.message || '請求失敗'], warnings: [], canSync: false }
  } finally {
    syncChecking.value = false
  }
}

const confirmSync = async () => {
  syncSyncing.value = true
  try {
    const res = await syncConfigToRemote(configProjectName.value)
    if (res.code === 1) {
      syncFinalResult.value = res.data
    } else {
      syncFinalResult.value = { syncResult: {}, warnings: [res.msg || '同步失敗'], errors: [res.msg] }
    }
  } catch (e) {
    syncFinalResult.value = { syncResult: {}, warnings: [e?.message || '請求失敗'] }
  } finally {
    syncSyncing.value = false
  }
}

// ── Docker Compose Editor ─────────────────────────────────────────────────
const composeDialogVisible = ref(false)
const composeLoading       = ref(false)
const composeSaving        = ref(false)
const composeProjectRow    = ref(null)
const composeEnv           = ref('prod')
const composeContent       = ref('')

// YAML 驗證狀態：null=未檢查, true=通過, false=失敗
const yamlValid = ref(null)
const yamlError = ref('')

// 內容一旦改動就重置驗證狀態，要求重新檢查才能儲存
watch(composeContent, () => {
  yamlValid.value = null
  yamlError.value = ''
})

const handleYamlCheck = () => {
  const content = composeContent.value.trim()
  if (!content) {
    yamlValid.value = false
    yamlError.value = '內容不可為空'
    return
  }
  // 偵測 tab 縮排（YAML 不允許）
  const tabLine = content.split('\n').findIndex(l => /^\t/.test(l))
  if (tabLine !== -1) {
    yamlValid.value = false
    yamlError.value = `第 ${tabLine + 1} 行使用了 Tab 縮排，YAML 只允許空格`
    return
  }
  try {
    const parsed = yaml.load(content)
    if (typeof parsed !== 'object' || parsed === null) {
      yamlValid.value = false
      yamlError.value = 'YAML 根節點須為物件格式（key: value）'
      return
    }
    if (!parsed.services) {
      yamlValid.value = false
      yamlError.value = '缺少 services: 區塊，請確認這是有效的 docker-compose.yml'
      return
    }
    yamlValid.value = true
    yamlError.value = ''
  } catch (e) {
    yamlValid.value = false
    yamlError.value = e.message || 'YAML 格式錯誤'
  }
}

const handleComposeOpen = (row) => {
  composeProjectRow.value = row
  composeEnv.value = row.hasProd ? 'prod' : 'dev'
  composeContent.value = ''
  yamlValid.value = null
  yamlError.value = ''
  composeDialogVisible.value = true
  loadCompose()
}
const loadCompose = async () => {
  composeLoading.value = true
  composeContent.value = ''
  yamlValid.value = null
  yamlError.value = ''
  try {
    const res = await getDockerCompose(composeProjectRow.value.name, composeEnv.value)
    if (res.code === 1) {
      composeContent.value = res.data.exists === 'false' ? '' : res.data.content
    } else {
      ElMessage.error(res.msg || '讀取失敗')
    }
  } catch { ElMessage.error('讀取 docker-compose.yml 失敗') }
  finally { composeLoading.value = false }
}
const handleComposeSave = async () => {
  if (yamlValid.value !== true) { ElMessage.warning('請先點「檢查格式」確認 YAML 正確後再儲存'); return }
  composeSaving.value = true
  try {
    const res = await saveDockerCompose(
      composeProjectRow.value.name, composeEnv.value, composeContent.value)
    if (res.code === 1) { ElMessage.success('docker-compose.yml 已儲存') }
    else ElMessage.error(res.msg || '儲存失敗')
  } catch { ElMessage.error('儲存失敗') }
  finally { composeSaving.value = false }
}

// ── Config Drawer ──────────────────────────────────────────────────────────
const handleConfig = async (row) => {
  configProjectName.value = row.name
  configTab.value = 'prod'
  configDrawerVisible.value = true
  configLoading.value = true
  try {
    const res = await getProjectConfig(row.name)
    if (res.code === 1) {
      configForm.value = { prod: res.data.prod || {}, dev: res.data.dev || {}, shared: res.data.shared || {} }
    } else {
      ElMessage.error(res.msg || '讀取 config.sh 失敗')
    }
  } catch { ElMessage.error('讀取 config.sh 失敗') }
  finally { configLoading.value = false }
}
const handleConfigSave = async () => {
  configSaving.value = true
  try {
    const res = await saveProjectConfig(configProjectName.value, configForm.value)
    if (res.code === 1) { ElMessage.success('config.sh 已更新'); configDrawerVisible.value = false }
    else ElMessage.error(res.msg || '儲存失敗')
  } catch { ElMessage.error('儲存失敗') }
  finally { configSaving.value = false }
}

onMounted(fetchList)
</script>

<template>
  <div class="pm-wrap">

    <!-- 頁頭 -->
    <div class="pm-header">
      <h2 class="pm-title">專案管理</h2>
      <div class="pm-header-actions">
        <el-button :icon="Refresh" circle @click="fetchList" title="重新整理" />
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增專案</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border class="pm-table">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="expand-detail">
            <div class="expand-row">
              <span class="expand-label">系統名稱</span><code>{{ row.name }}</code>
              <span class="expand-label">Script 目錄</span><code>{{ row.scriptName || row.name }}</code>
              <span class="expand-label">Image Keyword</span><code>{{ row.imageKeyword || row.name }}</code>
              <span class="expand-label">GitLab ID</span><code>{{ row.gitlabProjectId || '—' }}</code>
            </div>
            <div class="expand-row">
              <span class="expand-label">正式 Env</span><code>{{ row.prodEnv || 'prod' }}</code>
              <span class="expand-label">測試 Env</span><code>{{ row.devEnv || 'dev' }}</code>
              <span class="expand-label">預設分支</span><code>{{ row.defaultBranch || 'master' }}</code>
              <span class="expand-label">Jenkins Job</span><code>{{ row.jenkinsJobName || '{type}-{env}' }}</code>
            </div>
            <div v-if="row.description" class="expand-row">
              <span class="expand-label">描述</span><span class="expand-desc">{{ row.description }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="displayName" label="顯示名稱" min-width="130" />
      <el-table-column prop="name"        label="系統名稱" min-width="150">
        <template #default="{ row }"><code class="code-chip">{{ row.name }}</code></template>
      </el-table-column>
      <el-table-column prop="category" label="分類" width="95" align="center">
        <template #default="{ row }">
          <el-tag :type="row.category === 'frontend' ? 'warning' : 'primary'" size="small">{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="環境" width="95" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.hasProd" type="danger"  size="small" style="margin:1px">正</el-tag>
          <el-tag v-if="row.hasDev"  type="success" size="small" style="margin:1px">測</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="60" align="center" />
      <el-table-column prop="isActive" label="狀態" width="72" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
            {{ row.isActive === 1 ? '啟用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="warning" size="small" :icon="Setting" @click="handleConfig(row)">配置</el-button>
          <el-dropdown @command="cmd => handleRowAction(cmd, row)" trigger="click">
            <el-button size="small" style="margin-left:6px">
              更多<el-icon class="el-icon--right" style="margin-left:2px"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit"    :icon="Edit">編輯</el-dropdown-item>
                <el-dropdown-item command="init"    :icon="Tools">初始化</el-dropdown-item>
                <el-dropdown-item command="compose" :icon="Connection">docker-compose</el-dropdown-item>
                <el-dropdown-item command="delete"  :icon="Delete" style="color:#f56c6c">刪除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!-- ── 新增/編輯 Dialog ─────────────────────────────────────────── -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="default">
        <el-tabs v-model="dialogTab" class="dialog-tabs">

          <!-- 基本資訊 -->
          <el-tab-pane label="基本資訊" name="basic">
            <div class="tab-form-body">
              <el-form-item label="顯示名稱" prop="displayName">
                <el-input v-model="form.displayName" placeholder="TKB TV" />
              </el-form-item>
              <el-form-item label="系統名稱" prop="name">
                <el-input v-model="form.name" placeholder="tkbtv（對應 project_versions）" :disabled="isEdit" />
                <div class="fhint" v-if="isEdit">系統名稱建立後不可修改</div>
              </el-form-item>
              <el-form-item label="分類" prop="category">
                <el-select v-model="form.category" style="width:100%">
                  <el-option label="backend"  value="backend" />
                  <el-option label="frontend" value="frontend" />
                </el-select>
              </el-form-item>
              <el-form-item label="部署環境">
                <el-checkbox v-model="form.hasProd" :true-label="1" :false-label="0">正式機</el-checkbox>
                <el-checkbox v-model="form.hasDev"  :true-label="1" :false-label="0" style="margin-left:16px">測試機</el-checkbox>
              </el-form-item>
              <el-form-item label="排序">
                <el-input-number v-model="form.sortOrder" :min="1" :max="999" style="width:100%" />
              </el-form-item>
              <el-form-item label="狀態">
                <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0"
                           active-text="啟用" inactive-text="停用" />
              </el-form-item>
              <el-form-item label="GitLab ID">
                <el-input-number v-model="form.gitlabProjectId" :min="1" style="width:100%"
                                 placeholder="GitLab Project ID" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="2" />
              </el-form-item>
            </div>
          </el-tab-pane>

          <!-- 進階設定 -->
          <el-tab-pane label="進階設定" name="advanced">
            <div class="tab-form-body">
              <el-form-item label="Script 目錄">
                <el-input v-model="form.scriptName" placeholder="留空則使用系統名稱" />
                <div class="fhint">tools/ 下的目錄名，與系統名稱不同時才需填</div>
              </el-form-item>
              <el-form-item label="Image Keyword">
                <el-input v-model="form.imageKeyword" placeholder="留空則使用系統名稱" />
                <div class="fhint">Docker grep 關鍵字，如 goapi</div>
              </el-form-item>
              <el-form-item label="正式 Env">
                <el-input v-model="form.prodEnv" placeholder="留空預設 prod，特殊如 admin" />
              </el-form-item>
              <el-form-item label="測試 Env">
                <el-input v-model="form.devEnv" placeholder="留空預設 dev" />
              </el-form-item>
              <el-form-item label="預設分支">
                <el-input v-model="form.defaultBranch" placeholder="留空預設 master" />
              </el-form-item>
              <el-divider content-position="left" style="font-size:12px;color:#64748b">SSH 路由覆蓋</el-divider>
              <el-form-item label="PROD SSH 目標">
                <el-select v-model="form.prodSshEnv" style="width:100%" clearable placeholder="留空 = prod 機（預設）">
                  <el-option label="dev（走測試機）" value="dev" />
                  <el-option label="prod（走正式機）" value="prod" />
                </el-select>
                <div class="fhint">PROD 操作實際 SSH 目標。form-service 等全部在 dev 機的專案設為 dev</div>
              </el-form-item>
              <el-form-item label="DEV SSH 目標">
                <el-select v-model="form.devSshEnv" style="width:100%" clearable placeholder="留空 = dev 機（預設）">
                  <el-option label="prod（走正式機）" value="prod" />
                  <el-option label="dev（走測試機）" value="dev" />
                </el-select>
                <div class="fhint">一般不需設定</div>
              </el-form-item>
            </div>
          </el-tab-pane>

          <!-- Jenkins 設定 -->
          <el-tab-pane label="Jenkins" name="jenkins">
            <div class="tab-form-body">
              <el-divider content-position="left" style="font-size:12px;color:#64748b;margin-top:4px">正式 PROD（Blue）</el-divider>
              <el-form-item label="Job 名稱">
                <el-input v-model="form.jenkinsJobNameProd" placeholder="如 frontend-prod / backend-prod" />
              </el-form-item>
              <el-form-item label="Token">
                <el-input v-model="form.jenkinsTokenProd" placeholder="" show-password />
              </el-form-item>
              <el-divider content-position="left" style="font-size:12px;color:#64748b">備援 Backup（Green）<span style="color:#475569;font-size:11px;margin-left:6px">前端才有</span></el-divider>
              <el-form-item label="Job 名稱">
                <el-input v-model="form.jenkinsJobNameBackup" placeholder="如 frontend-prod-backup，留空代表無備援 job" />
              </el-form-item>
              <el-form-item label="Token">
                <el-input v-model="form.jenkinsTokenBackup" placeholder="留空則同 PROD token" show-password />
              </el-form-item>
              <el-divider content-position="left" style="font-size:12px;color:#64748b">測試 DEV</el-divider>
              <el-form-item label="Job 名稱">
                <el-input v-model="form.jenkinsJobNameDev" placeholder="如 frontend-dev / backend-dev" />
              </el-form-item>
              <el-form-item label="Token">
                <el-input v-model="form.jenkinsTokenDev" placeholder="" show-password />
              </el-form-item>
              <el-divider content-position="left" style="font-size:12px;color:#64748b">共用</el-divider>
              <el-form-item label="Pipeline Job">
                <el-input v-model="form.jenkinsPipelineName" placeholder="如 backend-pipeline，留空代表不使用 pipeline" />
              </el-form-item>
            </div>
          </el-tab-pane>

        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ isEdit ? '儲存修改' : '新增' }}</el-button>
      </template>
    </el-dialog>

    <!-- ── Config Drawer ────────────────────────────────────────────── -->
    <el-drawer
      v-model="configDrawerVisible"
      :title="`⚙ ${configProjectName} — config.sh`"
      size="600px"
      destroy-on-close
    >
      <div v-loading="configLoading" style="height:100%">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
          <el-alert type="info" :closable="false" style="flex:1;font-size:13px;margin-bottom:0">
            修改後點「儲存寫入」，後端直接覆寫本機
            <code>/opt/vcs/tools/{{ configProjectName }}/config.sh</code>，原始注解保留。
          </el-alert>
          <el-button size="small" style="margin-left:10px;white-space:nowrap" @click="handleCopyOpen">
            從其他專案複製
          </el-button>
        </div>

        <el-tabs v-model="configTab" type="border-card">
          <el-tab-pane label="正式機 PROD" name="prod">
            <!-- 健康檢查列 -->
            <div class="health-bar">
              <el-icon><Connection /></el-icon>
              <span class="health-label">Health Check</span>
              <el-button size="small" :loading="healthChecking.prod" @click="handleHealthCheck('prod')">測試</el-button>
              <template v-if="healthResult.prod">
                <span :class="healthResult.prod.blue.ok ? 'health-ok' : 'health-err'">
                  Blue {{ healthResult.prod.blue.ok ? '✅' : '❌' }} {{ healthResult.prod.blue.msg }}
                </span>
                <span :class="healthResult.prod.green.ok ? 'health-ok' : 'health-err'">
                  Green {{ healthResult.prod.green.ok ? '✅' : '❌' }} {{ healthResult.prod.green.msg }}
                </span>
              </template>
            </div>
            <el-form label-width="140px" size="small" class="cfg-form">
              <template v-for="f in PROD_FIELDS" :key="f.key">
                <el-form-item :label="f.label">
                  <el-select v-if="f.type==='select'" v-model="configForm.prod[f.key]" style="width:100%">
                    <el-option v-for="o in f.options" :key="o" :label="o" :value="o" />
                  </el-select>
                  <el-input v-else v-model="configForm.prod[f.key]" :placeholder="f.hint" clearable />
                </el-form-item>
              </template>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="測試機 DEV" name="dev">
            <!-- 健康檢查列 -->
            <div class="health-bar">
              <el-icon><Connection /></el-icon>
              <span class="health-label">Health Check</span>
              <el-button size="small" :loading="healthChecking.dev" @click="handleHealthCheck('dev')">測試</el-button>
              <template v-if="healthResult.dev">
                <span :class="healthResult.dev.blue.ok ? 'health-ok' : 'health-err'">
                  Blue {{ healthResult.dev.blue.ok ? '✅' : '❌' }} {{ healthResult.dev.blue.msg }}
                </span>
              </template>
            </div>
            <el-form label-width="140px" size="small" class="cfg-form">
              <template v-for="f in DEV_FIELDS" :key="f.key">
                <el-form-item :label="f.label">
                  <el-select v-if="f.type==='select'" v-model="configForm.dev[f.key]" style="width:100%">
                    <el-option v-for="o in f.options" :key="o" :label="o" :value="o" />
                  </el-select>
                  <el-input v-else v-model="configForm.dev[f.key]" :placeholder="f.hint" clearable />
                </el-form-item>
              </template>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="共用" name="shared">
            <el-form label-width="140px" size="small" class="cfg-form">
              <template v-for="f in SHARED_FIELDS" :key="f.key">
                <el-form-item :label="f.label">
                  <el-select v-if="f.type==='select'" v-model="configForm.shared[f.key]" style="width:100%" clearable :placeholder="f.hint">
                    <el-option v-for="o in f.options.filter(x => x !== '')" :key="o" :label="o" :value="o" />
                  </el-select>
                  <template v-else>
                    <el-input v-model="configForm.shared[f.key]" :placeholder="f.hint" clearable />
                  </template>
                </el-form-item>
              </template>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="configDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSaving" @click="handleConfigSave">
          儲存寫入 config.sh
        </el-button>
        <el-button type="success" :icon="Connection" :loading="syncChecking" @click="handleSync" style="margin-left:6px">
          同步到遠端
        </el-button>
      </template>
    </el-drawer>

    <!-- ── 初始化確認 Dialog ─────────────────────────────────────────────── -->
    <el-dialog v-model="initConfirmVisible" title="⚠️ 確認初始化" width="460px" destroy-on-close>
      <div style="line-height:1.7;font-size:14px">
        <p>即將對 <strong>{{ initPendingRow?.displayName || initPendingRow?.name }}</strong> 執行初始化，此操作將在遠端機器上執行以下動作：</p>
        <ul style="padding-left:20px;margin:8px 0;color:#606266;font-size:13px">
          <li>在 <code>/opt/docker_image/</code> 建立專案目錄與 <code>.env</code></li>
          <li>生成或<strong style="color:#e6a23c">覆蓋</strong> <code>deploy.sh</code>、<code>rollback.sh</code>、<code>switch_traffic.sh</code></li>
          <li>DEV：<strong style="color:#e6a23c">docker-compose.yml</strong> 請另外添加</li>
        </ul>
        <el-alert type="warning" :closable="false" style="margin:12px 0 4px">
          初始化通常只在<strong>新增專案後執行一次</strong>。<br>
          對已上線專案重複執行將<strong>覆蓋現有腳本</strong>，請確認您了解影響。
        </el-alert>
        <el-checkbox v-model="initConfirmChecked" style="margin-top:14px;font-size:13px">
          我了解上述影響，確認對此專案執行初始化
        </el-checkbox>
      </div>
      <template #footer>
        <el-button @click="initConfirmVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!initConfirmChecked" @click="handleInit">
          確認初始化
        </el-button>
      </template>
    </el-dialog>

    <!-- ── 初始化結果 Dialog ───────────────────────────────────────────── -->
    <el-dialog v-model="initDialogVisible" :title="`初始化專案：${initProjectName}`" width="500px" destroy-on-close>
      <div v-if="initLoading" style="text-align:center;padding:30px 0">
        <el-icon class="is-loading" style="font-size:32px"><Refresh /></el-icon>
        <div style="margin-top:10px;color:#909399">初始化中，請稍候…</div>
      </div>
      <div v-else-if="initResult">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="config.sh">
            <pre class="init-output">{{ initResult.config }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="initResult.prod" label="PROD 機器">
            <pre class="init-output">{{ initResult.prod }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="initResult.dev" label="DEV 機器">
            <pre class="init-output">{{ initResult.dev }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="initDialogVisible = false">關閉</el-button>
      </template>
    </el-dialog>

    <!-- ── 從其他專案複製 config.sh Dialog ──────────────────────────── -->
    <el-dialog v-model="copyDialogVisible" title="從其他專案複製 config.sh" width="420px" destroy-on-close>
      <div style="font-size:13px;color:#606266;margin-bottom:14px">
        選擇來源專案後，其 <strong>PROD / DEV / 共用</strong> 欄位將全部複製到
        <strong>{{ configProjectName }}</strong>，並立即覆蓋目前表單內容。
      </div>
      <el-select
        v-model="copySourceProject"
        placeholder="選擇來源專案"
        style="width:100%"
        filterable
      >
        <el-option
          v-for="p in tableData.filter(p => p.name !== configProjectName)"
          :key="p.name"
          :label="`${p.displayName}（${p.name}）`"
          :value="p.name"
        />
      </el-select>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="copyCopying"
          :disabled="!copySourceProject"
          @click="handleCopyConfirm"
        >
          確認複製
        </el-button>
      </template>
    </el-dialog>

    <!-- ── 同步 config.sh 驗證/結果 Dialog ─────────────────────────── -->
    <el-dialog v-model="syncDialogVisible" title="同步 config.sh 至遠端" width="500px" destroy-on-close>
      <!-- 驗證中 -->
      <div v-if="syncChecking" style="text-align:center;padding:24px 0">
        <el-icon class="is-loading" style="font-size:28px"><Refresh /></el-icon>
        <div style="margin-top:8px;color:#909399">欄位驗證中…</div>
      </div>


      <!-- 驗證結果（尚未同步） -->
      <template v-else-if="syncCheckResult && !syncFinalResult">
        <el-alert v-if="syncCheckResult.errors.length" type="error" :closable="false" style="margin-bottom:10px">
          <div style="font-weight:600;margin-bottom:4px">❌ 必填欄位未填寫，請修正後再同步：</div>
          <ul style="margin:4px 0 0;padding-left:18px">
            <li v-for="e in syncCheckResult.errors" :key="e">{{ e }}</li>
          </ul>
        </el-alert>
        <el-alert v-if="syncCheckResult.warnings.length" type="warning" :closable="false" style="margin-bottom:10px">
          <div style="font-weight:600;margin-bottom:4px">⚠️ 注意（不影響同步）：</div>
          <ul style="margin:4px 0 0;padding-left:18px">
            <li v-for="w in syncCheckResult.warnings" :key="w">{{ w }}</li>
          </ul>
        </el-alert>
        <el-alert v-if="syncCheckResult.canSync && !syncCheckResult.errors.length && !syncCheckResult.warnings.length"
          type="success" :closable="false">所有欄位驗證通過，可同步至遠端。</el-alert>
      </template>

      <!-- 同步中 -->
      <div v-else-if="syncSyncing" style="text-align:center;padding:24px 0">
        <el-icon class="is-loading" style="font-size:28px"><Refresh /></el-icon>
        <div style="margin-top:8px;color:#909399">同步中，請稍候…</div>
      </div>

      <!-- 同步結果 -->
      <template v-else-if="syncFinalResult">
        <el-alert v-if="syncFinalResult.warnings && syncFinalResult.warnings.length"
          type="warning" :closable="false" style="margin-bottom:10px">
          <ul style="margin:0;padding-left:18px">
            <li v-for="w in syncFinalResult.warnings" :key="w">{{ w }}</li>
          </ul>
        </el-alert>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item v-for="(msg, env) in syncFinalResult.syncResult" :key="env" :label="env.toUpperCase()">
            <pre class="init-output">{{ msg }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="syncDialogVisible = false">{{ syncFinalResult ? '關閉' : '取消' }}</el-button>
        <el-button
          v-if="syncCheckResult && !syncFinalResult && syncCheckResult.canSync"
          type="primary" :loading="syncSyncing" @click="confirmSync">
          確認同步
        </el-button>
      </template>
    </el-dialog>

    <!-- ── Docker Compose 編輯器 Dialog ─────────────────────────────── -->
    <el-dialog
      v-model="composeDialogVisible"
      :title="`docker-compose.yml — ${composeProjectRow?.displayName || composeProjectRow?.name}`"
      width="760px"
      destroy-on-close
    >
      <div style="display:flex;align-items:center;gap:10px;margin-bottom:12px">
        <span style="font-size:13px;color:#606266;white-space:nowrap">環境：</span>
        <el-radio-group v-model="composeEnv" size="small" @change="loadCompose">
          <el-radio-button value="prod" :disabled="!composeProjectRow?.hasProd">正式機 PROD</el-radio-button>
          <el-radio-button value="dev"  :disabled="!composeProjectRow?.hasDev">測試機 DEV</el-radio-button>
        </el-radio-group>
        <el-button size="small" :icon="Refresh" :loading="composeLoading" @click="loadCompose" style="margin-left:auto">
          重新讀取
        </el-button>
      </div>
      <div v-loading="composeLoading">
        <el-alert v-if="!composeContent && !composeLoading" type="info" :closable="false" style="margin-bottom:8px">
          遠端尚無此 docker-compose.yml 檔案，儲存後將自動建立。</el-alert>
        <el-input
          v-model="composeContent"
          type="textarea"
          :rows="20"
          placeholder="請輸入 docker-compose.yml 內容…"
          style="font-family:monospace;font-size:13px"
        />
        <!-- YAML 驗證結果 -->
        <el-alert
          v-if="yamlValid === true"
          type="success"
          :closable="false"
          style="margin-top:8px"
        >✅ YAML 格式正確，可儲存至遠端。</el-alert>
        <el-alert
          v-if="yamlValid === false"
          type="error"
          :closable="false"
          style="margin-top:8px"
        >❌ {{ yamlError }}</el-alert>
      </div>
      <template #footer>
        <el-button @click="composeDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleYamlCheck" :disabled="composeLoading || !composeContent">
          檢查格式
        </el-button>
        <el-button
          type="primary"
          :loading="composeSaving"
          :disabled="yamlValid !== true"
          @click="handleComposeSave"
        >
          儲存至遠端
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
.init-output {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  font-family: monospace;
  color: inherit;
}
</style>

