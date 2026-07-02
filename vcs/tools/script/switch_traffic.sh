#!/bin/bash
# ---------------------------------------------------------
#   switch_traffic.sh    blue | green [header]
#   blue                 → 將流量切 BLUE (正式)
#   green                → 將流量切 GREEN (備援)
#   header               → 只切 tkbtv_header_test
#   回傳碼：
#                 0  = 成功
#                 10 = 跳過（已經在此線）
#                 1  = 失敗
# ---------------------------------------------------------
TARGET=$1     # blue | green
HEADER_MODE=$2 # header | 空值

# ============ 修改 =======================
CONF="/etc/nginx/nginx-form-service.conf"
BLUE_PORT=8100
GREEN_PORT=8101
# =========================================

usage()
{
    cat <<EOF
Usage: switch_traffic.sh [PARAM]

Known values for PARAM are:

  參數1 = [blue|green] : Required
  參數2 = [header]     : Optional
  如果沒有加上 參數2 將會切換正式分流
EOF
}

# 檢查
if [[ "$TARGET" != "blue" && "$TARGET" != "green" ]]; then
  usage
  exit 99
fi

# 判斷要啟藍或綠的流量
if [[ "$TARGET" == "blue" ]]; then
    ACTIVE=$BLUE_PORT
    DOWN=$GREEN_PORT
else
    ACTIVE=$GREEN_PORT
    DOWN=$BLUE_PORT
fi

# Header only
if [[ "$HEADER_MODE" == "header" ]]; then
    traffic="header"
    # 修改
    BLOCKS=("form_service_backend_backup")
else
    traffic="正式"
    # 修改
    BLOCKS=("form_service_backend")
fi

# 檢查是否已在該線
BLOCK_TO_CHECK="${BLOCKS[0]}"
if sudo sed -n "/upstream[[:space:]]\+${BLOCK_TO_CHECK}[[:space:]]*{/,/}/p" "$CONF" \
   | grep -q "server 127.0.0.1:${ACTIVE}[[:space:]]*;" ; then
    echo "[SKIP] ${traffic} 流量已在 (${TARGET^^})，無需切換"
    exit 10
fi

# 執行替換
for block in "${BLOCKS[@]}"; do
  sudo sed -i "/upstream[[:space:]]\+${block}[[:space:]]*{/,/}/ {
    s|server 127.0.0.1:${DOWN}[[:space:]]*;|server 127.0.0.1:${DOWN} down;|;
    s|server 127.0.0.1:${ACTIVE}[[:space:]]*down;|server 127.0.0.1:${ACTIVE};|;
  }" "$CONF"
done

if sudo nginx -t; then
    sudo nginx -s reload
    echo "[OK] ${traffic} 流量切換到 (${TARGET^^})"
    echo "   Active Ports: ${ACTIVE[*]}"
    exit 0
else
    echo "[ERROR] Nginx config test failed!"
    exit 1
fi