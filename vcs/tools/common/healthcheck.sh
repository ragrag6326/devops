#!/bin/bash
# Usage: healthcheck.sh [prod|dev] [project] [blue|green]
usage() { echo "Usage: $0 [prod|dev] [project] [blue|green]"; exit 99; }

ENV=$1; PROJECT=$2; NODE=$3
[[ "$ENV"  != "prod" && "$ENV"  != "dev"   ]] && usage
[[ -z "$PROJECT" ]]                             && usage
[[ "$NODE" != "blue" && "$NODE" != "green" ]] && usage

source /opt/vcs/tools/common/init.sh   # 解析 BLUE/GREEN_CHECK_PORTS、HEALTH_* 等

[[ "$NODE" = "blue" ]] && CHECK_PORTS=("${BLUE_CHECK_PORTS[@]}") \
                       || CHECK_PORTS=("${GREEN_CHECK_PORTS[@]}")

HEALTH_PATH="${HEALTH_PATH:-}"

for PORT in "${CHECK_PORTS[@]}"; do
    IS_PORT_HEALTHY=false; FINAL_STATUS=0

    for i in {1..5}; do
        HEALTH_URL="${HEALTH_SCHEME}://localhost:${PORT}${HEALTH_PATH}"
        HTTP_STATUS=$(ssh_function "curl -sk -o /dev/null -w \"%{http_code}\" \"${HEALTH_URL}\" -H \"HOST: ${HEALTH_HOST}\"")
        [[ "$HTTP_STATUS" = "200" ]] && { IS_PORT_HEALTHY=true; break; }
        sleep 2; FINAL_STATUS=$HTTP_STATUS
    done

    if [[ "$IS_PORT_HEALTHY" == "false" ]]; then
        echo "$FINAL_STATUS"; exit 1
    fi
done

echo "200"; exit 0
