#!/bin/bash

# ---------------------------------------------------------
#   get_traffic.sh       blue | green
#   blue                  → 重啟 blue 
#   green                 → 重啟 green
#   回傳碼：
#                       0  = 重啟成功
#                       1  = 重啟失敗
# ---------------------------------------------------------

# 檢查模式 (blue 或 green)
MODE=$1


usage()
{
    cat <<EOF
Usage: restartContainer.sh [PARAM]

Known values for PARAM are:

  參數1 = [blue|green] : Required
EOF
}

# 檢查
if [[ "$MODE" != "blue" && "$MODE" != "green" ]]; then
  usage
  exit 99
fi

source /opt/vcs/tools/utils/sshToolUtil.sh

if [[ "$MODE" = "blue" ]]; then
    # 修改
    CONTAINER_NAME="tv"
elif [[ "$MODE" = "green" ]] ; then 
    # 修改
    CONTAINER_NAME="tv_test"
else
    echo "Error: Unknown Node"
    exit 1
fi

# 2>&1 確保將錯誤訊息(stderr)也導向到 result
result=$(ssh_function "docker restart ${CONTAINER_NAME}" 2>&1)

exit_code=$?

if [ $exit_code -eq 0 ]; then
    # --- 成功 ---
    echo "Restart Success: ${result}"
    exit 0
else
    # --- 失敗 ---
    echo "Restart Failed: ${result}"
    exit 1
fi