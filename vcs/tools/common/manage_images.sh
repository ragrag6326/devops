#!/bin/bash

# ---------------------------------------------------------
#   Docker Image 管理工具
#   用法: ./manage_images.sh [action] [project_keyword]
# ---------------------------------------------------------


case $1 in
        prod)
		ENV=$1
		;;
        dev)
		ENV=$1
		;;
        *)
		echo "$1 prod | dev"
		exit 0
		;;
esac

# 載入 SSH 方法
source /opt/vcs/tools/utils/sshToolUtil.sh $ENV


usage() {
    cat <<EOF
Usage: $0 [ACTION] [PROJECT_KEYWORD]

Actions:
  current   取得當前運行中的 Image 版本
  history   取得主機上所有的 Image 歷史版號
  delete    刪除指定的 Image 版本 (例如: $0 delete frontend-prod/go_nuxt-backup:1.0.6)

Example:
  $0 $1 delete frontend-prod/go_nuxt-backup:1.06
EOF
    exit 1
}

# 1. 取得當前運行中的版本
get_current_version() {

    # 從 docker ps 中精確抓取正在跑的 Image 版號
    local result=$(ssh_function "docker ps  --format '{{.Image}}' | grep -E 'frontend|backend' | uniq")
    echo "$result"
}

# 2. 取得歷史版號清單
get_history_version() {
	local result=$(ssh_function "docker images --format '{{.Repository}}:{{.Tag}}' | grep -E 'frontend|backend' | uniq")

    echo "$result"
}

# 3. 刪除指定的歷史版本
delete_history_version() {
    local image_target=$1
    if [ -z "$image_target" ]; then
        echo "錯誤: 請提供要刪除的完整 Image 名稱與版號 (例如 frontend-prod/go_nuxt:1.0.0)"
        exit 1
    fi

    echo "準備刪除 Image: $image_target ..."

    # 執行 docker rmi 刪除映像檔
    # 1. 先宣告變數
    local result

    # 2. 加上 2>&1 將錯誤訊息 (stderr) 也合併捕捉，確保報錯時也能印出來
    result=$(ssh_function "docker rmi $image_target" 2>&1)

    # 3. 捕捉上一行 ssh_function 執行完的 Exit Code
    local exit_code=$?
	
    # 印出 Docker 的執行結果
    echo "$result"
    
    # 4. 根據 Exit Code 判斷成功與否
    if [ $exit_code -eq 0 ]; then
        echo "✅ 刪除操作成功。"
        return 0  # 成功，回傳 0
    else
        echo "❌ 刪除操作失敗 (Exit Code: $exit_code)。"
        return $exit_code  # 失敗，將 Docker/SSH 的真實錯誤碼往上傳遞
    fi
}

# ==========================================
# 主程式邏輯：根據第一個參數決定執行哪個函式
# ==========================================

# 如果沒有給任何參數，顯示說明
if [ $# -eq 0 ]; then
    usage
fi

ENV=$1
ACTION=$2
TARGET=$3

case "$ACTION" in
    current)
        get_current_version
        ;;
    history)
        get_history_version
        ;;
    delete)
        delete_history_version "$TARGET"
        ;;
    *)
        echo "未知的操作: $ACTION"
        usage $ENV
        ;;
esac
