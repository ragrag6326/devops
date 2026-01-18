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
  CHECK_PORTS=(8333 8335 8336)
elif [[ "$1" = "green" ]] ; then
  CHECK_PORTS=(8334)
fi

HEADER_HOST="www.tkbgo.com.tw"


# -s: 靜音模式。
# -f: HTTP 錯誤時不輸出內容並返回非零代碼。
# -k: 允許不安全的 SSL 連線。


for PORT in "${CHECK_PORTS[@]}" ; do

  IS_PORT_HEALTHY=false
  FINAL_STATUS=0

  for i in {1..5} ; do
    HEALTH_URL="http://localhost:${PORT}"
    HTTP_STATUS=$(ssh_function "curl -sk -o /dev/null -w "%{http_code}" "${HEALTH_URL}"  -H \"HOST: ${HEADER_HOST}\"")

    if [[ $HTTP_STATUS = 200 ]] ; then
      IS_PORT_HEALTHY=true
      break
    fi

    sleep 2
    FINAL_STATUS=$HTTP_STATUS
  done

  if [[ "$IS_PORT_HEALTHY" == "false" ]] ; then
    # 回傳最後一次的錯誤狀態碼 (例如 502 或 000)
    echo $FINAL_STATUS
    exit 1
  fi
done

echo "200"
exit 0