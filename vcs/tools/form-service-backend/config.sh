#!/bin/bash
# ============================================================
#  form-service-backend 專案配置  (由 common/init.sh source)
#  主要運行於 dev 機器 (131.x)
# ============================================================

# ---- 正式機 (prod / 132.x) ---- form-service 無 prod 環境
PROD_BLUE_CHECK_PORTS=(8100)
PROD_GREEN_CHECK_PORTS=(8101)
PROD_HEALTH_HOST="formservice.tkbtv.com.tw"
PROD_HEALTH_SCHEME="http"
PROD_HEALTH_PATH="/api/v1/accounts/register"

PROD_NGINX_CONF="/etc/nginx/nginx-form-service.conf"
PROD_TRAFFIC_BLUE_PORT="8100"
PROD_LIVE_UPSTREAM="form_service_backend"
PROD_HEADER_UPSTREAM="form_service_backend_backup"

PROD_BLUE_CONTAINERS="form-service-backend-blue"
PROD_GREEN_CONTAINERS="form-service-backend-green"

PROD_DEPLOY_BASE="/opt/docker_image/form-service-backend"
PROD_SWITCH_SCRIPT=""${DEV_DEPLOY_BASE}/script/switch_traffic.sh" "
PROD_IMAGE_REPO="backend-admin"
BLUE_ENV_KEY="BLUE_VERSION"
GREEN_ENV_KEY="GREEN_VERSION"

# ---- 測試機 (dev / 131.x) ----
DEV_BLUE_CHECK_PORTS=(8102)
DEV_GREEN_CHECK_PORTS=()
DEV_HEALTH_HOST="formservice.tkbtv.com.tw"
DEV_HEALTH_SCHEME="http"
DEV_HEALTH_PATH="/api/v1/accounts/register"

DEV_NGINX_CONF="/etc/nginx/nginx-form-service.conf"
DEV_TRAFFIC_BLUE_PORT=""
DEV_LIVE_UPSTREAM=""
DEV_HEADER_UPSTREAM=""

DEV_BLUE_CONTAINERS="form-service-backend-test"
DEV_GREEN_CONTAINERS=""

DEV_DEPLOY_BASE=""
DEV_SWITCH_SCRIPT=""
DEV_IMAGE_REPO="backend-dev"

# ---- 共用 ----
IMAGE_KEYWORD="form-service-backend"