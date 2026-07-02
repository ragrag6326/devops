#!/bin/bash
# Usage: get_traffic.sh [prod|dev] [project] [live|header]
# exit 0 = BLUE_ACTIVE, exit 1 = GREEN_ACTIVE
usage() { echo "Usage: $0 [prod|dev] [project] [live|header]"; exit 99; }

ENV=$1; PROJECT=$2; MODE=$3
[[ "$ENV"  != "prod" && "$ENV"  != "dev"      ]] && usage
[[ -z "$PROJECT" ]]                                && usage
[[ "$MODE" != "live" && "$MODE" != "header" ]] && usage

source /opt/vcs/tools/common/init.sh   # 解析 NGINX_CONF、LIVE_UPSTREAM 等

check_traffic_status() {
    local UPSTREAM=$1
    # grep -c 回傳匹配行數：>0 表示 blue port 在 active server 裡
    local count
    count=$(ssh_function "grep -A 5 'upstream ${UPSTREAM} {' ${NGINX_CONF} | grep 'server' | grep -v 'down' | grep -c ${TRAFFIC_BLUE_PORT}")
    [[ "$count" -gt 0 ]] && echo "blue" || echo "green"
}

[[ "$MODE" == "live"   ]] && TRAFFIC=$(check_traffic_status "$LIVE_UPSTREAM")
[[ "$MODE" == "header" ]] && TRAFFIC=$(check_traffic_status "$HEADER_UPSTREAM")

echo "Traffic[${MODE}]: $TRAFFIC"
[[ "$TRAFFIC" == "blue" ]] && exit 0 || exit 1
