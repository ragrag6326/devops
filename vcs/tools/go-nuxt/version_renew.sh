#!/bin/bash

# ---------------------------------------------------------
#   version_renew      
#   回傳碼：
#                       0  = 
#                       1  = 
# ---------------------------------------------------------

BASE_PATH="/opt/docker_image/go_nuxt"   # 部署根路徑
ENV_FILE="${BASE_PATH}/.env"

NODE_TYPE=$1
NEW_VERSION=$2

if [ -z "$NODE_TYPE" ]; then
    echo "Usage: $0 <prod|backup>"
    exit 1
fi

if [ -z "$NEW_VERSION" ]; then
    echo "Usage: $0 <version: 1.0.0>"
    exit 1
fi

usage()
{
    cat <<EOF
Usage: version_renew.sh [PARAM]

Known values for PARAM are:

  參數1 = [ prod | backup ] : Required
  參數2 = [ 1.0.0 ]         : Required
EOF
}


if [ "$NODE_TYPE" = 'prod' ] ; then
    CONTAINER_NAME="go_nuxt go_nuxt2 go_nuxt3"
    VERSION="BLUE_VERSION"
elif [ "$NODE_TYPE" = 'backup' ]
    CONTAINER_NAME="go_nuxt_backup"
    VERSION="GREEN_VERSION"
fi


source /opt/vcs/tools/utils/sshToolUtil.sh

version_renew() {
    
    ssh_function "sed -i "s|${VERSION}=.*|${VERSION}=${NEW_VERSION}|g" "$ENV_FILE""
    local result=$(ssh_function "cd /opt/docker_image/go_nuxt ; docker compose up -d ${CONTAINER_NAME}")
    echo "$result"
}

version_renew