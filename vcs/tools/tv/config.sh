#!/bin/bash
# ============================================================
#  tv 專案配置  (由 common/init.sh source)
# ============================================================

# ---- 正式機 (prod / 132.x) ----
PROD_BLUE_CHECK_PORTS=(8087)
PROD_GREEN_CHECK_PORTS=(8090)
PROD_HEALTH_HOST="www.tkbtv.com.tw"
PROD_HEALTH_SCHEME="https"
PROD_HEALTH_PATH="/front/toHeader.action"

PROD_NGINX_CONF="/etc/nginx/conf.d/tv/nginx-tv.conf"
PROD_TRAFFIC_BLUE_PORT="8087"
PROD_LIVE_UPSTREAM="tkbtv"
PROD_HEADER_UPSTREAM="tkbtv_header_test"

PROD_BLUE_CONTAINERS="tv"
PROD_GREEN_CONTAINERS="tv_test"

PROD_DEPLOY_BASE="/opt/docker_image/tkbtv"
PROD_SWITCH_SCRIPT="${PROD_DEPLOY_BASE}/script/switch_traffic.sh"
PROD_IMAGE_REPO="backend-prod"
BLUE_ENV_KEY="BLUE_VERSION"
GREEN_ENV_KEY="GREEN_VERSION"

# ---- 測試機 (dev / 131.x) ----
DEV_BLUE_CHECK_PORTS=(8090)
DEV_GREEN_CHECK_PORTS=()
DEV_HEALTH_HOST="test.tkbtv.com.tw"
DEV_HEALTH_SCHEME="https"
DEV_HEALTH_PATH="/front/toHeader.action"

DEV_NGINX_CONF=""
DEV_TRAFFIC_BLUE_PORT=""
DEV_LIVE_UPSTREAM=""
DEV_HEADER_UPSTREAM=""

DEV_BLUE_CONTAINERS="tv_test"
DEV_GREEN_CONTAINERS=""

DEV_DEPLOY_BASE="/opt/docker_image/"
DEV_SWITCH_SCRIPT=""
DEV_IMAGE_REPO="backend-dev"

# ---- 共用 ----
IMAGE_KEYWORD="tkbtv"