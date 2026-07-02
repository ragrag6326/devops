#!/bin/bash
# ---------------------------------------------------------
# rollback.sh
# 作用：Green 測試失敗／人工 Abort／Timeout 時回復版本
#       1. 流量切回 Blue
#       2. 將 GREEN 版本同步成 BLUE 版本
#       3. 重啟 Green 容器
# ---------------------------------------------------------

# ============ 修改 =======================
PROJECT_NAME=form-service
BASE_PATH="/opt/docker_image/${PROJECT_NAME}"   # 部署根路徑
GREEN_SERVICE="${PROJECT_NAME}-green"
# =========================================

SCRIPT_PATH="${BASE_PATH}/script"
ENV_FILE="${BASE_PATH}/.env"
DOCKER_COMPOSE="${BASE_PATH}/docker-compose.yml"
SWITCH_TRAFFIC_SCRIPT="switch_traffic.sh"



echo "🔍 開始執行 rollback"
# ---------------------------------------------------------
# 1. 讀取 .env 版本
# ---------------------------------------------------------
if [ ! -f "${ENV_FILE}" ]; then
  echo "❌ 找不到 ${ENV_FILE} ，無法 rollback"
  exit 1
fi

BLUE_VERSION=$(grep '^BLUE_VERSION=' "$ENV_FILE" | cut -d '=' -f2)
GREEN_VERSION=$(grep '^GREEN_VERSION=' "$ENV_FILE" | cut -d '=' -f2)

echo "🔵 Blue 版本:  ${BLUE_VERSION}"
echo "🟢 Green 版本: ${GREEN_VERSION}"


# ---------------------------------------------------------
# 3. Green 版本同步 Blue 版本
# ---------------------------------------------------------
if [ "$BLUE_VERSION" != "$GREEN_VERSION" ]; then
    echo "🔁 將 GREEN 版本恢復成與 BLUE 一致 → ${BLUE_VERSION}"
    sed -i "s|GREEN_VERSION=.*|GREEN_VERSION=${BLUE_VERSION}|g" "$ENV_FILE"
else
    echo "✔️ GREEN 版本已經與 BLUE 相同，跳過同步版本"
fi

# ---------------------------------------------------------
# 4️. 重啟 Green 容器，恢復為 Blue 的版本
# ---------------------------------------------------------
echo "🔃 重啟 Green (${GREEN_SERVICE})..."
cd "$BASE_PATH"
docker compose up -d ${GREEN_SERVICE}
sleep 3

echo "🎯 rollback 完成"
echo "🔵 正式線   ：Blue ($BLUE_VERSION)"
echo "🟢 測試線   ：Green ($GREEN_VERSION — 已重置為 $BLUE_VERSION )"
exit 0