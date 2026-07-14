<script setup>
/**
 * project_deploy.json 專案節點的「表單 / JSON」雙模式編輯器。
 *
 * 設計原則：
 * 1. v-model 的 JSON 字串永遠是唯一真相，表單只是它的視圖 ——
 *    表單任何改動立即序列化回 JSON，切換模式或儲存都不會有兩套狀態。
 * 2. 表單只顯示常用欄位（envs / devMachine），但序列化時「保留所有未知欄位」
 *    （npmBuild、blueGreen、未來新增的任何 key），不會重演舊版表單把 envs 蓋掉的事故。
 * 3. 進階區塊（npmBuild / blueGreen）在表單模式顯示為標籤提示，切到 JSON 模式編輯。
 */
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, ArrowDown, QuestionFilled, DocumentCopy } from '@element-plus/icons-vue'
import { ENVS, SNIPPETS, deployJsonWarnings } from './deployConfigSchema'

const props = defineProps({
  modelValue: { type: String, default: '{}' },
})
const emit = defineEmits(['update:modelValue', 'show-docs'])

const mode = ref('form') // form | json

// ── JSON 模式：textarea 直接代理 v-model ─────────────────────────
const jsonText = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const warnings = computed(() => deployJsonWarnings(props.modelValue))

const formatJson = () => {
  try {
    emit('update:modelValue', JSON.stringify(JSON.parse(props.modelValue), null, 2))
  } catch (e) {
    ElMessage.warning(`JSON 格式錯誤：${e.message}`)
  }
}

// ── 表單模式狀態 ─────────────────────────────────────────────────
// envRows: [{env, buildType, sshUser, javaVersion, npmVersion, templateOverride, vmIP, _extra}]
const envRows = ref([])
const devMachineEnabled = ref(false)
const devM = ref({ containerName: '', useProjectSubdir: false, versionFile: 'docker-compose', versionEnvKey: 'TEST_VERSION', cleanupPaths: [], _extra: {} })
// envs / devMachine 以外的頂層欄位（npmBuild、blueGreen…），原樣保留
const restObj = ref({})

const advancedKeys = computed(() => Object.keys(restObj.value))

// 序列化中的旗標，避免自己 emit 的更新又觸發 loadFromJson
let lastEmitted = null

const loadFromJson = () => {
  let obj
  try {
    obj = JSON.parse(props.modelValue)
  } catch {
    return false
  }
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return false

  const rows = []
  if (obj.envs && typeof obj.envs === 'object') {
    Object.entries(obj.envs).forEach(([env, c]) => {
      const { buildType, sshUser, javaVersion, npmVersion, templateOverride, vmIP, ...extra } = c || {}
      rows.push({
        env,
        buildType: buildType || 'maven',
        sshUser: sshUser || '',
        javaVersion: javaVersion || '',
        npmVersion: npmVersion || '',
        templateOverride: templateOverride || '',
        vmIP: vmIP || '',
        _extra: extra, // 未知的環境層欄位原樣保留
      })
    })
  }
  envRows.value = rows

  if (obj.devMachine && typeof obj.devMachine === 'object') {
    const { containerName, useProjectSubdir, versionFile, versionEnvKey, cleanupPaths, ...extra } = obj.devMachine
    devMachineEnabled.value = true
    devM.value = {
      containerName: containerName || '',
      useProjectSubdir: !!useProjectSubdir,
      versionFile: versionFile || 'docker-compose',
      versionEnvKey: versionEnvKey || 'TEST_VERSION',
      cleanupPaths: Array.isArray(cleanupPaths) ? [...cleanupPaths] : [],
      _extra: extra,
    }
  } else {
    devMachineEnabled.value = false
    devM.value = { containerName: '', useProjectSubdir: false, versionFile: 'docker-compose', versionEnvKey: 'TEST_VERSION', cleanupPaths: [], _extra: {} }
  }

  const rest = {}
  Object.entries(obj).forEach(([k, v]) => {
    if (k !== 'envs' && k !== 'devMachine') rest[k] = v
  })
  restObj.value = rest
  return true
}

const serialize = () => {
  const obj = {}
  // envs 放最前面，維持既有 JSON 的閱讀順序
  const envs = {}
  envRows.value.forEach((r) => {
    if (!r.env?.trim()) return
    const c = { ...r._extra }
    if (r.buildType) c.buildType = r.buildType
    if (r.sshUser?.trim()) c.sshUser = r.sshUser.trim()
    if (r.buildType === 'npm') {
      if (r.npmVersion?.trim()) c.npmVersion = r.npmVersion.trim()
    } else if (r.javaVersion?.trim()) {
      c.javaVersion = r.javaVersion.trim()
    }
    if (r.templateOverride?.trim()) c.templateOverride = r.templateOverride.trim()
    if (r.vmIP?.trim()) c.vmIP = r.vmIP.trim()
    envs[r.env.trim()] = c
  })
  if (Object.keys(envs).length) obj.envs = envs

  if (devMachineEnabled.value) {
    const d = { ...devM.value._extra }
    if (devM.value.containerName?.trim()) d.containerName = devM.value.containerName.trim()
    d.useProjectSubdir = !!devM.value.useProjectSubdir
    d.versionFile = devM.value.versionFile
    if (devM.value.versionFile === 'env') d.versionEnvKey = devM.value.versionEnvKey || 'TEST_VERSION'
    const paths = devM.value.cleanupPaths.filter((p) => p && p.trim())
    if (paths.length) d.cleanupPaths = paths
    obj.devMachine = d
  }

  Object.entries(restObj.value).forEach(([k, v]) => {
    obj[k] = v
  })

  const text = JSON.stringify(obj, null, 2)
  lastEmitted = text
  emit('update:modelValue', text)
}

// 表單改動 → 即時寫回 JSON（唯一真相）
watch([envRows, devMachineEnabled, devM], () => {
  if (mode.value === 'form') serialize()
}, { deep: true })

// 外部改動 modelValue（如精靈的「從既有專案複製」、插入區塊）→ 重載表單
watch(() => props.modelValue, (v) => {
  if (v !== lastEmitted && mode.value === 'form') {
    loadFromJson()
  }
})

const switchMode = (m) => {
  if (m === 'form') {
    if (!loadFromJson()) {
      ElMessage.warning('JSON 格式錯誤，請先修正才能切回表單模式')
      mode.value = 'json'
      return
    }
  }
  mode.value = m
}

// ── 表單操作 ─────────────────────────────────────────────────────
const usedEnvs = computed(() => envRows.value.map((r) => r.env))
const addEnvRow = () => {
  const next = ENVS.find((e) => !usedEnvs.value.includes(e)) || ''
  envRows.value.push({
    env: next, buildType: 'maven', sshUser: 'tkb0001662',
    javaVersion: '17', npmVersion: '18', templateOverride: '', vmIP: '', _extra: {},
  })
}
const removeEnvRow = (i) => envRows.value.splice(i, 1)
const addCleanupPath = () => devM.value.cleanupPaths.push('')
const removeCleanupPath = (i) => devM.value.cleanupPaths.splice(i, 1)

// ── JSON 模式：插入範本區塊 ──────────────────────────────────────
const insertSnippet = (key) => {
  const s = SNIPPETS[key]
  let obj
  try {
    obj = JSON.parse(props.modelValue)
  } catch (e) {
    ElMessage.warning(`目前內容 JSON 格式錯誤，請先修正再插入：${e.message}`)
    return
  }
  let node = obj
  for (let i = 0; i < s.path.length - 1; i++) {
    const k = s.path[i]
    if (node[k] === undefined || node[k] === null || typeof node[k] !== 'object') node[k] = {}
    node = node[k]
  }
  const leaf = s.path[s.path.length - 1]
  if (node[leaf] !== undefined) {
    ElMessage.warning(`「${s.path.join('.')}」已存在，未覆蓋，請直接修改現有內容`)
    return
  }
  node[leaf] = s.value
  emit('update:modelValue', JSON.stringify(obj, null, 2))
  ElMessage.success(`已插入 ${s.path.join('.')} 範本，請依實際需求修改值`)
}

onMounted(() => {
  // 預設進表單模式；JSON 壞掉（不該發生於既有資料）就停在 JSON 模式
  if (!loadFromJson()) mode.value = 'json'
})

defineExpose({ mode })
</script>

<template>
  <div class="dc-editor">
    <div class="dc-toolbar">
      <el-radio-group :model-value="mode" size="small" @update:model-value="switchMode">
        <el-radio-button value="form">表單模式</el-radio-button>
        <el-radio-button value="json">JSON 模式</el-radio-button>
      </el-radio-group>

      <el-dropdown v-if="mode === 'json'" @command="insertSnippet">
        <el-button size="small" :icon="Plus">插入設定區塊<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="(s, k) in SNIPPETS" :key="k" :command="k">{{ s.label }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button v-if="mode === 'json'" size="small" :icon="DocumentCopy" @click="formatJson">格式化</el-button>

      <el-button link type="primary" :icon="QuestionFilled" @click="$emit('show-docs')">欄位說明</el-button>
    </div>

    <el-alert
      v-for="(w, i) in warnings"
      :key="i"
      :title="w"
      type="warning"
      :closable="false"
      class="dc-warn"
    />

    <!-- ═══ 表單模式 ═══ -->
    <div v-if="mode === 'form'">
      <div class="dc-section-title">
        部署環境（envs，必填至少一個）
        <el-button link type="primary" :icon="Plus" @click="addEnvRow">新增環境</el-button>
      </div>
      <div v-for="(r, i) in envRows" :key="i" class="dc-env-row">
        <div class="dc-row">
          <div class="dc-field">
            <span class="dc-label">部署環境（PROJECT_ENV）</span>
            <el-select v-model="r.env" filterable allow-create placeholder="dev / prod..." style="width: 170px">
              <el-option value="dev" label="dev（測試機）" />
              <el-option value="local" label="local（區網機）" />
              <el-option value="prod" label="prod（正式機）" />
              <el-option value="admin" label="admin（form-service 型）" />
            </el-select>
          </div>
          <div class="dc-field">
            <span class="dc-label">打包方式（buildType）</span>
            <el-select v-model="r.buildType" style="width: 160px">
              <el-option value="maven" label="maven（Java jar）" />
              <el-option value="war" label="war（Tomcat）" />
              <el-option value="npm" label="npm（前端）" />
            </el-select>
          </div>
          <div class="dc-field">
            <span class="dc-label">SSH 帳號（sshUser）</span>
            <el-input v-model="r.sshUser" placeholder="必填，如 tkb0001662" style="width: 160px" />
          </div>
          <div class="dc-field" v-if="r.buildType === 'npm'">
            <span class="dc-label">Node 版本（nvm use）</span>
            <el-input v-model="r.npmVersion" placeholder="預設 18" style="width: 130px" />
          </div>
          <div class="dc-field" v-else>
            <span class="dc-label">Java 版本</span>
            <el-input v-model="r.javaVersion" placeholder="預設 17" style="width: 130px" />
          </div>
          <el-button link type="danger" :icon="Delete" class="dc-row-del" @click="removeEnvRow(i)" />
        </div>
        <div class="dc-row dc-row-sub">
          <div class="dc-field">
            <span class="dc-label">部署樣板（templateOverride，選填）</span>
            <el-select v-model="r.templateOverride" clearable filterable allow-create placeholder="留空 = 依環境用預設樣板" style="width: 300px">
              <el-option value="prodDeployTemplate.sh" label="prodDeployTemplate.sh（admin 走正式流程）" />
              <el-option value="frontend-prodDeployTemplate.sh" label="frontend-prodDeployTemplate.sh（前端藍綠）" />
            </el-select>
          </div>
          <div class="dc-field">
            <span class="dc-label">目標機器 IP（vmIP，選填）</span>
            <el-input v-model="r.vmIP" placeholder="留空 = 用 vmIP.json 的環境預設" style="width: 280px" />
          </div>
        </div>
      </div>
      <div v-if="!envRows.length" class="dc-empty">尚未設定任何環境，點「新增環境」開始</div>

      <div class="dc-section-title">
        測試機設定（devMachine）
        <el-switch v-model="devMachineEnabled" size="small" />
      </div>
      <template v-if="devMachineEnabled">
        <el-form label-width="140px" size="small">
          <el-form-item label="Container 名稱">
            <el-input v-model="devM.containerName" placeholder="留空 = PROJECT_NAME" style="width: 260px" />
          </el-form-item>
          <el-form-item label="使用專案子目錄">
            <el-switch v-model="devM.useProjectSubdir" />
            <span class="dc-hint">開啟後路徑為 /opt/docker_image/{PROJECT_NAME}（form-service 系列）</span>
          </el-form-item>
          <el-form-item label="版本檔案">
            <el-radio-group v-model="devM.versionFile">
              <el-radio value="docker-compose">docker-compose.yml（改 image tag）</el-radio>
              <el-radio value="env">.env（改指定 KEY）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="devM.versionFile === 'env'" label="版本 ENV KEY">
            <el-input v-model="devM.versionEnvKey" placeholder="例如 TEST_VERSION" style="width: 200px" />
          </el-form-item>
          <el-form-item label="重啟前清理路徑">
            <div v-for="(p, i) in devM.cleanupPaths" :key="i" class="dc-row" style="margin-bottom: 6px">
              <el-input v-model="devM.cleanupPaths[i]" placeholder="例如 webapp/*" style="width: 260px" />
              <el-button link type="danger" :icon="Delete" @click="removeCleanupPath(i)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="addCleanupPath">新增清理路徑</el-button>
          </el-form-item>
        </el-form>
      </template>

      <div v-if="advancedKeys.length" class="dc-advanced">
        含進階區塊：
        <el-tag v-for="k in advancedKeys" :key="k" size="small" style="margin-right: 6px">{{ k }}</el-tag>
        <span class="dc-hint">表單不顯示這些區塊，請切到 JSON 模式編輯；表單儲存不會遺失它們</span>
      </div>
    </div>

    <!-- ═══ JSON 模式 ═══ -->
    <el-input
      v-else
      v-model="jsonText"
      type="textarea"
      :rows="16"
      class="dc-json"
      spellcheck="false"
    />
  </div>
</template>

<style scoped>
.dc-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.dc-warn {
  margin-bottom: 8px;
}
.dc-section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  margin: 12px 0 8px;
  border-left: 3px solid var(--el-color-primary);
  padding-left: 8px;
}
.dc-env-row {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 8px;
}
.dc-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}
.dc-row-sub {
  margin-top: 8px;
}
.dc-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.dc-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
.dc-row-del {
  margin-bottom: 4px;
}
.dc-empty {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 8px 0;
}
.dc-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.dc-advanced {
  margin-top: 12px;
  font-size: 13px;
}
.dc-json :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, Menlo, monospace;
  font-size: 13px;
  line-height: 1.5;
}
</style>
