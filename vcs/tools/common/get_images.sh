#!/bin/bash
# Usage: get_images.sh [prod|dev] [project]
# 列出遠端機器上指定專案的所有 image（repository:tag 格式）
usage() { echo "Usage: $0 [prod|dev] [project]"; exit 1; }

ENV=$1; PROJECT=$2
source /opt/vcs/tools/common/ssh_env.sh
valid_ssh_env "$ENV" || usage
[[ -z "$PROJECT" ]]                        && usage

source /opt/vcs/tools/common/init.sh   # 解析 IMAGE_REPO、IMAGE_KEYWORD

result=$(ssh_function "docker images --format \"{{.Repository}}:{{.Tag}}\" | grep \"${IMAGE_KEYWORD}\" | grep \"${IMAGE_REPO}\" | cut -d '/' -f 2")
echo "$result"