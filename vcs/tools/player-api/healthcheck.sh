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

source /opt/vcs/tools/utils/sshToolUtil.sh

# 檢查
if [[ "$1" = "blue" ]] ; then
  CHECK_PORT=8085
elif [[ "$1" = "green" ]] ; then
  CHECK_PORT=8088
fi

HEADER_HOST="www.tkbgo.com.tw"
HEALTH_URL="https://localhost:${CHECK_PORT}/api/member/test"

# -s: 靜音模式。
# -f: HTTP 錯誤時不輸出內容並返回非零代碼。
# -k: 允許不安全的 SSL 連線。

for i in {1..5} ; do

  HTTP_STATUS=$(ssh_function "curl -sk -o /dev/null -w "%{http_code}" "${HEALTH_URL}"  -H \"HOST: ${HEADER_HOST}\"")

  if [[ $HTTP_STATUS = 200 ]] ; then
    echo $HTTP_STATUS
    exit 0;
  fi
done

if [[ $HTTP_STATUS != 200 ]] ; then
  echo $HTTP_STATUS
  exit 1;
fi