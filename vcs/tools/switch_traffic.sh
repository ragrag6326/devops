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
Usage: switch_traffic.sh [PARAM]

Known values for PARAM are:

  參數1 = [blue|green] : Required
  參數2 = [header]     : Optional
  如果沒有加上 參數2 將會切換正式分流
EOF
}

TARGET=$1      # blue | green
HEADER_MODE=$2 # header | 空值
SCRIPT_PATH="/opt/docker_image/tkbtv/script"

# 檢查
if [[ "$TARGET" != "blue" && "$TARGET" != "green" ]]; then
  usage
  exit 99
fi

source /opt/vcs/tools/utils/sshToolUtil.sh


result=$(ssh_function "bash ${SCRIPT_PATH}/switch_traffic.sh ${TARGET} ${HEADER_MODE}")

exit_code=$?

# 3. 進行判斷
if [ $exit_code -eq 0 ]; then
    echo "切換成功！"
    echo "遠端回傳訊息: $result"
elif [ $exit_code -eq 0 ] ; then
    echo "無須切換！"
    echo "遠端回傳訊息: $result"
else
    echo "切換失敗，錯誤代碼: $exit_code"
    echo "錯誤訊息: $result"
    exit 1
fi


