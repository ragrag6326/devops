#!/bin/bash

# ================= 設定區 =================
USER="tkb0001662"            # 遠端使用者名稱
HOST="132.145.125.250"       # 遠端 IP


TYPE=$1

# 金鑰路徑
# KEY_PATH="~/.ssh/prod.pem"
# =========================================

case $1 in
	prod)
		KEY_PATH="~/.ssh/prod.pem"
		HOST="132.145.125.250"
		;;
	dev)
		KEY_PATH="~/.ssh/dev.pem"
		HOST="131.186.44.40"
		;;
	*)
		echo "prod | dev"
		;;
esac


ssh_connect_check() {

    result=$(ssh -C -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $KEY_PATH $USER@$HOST "echo success")

    echo ${result}
}

ssh_function() {
    local cmd=$1
    # ssh -C -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i $key $user@$host "$cmd" 2>/dev/null
    ssh -C -o LogLevel=ERROR -o StrictHostKeyChecking=no -i $KEY_PATH $USER@$HOST "$cmd"
}

scp_file() {
    local source=$1
    local dist=$2
    #rsync -e "ssh -o PubkeyAuthentication=yes -o stricthostkeychecking=no" -i $key -az --delete $source $dist
    scp -o PubkeyAuthentication=yes -o stricthostkeychecking=no -i $KEY_PATH $source $dist
}

