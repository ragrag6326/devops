# TKB DevOps 系統架構文件
**VCS Platform Architecture & Roadmap**

版本：v1.1　　更新：2026-06-28

本文件涵蓋三個 Repo 的架構設計、DB 設定欄位說明、API 路徑整理、新增專案 SOP，以及未來「靈活配置管理」功能的規劃方向。

---

## 目錄

1. [系統概覽](#1-系統概覽)
2. [DB：project_config 欄位完整說明](#2-dbproject_config-欄位完整說明)
3. [Shell Script 架構](#3-shell-script-架構)
4. [現有專案環境配置](#4-現有專案環境配置)
5. [後端 API 路徑整理](#5-後端-api-路徑整理)
6. [前端頁面架構](#6-前端頁面架構)
7. [新增專案 SOP](#7-新增專案-sop)
8. [已實作功能清單](#8-已實作功能清單)
9. [已知問題修復記錄](#9-已知問題修復記錄)
10. [未來規劃：靈活配置管理](#10-未來規劃靈活配置管理)

---

## 1. 系統概覽

### 三個 Repo 職責

| Repo | 技術棧 | 職責 |
|------|--------|------|
| `tkb` | Spring Boot 3 + MyBatis-Plus | API Server：版本管理 / 部署觸發 / Shell 調度 / GitLab 整合 |
| `web` | Vue 3 + Element Plus | 前端操作介面：版本歷史 / Monitor / 專案管理 / 首頁 |
| `tools` | Bash Shell Script | 遠端機器操作：部署 / 健康檢查 / 流量切換 / Image 管理 |

### 1.1 單次部署資料流程

```
① 前端   使用者填版號 → 選環境 → 選部署類型（正式/備援）
② 後端   checkDeployable → saveNewVersion → triggerJenkins → updateJenkinsBuildId
③ Jenkins 拉 image → docker-compose up → Pipeline 確認健康
④ Shell  manage_images.sh / healthcheck.sh / switch_traffic.sh（按需）
⑤ 前端   輪詢取 Console Log → 顯示結果 → live board 更新
```

### 1.2 環境對應

| UI 顯示 | 實際 Jenkins env | 機器 IP 段 | 說明 |
|---------|-----------------|-----------|------|
| 正式（prod） | `prod`（預設）或自訂如 `admin` | 132.x.x.x | 正式機，`has_prod=1` 的專案才出現 |
| 備援（backup） | 同上（同台機器） | 132.x.x.x | 使用 backup 部署類型 |
| 測試（dev） | `dev`（預設）或自訂 | 131.x.x.x | 測試機，`has_dev=1` 的專案才出現 |

---

## 2. DB：project_config 欄位完整說明

所有專案的識別與配置集中於此表，新增專案只需插入一行，不需改任何程式碼。

### 2.1 基本識別欄位

| 欄位 | 型別 | 說明 / 範例 |
|------|------|------------|
| `id` | BIGINT AUTO | 主鍵 |
| `name` | VARCHAR(100) | 系統識別名稱，對應 `project_versions.project_name`。例：`tkbgoapi` |
| `display_name` | VARCHAR(100) | 前端顯示名稱。例：`TKB Go API` |
| `description` | VARCHAR(500) | 專案描述（選填） |
| `category` | VARCHAR(20) | `frontend` / `backend` |
| `is_active` | TINYINT(1) | 1=啟用，0=停用 |
| `sort_order` | INT | 前端清單排序，數字越小越前 |
| `gitlab_project_id` | BIGINT | GitLab Project ID（整合 MR Review 用） |

### 2.2 Shell / Docker 配置欄位

| 欄位 | 型別 | 說明 / 範例 |
|------|------|------------|
| `script_name` | VARCHAR(100) | `tools/` 下的目錄名。null → 使用 `name`。例：`tv`（DB name=`tkbtv`） |
| `has_prod` | TINYINT(1) | 1=部署於正式機（132.x），0=否 |
| `has_dev` | TINYINT(1) | 1=部署於測試機（131.x），0=否 |
| `image_keyword` | VARCHAR(100) | Docker image grep 關鍵字。null → 使用 `name`。例：`goapi` |

### 2.3 Jenkins 配置欄位

| 欄位 | 型別 | 說明 / 範例 |
|------|------|------------|
| `prod_env` | VARCHAR(50) | 正式部署 Jenkins env 名稱。null → 預設 `prod`。例：`admin` |
| `dev_env` | VARCHAR(50) | 測試部署 Jenkins env 名稱。null → 預設 `dev` |
| `jenkins_job_name` | VARCHAR(100) | Jenkins Job 名稱。null → 組合 `{type}-{env}`。例：`form-service-frontend` |
| `jenkins_token` | VARCHAR(200) | Jenkins Token。null → 組合 `{env}-yjjnoXvHXUE16TAmBzP4` |
| `jenkins_pipeline_name` | VARCHAR(100) | Pipeline Job 名稱。null → 組合 `{type}-pipeline` |
| `default_branch` | VARCHAR(50) | 預設部署分支。null → `master`。例：`main`（form-service） |

### 2.4 Fallback 優先規則

- `jenkins_job_name` 有值 → 直接使用；否則組合 `{type}-{env}`
- `jenkins_token` 有值 → 直接使用；否則組合 `{env}-yjjnoXvHXUE16TAmBzP4`
- `script_name` 有值 → Shell 使用此目錄；否則使用 `name`
- `image_keyword` 有值 → Docker grep 使用此關鍵字；否則使用 `name`
- `prod_env` / `dev_env` 有值 → 作為實際 Jenkins env；否則使用 `prod` / `dev`
- `default_branch` 有值 → 部署時預設分支；否則使用 `master`

### 2.5 建立 SQL

```sql
ALTER TABLE project_config
  ADD COLUMN prod_env              VARCHAR(50)  NULL COMMENT '正式 Jenkins env，null 預設 prod',
  ADD COLUMN dev_env               VARCHAR(50)  NULL COMMENT '測試 Jenkins env，null 預設 dev',
  ADD COLUMN jenkins_job_name      VARCHAR(100) NULL COMMENT 'Jenkins Job 名稱，null 用 {type}-{env}',
  ADD COLUMN jenkins_token         VARCHAR(200) NULL COMMENT 'Jenkins Token，null 用 {env}-token',
  ADD COLUMN jenkins_pipeline_name VARCHAR(100) NULL COMMENT 'Pipeline Job，null 用 {type}-pipeline',
  ADD COLUMN default_branch        VARCHAR(50)  NULL COMMENT '預設分支，null 用 master';

-- form-service 特例設定
UPDATE project_config SET
  prod_env = 'admin',
  jenkins_job_name = 'form-service-frontend',
  jenkins_token = 'admin-yjjnoXvHXUE16TAmBzP4',
  default_branch = 'main'
WHERE name IN ('form-service-frontend', 'form-service-backend');
```

---

## 3. Shell Script 架構

### 3.1 目錄結構

```
tools/
  common/                    # 所有通用腳本（不需修改）
    init.sh                  # 載入 config.sh + sshToolUtil.sh
    manage_images.sh         # {env} {current|history|delete}
    version_renew.sh         # 退版（更新 image tag）
    healthcheck.sh           # 容器健康檢查
    get_traffic.sh           # 查詢 nginx 流量方向
    switch_traffic.sh        # 切換 nginx 流量
    restartContainer.sh      # 重啟容器
  utils/
    sshToolUtil.sh           # SSH 路由：env=prod→132.x，env=dev→131.x
  {project}/                 # 每個專案一個目錄（= scriptName）
    config.sh                # 專案特定配置（唯一需要維護的檔案）
```

### 3.2 config.sh 標準欄位

| 變數 | 說明 |
|------|------|
| `PROD_BLUE_CONTAINERS` | 正式機 Blue 容器名稱（空格分隔多個） |
| `PROD_GREEN_CONTAINERS` | 正式機 Green/Backup 容器名稱 |
| `PROD_NGINX_CONF` | 正式機 nginx conf 完整路徑 |
| `PROD_LIVE_UPSTREAM` | 正式流量 nginx upstream 名稱 |
| `PROD_HEADER_UPSTREAM` | 測試 Header 流量 nginx upstream 名稱 |
| `PROD_TRAFFIC_BLUE_PORT` | Blue 節點 port（健康檢查用） |
| `PROD_DEPLOY_BASE` | 正式機 docker-compose 目錄路徑 |
| `DEV_*` | 同上，對應測試機（131.x）設定 |
| `IMAGE_KEYWORD` | Docker image grep 關鍵字（可被 DB `image_keyword` 覆蓋） |

### 3.3 Java 調用方式

```java
ShellExecutor.execMerged("common/script.sh", ENV, PROJECT_NAME, ...)
```

- `init.sh` 載入 `tools/{scriptName}/config.sh`，解析 `PROD_*` / `DEV_*` 變數
- `sshToolUtil.sh` 依 ENV 決定 SSH 到正式機（132.x）或測試機（131.x）

---

## 4. 現有專案環境配置

> 資料來源：`tools/*/config.sh`　　SSH 路由：`utils/sshToolUtil.sh`

### 機器資訊

| 環境 | IP | SSH 金鑰 | 用途 |
|------|-----|---------|------|
| prod | `132.145.125.250` | `~/.ssh/prod.pem` | 正式機 |
| dev  | `131.186.44.40`   | `~/.ssh/dev.pem`  | 測試機 |

SSH 使用者：`tkb0001662`

---

### 4.1 tv（`tools/tv/`）

`IMAGE_KEYWORD=tkbtv`　　`script_name=tv`（DB name=`tkbtv`）

**正式機 prod（132.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `tv` |
| Green 容器 | `tv_test` |
| Blue Port | `8087` |
| Green Port | `8090` |
| nginx conf | `/etc/nginx/conf.d/tv/nginx-tv.conf` |
| live upstream | `tkbtv` |
| header upstream | `tkbtv_header_test` |
| deploy base | `/opt/docker_image/tkbtv` |
| health | `https://www.tkbtv.com.tw/front/toHeader.action` |
| image repo | `backend-prod` |

**測試機 dev（131.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `tv_test` |
| Green 容器 | 無 |
| Blue Port | `8090` |
| nginx conf | 無（dev 不做流量切換） |
| deploy base | `/opt/docker_image/` ⚠️ TODO |
| health | `https://www.tkbtv.com.tw/front/toHeader.action` |
| image repo | `backend-dev` |

---

### 4.2 go-api（`tools/go-api/`）

`IMAGE_KEYWORD=goapi`

**正式機 prod（132.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `go-api-test` |
| Green 容器 | `go-api-backup` |
| Blue Port | `8091` |
| Green Port | `8094` |
| nginx conf | `/etc/nginx/conf.d/goapi/nginx-go-re-pro.conf` |
| live upstream | `tkbgo_api_test` |
| header upstream | `tkbgo_api_backup` |
| deploy base | `/opt/docker_image/tkbgoapi` |
| health | `http://www.tkbgo.com.tw/api/v1/bookshop/book/pre-orders` |
| image repo | `backend-prod` |

**測試機 dev（131.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `go-api-test` |
| Green 容器 | 無 |
| Blue Port | `8091` |
| nginx conf | 無（dev 不做流量切換） |
| deploy base | `/opt/docker_image/` |
| health | `http://www.tkbgo.com.tw/api/v1/bookshop/book/pre-orders` |
| image repo | `backend-dev` |

---

### 4.3 go_nuxt（`tools/go_nuxt/`）

`IMAGE_KEYWORD=go_nuxt`

**正式機 prod（132.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `go_nuxt` `go_nuxt2` `go_nuxt3` |
| Green 容器 | `go_nuxt_backup` |
| Blue Port | `8333` |
| Green Port | `8334` |
| 其他 Blue Ports | `8335` `8336` |
| nginx conf | `/etc/nginx/conf.d/goapi/nginx-go-re-pro.conf` |
| live upstream | `tkbgo_nuxt` |
| header upstream | `tkbgo_nuxt_backup` |
| deploy base | `/opt/docker_image/go_nuxt` |
| health | `http://www.tkbgo.com.tw/` |
| image repo | `frontend-prod` |

**測試機 dev（131.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `go_nuxt` |
| Green 容器 | 無 |
| Blue Port | `8333` |
| nginx conf | 無（dev 不做流量切換） |
| deploy base | `/opt/docker_image` |
| image repo | `frontend-dev` |

---

### 4.4 player（`tools/player/`）

`IMAGE_KEYWORD=player`

**正式機 prod（132.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `player-api` |
| Green 容器 | `player-api-backup` |
| Blue Port | `8085` |
| Green Port | `8088` |
| nginx conf | `/etc/nginx/conf.d/player/nginx-player.conf` ⚠️ TODO |
| live upstream | `player` ⚠️ TODO |
| header upstream | `player_backup` ⚠️ TODO |
| deploy base | `/opt/docker_image/player` ⚠️ TODO |
| health | `https://www.tkbgo.com.tw/api/member/test` |
| image repo | `backend-prod` |

**測試機 dev（131.x）**

> ⚠️ player 未部署在 dev 機器，dev 相關欄位為空。

---

### 4.5 form-service-backend（`tools/form-service-backend/`）

`IMAGE_KEYWORD=form-service-backend`　　`prod_env=admin`（DB）

**正式機 prod（132.x）**

> ⚠️ form-service-backend 無正式機環境，PROD_* 欄位全部為空。

**測試機 dev（131.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `form-service-backend-blue` |
| Green 容器 | `form-service-backend-green` |
| Blue Port | `8100` |
| Green Port | `8101` |
| nginx conf | `/etc/nginx/nginx-form-service.conf` |
| live upstream | `form_service_backend` |
| header upstream | `form_service_backend_backup` |
| deploy base | `/opt/docker_image/form-service-backend` |
| health | `https://formservice.tkbtv.com.tw/front/toHeader.action` ⚠️ TODO 確認 backend health path |
| image repo | `backend-dev` |

---

### 4.6 form-service-frontend（`tools/form-service-frontend/`）

`IMAGE_KEYWORD=form-service-frontend`　　`prod_env=admin`、`default_branch=main`（DB）

**正式機 prod（132.x）**

> ⚠️ form-service-frontend 無正式機環境，PROD_* 欄位全部為空。

**測試機 dev（131.x）**

| 項目 | 值 |
|------|-----|
| Blue 容器 | `form-service-frontend-blue` |
| Green 容器 | `form-service-frontend-green` |
| Blue Port | `8110` ⚠️ TODO 確認 |
| Green Port | `8111` ⚠️ TODO 確認 |
| nginx conf | `/etc/nginx/nginx-form-service.conf` |
| live upstream | `form_service_frontend` |
| header upstream | `form_service_frontend_backup` |
| deploy base | `/opt/docker_image/form-service-frontend` |
| health | `https://formservice.tkbtv.com.tw/front/toHeader.action` |
| image repo | `frontend-dev` |

---

### 4.7 各專案環境部署矩陣

| 專案 | 正式機(prod) | 測試機(dev) | Blue Port(prod) | Green Port(prod) | Blue Port(dev) |
|------|:-----------:|:-----------:|:---------------:|:----------------:|:--------------:|
| tv | ✅ | ✅ | 8087 | 8090 | 8090 |
| go-api | ✅ | ✅ | 8091 | 8094 | 8091 |
| go_nuxt | ✅ | ✅ | 8333 | 8334 | 8333 |
| player | ✅ | ❌ | 8085 | 8088 | — |
| form-service-backend | ❌ | ✅ | — | — | 8100 |
| form-service-frontend | ❌ | ✅ | — | — | 8110 |

---

### 4.8 待確認事項（⚠️ TODO）

| 專案 | 待確認項目 |
|------|----------|
| tv | dev `DEPLOY_BASE` 路徑（目前 `/opt/docker_image/` 缺尾綴） |
| player | nginx conf 路徑、upstream 名稱、compose 目錄是否正確 |
| form-service-backend | backend health check path（`/front/toHeader.action` 是 frontend path） |
| form-service-frontend | Blue/Green port 是否為 8110/8111 |

---

## 5. 後端 API 路徑整理

### 5.1 版本管理 `/api/version`

| Method | 路徑 | 說明 |
|--------|------|------|
| GET | `/next` | 取下一個版號（`?projectName=&env=`） |
| GET | `/check-deployable` | 部署前檢查，支援自訂 env（如 `admin`） |
| POST | `/saveNewVersion` | 寫入新版本紀錄 |
| GET | `/page` | 版本歷史分頁查詢 |
| GET | `/getReleaseNote` | 取 GitLab MR Release Note |
| PATCH | `/editRemark` | 修改版本備註 |
| PUT | `/updateJenkinsBuildId` | 更新 Jenkins Build ID |

### 5.2 監控 `/api/monitor`

| Method | 路徑 | 說明 |
|--------|------|------|
| GET | `/healthCheck` | 容器健康檢查 |
| GET | `/getTraffic` | 查詢 nginx 流量方向 |
| POST | `/switchTraffic` | 切換 nginx 流量（Blue/Green） |
| POST | `/restartService` | 重啟容器 |
| GET | `/getImageVersion/{type}` | 取 current/history image 清單 |
| GET | `/getRollBackImageVersion` | 取退版可選版本（分 prod/backup bucket） |
| GET | `/deleteImage` | 刪除 Docker image |
| POST | `/renewImage` | 退版（更新 image tag） |
| GET | `/page` | 操作 Log 分頁查詢 |

### 5.3 專案管理 `/api/project`

| Method | 路徑 | 說明 |
|--------|------|------|
| GET | `/list` | 取啟用中專案清單（前端通用） |
| GET | `/list/all` | 取全部專案（管理頁） |
| POST | `/` | 新增專案 |
| PUT | `/` | 修改專案 |
| DELETE | `/{id}` | 停用 / 刪除專案 |

### 5.4 GitLab MR `/api/mr`

| Method | 路徑 | 說明 |
|--------|------|------|
| GET | `/review/page` | MR AI Review 分頁查詢 |
| GET | `/review/detail` | 取單筆 MR Review 詳情 |
| POST | `/review/scan/{project}` | 掃描指定專案 MR |
| POST | `/review/scan/all` | 掃描所有專案 MR |
| GET | `/mrs/between` | 查兩版號間已 merge 的 MR 清單 |

---

## 6. 前端頁面架構

| 頁面路由 | 元件 | 主要功能 |
|---------|------|---------|
| `/version/history` | `version/history/index.vue` | 版本歷史、觸發部署、Console Log、Image 管理 |
| `/monitor/detail` | `monitor/detail/index.vue` | Monitor Detail：健康/流量/Image/退版（單一專案） |
| `/monitor/overview` | `monitor/overview/index.vue` | Monitor Overview：多專案總覽 Dashboard |
| `/mr/review` | `mr/review/index.vue` | MR AI Code Review 查詢與詳情 |
| `/system/log` | `system/log_query/index.vue` | 操作 Log 查詢（Shell 執行結果） |
| `/system/project` | `system/ProjectManage.vue` | 專案管理 CRUD（`project_config` 表） |
| `/` | `homepage/index.vue` | 首頁：各專案最新版 Release Note |

### 6.1 共用 API helpers

- `src/api/project.js` → `getProjectList()`：所有頁面動態載入專案清單
- `src/api/jenkins.js` → `triggerJenkinsBuild(projectName, env, branch, type, jobNameOverride, tokenOverride)`
- `resolveEnv(projectName, uiEnv)`：UI `prod`/`dev` → 實際 `prodEnv`/`devEnv`（version/history 用）
- `parseImageLine(line)`：從 Docker image 路徑提取 `projectName` / `nodeType` / `repoEnv` / `version`

---

## 7. 新增專案 SOP

### 步驟 1 — 插入 DB

```sql
INSERT INTO project_config
  (name, display_name, category, is_active, sort_order,
   has_prod, has_dev, script_name, image_keyword,
   prod_env, dev_env, jenkins_job_name, jenkins_token,
   jenkins_pipeline_name, default_branch)
VALUES
  ('my-service', 'My Service', 'backend', 1, 99,
   1, 1, NULL, NULL,
   NULL, NULL, NULL, NULL,
   NULL, NULL);
-- 若名稱特殊才填 prod_env, jenkins_job_name 等覆蓋值
```

### 步驟 2 — 建立 tools 目錄

- 在 `tools/` 建立 `{scriptName}/` 目錄（`scriptName` = `name`，除非另設）
- 複製 `tools/template/config.sh`，填入容器名稱、nginx conf 路徑、deploy 路徑
- 不需修改任何 `common/` 腳本

### 步驟 3 — Jenkins 設定

- 建立 `{type}-{env}` Job（或填入 `jenkins_job_name` 自訂）
- 設定 Token（或填入 `jenkins_token` 自訂）
- Pipeline 名稱預設 `{type}-pipeline`，如不同填入 `jenkins_pipeline_name`

### 步驟 4 — 重啟確認

- DB 欄位資料變更：**不需後端重啟**（MyBatis-Plus 即時查詢）
- Java entity 新增欄位：需重新編譯並部署後端
- 前端 `vite build` 無需特別動作（動態從 API 取資料）

### 步驟 5 — 驗證

- `/api/project/list` 確認新專案出現，所有 key 正確
- 前端 version/history 頁面確認新專案可在下拉選單選取
- Monitor Overview 確認依 `has_prod`/`has_dev` 顯示正確
- 觸發一次測試部署，確認 Jenkins Job 可被調用

---

## 8. 已實作功能清單

| 功能模組 | 狀態 | 說明 |
|---------|------|------|
| Config-driven Shell 架構 | ✅ 完成 | 所有專案共用 `common/` 腳本，各自維護 `config.sh` |
| DB 驅動專案清單 | ✅ 完成 | 所有頁面從 `/api/project/list` 動態取得，不 hardcode |
| 自訂 Jenkins env（`admin`） | ✅ 完成 | `prod_env`/`dev_env` 欄位，`resolveEnv()` / `isDevEnv()` 轉換 |
| 自訂 Jenkins Job 名稱 / Token | ✅ 完成 | `jenkins_job_name` / `jenkins_token` 欄位 |
| 部署類型 正式 / 備援選擇器 | ✅ 完成 | 前端 `category=frontend` + `env=prod` 時顯示 |
| `checkDeployable` 自訂 env | ✅ 完成 | 支援 `admin` 等非標準 env 名稱 |
| `GitlabMrServiceImpl` env 正規化 | ✅ 完成 | `isDevEnv` / `isProdEnv` 動態判斷，不 hardcode `prod`/`dev` |
| Image 分類（test / admin suffix） | ✅ 完成 | `parseImageLine` 從 repo 前綴提取 `repoEnv` 作 `nodeType` |
| Image 刪除全路徑修正 | ✅ 完成 | `selectedImageVersions` 改存 `fullString` |
| Monitor Overview DB 驅動 | ✅ 完成 | `hasProd`/`hasDev` 過濾，不 hardcode 專案名稱 |
| ProjectManage CRUD 頁面 | ✅ 完成 | 前端可新增 / 修改 / 停用專案 |
| Homepage Release Note 動態化 | ✅ 完成 | 依 DB `hasProd`/`hasDev` 產生 tab，顯示各專案最新版 |
| `jenkins_pipeline_name` 欄位 | ✅ 完成 | Pipeline Job 名稱可客製化 |
| `default_branch` 欄位 | ✅ 完成 | 部署分支可客製化（`master` / `main`） |

---

## 9. 已知問題修復記錄

| 問題現象 | 根本原因 | 解決方案 |
|---------|---------|---------|
| form-service `env=prod` 仍呼叫 prod API | 後端 JVM 未更新 `ProjectEntity`（新欄位未被序列化） | 重新編譯部署後端 |
| image 分類忽略 `backend-admin` 前綴 | bucket 邏輯只檢查 `-prod` / `-dev` / `-backup` 後綴 | `parseImageLine` 從 repo 前綴取 `repoEnv` 作 `nodeType` |
| `form-service-frontend-test` 識別錯誤 | `NODE_SUFFIXES` 不含 `test` | 加入 `test` / `blue` / `green` 到 `NODE_SUFFIXES` |
| 刪除 image 路徑錯誤（`frontend-dev`） | `buildRemoveImagePath` hardcode `envSuffix` | 改存 / 傳 `fullString`，直接使用完整路徑 |
| `checkDeployable` 回傳「未知環境設定」 | `"prod".equals("admin")` 永遠為 false | 注入 `ProjectService`，動態取 `actualProdEnv` |
| GitlabMr `releasedProd` 標記不寫入 | `"prod".equalsIgnoreCase("admin")` false | `isProdEnv()` / `isDevEnv()` helper 動態判斷 |

---

## 10. 未來規劃：靈活配置管理

目前每個專案的部署拓撲（容器名稱、Port、nginx conf 路徑、upstream 等）儲存於 `tools/{project}/config.sh`，需 SSH 到機器修改。未來目標：讓這些設定也能從前端 CRUD，儲存於 DB，並可自動同步到遠端 `config.sh`。

### 10.1 規劃新 DB 表：`project_env_config`

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT AUTO | 主鍵 |
| `project_id` | BIGINT | FK → `project_config.id` |
| `env_type` | VARCHAR(20) | `prod` / `dev` |
| `blue_containers` | VARCHAR(500) | Blue 容器名稱（逗號分隔） |
| `green_containers` | VARCHAR(500) | Green/Backup 容器名稱 |
| `blue_port` | INT | Blue 節點 Port |
| `green_port` | INT | Green/Backup 節點 Port |
| `nginx_conf_path` | VARCHAR(500) | nginx conf 完整路徑 |
| `nginx_live_upstream` | VARCHAR(200) | 正式流量 upstream 名稱 |
| `nginx_header_upstream` | VARCHAR(200) | 測試 Header 流量 upstream 名稱 |
| `deploy_base_path` | VARCHAR(500) | docker-compose 目錄路徑 |
| `health_host` | VARCHAR(200) | 健康檢查 host |
| `health_path` | VARCHAR(200) | 健康檢查 API path |
| `health_scheme` | VARCHAR(10) | `http` / `https` |

### 10.2 前端配置管理頁面（規劃）

- 在 `ProjectManage` 頁面擴充「部署拓撲」子設定區塊
- 每個專案可展開查看 prod/dev 的容器名稱、Port、nginx 路徑
- 修改後儲存到 DB，並可選擇「同步到遠端 config.sh」
- 同步操作由後端呼叫 Shell，透過 SSH 覆寫遠端 `config.sh`

### 10.3 後端配合調整

- 新增 `ProjectEnvConfigController` / `Service` / `Mapper` / `Entity`
- 新增 `/api/project/{id}/env-config` CRUD 端點
- 新增 `/api/config/sync` API：後端 SSH 到遠端機器，覆寫 `config.sh`
- Shell 腳本：支援從後端傳入覆蓋值，或由後端直接生成 `config.sh` 內容後 scp 上傳

### 10.4 nginx conf 管理（規劃）

- 前端可顯示遠端 nginx conf 原始內容（後端 SSH `cat` 回傳）
- 支援修改 upstream port、server_name 後 reload nginx
- 後端提供 `/api/nginx/read`、`/api/nginx/write`、`/api/nginx/reload` 端點
- 寫入前執行 `nginx -t` 語法驗證，通過才執行 reload

> ⚠️ **注意**：直接修改 nginx conf 和 config.sh 具有高風險，需加入：操作確認對話框、操作記錄（audit log）、自動備份機制（修改前備份原始檔）。

### 10.5 架構演進藍圖

| 階段 | 目標 | 預計範圍 |
|------|------|---------|
| 現階段（完成） | DB 驅動，消除 hardcode | `project_config` 所有欄位，前端 CRUD 專案 |
| Phase 2 | 部署拓撲可視化 | `project_env_config` 表，容器 / Port / 路徑 DB 化 |
| Phase 3 | 遠端配置同步 | 前端修改 → 後端 SSH → 覆寫 `config.sh` / nginx conf |
| Phase 4 | 完整 GitOps（長期） | 配置變更自動提交 Git，Code Review 後再套用到機器 |
