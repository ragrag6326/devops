#!/bin/bash
# ============================================================
#  go-api 專案配置  (由 common/init.sh source)
# ============================================================

# ---- 正式機 (prod / 132.x) ----
PROD_BLUE_CHECK_PORTS=(8091)
PROD_GREEN_CHECK_PORTS=(8094)
PROD_HEALTH_HOST="www.tkbgo.com.tw"
PROD_HEALTH_SCHEME="http"
PROD_HEALTH_PATH="/api/v1/bookshop/book/pre-orders"

PROD_NGINX_CONF="/etc/nginx/conf.d/goapi/nginx-go-re-pro.conf"
PROD_TRAFFIC_BLUE_PORT="8091"
PROD_LIVE_UPSTREAM="tkbgo_api_test"
PROD_HEADER_UPSTREAM="tkbgo_api_backup"

PROD_BLUE_CONTAINERS="go-api-blue"
PROD_GREEN_CONTAINERS="go-api-green"

PROD_DEPLOY_BASE="/opt/docker_image/tkbgoapi"
PROD_SWITCH_SCRIPT=""${PROD_DEPLOY_BASE}/script/switch_traffic.sh""
PROD_IMAGE_REPO="backend-prod"
BLUE_ENV_KEY="BLUE_VERSION"
GREEN_ENV_KEY="GREEN_VERSION"

# ---- 測試機 (dev / 131.x) ----
DEV_BLUE_CHECK_PORTS=(8091)
DEV_GREEN_CHECK_PORTS=()
DEV_HEALTH_HOST="tvadmin.tkbtv.com.tw"
DEV_HEALTH_SCHEME="http"
DEV_HEALTH_PATH="/api/v1/bookshop/book/pre-orders"

DEV_NGINX_CONF=""
DEV_TRAFFIC_BLUE_PORT=""
DEV_LIVE_UPSTREAM=""
DEV_HEADER_UPSTREAM=""

DEV_BLUE_CONTAINERS="go-api-test"
DEV_GREEN_CONTAINERS=""

DEV_DEPLOY_BASE="/opt/docker_image/"
DEV_SWITCH_SCRIPT=""
DEV_IMAGE_REPO="backend-dev"

# ---- 共用 ----
IMAGE_KEYWORD="goapi"