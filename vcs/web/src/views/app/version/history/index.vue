<script setup>
import { onMounted, ref , watch } from 'vue'
import { queryVersionPage } from "@/api/version";
import { triggerJenkinsBackendBuild } from "@/api/jenkins";
import { ElMessage , ElMessageBox } from 'element-plus'


const token = ref ('');

// ---------------- 版本數據列表 ----------------
const versionList = ref([])
const loading = ref(false)

// 搜索欄
const projectNameOptions = [
    { label: "tkbgoapi", value: "tkbgoapi" },
    { label: "tkbgotv", value: "tkbgotv" },
    { label: "tkbnuxt", value: "tkbnuxt" },
]

const projectEnvOptions = [
    { label: "prod", value: "prod" },
    { label: "dev", value: "dev" },
]

const stateOptions = [
  { label: "部屬中", value: 0 },
  { label: "部屬成功", value: 1 },
  { label: "失敗", value: 2 },
  { label: "回滾", value: 3 },
]


// ----------  搜索表單  ---------- 
const searchForm = ref({
    name: "",
    state: "",
    env: "",
})

// ----------  搜索表單清空  ---------- 
const clear = () => {
  searchForm.value = { name: "", state: "",  env: "" }
  search()
}

// ----------------- 分頁展示 --------------------------------- 
const currentPage= ref(1) // 頁碼
const pageSize = ref(10) // 每頁展示紀錄數
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

// --------------------------------- 查詢歷史紀錄 ---------------------------------
const search = async () => {
    try {
        const result = await queryVersionPage (      
            currentPage.value,
            pageSize.value,
            searchForm.value.name,
            searchForm.value.env,
            searchForm.value.state
        )
        console.log('queryMrPage result:', result)

        if (result?.code) {
            versionList.value = result.data.rows || []
            totalPage.value = result.data.total || 0
            
            // 取得所有 projectName 排除重複
            // const names = versionList.value.map(name => name.projectName).filter(name => name && name.trim() !== "") 
            // projectNameOptions.value = [...new Set(names)]

        } else {
            ElMessage.error(result?.msg || "查詢失敗")
        }

    } catch (error) {
        console.error('Error fetching MR data:', error)
        ElMessage.error('查詢 MR 歷史紀錄失敗! 請查看 console log')
    }
}


// ----------------- 版號表單對話框 --------------------------------- 
const formTitle = ref('');
const addDialogVisible = ref(false);  // 對話框默認隱藏
const versionFormRef = ref();          // 表單驗證
// (新增、修改) 版號表單 數據回顯
const versionForm = ref ({ 
    name : "" ,
    env: "",
    version: "",
    branch: "",
    remark: ""
})


// 版號表單驗證規則
const rules = {
    name: [{ required: true, message: "請選擇專案", trigger: "change" }],
    env: [{ required: true, message: "請選擇環境", trigger: "change" }],
    branch: [{ required: true, message: "請輸入分支", trigger: "change" }]
}

// 版號表單初始化
const InitEditForm = () => {

    // 點擊添加前 清空的表單
    for (let key in versionForm.value ) {
        if (!Array.isArray(versionForm.value[key]) ) {
            versionForm.value[key] = '';
        } else {
            versionForm.value[key] = [];
        }
    }

    formTitle.value = '新增版號';
    addDialogVisible.value = true;
}


// 點開(編輯)版號表單 - 數據回顯
const handleEdit = async (row) => {
    const result = await queryVersionByIdApi( row.id ) ;
    if ( result.code ) {
        InitEditForm();
        formTitle.value = '編輯版號';
        versionForm.value = result.data;
    }
}

// 確認 版號表單 ( 新增 、 修改 )
const submitVersionAddandEdit = async () => {

    // 表單驗證
    if (!versionFormRef.value) return;

    versionFormRef.value.validate( async (valid) => { // valid 表示是否校驗通過 : true 通過、false 未通過
        if (valid) {

            let result ;

            if ( versionForm.value.id ) { 

                // 修改
                ElMessage.success("修改")

            } else { 

                ElMessageBox.confirm(`此操作將進行環境更新 是否繼續? `, '提示', {
                    cancelButtonText: '取消',
                    confirmButtonText: '確定',
                    type: 'warning' // 跳出的樣式 
                }).then(async () => {
                    const result = await triggerJenkinsBackendBuild(versionForm.value.name , versionForm.value.env , versionForm.value.branch )
                    if ( result.code ) {
                        ElMessage.success('正在部屬');
                        search(); 
                    } else {
                        ElMessage.error('部屬失敗:' + result.msg);
                    }
                }).catch(() => {
                    ElMessage.info('已取消');
                })
            }
            
            addDialogVisible.value = false;
            search()

        } else {
            ElMessage.error("表單驗證未通過 .... 請重新確認")
        }
    })
}



// ------------------------------------------------------------------------------------------- 
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
    <h1>版本歷史紀錄查詢</h1>

    <!-- 搜索欄 -->
    <div id="container">

        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
            <!-- {{ projectStateOptions  }} -->
            <el-form-item label="專案名稱">
                <el-select v-model="searchForm.name" placeholder="全部" clearable style="width:120px">
                    <el-option  v-for="name in projectNameOptions" :key="name.value" :label="name.label" :value="name.value" />
                </el-select>
            </el-form-item>
            
            <el-form-item label="環境">
                <el-select v-model="searchForm.env" placeholder="全部" clearable style="width:120px">
                    <el-option  v-for="env in projectEnvOptions" :key="env.value" :label="env.label" :value="env.value" />
                </el-select>
            </el-form-item>

            <el-form-item label="狀態">
                <el-select v-model="searchForm.state" placeholder="全部" clearable style="width:120px">
                    <el-option  v-for="state in stateOptions" :key="state.value" :label="state.label" :value="state.value" />
                </el-select>
            </el-form-item>

            <el-form-item>
                <el-button el-button type="primary" @click="search">查詢</el-button>
                <el-button el-button type="info" @click="clear">清空</el-button>
            </el-form-item>
        </el-form>

        <el-button el-button type="primary" @click="InitEditForm">新增版號</el-button>
        <el-button el-button type="danger"  @click="DeleteVersion"> 批量刪除</el-button>

    </div>



    
    <!-- 數據表格顯示 -->
    <div class="table-container">
        <!-- {{versionList}} -->
        <el-table :data="versionList" border style="width:100%" v-loading="loading">
                <el-table-column type="selection" width="35" align="center" />
                <el-table-column prop="id" label="編號" min-width="20"/>
                <el-table-column prop="projectName" label="專案名稱" min-width="30" show-overflow-tooltip />
                <el-table-column prop="projectEnv" label="環境" min-width="30" />
                <el-table-column prop="version" label="版本" min-width="30"/>
                <el-table-column prop="state" label="狀態" min-width="35">
                    <template #default="scope">
                    <el-tag :type="scope.row.state === 1 ? 'success' : scope.row.state === 2 ? 'danger' : 'warning' ">
                        {{ stateOptions.find(s => s.value === scope.row.state)?.label || "未知" }}
                    </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="remark" label="備註" min-width="60"/>
                <el-table-column prop="createdTime" label="建立時間" min-width="60"/>
                <el-table-column prop="updatedTime" label="更新時間" min-width="60" />

                <el-table-column label="操作" min-width="60" >
                    <template #default="scope">
                        <el-button type="primary"  @click="handleEdit(scope.row)"> <el-icon><EditPen /></el-icon>  &nbsp; 編輯</el-button>
                        <el-button type="danger"  @click="handleDelete(scope.row)"><el-icon><Delete /></el-icon> &nbsp; 刪除</el-button>
                    </template>
                </el-table-column>
        </el-table>
        <br>
    </div>

    <!-- 分頁 -->
    <div class="page-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 30, 40, 50, 60]"
            :background="background"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalPage"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            class="custom-pagination"
        />
    </div>

    <!-- 新增版號(add) dialog -->
    <el-dialog v-model="addDialogVisible" :title="formTitle" width="600px">

        <el-form :model="versionForm" :rules="rules" ref="versionFormRef" label-width="90px">

            <el-form-item label="專案" prop="name">
                <el-select v-model="versionForm.name" style="width:50%">
                    <el-option v-for="n in projectNameOptions" :key="n.value" :label="n.label" :value="n.value" />
                </el-select>
            </el-form-item>

            <el-form-item label="環境" prop="env">
                <el-select v-model="versionForm.env" style="width:50%">
                    <el-option v-for="e in projectEnvOptions" :key="e.value" :label="e.label" :value="e.value" />
                </el-select>
            </el-form-item>

            <el-form-item label="分支" prop="branch">
                <el-input v-model="versionForm.branch" placeholder="develop" style="width:50%"/>
            </el-form-item>

            <el-form-item label="備註">
                <el-input type="textarea" v-model="versionForm.remark" />
            </el-form-item>

        </el-form>

        <template #footer>
            <el-button @click="addDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitVersionAddandEdit()">確認</el-button>
        </template>
    </el-dialog>


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