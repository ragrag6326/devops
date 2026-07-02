#!/bin/bash
# Usage: switch_traffic.sh [prod|dev] [project] [blue|green] [header?]
usage() { echo "Usage: $0 [prod|dev] [project] [blue|green] [header?]"; exit 99; }

ENV=$1; PROJECT=$2; TARGET=$3; HEADER_MODE=$4
[[ "$ENV"    != "prod" && "$ENV"    != "dev"   ]] && usage
[[ -z "$PROJECT" ]]                                && usage
[[ "$TARGET" != "blue" && "$TARGET" != "green" ]] && usage

source /opt/vcs/tools/common/init.sh   # 解析 SWITCH_SCRIPT

result=$(ssh_function "bash ${SWITCH_SCRIPT} ${TARGET} ${HEADER_MODE}")
exit_code=$?

if   [ $exit_code -eq 0  ]; then echo "切換成功！遠端: $result";           exit 0
elif [ $exit_code -eq 10 ]; then echo "無須切換！遠端: $result";           exit 10
else                              echo "切換失敗 (code: $exit_code): $result"; exit 1
fi
