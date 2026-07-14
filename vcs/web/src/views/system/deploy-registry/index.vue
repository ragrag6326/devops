<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, MagicStick } from '@element-plus/icons-vue'
import { getSshHostsRaw, saveSshHosts } from '@/api/project'
import DeployConfigEditor from './DeployConfigEditor.vue'
import { ENVS, SKELETON, deployJsonWarnings, isDeployJsonBroken } from './deployConfigSchema'
import {
  getDeployRegistry,
  getDeployRegistryMeta,
  saveDeployRegistryProject,
  deleteDeployRegistryProject,
  getGitlabRepoMap,
  saveGitlabRepo,
  deleteGitlabRepo,
  listDockerTemplates,
  getDockerTemplate,
  saveDockerTemplate,
  deleteDockerTemplate,
  getVmIpMap,
  saveVmIpMap,
  initProject,
} from '@/api/deployRegistry'

// ── 說明 ────────────────────────────────────────────────────────────────
// 管理 deploy.git（Jenkins buildWithParameters 用的部署腳本倉庫）內的設定檔：
//   1. config/project_deploy.json  — 部署對照表（純 JSON 編輯，避免表單 schema 跟 shell 不同步）
//   2. config/remote_gitlab_repo.json — TYPE/PROJECT_NAME → repo URL 對照
//   3. template/{ENV}/{NAME}_Dockerfile — Dockerfile 模板
//   4. config/vmIP.json — 各環境預設目標機器 IP（專案特例請在 envs.{ENV}.vmIP 設定）
// 所有存檔都會直接 commit + push 回 deploy.git，Jenkins 下次執行即套用。

const activeTab = ref('registry')

// 後端 Result: {code: 1 成功 / 0 失敗, msg, data}（失敗仍是 HTTP 200，需自行檢查）
const unwrap = (res, fallback = '操作失敗') => {
  if (res && res.code === 1) return res.data
  throw new Error(res?.msg || fallback)
}
const showErr = (e) => {
  if (e?.message) ElMessage.error(e.message)
}

// ═══════════════════ Tab 1: project_deploy.json ═══════════════════

const regLoading = ref(false)
const rawRegistry = ref({})

const registryTable = computed(() =>
  Object.keys(rawRegistry.value)
    .filter((k) => !k.startsWith('_'))
    .sort()
    .map((name) => {
      const p = rawRegistry.value[name] || {}
      const envs = p.envs && typeof p.envs === 'object'
        ? Object.entries(p.envs).map(([env, c]) => `${env}(${c?.buildType || '?'})`).join('、')
        : '⚠ 缺 envs'
      return {
        name,
        envs,
        containerName: p.devMachine?.containerName || '—',
        hasNpmBuild: p.npmBuild ? (p.npmBuild.envTemplate ? '環境變數替換' : '檔名修正') : '—',
        vmIpOverride: p.envs && typeof p.envs === 'object'
          ? Object.entries(p.envs).filter(([, c]) => c?.vmIP).map(([env, c]) => `${env}→${c.vmIP}`).join('、') || '—'
          : '—',
      }
    })
)

const fetchRegistry = async () => {
  regLoading.value = true
  try {
    rawRegistry.value = unwrap(await getDeployRegistry(), '讀取部署對照表失敗') || {}
  } catch (e) {
    showErr(e)
  } finally {
    regLoading.value = false
  }
}

// ── 純 JSON 編輯 dialog ──
const editVisible = ref(false)
const editIsNew = ref(false)
const editName = ref('')
const editJson = ref('')
const editLastCommitId = ref('')
const editSaving = ref(false)

// JSON 連 object 都不是時禁止儲存（欄位警告由 DeployConfigEditor 顯示）
const editJsonBroken = computed(() => isDeployJsonBroken(editJson.value))

// 欄位說明抽屜（DeployConfigEditor 以 show-docs 事件開啟）
const docsVisible = ref(false)

const openRegistryEdit = async (row) => {
  editIsNew.value = !row
  editName.value = row ? row.name : ''
  editJson.value = JSON.stringify(row ? rawRegistry.value[row.name] : SKELETON, null, 2)
  editLastCommitId.value = ''
  editVisible.value = true
  try {
    // 樂觀鎖：記住編輯當下的檔案版本，存檔時帶回
    editLastCommitId.value = unwrap(await getDeployRegistryMeta())?.lastCommitId || ''
  } catch {
    /* 拿不到就退回無鎖模式 */
  }
}

const saveRegistryEdit = async () => {
  const name = editName.value?.trim()
  if (!name) {
    ElMessage.warning('請輸入 PROJECT_NAME')
    return
  }
  let payload
  try {
    payload = JSON.parse(editJson.value)
  } catch (e) {
    ElMessage.error(`JSON 格式錯誤，無法儲存：${e.message}`)
    return
  }
  editSaving.value = true
  try {
    unwrap(await saveDeployRegistryProject(
      name, payload, `[deploy-registry] 前端更新 ${name} 設定`, editLastCommitId.value))
    ElMessage.success('已儲存並 push 回 deploy.git')
    editVisible.value = false
    await fetchRegistry()
  } catch (e) {
    showErr(e)
    if (/has changed|400/.test(e?.message || '')) {
      ElMessage.warning('內容可能已被其他人更新，請重新整理後再編輯')
    }
  } finally {
    editSaving.value = false
  }
}

// ── 移除 dialog（可勾選一併移除模板 / repo 對照，單一 commit）──
const delVisible = ref(false)
const delName = ref('')
const delRemoveTemplates = ref(true)
const delRemoveRepo = ref(true)
const delRunning = ref(false)

const openRegistryDelete = (row) => {
  delName.value = row.name
  delRemoveTemplates.value = true
  delRemoveRepo.value = true
  delVisible.value = true
}

const confirmRegistryDelete = async () => {
  delRunning.value = true
  try {
    unwrap(await deleteDeployRegistryProject(delName.value, {
      removeTemplates: delRemoveTemplates.value,
      removeRepo: delRemoveRepo.value,
      commitMessage: `[deploy-registry] 前端移除 ${delName.value} 設定`,
    }))
    ElMessage.success('已移除並 push 回 deploy.git')
    delVisible.value = false
    await Promise.all([fetchRegistry(), fetchTemplates(), fetchRepo()])
  } catch (e) {
    showErr(e)
  } finally {
    delRunning.value = false
  }
}

// ═══════════════════ Tab 2: remote_gitlab_repo.json ═══════════════════

const repoLoading = ref(false)
const rawRepo = ref({})

const repoTable = computed(() => {
  const rows = []
  ;['backend', 'frontend'].forEach((type) => {
    const arr = rawRepo.value?.[type]
    if (!Array.isArray(arr)) return
    arr.forEach((holder) => {
      Object.entries(holder || {}).forEach(([name, v]) => {
        rows.push({ type, name, url: v?.repository_url || '' })
      })
    })
  })
  return rows.sort((a, b) => a.type.localeCompare(b.type) || a.name.localeCompare(b.name))
})

const fetchRepo = async () => {
  repoLoading.value = true
  try {
    rawRepo.value = unwrap(await getGitlabRepoMap(), '讀取 repo 對照失敗') || {}
  } catch (e) {
    showErr(e)
  } finally {
    repoLoading.value = false
  }
}

const repoDialogVisible = ref(false)
const repoIsEdit = ref(false)
const repoSaving = ref(false)
const repoForm = ref({ type: 'backend', name: '', url: '' })

const openRepoDialog = (row) => {
  repoIsEdit.value = !!row
  repoForm.value = row
    ? { type: row.type, name: row.name, url: row.url }
    : { type: 'backend', name: '', url: '' }
  repoDialogVisible.value = true
}

const saveRepo = async () => {
  const { type, name, url } = repoForm.value
  if (!name?.trim() || !url?.trim()) {
    ElMessage.warning('請填寫 PROJECT_NAME 與 repository_url')
    return
  }
  if (!/^ssh:\/\/git@.+\.git$/.test(url.trim())) {
    ElMessage.warning('repository_url 格式應為 ssh://git@.../xxx.git')
    return
  }
  repoSaving.value = true
  try {
    unwrap(await saveGitlabRepo(type, name.trim(), url.trim(),
      `[deploy-registry] 前端更新 repo 對照 ${type}/${name.trim()}`))
    ElMessage.success('已儲存並 push 回 deploy.git')
    repoDialogVisible.value = false
    await fetchRepo()
  } catch (e) {
    showErr(e)
  } finally {
    repoSaving.value = false
  }
}

const removeRepoRow = (row) => {
  ElMessageBox.confirm(
    `確定移除 ${row.type}/${row.name} 的 repo 對照嗎？移除後 Jenkins 將無法 clone 該專案。`,
    '確認移除', { type: 'warning' })
    .then(async () => {
      try {
        unwrap(await deleteGitlabRepo(row.type, row.name,
          `[deploy-registry] 前端移除 repo 對照 ${row.type}/${row.name}`))
        ElMessage.success('已移除並 push 回 deploy.git')
        await fetchRepo()
      } catch (e) {
        showErr(e)
      }
    })
    .catch(() => {})
}

// ═══════════════════ Tab 3: Dockerfile 模板 ═══════════════════

const tplLoading = ref(false)
const templates = ref([])

const fetchTemplates = async () => {
  tplLoading.value = true
  try {
    templates.value = unwrap(await listDockerTemplates(), '讀取模板清單失敗') || []
  } catch (e) {
    showErr(e)
  } finally {
    tplLoading.value = false
  }
}

const tplDialogVisible = ref(false)
const tplIsEdit = ref(false)
const tplSaving = ref(false)
const tplContentLoading = ref(false)
const tplForm = ref({ env: 'dev', projectName: '', content: '' })
const tplCopyFrom = ref('')

const openTplDialog = async (row) => {
  tplIsEdit.value = !!row
  tplCopyFrom.value = ''
  tplForm.value = row
    ? { env: row.env, projectName: row.projectName, content: '' }
    : { env: 'dev', projectName: '', content: '' }
  tplDialogVisible.value = true
  if (row) {
    tplContentLoading.value = true
    try {
      tplForm.value.content = unwrap(await getDockerTemplate(row.env, row.projectName)) || ''
    } catch (e) {
      showErr(e)
    } finally {
      tplContentLoading.value = false
    }
  }
}

const applyTplCopyFrom = async () => {
  if (!tplCopyFrom.value) return
  const [env, projectName] = tplCopyFrom.value.split('|')
  tplContentLoading.value = true
  try {
    tplForm.value.content = unwrap(await getDockerTemplate(env, projectName)) || ''
    ElMessage.success(`已帶入 ${env}/${projectName} 的模板內容`)
  } catch (e) {
    showErr(e)
  } finally {
    tplContentLoading.value = false
  }
}

const saveTpl = async () => {
  const { env, projectName, content } = tplForm.value
  if (!projectName?.trim()) {
    ElMessage.warning('請輸入 PROJECT_NAME')
    return
  }
  if (!content?.trim()) {
    ElMessage.warning('Dockerfile 內容不可為空')
    return
  }
  tplSaving.value = true
  try {
    unwrap(await saveDockerTemplate(env, projectName.trim(), content,
      `[deploy-registry] 前端更新 Dockerfile 模板 template/${env}/${projectName.trim()}_Dockerfile`))
    ElMessage.success('已儲存並 push 回 deploy.git')
    tplDialogVisible.value = false
    await fetchTemplates()
  } catch (e) {
    showErr(e)
  } finally {
    tplSaving.value = false
  }
}

const removeTpl = (row) => {
  ElMessageBox.confirm(
    `確定移除 ${row.path} 嗎？該環境的 build 將因找不到 Dockerfile 模板而失敗。`,
    '確認移除', { type: 'warning' })
    .then(async () => {
      try {
        unwrap(await deleteDockerTemplate(row.env, row.projectName,
          `[deploy-registry] 前端移除 Dockerfile 模板 ${row.path}`))
        ElMessage.success('已移除並 push 回 deploy.git')
        await fetchTemplates()
      } catch (e) {
        showErr(e)
      }
    })
    .catch(() => {})
}

// ═══════════════════ Tab 4: vmIP.json ═══════════════════

const vmLoading = ref(false)
const vmSaving = ref(false)
const vmEntries = ref([]) // [{env, ip}]
// 同步 VCS 主機的 tools/common/ssh_hosts.json（sshToolUtil.sh 遠端操作查表用）
const vmSyncSshHosts = ref(true)

const fetchVmIp = async () => {
  vmLoading.value = true
  try {
    const data = unwrap(await getVmIpMap(), '讀取 vmIP.json 失敗') || {}
    vmEntries.value = Object.entries(data).map(([env, ip]) => ({ env, ip }))
  } catch (e) {
    showErr(e)
  } finally {
    vmLoading.value = false
  }
}

const addVmRow = () => vmEntries.value.push({ env: '', ip: '' })
const removeVmRow = (i) => vmEntries.value.splice(i, 1)

const saveVmIp = async () => {
  const data = {}
  for (const { env, ip } of vmEntries.value) {
    if (!env?.trim() || !ip?.trim()) {
      ElMessage.warning('環境名稱與 IP 皆不可為空')
      return
    }
    if (data[env.trim()]) {
      ElMessage.warning(`環境 ${env.trim()} 重複`)
      return
    }
    data[env.trim()] = ip.trim()
  }
  if (!Object.keys(data).length) {
    ElMessage.warning('至少需要一筆環境設定')
    return
  }
  vmSaving.value = true
  try {
    unwrap(await saveVmIpMap(data, '[deploy-registry] 前端更新 vmIP.json'))
    ElMessage.success('vmIP.json 已儲存並 push 回 deploy.git')

    // 同步 ssh_hosts.json：既有環境只更新 host（保留 user/key/label），
    // 新環境依慣例補上預設 user 與金鑰路徑（金鑰檔需自行放置）
    if (vmSyncSshHosts.value) {
      try {
        const raw = unwrap(await getSshHostsRaw()) || {}
        const newEnvs = []
        Object.entries(data).forEach(([env, ip]) => {
          if (raw[env] && typeof raw[env] === 'object') {
            raw[env].host = ip
          } else {
            raw[env] = { host: ip, user: 'tkb0001662', key: `/opt/vcs/tools/key/${env}.pem`, label: env }
            newEnvs.push(env)
          }
        })
        unwrap(await saveSshHosts(raw))
        if (newEnvs.length) {
          ElMessage.warning(`ssh_hosts.json 已同步，新環境 ${newEnvs.join('、')} 請記得把金鑰放到 /opt/vcs/tools/key/`)
        } else {
          ElMessage.success('ssh_hosts.json 已同步')
        }
      } catch (e2) {
        ElMessage.warning(`vmIP.json 已存，但 ssh_hosts.json 同步失敗：${e2?.message || ''}`)
      }
    }
    await fetchVmIp()
  } catch (e) {
    showErr(e)
  } finally {
    vmSaving.value = false
  }
}

// ═══════════════════ 新增專案精靈（單一 commit）═══════════════════

const wizVisible = ref(false)
const wizStep = ref(0)
const wizSubmitting = ref(false)
const wizForm = ref(null)
const wizCopyFromProject = ref('')

const emptyWizard = () => ({
  projectName: '',
  type: 'backend',
  repositoryUrl: '',
  deployJson: JSON.stringify(SKELETON, null, 2),
  dockerfiles: Object.fromEntries(ENVS.map((e) => [e, { enabled: false, content: '', copyFrom: '' }])),
})

const openWizard = () => {
  wizForm.value = emptyWizard()
  wizCopyFromProject.value = ''
  wizStep.value = 0
  wizVisible.value = true
}

// 驗證邏輯與 DeployConfigEditor 顯示的警告共用同一份（deployConfigSchema.js）
const wizJsonWarnings = computed(() =>
  wizForm.value ? deployJsonWarnings(wizForm.value.deployJson) : [])

const applyWizCopyProject = () => {
  if (!wizCopyFromProject.value) return
  const src = rawRegistry.value[wizCopyFromProject.value]
  if (src) {
    wizForm.value.deployJson = JSON.stringify(src, null, 2)
    ElMessage.success(`已帶入 ${wizCopyFromProject.value} 的設定作為範本`)
  }
}

const applyWizTplCopy = async (env) => {
  const sel = wizForm.value.dockerfiles[env].copyFrom
  if (!sel) return
  const [srcEnv, srcName] = sel.split('|')
  try {
    wizForm.value.dockerfiles[env].content = unwrap(await getDockerTemplate(srcEnv, srcName)) || ''
    ElMessage.success(`已帶入 ${srcEnv}/${srcName} 的模板內容`)
  } catch (e) {
    showErr(e)
  }
}

const wizNext = () => {
  const f = wizForm.value
  if (wizStep.value === 0) {
    if (!f.projectName?.trim()) return ElMessage.warning('請輸入 PROJECT_NAME')
    if (!/^[A-Za-z0-9._-]+$/.test(f.projectName.trim())) return ElMessage.warning('PROJECT_NAME 僅允許英數字與 . _ -')
    if (rawRegistry.value[f.projectName.trim()]) return ElMessage.warning('專案已存在於對照表，請改用編輯功能')
    if (!/^ssh:\/\/git@.+\.git$/.test(f.repositoryUrl?.trim() || '')) return ElMessage.warning('repository_url 格式應為 ssh://git@.../xxx.git')
  }
  if (wizStep.value === 1) {
    try {
      JSON.parse(f.deployJson)
    } catch (e) {
      return ElMessage.warning(`JSON 格式錯誤：${e.message}`)
    }
    if (wizJsonWarnings.value.length) return ElMessage.warning('請先修正設定警告再繼續')
    // 依 envs 自動勾選對應環境的 Dockerfile
    const envs = Object.keys(JSON.parse(f.deployJson).envs || {})
    envs.forEach((e) => {
      if (ENVS.includes(e)) f.dockerfiles[e].enabled = true
    })
  }
  wizStep.value++
}

const wizSubmit = async () => {
  const f = wizForm.value
  let deployConfig
  try {
    deployConfig = JSON.parse(f.deployJson)
  } catch (e) {
    return ElMessage.warning(`JSON 格式錯誤：${e.message}`)
  }
  const dockerfiles = {}
  for (const env of ENVS) {
    const d = f.dockerfiles[env]
    if (d.enabled) {
      if (!d.content?.trim()) return ElMessage.warning(`環境 ${env} 已勾選但 Dockerfile 內容為空`)
      dockerfiles[env] = d.content
    }
  }
  if (!Object.keys(dockerfiles).length) {
    try {
      await ElMessageBox.confirm(
        '未提供任何 Dockerfile 模板，BuildUtil.sh 建置時會因找不到 template/{ENV}/{NAME}_Dockerfile 而失敗。確定繼續嗎？',
        '確認', { type: 'warning' })
    } catch {
      return
    }
  }
  wizSubmitting.value = true
  try {
    const result = unwrap(await initProject({
      projectName: f.projectName.trim(),
      type: f.type,
      repositoryUrl: f.repositoryUrl.trim(),
      deployConfig,
      dockerfiles,
    }))
    ElMessage.success(`已以單一 commit 寫入 ${result?.committedFiles?.length ?? ''} 個檔案，Jenkins 下次 buildWithParameters 即生效`)
    wizVisible.value = false
    await Promise.all([fetchRegistry(), fetchRepo(), fetchTemplates()])
  } catch (e) {
    showErr(e)
  } finally {
    wizSubmitting.value = false
  }
}

// ═══════════════════ init ═══════════════════

const refreshAll = () => Promise.all([fetchRegistry(), fetchRepo(), fetchTemplates(), fetchVmIp()])
onMounted(refreshAll)
</script>

<template>
  <div class="deploy-registry-page">
    <div class="page-header">
      <div>
        <h2>部署設定檔管理（deploy.git）</h2>
        <p class="page-desc">
          管理 <code>config/project_deploy.json</code>（純 JSON 編輯）、
          <code>config/remote_gitlab_repo.json</code>、
          <code>template/{ENV}/{NAME}_Dockerfile</code> 與 <code>config/vmIP.json</code>。
          存檔會直接 commit + push 回 deploy.git，Jenkins 下次執行即套用。
        </p>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="refreshAll">重新整理</el-button>
        <el-button type="primary" :icon="MagicStick" @click="openWizard">新增專案精靈</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- ═══ Tab 1: 部署對照表 ═══ -->
      <el-tab-pane label="部署對照表" name="registry">
        <div class="tab-toolbar">
          <el-button link type="primary" :icon="Plus" @click="openRegistryEdit(null)">
            新增單筆設定（進階，僅寫 project_deploy.json）
          </el-button>
        </div>
        <el-table :data="registryTable" v-loading="regLoading" border>
          <el-table-column prop="name" label="PROJECT_NAME" min-width="150" fixed="left" />
          <el-table-column prop="envs" label="環境 (buildType)" min-width="220" show-overflow-tooltip />
          <el-table-column prop="containerName" label="測試機 Container" min-width="160" />
          <el-table-column prop="hasNpmBuild" label="npm build 設定" width="130" />
          <el-table-column prop="vmIpOverride" label="vmIP 特例" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click="openRegistryEdit(row)">編輯</el-button>
              <el-button link type="danger" :icon="Delete" @click="openRegistryDelete(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ═══ Tab 2: GitLab Repo 對照 ═══ -->
      <el-tab-pane label="GitLab Repo 對照" name="repo">
        <div class="tab-toolbar">
          <el-button link type="primary" :icon="Plus" @click="openRepoDialog(null)">新增對照</el-button>
        </div>
        <el-table :data="repoTable" v-loading="repoLoading" border>
          <el-table-column prop="type" label="TYPE" width="110" />
          <el-table-column prop="name" label="PROJECT_NAME" min-width="170" />
          <el-table-column prop="url" label="repository_url" min-width="360" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click="openRepoDialog(row)">編輯</el-button>
              <el-button link type="danger" :icon="Delete" @click="removeRepoRow(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ═══ Tab 3: Dockerfile 模板 ═══ -->
      <el-tab-pane label="Dockerfile 模板" name="templates">
        <div class="tab-toolbar">
          <el-button link type="primary" :icon="Plus" @click="openTplDialog(null)">新增模板</el-button>
        </div>
        <el-table :data="templates" v-loading="tplLoading" border>
          <el-table-column prop="env" label="PROJECT_ENV" width="130" />
          <el-table-column prop="projectName" label="PROJECT_NAME" min-width="170" />
          <el-table-column prop="path" label="路徑" min-width="300" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click="openTplDialog(row)">編輯</el-button>
              <el-button link type="danger" :icon="Delete" @click="removeTpl(row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ═══ Tab 4: vmIP ═══ -->
      <el-tab-pane label="機器 IP（vmIP.json）" name="vmip">
        <p class="page-desc">
          各環境的「預設」目標機器 IP。單一專案要指到不同機器時，
          請在部署對照表該專案的 <code>envs.{ENV}.vmIP</code> 設定（getVMIP.sh 會優先讀取）。
        </p>
        <div v-loading="vmLoading" class="vmip-editor">
          <div v-for="(row, i) in vmEntries" :key="i" class="dynamic-row">
            <el-input v-model="row.env" placeholder="環境（PROJECT_ENV），如 prod" style="width: 220px" />
            <el-input v-model="row.ip" placeholder="IP，如 132.145.125.250" style="width: 260px" />
            <el-button link type="danger" :icon="Delete" @click="removeVmRow(i)" />
          </div>
          <el-checkbox v-model="vmSyncSshHosts" style="margin-top: 8px">
            同步至 VCS 主機的 ssh_hosts.json（遠端操作機器表；新環境會補預設 user 與金鑰路徑，金鑰檔需自行放置）
          </el-checkbox>
          <div class="vmip-actions">
            <el-button link type="primary" :icon="Plus" @click="addVmRow">新增環境</el-button>
            <el-button type="primary" :loading="vmSaving" @click="saveVmIp">儲存並 push 回 deploy.git</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ═══ 對照表 純 JSON 編輯 dialog ═══ -->
    <el-dialog
      v-model="editVisible"
      :title="editIsNew ? '新增單筆設定' : `編輯專案設定：${editName}`"
      width="760px"
      top="4vh"
    >
      <el-form label-width="130px">
        <el-form-item label="PROJECT_NAME" required>
          <el-input v-model="editName" :disabled="!editIsNew" placeholder="需與 Jenkins 參數值一致" />
        </el-form-item>
      </el-form>

      <DeployConfigEditor v-if="editVisible" v-model="editJson" @show-docs="docsVisible = true" />

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" :disabled="editJsonBroken" @click="saveRegistryEdit">
          儲存並 push 回 deploy.git
        </el-button>
      </template>
    </el-dialog>

    <!-- ═══ 對照表 移除 dialog ═══ -->
    <el-dialog v-model="delVisible" :title="`移除專案：${delName}`" width="520px">
      <p>將從 <code>project_deploy.json</code> 移除「{{ delName }}」，並直接 commit + push 回 deploy.git。</p>
      <el-checkbox v-model="delRemoveTemplates">一併移除該專案所有環境的 Dockerfile 模板</el-checkbox>
      <br />
      <el-checkbox v-model="delRemoveRepo">一併移除 remote_gitlab_repo.json 內的 repo 對照</el-checkbox>
      <template #footer>
        <el-button @click="delVisible = false">取消</el-button>
        <el-button type="danger" :loading="delRunning" @click="confirmRegistryDelete">確認移除（單一 commit）</el-button>
      </template>
    </el-dialog>

    <!-- ═══ Repo 對照 dialog ═══ -->
    <el-dialog
      v-model="repoDialogVisible"
      :title="repoIsEdit ? `編輯 repo 對照：${repoForm.type}/${repoForm.name}` : '新增 repo 對照'"
      width="600px"
    >
      <el-form label-width="150px">
        <el-form-item label="TYPE" required>
          <el-radio-group v-model="repoForm.type" :disabled="repoIsEdit">
            <el-radio value="backend">backend</el-radio>
            <el-radio value="frontend">frontend</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="PROJECT_NAME" required>
          <el-input v-model="repoForm.name" :disabled="repoIsEdit" placeholder="需與 Jenkins 參數值一致" />
        </el-form-item>
        <el-form-item label="repository_url" required>
          <el-input v-model="repoForm.url" placeholder="ssh://git@192.168.1.35:2224/tkb/xxx.git" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repoDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="repoSaving" @click="saveRepo">儲存並 push 回 deploy.git</el-button>
      </template>
    </el-dialog>

    <!-- ═══ Dockerfile 模板 dialog ═══ -->
    <el-dialog
      v-model="tplDialogVisible"
      :title="tplIsEdit ? `編輯模板：template/${tplForm.env}/${tplForm.projectName}_Dockerfile` : '新增 Dockerfile 模板'"
      width="760px"
      top="4vh"
    >
      <el-form label-width="150px">
        <el-form-item label="PROJECT_ENV" required>
          <el-select v-model="tplForm.env" :disabled="tplIsEdit" style="width: 200px">
            <el-option v-for="e in ENVS" :key="e" :value="e" :label="e" />
          </el-select>
        </el-form-item>
        <el-form-item label="PROJECT_NAME" required>
          <el-input v-model="tplForm.projectName" :disabled="tplIsEdit" placeholder="需與 Jenkins 參數值一致" style="width: 320px" />
        </el-form-item>
        <el-form-item label="從既有模板複製">
          <el-select v-model="tplCopyFrom" placeholder="選擇來源模板" clearable style="width: 320px" @change="applyTplCopyFrom">
            <el-option
              v-for="t in templates"
              :key="t.path"
              :value="`${t.env}|${t.projectName}`"
              :label="`${t.env} / ${t.projectName}`"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-input
        v-model="tplForm.content"
        v-loading="tplContentLoading"
        type="textarea"
        :rows="16"
        class="json-editor"
        spellcheck="false"
        placeholder="FROM ..."
      />
      <template #footer>
        <el-button @click="tplDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tplSaving" @click="saveTpl">儲存並 push 回 deploy.git</el-button>
      </template>
    </el-dialog>

    <!-- ═══ 新增專案精靈 ═══ -->
    <el-dialog v-model="wizVisible" title="新增專案精靈（單一 commit 原子寫入）" width="820px" top="4vh">
      <template v-if="wizForm">
        <el-steps :active="wizStep" finish-status="success" simple class="wiz-steps">
          <el-step title="基本資料" />
          <el-step title="部署設定 JSON" />
          <el-step title="Dockerfile 模板" />
        </el-steps>

        <!-- Step 1 -->
        <div v-show="wizStep === 0">
          <el-form label-width="150px">
            <el-form-item label="PROJECT_NAME" required>
              <el-input v-model="wizForm.projectName" placeholder="即 Jenkins 帶參數建置的 PROJECT_NAME" />
            </el-form-item>
            <el-form-item label="TYPE" required>
              <el-radio-group v-model="wizForm.type">
                <el-radio value="backend">backend</el-radio>
                <el-radio value="frontend">frontend</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="repository_url" required>
              <el-input v-model="wizForm.repositoryUrl" placeholder="ssh://git@192.168.1.35:2224/tkb/xxx.git" />
            </el-form-item>
          </el-form>
        </div>

        <!-- Step 2 -->
        <div v-show="wizStep === 1">
          <el-form label-width="150px">
            <el-form-item label="從既有專案複製">
              <el-select v-model="wizCopyFromProject" placeholder="選擇範本專案" clearable style="width: 320px" @change="applyWizCopyProject">
                <el-option v-for="r in registryTable" :key="r.name" :value="r.name" :label="r.name" />
              </el-select>
            </el-form-item>
          </el-form>
          <DeployConfigEditor v-if="wizStep === 1" v-model="wizForm.deployJson" @show-docs="docsVisible = true" />
        </div>

        <!-- Step 3 -->
        <div v-show="wizStep === 2">
          <p class="page-desc">
            依 envs 勾選需要的環境，各環境一份 <code>template/{ENV}/{{ wizForm.projectName || '{NAME}' }}_Dockerfile</code>。
          </p>
          <div v-for="env in ENVS" :key="env" class="wiz-env-block">
            <div class="wiz-env-head">
              <el-checkbox v-model="wizForm.dockerfiles[env].enabled">{{ env }}</el-checkbox>
              <el-select
                v-if="wizForm.dockerfiles[env].enabled"
                v-model="wizForm.dockerfiles[env].copyFrom"
                placeholder="從既有模板複製"
                clearable
                size="small"
                style="width: 260px"
                @change="applyWizTplCopy(env)"
              >
                <el-option
                  v-for="t in templates"
                  :key="t.path"
                  :value="`${t.env}|${t.projectName}`"
                  :label="`${t.env} / ${t.projectName}`"
                />
              </el-select>
            </div>
            <el-input
              v-if="wizForm.dockerfiles[env].enabled"
              v-model="wizForm.dockerfiles[env].content"
              type="textarea"
              :rows="8"
              class="json-editor"
              spellcheck="false"
              placeholder="FROM ..."
            />
          </div>
        </div>
      </template>

      <template #footer>
        <el-button v-if="wizStep > 0" @click="wizStep--">上一步</el-button>
        <el-button @click="wizVisible = false">取消</el-button>
        <el-button v-if="wizStep < 2" type="primary" @click="wizNext">下一步</el-button>
        <el-button v-else type="primary" :loading="wizSubmitting" @click="wizSubmit">
          建立專案（單一 commit push 回 deploy.git）
        </el-button>
      </template>
    </el-dialog>

    <!-- ═══ 欄位說明抽屜 ═══ -->
    <el-drawer v-model="docsVisible" title="project_deploy.json 欄位說明" size="620px">
      <div class="docs">
        <p>
          這份 JSON 是 Jenkins 部署腳本（deploy.git）的「專案對照表」。
          Jenkins 帶參數建置時，shell 會用 <code>jq</code> 依 PROJECT_NAME / PROJECT_ENV
          查這裡的值，<b>不用改任何 shell 就能新增或調整專案</b>。
          不確定怎麼寫時：用「從既有專案複製」帶入相似專案，再用「插入設定區塊」補缺的部分。
        </p>

        <h4>envs.{環境}（必填，至少一個環境）</h4>
        <p class="docs-src">讀取者：core/deploy/genericDeploy.sh（決定怎麼 build、用誰 SSH）</p>
        <ul>
          <li><code>buildType</code>（必填）：打包方式。<code>maven</code>=Java jar、<code>war</code>=舊 Tomcat war、<code>npm</code>=前端 Nuxt</li>
          <li><code>sshUser</code>（必填）：登入目標機器的 SSH 帳號（一般機器 <code>tkb0001662</code>、local 機 <code>tkbuser</code>）</li>
          <li><code>javaVersion</code>：maven/war 用，預設 <code>17</code></li>
          <li><code>npmVersion</code>：npm 用（nvm use 的版本），預設 <code>18</code></li>
          <li><code>templateOverride</code>（選填）：部署流程樣板。不填=依環境用 <code>{環境}DeployTemplate.sh</code>。
            特殊情況：admin 環境要走正式流程填 <code>prodDeployTemplate.sh</code>；
            前端藍綠部署填 <code>frontend-prodDeployTemplate.sh</code></li>
          <li><code>vmIP</code>（選填）：這個專案這個環境要部署到的機器 IP。
            不填=用「機器 IP」分頁（vmIP.json）的環境預設值</li>
        </ul>
        <p class="docs-note">環境名稱對應 Jenkins 的 PROJECT_ENV：<code>dev</code>=測試機、<code>local</code>=區網機、
          <code>prod</code>=正式機、<code>admin</code>=配置走 prod 流程但跑在測試機（form-service 型）</p>

        <h4>devMachine（測試機 dev 部署用，走 dev 流程的專案建議填）</h4>
        <p class="docs-src">讀取者：core/deploy/SSHUtil.sh（測試機改版本號與重啟 container）</p>
        <ul>
          <li><code>containerName</code>：docker compose 重啟的服務名稱，不填=PROJECT_NAME</li>
          <li><code>useProjectSubdir</code>：<code>true</code>=部署路徑為 /opt/docker_image/{PROJECT_NAME}（form-service 系列）；
            <code>false</code>=直接在 /opt/docker_image</li>
          <li><code>versionFile</code>：版本號寫在哪。<code>docker-compose</code>=改 docker-compose.yml 的 image tag；
            <code>env</code>=改 .env 的指定 KEY</li>
          <li><code>versionEnvKey</code>：versionFile=env 時要改的 KEY，預設 <code>TEST_VERSION</code></li>
          <li><code>cleanupPaths</code>：重啟前要清掉的路徑陣列（相對 /opt/docker_image/{PROJECT_NAME}/），如 <code>["webapp/*"]</code></li>
        </ul>

        <h4>npmBuild（僅 buildType=npm 的專案，選填）</h4>
        <p class="docs-src">讀取者：core/deploy/BuildUtil.sh（npm build 前置處理）</p>
        <ul>
          <li><code>filenameFixes</code>：檔案「內容」字串修正清單 <code>[{dir, from, to}]</code>（go_nuxt 型）</li>
          <li><code>renameFiles</code>：build 前檔案「改名」清單 <code>[{from, to}]</code>，檔案不存在自動略過</li>
          <li><code>envTemplate</code> + <code>backendApiPlaceholder</code> + <code>nodeTypeReplacements</code>：
            build 時複製 env 範本，依 Jenkins 的 NODE_TYPE（prod/backup/test）把預設 API URL 換成對應後端位址，
            <code>extraEnvLine</code> 可額外附加一行 .env（form-service-frontend 型）</li>
        </ul>

        <h4>blueGreen（前端藍綠部署專案，選填）</h4>
        <p class="docs-src">讀取者：core/deploy/SSHUtil.sh 的 remote_blue_green_prod_update</p>
        <ul>
          <li>走不走藍綠由 <code>envs.{ENV}.templateOverride = frontend-prodDeployTemplate.sh</code> 決定，
            <b>不需要</b>在任何 shell 加專案名單</li>
          <li>此區塊只是覆寫機器端腳本位置，全部有預設值：<code>scriptPath</code>（script）、
            <code>deployScript</code>（deploy.sh）、<code>switchScript</code>（switch_traffic.sh）、
            <code>headerSwitch</code>（true，更新後是否執行 green header 切換）</li>
        </ul>

        <h4>完整範例</h4>
        <pre class="docs-example">{
  "envs": {
    "dev":  { "buildType": "maven", "sshUser": "tkb0001662", "javaVersion": "17" },
    "prod": { "buildType": "maven", "sshUser": "tkb0001662", "javaVersion": "17" }
  },
  "devMachine": {
    "containerName": "my-project-test",
    "useProjectSubdir": false,
    "versionFile": "docker-compose",
    "cleanupPaths": ["webapp/*"]
  }
}</pre>
        <p class="docs-note">
          存檔後可到 deploy.git 的 <code>config/project_deploy.json</code> 對照確認；
          既有專案（tkbgoapi、go_nuxt、form-service-frontend）的設定就是最好的實例參考。
        </p>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.deploy-registry-page {
  padding: 16px;
}

/* 表格跟隨全站主題（theme-color.css 的 --table-* 變數），
   避免 Element Plus 預設色在深色主題下出現一深一淺的背景不一致 */
:deep(.el-table) {
  --el-table-bg-color: var(--table-bg-color) !important;
  --el-table-tr-bg-color: var(--table-bg-color) !important;
  --el-table-header-bg-color: var(--table-header-bg, var(--table-bg-color)) !important;
  --el-table-border-color: var(--table-border-color) !important;
  --el-table-border: 1px solid var(--el-table-border-color) !important;
  --el-table-text-color: var(--table-text-color) !important;
  --el-table-header-text-color: var(--table-header-text-color) !important;
  --el-table-row-hover-bg-color: var(--table-hover-bg) !important;
  background-color: transparent !important;
}
:deep(.el-table__inner-wrapper) {
  background-color: transparent !important;
}
:deep(.el-table th.el-table__cell),
:deep(.el-table td.el-table__cell) {
  background-color: var(--table-bg-color) !important;
}
/* 保留 hover 高亮（上面的 td !important 會蓋掉預設 hover） */
:deep(.el-table .el-table__row:hover td.el-table__cell) {
  background-color: var(--table-hover-bg) !important;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 16px;
}
.page-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  max-width: 720px;
}
.page-actions {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}
.tab-toolbar {
  margin-bottom: 8px;
}
.json-editor :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 13px;
  line-height: 1.5;
}
.warn-alert {
  margin-bottom: 8px;
}
.dynamic-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.vmip-editor {
  max-width: 640px;
}
.vmip-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
}
.wiz-steps {
  margin-bottom: 16px;
}
.wiz-env-block {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
}
.wiz-env-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.docs {
  font-size: 13px;
  line-height: 1.7;
}
.docs h4 {
  margin: 16px 0 4px;
  border-left: 3px solid var(--el-color-primary);
  padding-left: 8px;
}
.docs ul {
  padding-left: 18px;
  margin: 4px 0;
}
.docs-src {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin: 0 0 4px;
}
.docs-note {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.docs-example {
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 10px 12px;
  font-family: Consolas, Menlo, monospace;
  font-size: 12px;
  overflow: auto;
}
</style>
