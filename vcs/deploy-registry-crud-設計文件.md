# 部署對照表管理 改版設計文件

> 範圍：deploy.git 的 `config/project_deploy.json`（改純 JSON 編輯）、`config/remote_gitlab_repo.json`、`template/{ENV}/{PROJECT_NAME}_Dockerfile` 的前端 CRUD、「新增專案」單一 commit 精靈、`vmIP.json` 擴充方案，以及 core/deploy shell 檢查結果。
> 日期：2026-07-06
> **狀態：已全部實作**（含 §5.2 shell 修正與 §6 vmIP 方案）。剩餘驗證：請在本機跑 `tkb: gradlew compileJava` 與 `web: npm run build`（沙箱無法連 Maven Central / 掛載同步限制），並依 §7 驗證計畫實測。

---

## 1. 現況檢查結果

### 1.1 嚴重 bug：前端 deploy-registry 頁面 schema 過時

`web/src/views/system/deploy-registry/index.vue` 是照舊版 schema 寫的：

| 項目 | 前端頁面 | 實際 JSON / shell 讀取 |
|---|---|---|
| 測試機設定 key | `dev` | `devMachine`（SSHUtil.sh） |
| 建置設定 | 表單完全沒有 | `envs.{PROJECT_ENV}`（genericDeploy.sh 必讀） |

後果：

1. 表格顯示的 Container/版本檔案/清理路徑全是 fallback 假值（讀不到 `dev` key）。
2. `buildPayload()` 只組出 `{dev, npmBuild}`，而後端 `writeProject()` 是**整個專案節點覆蓋**。從這頁儲存任何既有專案 → `envs` 與 `devMachine` 全被刪除 → genericDeploy.sh 報「找不到 buildType/sshUser」→ 該專案部署直接失敗。

**在改版上線前，此頁面不可用於儲存。** 改成純 JSON 編輯後此類 schema 漂移問題不再發生。

### 1.2 次要問題

- 讀取→修改→commit 無並發保護，後存者覆蓋前者。GitLab commit action 支援 `last_commit_id`，可做樂觀鎖（見 §2.5）。
- `deleteProject()` 刪除不存在的專案仍會產生一次無異動 commit。加存在性檢查即可。
- `GitlabCommitRequest.Action` 已支援 create/update/delete，且**一個 commit 可帶多個 actions** → 新增專案精靈可原子寫入三處。

---

## 2. 後端設計（tkb）

### 2.1 抽出共用 GitLab 檔案存取

現在 `DeployRegistryServiceImpl` 內嵌 HttpClient 讀寫單一檔案。抽成：

```
com.tkb.api.gitlab.DeployGitFileClient
├── String  readFile(String filePath)            // 404 回 null
├── boolean fileExists(String filePath)
├── List<String> listTree(String path)           // GET /repository/tree?path=&recursive=true
└── void    commit(String message, List<GitlabCommitRequest.Action> actions)
```

沿用既有 JDK HttpClient + 手動 `%2F` 編碼作法（Feign 會 decode %2F，不可用）。`DeployRegistryServiceImpl` 改為呼叫此 client，行為不變。

### 2.2 project_deploy.json（既有 API，微調）

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/deploy-registry` | 不變 |
| GET | `/api/deploy-registry/{projectName}` | 不變 |
| PUT | `/api/deploy-registry/{projectName}` | 不變（收任意 JsonNode，正好配合純 JSON 編輯）。加：body 必須是 JSON object，否則 400 |
| DELETE | `/api/deploy-registry/{projectName}` | 加存在性檢查，不存在回錯誤、不 commit |

### 2.3 remote_gitlab_repo.json（新增）

檔案結構特殊（`backend` / `frontend` 各是「包一個物件的陣列」，shell 用 `jq ".${TYPE}[][\"${PROJECT_NAME}\"].repository_url"` 讀）→ **保留原結構不動**，後端寫入時固定操作 `.{type}[0]`，不必改任何 shell。

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/deploy-registry/gitlab-repo` | 回傳整份，前端攤平成列表 |
| PUT | `/api/deploy-registry/gitlab-repo/{type}/{projectName}` | body `{"repository_url": "..."}`，upsert 至 `.{type}[0].{projectName}` |
| DELETE | `/api/deploy-registry/gitlab-repo/{type}/{projectName}` | 移除 |

`type` 僅允許 `backend` / `frontend`（對應 Jenkins 的 `TYPE`）。

### 2.4 Dockerfile 模板（新增）

BuildUtil.sh 取用路徑：`template/{PROJECT_ENV}/{PROJECT_NAME}_Dockerfile`。

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/deploy-registry/templates` | 用 tree API 列出 `template/**/*_Dockerfile`，回 `[{env, projectName, path}]` |
| GET | `/api/deploy-registry/templates/{env}/{projectName}` | 回純文字內容，404 回空（配合新增流程） |
| PUT | `/api/deploy-registry/templates/{env}/{projectName}` | body 純文字。先 `fileExists()` 決定 action 用 create 或 update |
| DELETE | `/api/deploy-registry/templates/{env}/{projectName}` | 刪除模板 |

`env` 僅允許既有目錄慣例：`dev` / `local` / `prod` / `admin`。

### 2.5 新增專案精靈（新增，單一 commit）

```
POST /api/deploy-registry/init-project
{
  "projectName": "new_project",
  "type": "backend",                       // remote_gitlab_repo.json 的分類
  "repositoryUrl": "ssh://git@192.168.1.35:2224/tkb/xxx.git",
  "deployConfig": { ... },                 // 直接放進 project_deploy.json 的專案節點（原始 JSON）
  "dockerfiles": { "dev": "FROM ...", "prod": "FROM ..." }   // 每個環境一份
}
```

處理流程：一次組出多個 actions → **單一 GitLab commit**：

1. `update config/project_deploy.json`（塞入 `deployConfig`）
2. `update config/remote_gitlab_repo.json`（upsert repo URL）
3. `create template/{env}/{projectName}_Dockerfile` × N

原子性：全部成功或全部不寫入，不會出現「對照表有了但模板沒有」的半套狀態。已存在同名專案時回 409 拒絕（避免精靈誤覆蓋，改用編輯功能）。

**併發保護（選配，建議做）**：GET 回應附上檔案 `last_commit_id`；PUT 時前端帶回，後端塞進 action 的 `last_commit_id` 欄位，GitLab 發現期間有人動過會回 400 → 前端提示「內容已被別人更新，請重新載入」。

### 2.6 稽核

所有寫入沿用既有 commit message 慣例 `[deploy-registry] ...`，並帶操作者帳號（從 login session 取），方便在 deploy.git 的 git log 追溯是誰改的。

---

## 3. 前端設計（web）

`system/deploy-registry` 改為三個分頁 + 一個精靈按鈕：

### 3.1 分頁一：部署對照表（project_deploy.json）

- 列表照舊（欄位改讀正確的 `devMachine` / `envs`，僅供瀏覽摘要：PROJECT_NAME、envs 有哪些環境、buildType、測試機 container）。
- **編輯改為純 JSON**：點「編輯」跳出 dialog，內容是該專案節點的 JSON（textarea 或 codemirror，等寬字型 + 格式化按鈕）。
  - 儲存前 `JSON.parse` 驗證，parse 失敗直接擋下並標示錯誤。
  - 額外輕量檢查（只警告不阻擋）：缺 `envs`、envs 底下缺 `buildType`/`sshUser`。
  - 儲存 = PUT 該專案節點 → commit + push 回 deploy.git。
- 刪除照舊，另提供勾選「一併刪除該專案的 Dockerfile 模板與 repo 對照」（走一次多 action commit）。

### 3.2 分頁二：GitLab Repo 對照（remote_gitlab_repo.json）

- 列表：type（backend/frontend）、PROJECT_NAME、repository_url。
- 新增/編輯 dialog：三個欄位（type 下拉、名稱、URL）。URL 格式檢查 `ssh://git@...git`。

### 3.3 分頁三：Dockerfile 模板（template/）

- 列表：env、PROJECT_NAME、路徑。
- 編輯 dialog：純文字 editor 顯示 Dockerfile 內容。
- 新增：選 env + PROJECT_NAME + 內容；提供「從既有模板複製」下拉快速帶入。

### 3.4 新增專案精靈

一個 dialog 三步驟，最後**單一 commit** 呼叫 `POST /init-project`：

1. **基本資料**：PROJECT_NAME（即 Jenkins 參數）、TYPE（backend/frontend）、repository_url。
2. **project_deploy.json 節點**：JSON editor，預帶骨架（可選「從既有專案複製」）：
   ```json
   { "envs": { "dev": { "buildType": "maven", "sshUser": "tkb0001662", "javaVersion": "17" } } }
   ```
3. **Dockerfile 模板**：依 step 2 勾選的環境各一個 tab，textarea 填內容，可從既有專案模板帶入。

完成後提示：「已 commit 至 deploy.git（v2 分支），Jenkins 下次 buildWithParameters 即生效」。

### 3.5 特殊更新需求對應確認

- 後端正/備援同時更新、前端分開更新（NODE_TYPE=prod/backup）是 genericDeploy.sh 的通用規則與 Jenkins job 參數決定，**不進對照表、本次改版不涉及**。
- Dockerfile 模板僅按 `PROJECT_ENV` 區分（dev/local/prod/admin），前端藍綠不需要不同模板，維持現狀。

---

## 4. 檔案異動清單

**後端（tkb）**
- 新增 `api/gitlab/DeployGitFileClient.java`
- 修改 `service/impl/DeployRegistryServiceImpl.java`（改用 client、刪除加存在檢查）
- 新增 `service/DeployGitConfigService.java` + impl（repo 對照、模板、init-project）
- 修改 `controller/DeployRegistryController.java`（新增 §2.3–2.5 endpoints，或另拆 controller）

**前端（web）**
- 重寫 `views/system/deploy-registry/index.vue`（三分頁 + 精靈 + JSON editor）
- 修改 `api/deployRegistry.js`（補新 endpoints）

**deploy.git / tools**：不需任何修改（結構全部維持 shell 現有讀法）。

---

## 5. core/deploy shell 檢查結果（2026-07-06 補充）

### 5.1 行為對照確認（與實機版號一致）

| 描述 | 機制 | 佐證 |
|---|---|---|
| 後端正/備援同時更新 | Jenkins 只 sed `GREEN_VERSION` → 機器上 `deploy.sh blue` 先同步 `BLUE_VERSION=GREEN_VERSION` 再重啟，兩線必同版 | `backend-prod/tkbgoapi:1.0.73` 單一版號 |
| 前端正/備援分開更新 | NODE_TYPE=prod/backup 各自 build image（`-prod` / `-backup` 後綴） | `go_nuxt-prod:1.0.120` / `go_nuxt-backup:1.0.118` |
| 測試機不區分 | devDeployTemplate 直接改 docker-compose / .env 後重啟 | `backend-dev/*` 單一版號 |
| form-service 配置 prod、跑在測試機 | `PROJECT_ENV=admin` → `vmIP.json` 的 `admin`=131.186.44.40（=dev 機）+ `templateOverride: prodDeployTemplate.sh` | `*-admin/*` 版號 |

### 5.2 發現問題（依嚴重度）

1. **退版對 form-service 系列無效**：`SSHUtil.sh` 的 `remote_docker_compose_version_rollback` 固定 `cd /opt/docker_image/` 且只 sed `docker-compose.yml`，不理會 `devMachine.useProjectSubdir` / `versionFile=env`。form-service 部署失敗時退版改錯檔或無作用。→ 修法：重用 `_project_dev_registry` 組出與 `remote_dev_version_update` 相同的 target_dir/target_file。
2. **`project_npm_build` 缺 local/admin 分支**（BuildUtil.sh 一般 build 流程只有 dev / prod+NODE_TYPE）：`go_nuxt` 的 `local` 環境會跳過 `npm run build`，直接因 `.output` 不存在報錯。
3. **repo 內 `opt/devops/docker_compose/tkbgoapi/.env` typo**：`GREEN_VERSION_=1.0.16`（多一條底線）。拿這份初始化新機器時 GREEN 版本會是空字串。
4. `mv Claude.md CLAUDE.md` 寫死在 BuildUtil.sh 通用 npm（envTemplate）流程，其他專案沒此檔會失敗噪音 → 加 `[ -f Claude.md ] &&` 防護。
5. `remote_gounxt_prod_update` 的 `[ A ] || [ B ] && [ C ]` 靠左結合碰巧正確 → 用 `{ [ A ] || [ B ]; } && [ C ]` 明確分組；NODE_TYPE 非 prod/backup 時整段靜默跳過，應補 log_warn。
6. 小項：`dockerUtil.sh` 的 go_nuxt 特例分支與 else 相同（註解過時，可刪）；`init.sh` 的 `[ $gitlab_repo_url = "null" ]` 未加引號，空值時語法錯誤而非走 log_error；`transfer_image_to_vm` 的 go_nuxt prod 分支少了 `mkdir -p`。

以上為 shell 端修正，與本次前端 CRUD 改版可分開排程；第 1、2 項建議優先。

---

## 6. vmIP.json 擴充方案（之後新增遠端 IP）

**原則：vmIP.json 維持「環境預設值」不改結構，特例寫進 project_deploy.json。**

1. `project_deploy.json` 的 `envs.{ENV}` 新增選填欄位 `vmIP`（字串）。
2. `utils/vm/getVMIP.sh` 改為兩段查找（唯一要動的 shell，約 5 行）：

```bash
getVMIP() {
    local ip
    ip=$(jq -r --arg p "${PROJECT_NAME}" --arg e "${PROJECT_ENV}" \
        '.[$p].envs[$e].vmIP // empty' "${CONFIG_DIR}/project_deploy.json" 2>/dev/null)
    [ -z "${ip}" ] && ip=$(jq -r .${PROJECT_ENV} "${CONFIG_DIR}/vmIP.json")
    echo "${ip}"
}
```

好處：

- 新機器 / 特例專案（如 form-service 型「prod 配置跑在別台」）只改 project_deploy.json 一處，**前端 JSON 編輯器直接管到，不需新 UI**。
- 未來一個環境要部署多台時，`vmIP` 可改陣列、部署函式迴圈處理（屆時再議）。
- `vmIP.json` 本身另提供簡單 key/value 管理：`GET/PUT /api/deploy-registry/vm-ip`，前端併入分頁二或獨立小分頁。

> 注意：採用此方案後，§4「deploy.git 不需修改」多一個例外：`getVMIP.sh` 一次性小改。

---

## 7. 驗證計畫

1. 後端單元：repo 對照 upsert 後 jq filter `.backend[]["x"].repository_url` 仍可取值；project_deploy 寫回後 `_comment`/`_schema` 保留。
2. 對測試分支實測 init-project：確認單一 commit 內含三個檔案異動。
3. 前端：編輯既有專案（如 tkbgoapi）存檔後，diff 確認 `envs`/`devMachine` 完整保留。
4. Jenkins dev 環境跑一次新專案 buildWithParameters 全流程。
5. vmIP override：對一個 dev 專案設 `envs.dev.vmIP` 指到現行同一台，確認 getVMIP 取值正確、拿掉後 fallback 正常。
