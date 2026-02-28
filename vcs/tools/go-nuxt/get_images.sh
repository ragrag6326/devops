#!/bin/bash

# ---------------------------------------------------------
#   get_images_version       
#   回傳值：
#           go_nuxt-backup:1.0.6
#           go_nuxt-prod:1.0.3
# ---------------------------------------------------------


PROJECT_KEYWORD=$1

if [ -z "$PROJECT_KEYWORD" ]; then
    echo "Usage: $0 <project_keyword>"
    exit 1
fi

usage()
{
    cat <<EOF
Usage: get_images.sh [PARAM]

Known values for PARAM are:
 
  參數1 = [go_nuxt] : Required
EOF
}


source /opt/vcs/tools/utils/sshToolUtil.sh

get_version_list() {
    local result=$(ssh_function "docker images --format "{{.Repository}}:{{.Tag}}" | grep "$PROJECT_KEYWORD" | grep frontend-prod")
    echo "$result"
}

get_images_version