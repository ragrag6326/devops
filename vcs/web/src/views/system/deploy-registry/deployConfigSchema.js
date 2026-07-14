/**
 * project_deploy.json 專案節點的 schema 常數與驗證，
 * 由 index.vue（頁面）與 DeployConfigEditor.vue（表單/JSON 雙模式編輯器）共用，
 * 避免兩處各寫一份造成不同步。
 */

export const ENVS = ['dev', 'local', 'prod', 'admin']

export const BUILD_TYPES = ['maven', 'war', 'npm']

/** 部署樣板選項（core/deploy/ 下的檔名）。留空 = 依環境用 {ENV}DeployTemplate.sh */
export const TEMPLATE_OVERRIDES = ['prodDeployTemplate.sh', 'frontend-prodDeployTemplate.sh']

export const SKELETON = {
  envs: {
    dev: { buildType: 'maven', sshUser: 'tkb0001662', javaVersion: '17' },
  },
  devMachine: {
    containerName: '',
    useProjectSubdir: false,
    versionFile: 'docker-compose',
  },
}

/** JSON 模式可一鍵插入的區塊範本（key 不存在才插入） */
export const SNIPPETS = {
  'envs.dev': {
    label: '環境：dev（測試機）',
    path: ['envs', 'dev'],
    value: { buildType: 'maven', sshUser: 'tkb0001662', javaVersion: '17' },
  },
  'envs.local': {
    label: '環境：local（區網機）',
    path: ['envs', 'local'],
    value: { buildType: 'maven', sshUser: 'tkbuser', javaVersion: '17' },
  },
  'envs.prod': {
    label: '環境：prod（正式機）',
    path: ['envs', 'prod'],
    value: { buildType: 'maven', sshUser: 'tkb0001662', javaVersion: '17' },
  },
  'envs.admin': {
    label: '環境：admin（配置走 prod 流程、跑在測試機）',
    path: ['envs', 'admin'],
    value: { buildType: 'maven', sshUser: 'tkb0001662', javaVersion: '17', templateOverride: 'prodDeployTemplate.sh' },
  },
  devMachine: {
    label: '測試機設定 devMachine',
    path: ['devMachine'],
    value: { containerName: '', useProjectSubdir: false, versionFile: 'docker-compose', cleanupPaths: [] },
  },
  npmBuildFix: {
    label: 'npm：內容字串修正 filenameFixes',
    path: ['npmBuild'],
    value: { filenameFixes: [{ dir: 'pages/xxx', from: '原字串', to: '替換成' }] },
  },
  npmRenameFiles: {
    label: 'npm：build 前檔案改名 renameFiles',
    path: ['npmBuild', 'renameFiles'],
    value: [{ from: 'Claude.md', to: 'CLAUDE.md' }],
  },
  npmBuildEnv: {
    label: 'npm：依 NODE_TYPE 替換 API URL',
    path: ['npmBuild'],
    value: {
      envTemplate: '.env.example',
      backendApiPlaceholder: 'https://gotest.tkbtv.com.tw/api/v1',
      nodeTypeReplacements: {
        prod: { backendApiUrl: 'http://xxx-blue:8080/api/v1' },
        backup: { backendApiUrl: 'http://xxx-green:8080/api/v1', extraEnvLine: 'TEST_USER=BACKUP' },
        test: { backendApiUrl: 'http://xxx-test:8080/api/v1', extraEnvLine: 'TEST_USER=TEST' },
      },
    },
  },
  blueGreen: {
    label: '藍綠部署腳本覆寫（選填）',
    path: ['blueGreen'],
    value: { scriptPath: 'script', deployScript: 'deploy.sh', switchScript: 'switch_traffic.sh', headerSwitch: true },
  },
  vmIpOverride: {
    label: 'vmIP 特例（envs.dev.vmIP）',
    path: ['envs', 'dev', 'vmIP'],
    value: '192.168.x.x',
  },
}

/**
 * 儲存前的輕量檢查（只警告不阻擋，JSON 壞掉才需要阻擋儲存）。
 * @param {string} text - 專案節點 JSON 字串
 * @returns {string[]} 警告清單
 */
export const deployJsonWarnings = (text) => {
  let obj
  try {
    obj = JSON.parse(text)
  } catch (e) {
    return [`JSON 格式錯誤：${e.message}`]
  }
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) {
    return ['內容必須是 JSON object']
  }
  const w = []
  if (!obj.envs || typeof obj.envs !== 'object' || !Object.keys(obj.envs).length) {
    w.push('缺少 envs 區塊：genericDeploy.sh 會因取不到 buildType/sshUser 而部署失敗')
  } else {
    Object.entries(obj.envs).forEach(([env, c]) => {
      if (!c?.buildType) w.push(`envs.${env} 缺 buildType`)
      if (!c?.sshUser) w.push(`envs.${env} 缺 sshUser`)
    })
  }
  if (obj.dev) {
    w.push('偵測到頂層 "dev" 欄位：測試機設定的正確欄位是 "devMachine"，shell 讀不到 "dev"')
  }
  return w
}

/** JSON 字串是否連 object 都不是（此時禁止儲存） */
export const isDeployJsonBroken = (text) => {
  try {
    const o = JSON.parse(text)
    return !o || typeof o !== 'object' || Array.isArray(o)
  } catch {
    return true
  }
}
