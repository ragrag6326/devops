#!/bin/bash

# ---------------------------------------------------------
#   get_traffic.sh       live | header 
#   live                  → 正式流量 
#   header                → header 流量
#   回傳碼：
#                       0  = live 
#                       1  = backup                    
# ---------------------------------------------------------
usage()
{
    cat <<EOF
Usage: get_traffic.sh [PARAM]

Known values for PARAM are:

  參數1 = [live|header] : Required
  return 0 = live 正是流量
  return 1 = backup 備援流量
EOF
}

# 檢查
if [[ "$1" != "live" && "$1" != "header" ]]; then
  usage
  exit 99
fi

source utils/sshToolUtil.sh

TV_NGINX="/etc/nginx/conf.d/tv/nginx-tv.conf"

# 0 = 正式  1 = 備援
if [[ "$1" = 'live' ]] ; then
    Live_Traffic=$(ssh_function "grep -A 3 'upstream tkbtv {' ${TV_NGINX} | grep 'server' | grep -v 'down' | grep -q 8090 && echo 0 || echo 1")
    echo "Live_Traffic $Live_Traffic"
fi

# 0 = 正式  1 = 備援
if [[ "$1" = 'header' ]] ; then
    Header_Traffic=$(ssh_function "grep -A 3 'tkbtv_header_test {' ${TV_NGINX} | grep 'server' | grep -v 'down' | grep -q 8090 && echo 0 || echo 1")
    echo "Header_Traffic $Backup_Traffic"
fi