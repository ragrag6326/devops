#!/bin/bash
# ============================================================
#  form-service-frontend 專案配置  (由 common/init.sh source)
#  主要運行於 dev 機器 (131.x)
# ============================================================

# ---- 正式機 (prod / 132.x) ---- form-service 無 prod 環境
PROD_BLUE_CHECK_PORTS=(8110)
PROD_GREEN_CHECK_PORTS=(8111)
PROD_HEALTH_HOST="formservice.tkbtv.com.tw"
PROD_HEALTH_SCHEME="http"
PROD_HEALTH_PATH=""

PROD_NGINX_CONF="/etc/nginx/nginx-form-service.conf"
PROD_TRAFFIC_BLUE_PORT="8110"
PROD_LIVE_UPSTREAM="form_service_frontend"
PROD_HEADER_UPSTREAM="form_service_frontend_backup"

PROD_BLUE_CONTAINERS="form-service-frontend-blue"
PROD_GREEN_CONTAINERS="form-service-frontend-green"

PROD_DEPLOY_BASE="/opt/docker_image/form-service-frontend"
PROD_SWITCH_SCRIPT=""${DEV_DEPLOY_BASE}/script/switch_traffic.sh""
PROD_IMAGE_REPO="frontend-admin"
BLUE_ENV_KEY="BLUE_VERSION"
GREEN_ENV_KEY="GREEN_VERSION"

# ---- 測試機 (dev / 131.x) ----
DEV_BLUE_CHECK_PORTS=(8112)
DEV_GREEN_CHECK_PORTS=()
DEV_HEALTH_HOST="formservice.tkbtv.com.tw"
DEV_HEALTH_SCHEME="http"
DEV_HEALTH_PATH="/"

DEV_NGINX_CONF="/etc/nginx/nginx-form-service.conf"
DEV_TRAFFIC_BLUE_PORT=""
DEV_LIVE_UPSTREAM=""
DEV_HEADER_UPSTREAM=""

DEV_BLUE_CONTAINERS="form-service-frontend-test"
DEV_GREEN_CONTAINERS=""

DEV_DEPLOY_BASE="/opt/docker_image/form-service-frontend"
DEV_SWITCH_SCRIPT=""
DEV_IMAGE_REPO="frontend-admin"

# ---- 共用 ----
IMAGE_KEYWORD="form-service-frontend"

SSH_ENV_OVERRIDE="dev"