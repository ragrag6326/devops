#!/bin/bash
# ============================================================
#  player 專案配置  (由 common/init.sh source)
# ============================================================

# ---- 正式機 (prod / 132.x) ----
PROD_BLUE_CHECK_PORTS=(8085)
PROD_GREEN_CHECK_PORTS=(8088)
PROD_HEALTH_HOST="www.tkbgo.com.tw"
PROD_HEALTH_SCHEME="https"
PROD_HEALTH_PATH="/api/member/test"

PROD_NGINX_CONF="/etc/nginx/conf.d/player/nginx-player.conf" # TODO: 確認 nginx conf 路徑
PROD_TRAFFIC_BLUE_PORT=8085
PROD_LIVE_UPSTREAM="player"                                    # TODO: 確認 upstream 名稱
PROD_HEADER_UPSTREAM="player_backup"                           # TODO

PROD_BLUE_CONTAINERS="player-api"
PROD_GREEN_CONTAINERS="player-api-backup"

PROD_DEPLOY_BASE="/opt/docker_image/player" # TODO: 確認 compose 目錄
PROD_SWITCH_SCRIPT="${PROD_DEPLOY_BASE}/script/switch_traffic.sh"
PROD_IMAGE_REPO="backend-prod"
BLUE_ENV_KEY="BLUE_VERSION"
GREEN_ENV_KEY="GREEN_VERSION"

# ---- 測試機 (dev / 131.x) ---- # player 未部署在 dev 機器
DEV_BLUE_CHECK_PORTS=(8085)
DEV_GREEN_CHECK_PORTS=()
DEV_HEALTH_HOST="www.tkbgo.com.tw"
DEV_HEALTH_SCHEME="https"
DEV_HEALTH_PATH="/api/member/test"

DEV_NGINX_CONF=""              # player 無 dev 環境
DEV_TRAFFIC_BLUE_PORT=8085
DEV_LIVE_UPSTREAM=""
DEV_HEADER_UPSTREAM=""

DEV_BLUE_CONTAINERS=""         # player 無 dev 環境
DEV_GREEN_CONTAINERS=""

DEV_DEPLOY_BASE=""
DEV_SWITCH_SCRIPT=""
DEV_IMAGE_REPO="backend-dev"

# ---- 共用 ----
IMAGE_KEYWORD="player"
