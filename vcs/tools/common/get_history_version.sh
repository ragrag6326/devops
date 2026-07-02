#!/bin/bash

# ---------------------------------------------------------
#   get_history_version.sh   [prod|dev]
#   取得遠端機器所有 image 清單 (含歷史)
#   回傳碼：
#                       0  = 成功
#                       1  = 失敗
# ---------------------------------------------------------

usage()
{
    cat <<EOF
Usage: get_history_version.sh [ENV]

  ENV = prod | dev : Required
EOF
}

ENV=$1

if [[ "$ENV" != "prod" && "$ENV" != "dev" ]]; then
  usage
  exit 1
fi

source /opt/vcs/tools/utils/sshToolUtil.sh $ENV

get_all_project_version() {
    local result=$(ssh_function "docker images --format '{{.Repository}}:{{.Tag}}' | grep -E 'frontend|backend' | uniq")
    echo "$result"
}

get_all_project_version
