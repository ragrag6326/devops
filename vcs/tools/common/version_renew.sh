#!/bin/bash
# Usage: version_renew.sh [prod|dev] [project] [prod|backup] [version]
usage() { echo "Usage: $0 [prod|dev] [project] [prod|backup] [version]"; exit 1; }

ENV=$1; PROJECT=$2; NODE_TYPE=$3; NEW_VERSION=$4
[[ "$ENV"  != "prod" && "$ENV"  != "dev" ]] && usage
[[ -z "$PROJECT" || -z "$NODE_TYPE" || -z "$NEW_VERSION" ]] && usage

source /opt/vcs/tools/common/init.sh   # 解析 BLUE/GREEN_CONTAINERS、DEPLOY_BASE 等

case "$NODE_TYPE" in
    prod)   CONTAINERS="$BLUE_CONTAINERS";  VERSION_KEY="$BLUE_ENV_KEY"  ;;
    backup) CONTAINERS="$GREEN_CONTAINERS"; VERSION_KEY="$GREEN_ENV_KEY" ;;
    *)      echo "Error: NODE_TYPE must be 'prod' or 'backup'"; exit 1   ;;
esac

ENV_FILE="${DEPLOY_BASE}/.env"

# Step 1: 修改 .env 版號
ssh_function "sed -i \"s|${VERSION_KEY}=.*|${VERSION_KEY}=${NEW_VERSION}|g\" \"${ENV_FILE}\""
[ $? -ne 0 ] && { echo "Error: 修改 .env 失敗"; exit 1; }

# Step 2: docker compose 重啟容器
result=$(ssh_function "cd ${DEPLOY_BASE} && docker compose up -d ${CONTAINERS}")
exit_code=$?
echo "$result"
exit $exit_code