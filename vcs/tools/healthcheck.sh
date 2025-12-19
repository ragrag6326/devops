#!/bin/bash

# ---------------------------------------------------------
#   healthcheck.sh       blue | green
#     blue                  → 正式
#     green                 → 備援
#   回傳碼：
#                       0  = OK
#                       1  = FAILED
# ---------------------------------------------------------
usage()
{
    cat <<EOF
Usage: healthcheck.sh [PARAM]

Known values for PARAM are:

  參數1 = [blue|green] : Required
    return 0 = OK
    return 1 = FAILED
EOF
}

# 檢查
if [[ "$1" != "blue" && "$1" != "green" ]]; then
  usage
  exit 99
fi

source ../utils/sshToolUtil.sh


# 檢查
if [[ "$1" = "blue" ]] ; then
  CHECK_PORT=8087
elif [[ "$1" = "green" ]] ; then
  CHECK_PORT=8090
fi

HEADER_HOST="www.tkbtv.com.tw"
HEALTH_URL="https://localhost:${CHECK_PORT}/front/toHeader.action"

# -s: 靜音模式。
# -f: HTTP 錯誤時不輸出內容並返回非零代碼。
# -k: 允許不安全的 SSL 連線。

for i in {1..5} ; do
  health_check_code=$(ssh_function "if curl -sfk "${HEALTH_URL}"  -H \"HOST: ${HEADER_HOST}\" &> /dev/null ; then echo 0 ; else echo 1 ;fi")
  if [[ $health_check_code == 0 ]] ; then
    echo $health_check_code
    break
  fi
done

if [[ $health_check_code != 0 ]] ; then
  echo $health_check_code
fi