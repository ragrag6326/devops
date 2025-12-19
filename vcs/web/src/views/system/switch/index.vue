<script setup>
import { onMounted, ref , watch } from 'vue'
import { queryMRPage , queryByProjectName } from "@/api/gitlab";
import { ElMessage , ElMessageBox } from 'element-plus'


const token = ref ('');

// ---------------- MR 數據列表 ----------------
const mrList = ref([])
const loading = ref(false)
const projectNameOptions = ref([])   // 專案名稱選項

// ----------  搜索表單  ---------- 
const searchForm = ref({
    name: "",
    state: "",
    date: [],
    start: "",
    end: ""
})


// ----------------- 分頁展示 --------------------------------- 
const currentPage= ref(1) // 頁碼
const pageSize = ref(5) // 每頁展示紀錄數
const background = ref(true) // 頁碼背景色
const totalPage = ref(0)  // 總紀錄數

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  search()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  search()
}

// ----------  搜索表單清空  ---------- 
const clear = () => {
  searchForm.value = { name: "", state: "", date: [], start: "", end: "" }
  search()
}

// 查詢歷史紀錄
const search = async () => {
    try {
        const result = await queryMRPage (      
            currentPage.value,
            pageSize.value,
            searchForm.value.name,
            searchForm.value.state,
            searchForm.value.start,
            searchForm.value.end
        )
        console.log('queryMrPage result:', result)

        if (result?.code) {
            mrList.value = result.data.rows || []
            totalPage.value = result.data.total || 0
            
            // 取得所有 projectName 排除重複
            const names = mrList.value.map(name => name.projectName).filter(name => name && name.trim() !== "") 
            projectNameOptions.value = [...new Set(names)]

        } else {
            ElMessage.error(result?.msg || "查詢失敗")
        }

    } catch (error) {
        console.error('Error fetching MR data:', error)
        ElMessage.error('查詢 MR 歷史紀錄失敗! 請查看 console log')
    }
}


watch(() => searchForm.value.date, (val) => {
  if (val && val.length === 2) {
    searchForm.value.start = val[0]
    searchForm.value.end = val[1]
  } else {
    searchForm.value.start = ""
    searchForm.value.end = ""
  }
})

// 獲取 token
const getToken = () => {
    token.value = localStorage.getItem('jwt_token')
}

// ------------------------------------------------------------------------------------------- 
onMounted (() => {
    search();
    getToken();  // 獲取 token
})


</script>

<template>
    <h1>歷史紀錄查詢</h1>

    <!-- 搜索欄 -->
    <div id="container">

        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
            
            <el-form-item label="專案名稱">
                <el-select v-model="searchForm.name" placeholder="全部" clearable style="width:120px">
                    <el-option  v-for="name in projectNameOptions" :key="name" :label="name" :value="name" />
                </el-select>
            </el-form-item>
            
            <el-form-item label="狀態">
                <el-select v-model="searchForm.state" placeholder="全部" clearable style="width:120px">
                    <el-option label="merged" value="merged" />
                    <el-option label="opened" value="opened" />
                    <el-option label="closed" value="closed" />
                </el-select>
            </el-form-item>

            <el-form-item label="日期">
                <el-date-picker
                    v-model="searchForm.date"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="查詢開始日期"
                    end-placeholder="查詢結束日期"
                    value-format="YYYY-MM-DD"
                    clearable
                />
            </el-form-item>

            <el-form-item>
                <el-button el-button type="primary" @click="search">查詢</el-button>
                <el-button el-button type="info" @click="clear">清空</el-button>
            </el-form-item>
        </el-form>
    </div>

    
    <!-- 數據表格顯示 -->
    <div class="table-container">
         <el-table :data="mrList" border style="width:100%" v-loading="loading">
            <!-- <el-table-column type="selection" width="35" align="center" /> -->
                <el-table-column prop="iid" label="MR流水號" width="90"/>
                <el-table-column prop="projectName" label="專案名稱" min-width="90" show-overflow-tooltip />
                <el-table-column prop="title" label="標題" min-width="240" show-overflow-tooltip />
                <el-table-column prop="authorName" label="作者" width="80"/>
                <el-table-column prop="mergedBy" label="合併人" width="80"/>
                <el-table-column prop="state" label="狀態" width="90">
                    <template #default="scope">
                        <el-tag :type="scope.row.state === 'merged' ? 'success' : scope.row.state === 'opened' ? 'warning' : 'danger'">
                        {{ scope.row.state }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="分支" width="240">
                    <template #default="r">
                        <b>{{ r.row.sourceBranch }}</b> → <b>{{ r.row.targetBranch }}</b>
                    </template>
                </el-table-column>
                <el-table-column prop="versionProd" label="PROD關聯版號" width="90">
                    <template #default="scope">
                        <el-tag :type="scope.row.versionProd != null ? 'success' : (scope.row.state == null ? 'warning' : 'danger')"> {{ scope.row.versionProd || '未關聯' }} </el-tag>
                    </template>
                </el-table-column>
                
                <el-table-column prop="versionDev" label="DEV關聯版號" width="90">
                    <template #default="scope">
                        <el-tag :type="scope.row.versionDev != null ? 'success' : (scope.row.state == null ? 'warning' : 'danger')"> {{ scope.row.versionDev || '未關聯' }} </el-tag>
                    </template>
                </el-table-column>

                <el-table-column prop="mergedAt" label="合併時間" width="160"/>
                <el-table-column prop="createdAt" label="建立時間" width="160"/>
                
        </el-table>
        <br>
    </div>

    <!-- 分頁 -->
    <div class="page-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 30, 40, 50]"
            :background="background"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalPage"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            class="custom-pagination"
        />
    </div>


</template>

<style scoped>

.container{
    margin: 10px 0px;

}


/* =============================== */
/*    表格組件樣式 (Table)          */
/* =============================== */
/* template 中使用了 .table-container 包裹 el-table */
.table-container {
    /* 確保容器使用主題面板背景色，以消除白色殘留 */
    /* background-color: var(--panel, #1e293b);  */
    border-radius: 15px;
    padding: 0; /* 清除內部額外間距 */
    overflow: hidden;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
    margin-top: 20px; /* 給頂部按鈕留出空間 */

    /* <--- 設定最大寬度，您可以根據需要調整這個值 (例如 1000px, 1400px) */
    max-width: 1500px; /* 設定您覺得舒適的最大寬度 */
    width: 95%;       /* 確保在小螢幕下佔滿 */
    margin-left: auto;   /* <--- 讓左邊自動填滿空間 */
    margin-right: auto;  /* <--- 讓右邊自動填滿空間，實現水平居中 */
}

/* 核心：覆蓋 Element Plus 表格的 CSS 變量，採用低飽和度色彩 */
:deep(.el-table) {
    /* 讓表格體透明，這樣它會透出 .table-container 的深色背景 */
    --el-table-bg-color: var(--table-bg-color) !important;
    --el-table-tr-bg-color: var(--table-bg-color) !important;
    
    /* 表頭：使用一個更深的暗色，以提供視覺上的層次感（如您提供的範例圖所示）*/
    --el-table-header-bg-color: var(--table-bg-color) !important;
    
    /* 邊框顏色：保持極淡，融入背景 */
    --el-table-border-color: var(--table-border-color) !important;
    --el-table-border: 1px solid var(--el-table-border-color) !important;
    
    /* 文字顏色：使用標準淺灰色（非螢光色） */
    /* --el-table-text-color: #d2cbe1 !important; */
    --el-table-text-color: var(--table-text-color) !important;
    --el-table-header-text-color: var(--table-header-text-color) !important; /* 表頭文字稍亮 */
    
    /* Hover 高亮：微小的白色透明度 */
    --el-table-row-hover-bg-color: var(--table-hover-bg) !important;
    
    background-color: transparent !important;
}

/* 強制表格的內容區塊也是透明的 */
:deep(.el-table__inner-wrapper) {
    background-color: transparent !important;
}

/* 讓表格單元格的背景透明並調整邊框 */
:deep(.el-table th.el-table__cell),
:deep(.el-table td.el-table__cell) {
    background-color: var(--table-bg-color) !important;
    border-bottom: 1px solid var(--table-border-color) !important;
}

/* 調整空數據提示文字的顏色 */
:deep(.el-table__empty-text) {
    color: var(--table-header-text-color) !important;
    font-size: 15px;
}

/* 去除表格底部的白色線條（Element Plus 默認邊框） */
:deep(.el-table::before) {
    height: 0 !important;
}

/* =============================== */
/* 按鈕樣式加強 (如果全局沒生效) */
/* =============================== */

/* 主按鈕 (新增/編輯) */
:deep(.el-button--primary) {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    border: none;
    box-shadow: 0 2px 6px rgba(99, 102, 241, 0.4);
    color: white;
}
:deep(.el-button--primary:hover) {
    opacity: 0.9;
    transform: translateY(-1px);
}

/* 危險按鈕 (刪除) */
:deep(.el-button--danger) {
    background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
    border: none;
    box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}
:deep(.el-button--danger:hover) {
    opacity: 0.9;
    transform: translateY(-1px);
}

/* 表格內的小按鈕調整 */
:deep(.el-table .el-button) {
    padding: 6px 12px;
    height: auto;
    font-size: 12px;
}


/* =============================== */
/* 分頁組件樣式 (Pagination) */
/* =============================== */

.page-container {
    padding: 16px 0;
    display: flex;
    justify-content: center;
}

/* 核心：設定分頁組件的文字和背景基調 */
:deep(.custom-pagination) {
    /* 總體文字顏色 */
    --el-text-color-regular: #cbd5e1; /* slate-300 */
    /* 分頁背景透明 (讓它透出主內容區的背景) */
    --el-pagination-bg-color: transparent !important;
    padding: 15px 0; /* 增加上下間距 */

}

/* 針對 Prev/Next 按鈕和頁碼數字的背景/邊框/文字調整 */
:deep(.custom-pagination .btn-prev),
:deep(.custom-pagination .btn-next),
:deep(.custom-pagination .el-pager li),
:deep(.custom-pagination .el-select .el-input__wrapper) /* 調整 Size 選擇器的外觀 */
{
    /* 讓按鈕背景為極深藍 (slate-900)，比卡片背景 #1e293b 更深，以增加層次感 */
    background-color: #0f172a !important; 
    
    /* 邊框使用極淡的白色，達到科技感邊緣效果 */
    border: 1px solid rgba(255, 255, 255, 0.15); 
    color: #cbd5e1 !important;
    border-radius: 6px; /* 圓角調整 */
    transition: all 0.3s;
}

/* 活躍/選中頁碼的樣式 (核心高亮) */
:deep(.custom-pagination .el-pager li.is-active) {
    /* 漸層紫色高亮 */
    background: linear-gradient(45deg, #6366f1 0%, #8b5cf6 100%) !important; 
    border-color: #8b5cf6 !important; /* 邊框顏色與漸層呼應 */
    color: white !important;
    font-weight: bold;
    transform: scale(1.05); /* 輕微放大效果 */
} 

/* 頁碼懸停 (Hover) 效果 */
:deep(.custom-pagination .el-pager li:hover:not(.is-active)) {
    background-color: #1e293b !important; /* slate-800 hover */
    border-color: #681656; /* 懸停時邊框變成主題紫色 */
    color: #8b5cf6 !important;
}

/* 調整總數文字的顏色 */
:deep(.custom-pagination .el-pagination__total) {
    color: #94a3b8; /* slate-400 */
}

/* 調整跳頁輸入框的邊框 */
:deep(.custom-pagination .el-input.el-pagination__editor.is-in-pagination .el-input__wrapper) {
    background-color: #0f172a !important; 
    border: 1px solid rgba(255, 255, 255, 0.15) !important;
    box-shadow: none !important;
}



</style>