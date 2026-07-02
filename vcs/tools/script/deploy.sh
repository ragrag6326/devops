#!/bin/bash
# ---------------------------------------------------------
# deploy.sh blue | green
# 作用：
#   blue     → 部署正式線（同步 green 版本後啟動）
#   green    → 部署備援線（直接啟動）
#   回傳碼：
#            0 = 健康檢查成功
#            1 = 健康檢查失敗
# ---------------------------------------------------------
TARGET=$1   # blue / green

# ============ 修改 =======================
PROJECT_NAME=form-service-backend
BLUE_PORT=8100
GREEN_PORT=8101
HEADER_HOST="www.tkbtv.com.tw"
# HEADER_TEST_USER="TRUE"
# =========================================

BASE_PATH="/opt/docker_image/${PROJECT_NAME}"
SCRIPT_PATH="${BASE_PATH}/script"
ENV_FILE="${BASE_PATH}/.env"
SWITCH_TRAFFIC_SCRIPT="switch_traffic.sh"


SERVICE_BLUE="${PROJECT_NAME}-blue"
SERVICE_GREEN="${PROJECT_NAME}-green"


usage()
{
    cat <<EOF
Usage: deploy.sh [PARAM]

Known values for PARAM are:

  參數1 = [blue|green] : Required
EOF
}

if [[ "$TARGET" != "blue" && "$TARGET" != "green" ]]; then
  usage
  exit 99
fi

# 1. 讀取 .env 版本
BLUE_VERSION=$(grep '^BLUE_VERSION=' "$ENV_FILE" | cut -d '=' -f2)
GREEN_VERSION=$(grep '^GREEN_VERSION=' "$ENV_FILE" | cut -d '=' -f2)
echo "🔵 Blue 版本:  ${BLUE_VERSION}"
echo "🟢 Green 版本: ${GREEN_VERSION}"


# ---------------------------------------------------------
# 部署 BLUE 正式機
# ---------------------------------------------------------
if [[ "$TARGET" == "blue" ]]; then
  # 1. Blue 版本同步 Green 版本
  if [ "$BLUE_VERSION" != "$GREEN_VERSION" ]; then
      echo "🔁 將 BLUE 版本升級與 GREEN 一致"
      sudo sed -i "s|BLUE_VERSION=.*|BLUE_VERSION=${GREEN_VERSION}|g" "$ENV_FILE"
  else
      echo "✔️ BLUE 版本已經與 GREEN 相同，跳過同步版本"
  fi

  # 2. 更新 🔵 正式機
  cd "$BASE_PATH"
  sudo rm -rf ${BASE_PATH}/${TARGET}/webapp

  echo "🔃 重啟 BLUE (${SERVICE_BLUE})..."
  docker compose up -d --force-recreate ${SERVICE_BLUE}
  sleep 2


  # 3. 將 (測試Header) 切換到 正式機(BLUE)
  echo "🔁 將 (測試Header) 切換到 正式機(BLUE)"
  [ -f "${SCRIPT_PATH}/${SWITCH_TRAFFIC_SCRIPT}" ] && "${SCRIPT_PATH}/${SWITCH_TRAFFIC_SCRIPT}" blue header
  # 修改
  HEALTH_URL="http://localhost:${BLUE_PORT}/api/v1/login"
fi

# ---------------------------------------------------------
# 部署 GREEN 測試機
# ---------------------------------------------------------
if [[ "$TARGET" == "green" ]]; then

  # 1. 更新 🟢 備援機
  cd "$BASE_PATH"
  sudo rm -rf ${BASE_PATH}/${TARGET}/webapp

  echo "🔃 重啟 GREEN 備援線： (${SERVICE_GREEN})"
  docker compose up -d --force-recreate ${SERVICE_GREEN}
  sleep 2

  # Header流量切 GREEN
  echo "🔁 將 (測試Header) 切換到 正式機(GREEN)"
  [ -f "${SCRIPT_PATH}/${SWITCH_TRAFFIC_SCRIPT}" ] && "${SCRIPT_PATH}/${SWITCH_TRAFFIC_SCRIPT}" green header
  # 修改
  HEALTH_URL="http://localhost:${GREEN_PORT}/api/v1/login"
fi

# ---------------------------------------------------------
#  🔵 或 🟢 健康檢查
# ---------------------------------------------------------