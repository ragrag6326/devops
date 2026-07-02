#!/bin/bash

# ---------------------------------------------------------
#   delete_history_version.sh   [prod|dev] [imageName]
#   刪除遠端機器上指定的 Docker image
#   回傳碼：
#                       0  = 成功
#                       1  = 失敗
# ---------------------------------------------------------

usage()
{
    cat <<EOF
Usage: delete_history_version.sh [ENV] [IMAGE_NAME]

  ENV        = prod | dev                       : Required
  IMAGE_NAME = backend-prod/tkbtv:1.0.5         : Required
EOF
}

ENV=$1
IMAGE_NAME=$2

if [[ "$ENV" != "prod" && "$ENV" != "dev" ]]; then
  usage
  exit 1
fi

if [ -z "$IMAGE_NAME" ]; then
  usage
  exit 1
fi

source /opt/vcs/tools/utils/sshToolUtil.sh $ENV

delete_image() {
    local result=$(ssh_function "docker rmi ${IMAGE_NAME}" 2>&1)
    local exit_code=$?
    echo "$result"
    return $exit_code
}

delete_image
