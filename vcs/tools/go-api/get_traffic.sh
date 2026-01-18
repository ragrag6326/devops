#!/bin/bash

# ---------------------------------------------------------
#   get_traffic.sh       live | header
#   live                  → 正式流量
#   header                → header 流量
#   回傳碼：
#                       0  = live
#                       1  = backup
# ---------------------------------------------------------

# 檢查模式 (live 或 header)
MODE=$1

# ======= 參數修改 =======
NGINX_PATH="/etc/nginx/conf.d/goapi/nginx-go-re-pro.conf"
BLUE_PORT=8091
LIVE_BLOCK=tkbgo_api_test
HEADER_BLOCK=tkbgo_api_backup
# =======================

usage()
{
    cat <<EOF
Usage: get_traffic.sh [PARAM]

Known values for PARAM are:

  參數1 = [live|header] : Required
EOF
}

# 檢查
if [[ "$MODE" != "live" && "$MODE" != "header" ]]; then
  usage
  exit 99
fi

source /opt/vcs/tools/utils/sshToolUtil.sh

check_traffic_status() {
    local UPSTREAM_NAME=$1
    # 找到，回傳 blue；否則回傳 green
    local result=$(ssh_function "grep -A 5 'upstream ${UPSTREAM_NAME} {' ${NGINX_PATH} | grep 'server' | grep -v 'down' | grep -q ${BLUE_PORT} && echo blue || echo green")
    echo "$result"
}

if [[ "$MODE" == 'live' ]]; then
    TRAFFIC=$(check_traffic_status "$LIVE_BLOCK")
    echo "Current_Live_Traffic: $TRAFFIC"

    if [[ "$TRAFFIC" == "blue" ]]; then
        exit 0  # 0 代表 Blue (正式)
    else
        exit 1  # 1 代表 Green (備援)
    fi

elif [[ "$MODE" == 'header' ]]; then
    TRAFFIC=$(check_traffic_status "$HEADER_BLOCK")
    echo "Current_Header_Traffic: $TRAFFIC"

    # 修正：這裡應該判斷 $TRAFFIC 而不是之前的 $Live_Traffic
    if [[ "$TRAFFIC" == "blue" ]]; then
        exit 0  # 0 代表 Blue (正式)
    else
        exit 1  # 1 代表 Green (備援)
    fi

else
    echo "錯誤: 未知的模式 '$MODE'，請使用 live 或 header"
    exit 2
fi