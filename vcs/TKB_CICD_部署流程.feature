# =====================================================================
#  TKB CI/CD 部署流程 — Gherkin 規格
#  依實際腳本行為整理（deploy.git v2 / tools / tv_pipeline.groovy）
#  參考：TKB_CICD_系統說明文檔.md
# =====================================================================

Feature: Jenkins 帶參數建置的共同前置流程
  所有部署（不分環境）都先走這段初始化

  Background:
    Given Jenkins Job 已設定固定參數 PROJECT_NAME 與 BRANCH
    And Job 的 Shell 內已設定 TYPE（backend/frontend）與 PROJECT_ENV（dev/local/prod/admin）

  Scenario: 初始化與版號取得
    Given Jenkins 觸發 buildWithParameters
    When init/init.sh 執行
    Then 從 config/remote_gitlab_repo.json 以 TYPE + PROJECT_NAME 查出 gitlab_repo_url
    And 目標機器 IP 先查 project_deploy.json 的 envs.{ENV}.vmIP（專案特例）、否則用 vmIP.json 的環境預設
    And 向 VCS 平台 API 取得下一個版號 VERSION（1.0.x 自動遞增）
    And git clone 專案原始碼並進入 category/{PROJECT_NAME}/{PROJECT_ENV}/

  Scenario: 查表決定建置方式與部署樣板
    Given init 完成
    When genericDeploy.sh 執行
    Then 從 project_deploy.json 的 envs.{PROJECT_ENV} 取得 buildType、sshUser、java/npm 版本
    And IMAGE_NAME 組成 {TYPE}-{ENV}/{PROJECT_NAME}，前端帶 NODE_TYPE 時加上 -prod / -backup 後綴
    And 部署樣板使用 envs.{ENV}.templateOverride，未設定則用 {ENV}DeployTemplate.sh

  Scenario: 對照表缺必要設定
    Given project_deploy.json 內該專案缺少 envs.{ENV}.buildType 或 sshUser
    When genericDeploy.sh 查表
    Then 立即以明確錯誤訊息中止，不進入 build

# =====================================================================

Feature: 測試機部署（PROJECT_ENV = dev）
  單容器直接換版，不分正/備援

  Scenario: 更新測試機 image
    Given 專案已設定 devMachine 區塊（containerName / versionFile / cleanupPaths）
    When devDeployTemplate.sh 執行
    Then 依 buildType 打包（maven jar / war / npm .output）並套用 template/dev/{PROJECT_NAME}_Dockerfile
    And docker build 出 backend-dev/{PROJECT_NAME}:{VERSION} 並壓縮傳輸至測試機 load
    And 依 devMachine.versionFile 改 docker-compose.yml 的 image tag 或 .env 的 versionEnvKey
    And 依 devMachine.cleanupPaths 清除殘留檔案後 docker compose up -d --force-recreate {containerName}
    And 通知 VCS 標記部署 SUCCESS

  Scenario: 測試機部署失敗自動退版
    Given 部署過程任一階段回傳非 0（DEPLOY_FAILED=true）
    When 部署腳本結束
    Then 依 devMachine 設定將版本 sed 回 OLD_VERSION 並重啟（含 useProjectSubdir / versionFile=env 的專案）
    And 移除傳輸失敗的 image 與 .gz 檔
    And 通知 VCS 標記 FAIL 並發送 n8n 通知
    But 若 OLD_VERSION 為空則跳過退版，避免把版本 sed 成空字串

# =====================================================================

Feature: 正式後端部署（藍綠、正/備援同版）
  Jenkins 只推 GREEN，藍綠切換與版本同步由機器端腳本＋pipeline 完成

  Scenario: 更新正式後端的 image
    Given 更新正式後端的 image
    When 專案是 tkbgoapi（或其他 backend prod 專案）
    Then Jenkins buildWithParameters 走 prodDeployTemplate.sh：build → 傳輸 → load
    And 只將機器上 .env 的 GREEN_VERSION 改成新版號（BLUE_VERSION 不動）

  Scenario: pipeline 藍綠上線（tv_pipeline.groovy 流程）
    Given buildWithParameters 已完成、GREEN_VERSION 為新版號
    When Jenkins pipeline 執行
    Then 先檢查機器上 deploy.sh / rollback.sh / switch_traffic.sh 齊全
    And 執行 deploy.sh green 部署備援線並跑健康檢查
    And 暫停等待人工核可（GREEN 線驗證）
    When 我核可通過
    Then 執行 switch_traffic.sh green 將正式流量切到 GREEN
    And 執行 deploy.sh blue —— 腳本內部先將 BLUE_VERSION 同步成 GREEN_VERSION 再重啟 BLUE
    And 執行 switch_traffic.sh blue 將流量切回 BLUE，並以 green header 保留備援測試通道
    And 正/備援自此為同一版號（如 backend-prod/tkbgoapi:1.0.73）

  Scenario: GREEN 驗證不通過（人工 Abort / Timeout）
    Given pipeline 停在人工核可階段
    When 我拒絕或逾時
    Then 執行 rollback.sh：流量切回 BLUE、GREEN_VERSION 恢復成 BLUE_VERSION、重啟 GREEN
    And pipeline 標記失敗

# =====================================================================

Feature: 正式前端部署（藍綠、正/備援獨立版號）

  Scenario: 更新備援線（NODE_TYPE = backup）
    Given 專案 envs.prod.templateOverride = frontend-prodDeployTemplate.sh
    When Jenkins 以 NODE_TYPE=backup 建置
    Then npm 依對照表做前置處理（renameFiles / filenameFixes / 依 NODE_TYPE 替換 API URL）後 build
    And image 名稱帶 -backup 後綴（如 frontend-prod/go_nuxt-backup:{VERSION}）
    And 更新 .env 的 GREEN_VERSION 後執行機器端 deploy.sh green

  Scenario: 更新正式線（NODE_TYPE = prod）
    Given 同上前端專案
    When Jenkins 以 NODE_TYPE=prod 建置
    Then image 名稱帶 -prod 後綴、更新 BLUE_VERSION
    And 機器端依序：switch_traffic green（流量避開）→ deploy.sh blue → switch_traffic blue（切回）→ switch_traffic green header
    And 前端 deploy.sh blue 不做版本同步 —— BLUE/GREEN 是獨立建置與版號（如 go_nuxt-prod:1.0.120 / go_nuxt-backup:1.0.118）

  Scenario: 前端 rollback 不同步版號
    Given 前端專案（PROD_IMAGE_REPO 以 frontend 開頭）
    When rollback.sh 執行
    Then 只切流量回 BLUE 並重啟 GREEN
    But 不把 GREEN_VERSION 改成 BLUE 的版號（那會指到不存在的 -backup image tag）

# =====================================================================

Feature: 流量切換（switch_traffic.sh）

  Scenario: 以 port 區分藍綠（傳統機器）
    Given Blue/Green 健檢 port 不同（如 tkbgoapi 8091/8094）
    When switch_traffic.sh blue|green [header] 執行
    Then 對 nginx upstream 內 server 127.0.0.1:{port} 加/移除 down 標記
    And nginx -t 通過才 reload，失敗則報錯不套用
    And 已在目標線時回傳 10（SKIP）

  Scenario: 以容器名稱區分藍綠（gallery 型機器）
    Given Blue/Green port 相同、nginx upstream 走 docker network 直連容器
    When switch_traffic.sh 執行
    Then 改以 PROD_BLUE_CONTAINERS / PROD_GREEN_CONTAINERS 的容器名稱:port 切換 down 標記
    And nginx 指令使用 config.sh 的 PROD_NGINX_EXEC（容器內 nginx 如 docker exec nginx nginx，不可含 -it）

# =====================================================================

Feature: 特殊配置專案

  Scenario: form-service（admin 環境：正式配置、跑在測試機）
    Given 專案 envs.admin 設定 templateOverride（走 prod 流程）
    And vmIP.json 的 admin 指向測試機 131.186.44.40
    When 以 PROJECT_ENV=admin 建置
    Then 走正式部署流程但部署到測試機
    And 前端 build 時依 NODE_TYPE 把 API URL 替換為 form-service-backend-blue/green/test 容器位址

  Scenario: 部署到獨立機器的專案（gallery 型）
    Given vmIP.json（或 envs.{ENV}.vmIP）已新增 gallery 機器 IP
    And tools/common/ssh_hosts.json 已加 gallery 且金鑰放在 /opt/vcs/tools/key/gallery.pem
    And gallery 機器已完成：authorized_keys 裝入公鑰、NOPASSWD sudo、docker compose 可用
    When 以該環境建置
    Then 流程與一般 prod 相同，僅目標機器不同

# =====================================================================

Feature: VCS 平台的遠端操作（監控 / 健康檢查 / 初始化）

  Scenario: 健康檢查
    Given VCS 後端（容器內）呼叫 tools/common/healthcheck.sh {env} {project} {blue|green}
    When 腳本執行
    Then 環境名稱以 ssh_hosts.json 驗證（jq → python3 → 純 bash 三層解析 fallback）
    And 依 config.sh 的 PROD_*/DEV_* 取得健檢 port 與 HOST header
    And SSH 至目標機器 curl localhost:{port}，5 次內見 200 即為健康

  Scenario: 初始化新專案的機器環境
    Given /system/project 對專案執行「初始化」
    When init_project.sh {sshEnv} {project} {scriptName} [initType] 執行
    Then sshEnv 以 ssh_hosts.json 驗證（任意機器）、initType 僅允許 prod|dev（流程種類）
    And 在目標機器建立 /opt/docker_image/{project}/、.env、script/ 並由模板生成三支腳本
    And 同步 tools/{scriptName}/config.sh 至遠端

# =====================================================================

Feature: 新增專案 / 新增機器（設定面）

  Scenario: 新增部署專案
    Given 在 /system/deploy-registry 開啟「新增專案精靈」
    When 填妥 PROJECT_NAME、TYPE、repo URL、envs 設定（表單或 JSON）、各環境 Dockerfile
    Then 以單一 GitLab commit 原子寫入 project_deploy.json + remote_gitlab_repo.json + template/{ENV}/{NAME}_Dockerfile
    And Jenkins 下次 buildWithParameters 即套用，不需修改任何 shell

  Scenario: 新增目標機器
    Given 在「機器 IP」分頁新增環境與 IP 並勾選同步 ssh_hosts.json
    When 儲存
    Then vmIP.json commit 回 deploy.git，ssh_hosts.json 同步寫入 VCS 主機
    And 我仍需手動：放金鑰至 /opt/vcs/tools/key/{env}.pem（600）、在目標機器裝公鑰與 NOPASSWD sudo
