<script setup>
import { onMounted, ref , watch } from 'vue'
import { queryEmpPage , addempApi , queryEmpByIdApi , updateEmpApi , deleteEmpApi } from "@/api/emp";
import { queryUserPage , addUserApi , updateUserApi , deleteUserApi  } from "@/api/user";

import { ElMessage , ElMessageBox } from 'element-plus'


const token = ref ('');


// ----------  搜索表單  ---------- 
const UserForm = ref ({
    name : "" 
})

// 查詢員工列表
const search = async () => {

    try {
        const result = await queryEmpPage(
            UserForm.value.name,
            currentPage.value,
            pageSize.value
        )
        console.log(result);
        if (result && result.code) {
            empList.value = result.data.rows;
            totalpage.value = result.data.total;
        }
    } catch (error) {
        console.error('Error fetching user data:', error);
    }
}

// 清空 Form 表單
const clear = () => {
    for (let key in UserForm.value) {
        UserForm.value[key] = ''; 
    }
}


// ----------------- 分頁展示 --------------------------------- 
const currentPage= ref(1) // 頁碼
const pageSize = ref(5) // 每頁展示紀錄數
const background = ref(true) // 頁碼背景色
const totalpage = ref()  // 總紀錄數

const handleSizeChange = (val) => {
  console.log(`${val} items per page`)
  search() 
}
const handleCurrentChange = (val) => {
  console.log(`current page: ${val}`)
  search() 
}

// ----------------- 新增 、 修改 使用者 --------------------------------- 
const formTitle = ref('');
const addDialogVisible = ref(false); // 對話框默認隱藏
const EmpaddFormRef = ref(); // 表單驗證

const EmpaddForm = ref ({ 
    id : "",
    username : "",
    name : "" ,
})

const AddEmp = () => {
    // 點擊添加前 清空add的表單
    for (let key in EmpaddForm.value ) {
        if (!Array.isArray(EmpaddForm.value[key]) ) {
            EmpaddForm.value[key] = '';
        } else {
            EmpaddForm.value[key] = [];
        }
    }

    // 重製表單驗證
    if (EmpaddFormRef.value) {
        EmpaddFormRef.value.resetFields();
    };

    searchDept();
    formTitle.value = '新增員工';
    addDialogVisible.value = true;

}


// 保存前表單驗證
const rules = {
    username: [
        { required: true, message: '請輸入使用者名', trigger: 'blur' },
        { min: 2, max: 20, message: '長度在 2 到 20 個字符', trigger: 'blur' }
    ] ,
    password: [
        { required: true, message: '請輸入密碼', trigger: 'blur' },
        { min: 6, max: 20, message: '密碼長度在 6 到 20 個字符', trigger: 'blur' }
    ] ,
};

// 確認 保存員工 ( 新增 、 修改 )
const submitempAdd = async () => {

    // 表單驗證
    if (!EmpaddFormRef.value) return;
    EmpaddFormRef.value.validate( async (valid) => { // valid 表示是否校驗通過 : true 通過、false 未通過
        if (valid) {

            let result ;

            if ( EmpaddForm.value.id ) { 
                // 修改
                result = await updateEmpApi(EmpaddForm.value)
                if ( result.code ) {
                    ElMessage.success("員工修改成功")
                } else {
                    ElMessage.error("員工修改失敗:" + result.msg )
                }

            } else { 
                // 新增
                result = await addempApi(EmpaddForm.value)
                if ( result.code ) {
                    ElMessage.success("新增員工成功")
                } else {
                    ElMessage.error("新增員工失敗:" + result.msg ) 
                }
            }
            
            addDialogVisible.value = false;
            search()

        } else {
            ElMessage.error("表單驗證未通過 .... 請重新確認")
        }
    })
}

// ----------------------- 編輯員工 --------------------------------- 
const editForm = ref({ id: '' })
const handleEdit = async (row) => {

    const result = await queryEmpByIdApi( row.id ) ;
    if ( result.code ) {
        AddEmp();
        formTitle.value = '編輯員工';
        EmpaddForm.value = result.data;

        let exprList = EmpaddForm.value.exprList;
        if ( exprList && exprList.length > 0 ) {
            exprList.forEach( (expr) => {
                expr.exprDate = [expr.begin, expr.end];
            });
        }
    }
}

// ----------------------- 刪除員工 --------------------------------- 

// 刪除單個
const handleDelete =  (emp) => {
    ElMessageBox.confirm(`此操作將永久刪除 "${emp.name}" 員工 是否繼續?`, '提示', {
        cancelButtonText: '取消',
        confirmButtonText: '確定刪除',
        type: 'warning' // 跳出的樣式 
    }).then(async () => {
        const result = await deleteEmpApi(emp.id);
        if ( result.code ) {
            ElMessage.success('刪除成功');
            search(); 
        } else {
            ElMessage.error('刪除失敗:' + result.msg);
        }
    }).catch(() => {
        ElMessage.info('已取消刪除');
    })
}

// 批量刪除
const selectedEmployees = ref();
// 勾選框處理多選時的變化 - selection: 當前選重的紀錄 (Array)
const SelectionChange = (selection) => {
    // selectedEmployees.value = selection;
    selectedEmployees.value = selection.map( (emp) => emp.id) ;
    
};

// 批量刪除
const DeleteEmps = () => {
     ElMessageBox.confirm(`此操作將永久刪除選中的員工 是否繼續? `, '提示', {
        cancelButtonText: '取消',
        confirmButtonText: '確定刪除',
        type: 'warning' // 跳出的樣式 

    }).then(async () => {

        if ( selectedEmployees.value.length === 0 ) {
            ElMessage.warning('請選擇要刪除的員工');
            return;
        }

        const result = await deleteEmpApi(selectedEmployees.value);
        if ( result.code ) {
            ElMessage.success('刪除成功');
            search(); 
        } else {
            ElMessage.error('刪除失敗:' + result.msg);
        }
    }).catch(() => {
        ElMessage.info('已取消刪除');
    })
}



// ------------------------------------------------------------------------------------------- 
onMounted (() => {
    search();
    getToken(); // 獲取 token
})

// 獲取 token
const getToken = () => {
    token.value = localStorage.getItem('jwt_token')
}

</script>

<template>
    <h1>使用者管理</h1>

    <!-- 搜索欄 -->
    <div id="container">
        <!-- {{ UserForm }} -->
        <el-form :inline="true" :model="UserForm" class="demo-form-inline">
            <el-form-item label="使用者名稱">
                <el-input v-model="UserForm.name" placeholder="請輸入姓名" clearable />
            </el-form-item>
  
            <el-form-item>
                <el-button el-button type="primary" @click="search">查詢</el-button>
                <el-button el-button type="info" @click="clear">清空</el-button>
            </el-form-item>
        </el-form>
    </div>

    <!-- 新增 、批量員工按鈕 -->
    <div class="contanier">
        <el-button el-button type="primary" @click="AddEmp">新增使用者</el-button>
        <el-button el-button type="danger"  @click="DeleteEmps"> 批量刪除</el-button>
    </div><br>
    
    <!-- {{ selectedEmployees }} -->

    <!-- 數據表格顯示 -->
    <div class="table-container">
        <el-table  :data="empList" style="width: 100%" class="custom-table" @selection-change="SelectionChange">
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column type="index" label="編號" width="80" align="center"/>
            <el-table-column prop="username" label="帳號" width="150" align="center"/>
            <el-table-column prop="name" label="姓名" width="150" align="center" />
            
            <el-table-column prop="role" label="角色" width="120" align="center">
                <template #default="scope">
                    <el-tag :type="scope.row.role === 'ADMIN' ? 'success' : 'info'" size="small">
                        {{ scope.row.role === 'ADMIN' ? '管理員' : '一般員工' }}
                    </el-tag>
                </template>
            </el-table-column>
            
            <el-table-column prop="createdTime" label="創建時間" min-width="180" align="center" />

            <el-table-column label="操作" min-width="180" align="center" >
                <template #default="scope">
                    <el-button size="small" type="primary" @click="handleEdit(scope.row)"> 
                        <el-icon><EditPen /></el-icon> 編輯
                    </el-button>
                    <el-button size="small" type="danger" @click="handleDelete(scope.row)">
                        <el-icon><Delete /></el-icon> 刪除
                    </el-button>
                </template>
            </el-table-column>

            <template #empty>
                <div class="py-8 flex flex-col items-center justify-center text-slate-500">
                    <el-icon size="40" class="mb-2 text-purple-400"></el-icon>
                    <span class="text-lg">目前沒有使用者資料</span>
                </div>
            </template>
        </el-table>
        <br>
    </div>

    <!-- 分頁展示 -->
    <div class="contanier" >
        <!-- {{ currentPage }} : {{ pageSize }} -->
    <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[5 ,10, 20, 30, 40, 50]"
        :size="size"
        :disabled="disabled"
        :background="background"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalpage"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />
    </div>

    <!-- 新增員工(add) dialog -->
    <el-dialog v-model="addDialogVisible" :title="formTitle" >
        
       <el-form :model="EmpaddForm" :rules="rules" ref="EmpaddFormRef" label-width="90px">
            <el-row>
                <el-col :span="12">
                    <el-form-item label="使用者名稱" prop="username">
                        <el-input v-model="EmpaddForm.username" placeholder="請輸入用戶名" />
                    </el-form-item>
                </el-col>
                <el-col :span="12">
                    <el-form-item label="姓名" prop="name">
                        <el-input v-model="EmpaddForm.name" placeholder="請輸入姓名" />
                    </el-form-item>
                </el-col>
            </el-row>
            <el-row v-if="!EmpaddForm.id"> 
                <el-col :span="12">
                    <el-form-item label="密碼" prop="password">
                        <el-input v-model="EmpaddForm.password" placeholder="請輸入密碼" type="password" show-password/>
                    </el-form-item>
                </el-col>
            </el-row>

            <el-form-item size="small" lable-width="0px">
                <el-button type="danger" @click="deleteExperience">清除表單</el-button>
            </el-form-item>

        </el-form>

        <template #footer>
            <span class="dialog-footer">
                <el-button type="primary" @click="submitempAdd">確認{{ EmpaddForm.id ? '修改' : '新增' }}</el-button>
                <el-button type="danger" @click="addDialogVisible = false">取消</el-button>
            </span>
        </template>
    </el-dialog>


</template>

<style scoped>

.container{
    margin: 10px 0px;

}

/* 上傳的圖片樣式（撐滿容器） */
.avatar {
    height: 50%;
    object-fit: cover;  /* 避免圖片變形 */
}

.avatar-uploader .avatar {
    width: 78px;
    height: 78px;
    display: block; /* 將圖片轉換為塊級元素 */
}


.avatar-uploader .el-upload {
    border: 1px dashed var(--el-border-color);  
    border-radius: 6px;  /*圓角*/
    cursor: pointer;  /* 鼠標變為手型*/
    position: relative;  /* 使子元素可以相對於父元素定位*/
    overflow: hidden;  /* 隱藏溢出的內容*/
    transition: var(--el-transition-duration-fast); /* 平滑的轉換效果*/
}

/* 滑鼠懸停時邊框變色 */
.avatar-uploader:hover {
    border-color: #409EFF;  /* Element UI 的主色 */
}

.el-icon.avatar-uploader-icon  {
    font-size: 28px;  /* 圖標大小 */
    color: #8c939d;  /* 圖標顏色 */
    width: 150px;
    height: 150px;
    text-align: center;
    border-radius: 10px;
    border: 1px dashed var(--el-border-color);
}


/* 這裡假設您在 template 中使用了 .table-container 包裹 el-table */
.table-container {
    /* 確保容器使用主題面板背景色，以消除白色殘留 */
    background-color: var(--panel, #1e293b); 
    border-radius: 12px;
    padding: 0; /* 清除內部額外間距 */
    overflow: hidden;
    margin-top: 20px; /* 給頂部按鈕留出空間 */
}

/* 核心：覆蓋 Element Plus 表格的 CSS 變量，採用低飽和度色彩 */
:deep(.el-table) {
    /* 讓表格體透明，這樣它會透出 .table-container 的深色背景 */
    --el-table-bg-color: transparent !important;
    --el-table-tr-bg-color: transparent !important;
    
    /* 表頭：使用一個更深的暗色，以提供視覺上的層次感（如您提供的範例圖所示）*/
    --el-table-header-bg-color: #1a2434 !important;
    
    /* 邊框顏色：保持極淡，融入背景 */
    --el-table-border-color: rgba(255, 255, 255, 0.08) !important;
    --el-table-border: 1px solid var(--el-table-border-color) !important;
    
    /* 文字顏色：使用標準淺灰色（非螢光色） */
    --el-table-text-color: #cbd5e1 !important;
    --el-table-header-text-color: #f1f5f9 !important; /* 表頭文字稍亮 */
    
    /* Hover 高亮：微小的白色透明度 */
    --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.05) !important;
    
    background-color: transparent !important;
}

/* 強制表格的內容區塊也是透明的 */
:deep(.el-table__inner-wrapper) {
    background-color: transparent !important;
}

/* 讓表格單元格的背景透明並調整邊框 */
:deep(.el-table th.el-table__cell),
:deep(.el-table td.el-table__cell) {
    background-color: transparent !important; 
    border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
}

/* 調整空數據提示文字的顏色 */
:deep(.el-table__empty-text) {
    color: #94a3b8 !important; 
    font-size: 15px;
}

/* 去除表格底部的白色線條（Element Plus 默認邊框） */
:deep(.el-table::before) {
    height: 0 !important;
}

/* =============================== */
/* 3. 按鈕樣式加強 (如果全局沒生效) */
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


</style>