#!/bin/bash
# Usage: restartContainer.sh [prod|dev] [project] [blue|green]
usage() { echo "Usage: $0 [prod|dev] [project] [blue|green]"; exit 99; }

ENV=$1; PROJECT=$2; MODE=$3
[[ "$ENV"  != "prod" && "$ENV"  != "dev"   ]] && usage
[[ -z "$PROJECT" ]]                             && usage
[[ "$MODE" != "blue" && "$MODE" != "green" ]] && usage

source /opt/vcs/tools/common/init.sh   # 解析 BLUE/GREEN_CONTAINERS

[[ "$MODE" = "blue" ]] && CONTAINER_NAME="$BLUE_CONTAINERS" \
                       || CONTAINER_NAME="$GREEN_CONTAINERS"

result=$(ssh_function "docker restart ${CONTAINER_NAME}" 2>&1)
exit_code=$?

if [ $exit_code -eq 0 ]; then
    echo "Restart Success: ${result}"; exit 0
else
    echo "Restart Failed: ${result}"; exit 1
fi
