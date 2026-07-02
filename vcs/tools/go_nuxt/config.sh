#!/bin/bash
# ============================================================
#  go_nuxt 專案配置  (由 common/init.sh source)
# ============================================================

# ---- 正式機 (prod / 132.x) ----
PROD_BLUE_CHECK_PORTS=(8333 8335 8336)
PROD_GREEN_CHECK_PORTS=(8334)
PROD_HEALTH_HOST="www.tkbgo.com.tw"
PROD_HEALTH_SCHEME="http"
PROD_HEALTH_PATH=""

PROD_NGINX_CONF="/etc/nginx/conf.d/goapi/nginx-go-re-pro.conf"
PROD_TRAFFIC_BLUE_PORT="8333"
PROD_LIVE_UPSTREAM="tkbgo_nuxt"
PROD_HEADER_UPSTREAM="tkbgo_nuxt_backup"

PROD_BLUE_CONTAINERS="go_nuxt go_nuxt2 go_nuxt3"
PROD_GREEN_CONTAINERS="go_nuxt_backup"

PROD_DEPLOY_BASE="/opt/docker_image/go_nuxt"
PROD_SWITCH_SCRIPT="${PROD_DEPLOY_BASE}/script/switch_traffic.sh"
PROD_IMAGE_REPO="frontend-prod"
BLUE_ENV_KEY="BLUE_VERSION"
GREEN_ENV_KEY="GREEN_VERSION"

# ---- 測試機 (dev / 131.x) ----
DEV_BLUE_CHECK_PORTS=(8333)
DEV_GREEN_CHECK_PORTS=()
DEV_HEALTH_HOST="tvadmin.tkbtv.com.tw"
DEV_HEALTH_SCHEME="http"
DEV_HEALTH_PATH=""

DEV_NGINX_CONF=""
DEV_TRAFFIC_BLUE_PORT=""
DEV_LIVE_UPSTREAM=""
DEV_HEADER_UPSTREAM=""

DEV_BLUE_CONTAINERS="go_nuxt"
DEV_GREEN_CONTAINERS=""

DEV_DEPLOY_BASE="/opt/docker_image"
DEV_SWITCH_SCRIPT=""
DEV_IMAGE_REPO="frontend-dev"

# ---- 共用 ----
IMAGE_KEYWORD="go_nuxt"