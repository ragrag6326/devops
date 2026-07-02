#!/bin/bash
# ============================================================
#  common/init.sh
#  所有 common/ 腳本在驗證 ENV、PROJECT 後 source 此檔
#  前置條件：$ENV 和 $PROJECT 已設定
#
#  執行後可用的變數（依 ENV 自動解析）：
#    BLUE_CHECK_PORTS  GREEN_CHECK_PORTS
#    HEALTH_HOST       HEALTH_SCHEME     HEALTH_PATH
#    NGINX_CONF        TRAFFIC_BLUE_PORT
#    LIVE_UPSTREAM     HEADER_UPSTREAM
#    BLUE_CONTAINERS   GREEN_CONTAINERS
#    DEPLOY_BASE       SWITCH_SCRIPT
#    IMAGE_REPO        IMAGE_KEYWORD
# ============================================================

CONFIG="/opt/vcs/tools/${PROJECT}/config.sh"
if [[ ! -f "$CONFIG" ]]; then
    echo "Error: config not found for project '${PROJECT}' (${CONFIG})"
    exit 1
fi
source "$CONFIG"
source /opt/vcs/tools/utils/sshToolUtil.sh "$ENV"

# 依 ENV 解析出實際使用的變數（PROD_* 或 DEV_*）
if [[ "$ENV" == "prod" ]]; then
    P="PROD"
else
    P="DEV"
fi

eval "BLUE_CHECK_PORTS=(\"\${${P}_BLUE_CHECK_PORTS[@]}\")"
eval "GREEN_CHECK_PORTS=(\"\${${P}_GREEN_CHECK_PORTS[@]}\")"
eval "HEALTH_HOST=\"\${${P}_HEALTH_HOST}\""
eval "HEALTH_SCHEME=\"\${${P}_HEALTH_SCHEME}\""
eval "HEALTH_PATH=\"\${${P}_HEALTH_PATH:-}\""
eval "NGINX_CONF=\"\${${P}_NGINX_CONF}\""
eval "TRAFFIC_BLUE_PORT=\"\${${P}_TRAFFIC_BLUE_PORT}\""
eval "LIVE_UPSTREAM=\"\${${P}_LIVE_UPSTREAM}\""
eval "HEADER_UPSTREAM=\"\${${P}_HEADER_UPSTREAM}\""
eval "BLUE_CONTAINERS=\"\${${P}_BLUE_CONTAINERS}\""
eval "GREEN_CONTAINERS=\"\${${P}_GREEN_CONTAINERS}\""
eval "DEPLOY_BASE=\"\${${P}_DEPLOY_BASE}\""
eval "SWITCH_SCRIPT=\"\${${P}_SWITCH_SCRIPT}\""
eval "IMAGE_REPO=\"\${${P}_IMAGE_REPO}\""
# IMAGE_KEYWORD 不分 env，直接從 config 取
