<script setup>
import { onMounted, ref } from 'vue'
import {queryDeptsApi , addDeptApi , updateDeptApi , deleteDeptApi } from '@/api/dept'
import { ElMessage , ElMessageBox } from 'element-plus'

// ----------------  查詢部門 ---------------- 
const DeptList = ref ([])
const search = async () => {

    let result = await queryDeptsApi();
    // console.log(result);
    
    if ( result.code ) { // JS 隱氏轉換 0 、 '' 、 undefined -> false  ， 其他都是 -> true
        DeptList.value = result.data;
    }
}

// ----------------  新增部門 ---------------- 
const formTitle = ref('');
const addDialogVisible = ref(false); // 對話框默認隱藏
const DeptaddForm = ref({ name: '' })

const DeptFormRef = ref();

// 新增部門的表單驗證規則
const rules = {
    name: [
        { required: true, message: '部門名稱必填', trigger: 'blur' },
        { min: 2, max: 20, message: '長度在 2 到 20 個字符', trigger: 'blur' }
    ]
};

const handleAddDept = () => {
    addDialogVisible.value = true; 
    formTitle.value = '新增部門';
    DeptaddForm.value.name = ''

    // 重製表單驗證
    if (!DeptFormRef.value) return;
    DeptFormRef.value.resetFields();

}
const submitDeptAdd = async () =>  {
    
    // 表單驗證
    if (!DeptFormRef.value) return;
    DeptFormRef.value.validate(async (valid) => { // valid 表示是否校驗通過 : true 通過、false 未通過
        if (valid) {
            const result = await addDeptApi(DeptaddForm.value);
            if ( result.code ) {
                ElMessage.success('操作成功');
                search();
                addDialogVisible.value = false;
            } else {
                ElMessage.error('操作失敗: ' + result.msg);
            }
        } else {
            ElMessage.error('操作失敗 : 表單驗證失敗');
        }
    });
}

// ----------------  編輯部門操作 打開編輯對話框 ---------------- 
const editForm = ref({ id: '', name: '' });
const editFormRef = ref(null);
const editDialogVisible = ref(false);

const handleEdit = async (row) => {
    
    formTitle.value = '編輯部門';

    // 將編輯的部門信息賦值給 editForm
    editForm.value = {
        id: row.id,
        name: row.name
    };
    editDialogVisible.value = true;

    // 重製表單驗證
    if (!DeptFormRef.value) return;
    DeptFormRef.value.resetFields();
    
}

const submitEdit = async () => {

    // 表單驗證
    if (!DeptFormRef.value) return;
    DeptFormRef.value.validate(async (valid) => { // valid 表示是否校驗通過 : true 通過、false 未通過
        if (valid) {
            const result = await updateDeptApi(editForm.value);
            if ( result.code ) {
                ElMessage.success('操作成功');
                search();
                editDialogVisible.value = false; 
            } else {
                ElMessage.error('操作失敗:'+ result.msg);
            }
        } else {
            ElMessage.error('操作失敗 : 表單驗證失敗');
        }
    })
}


// ----------------  刪除部門 ---------------- 
const handleDelete = async (row) => {

    ElMessageBox.confirm('此操作將永久刪除該部門, 是否繼續?', '提示', {
        cancelButtonText: '取消',
        confirmButtonText: '確定刪除',
        type: 'warning' // 跳出的樣式
    }).then(async ()  => { // 點擊 confirmButtonText 確定時觸發 
        const result = await deleteDeptApi(row.id); 
        if ( result.code ) {
            ElMessage.success('操作成功');
            search(); 
        } else {
            ElMessage.error('操作失敗:'+ result.msg); 
        }
    }) .catch(() => { // 點擊 cancelButtonText 取消時觸發 
        ElMessage.info('已取消刪除'); 
    })
}


// ---------------------------------------------------------------- 
onMounted(() => {
    search(); 
})

</script>

<template>
    <h1>版本管理</h1>

        <!-- 頂部按鈕 -->
        <div class="table-actions">
            <el-button type="primary" @click="handleAddDept">
            <el-icon><Plus /></el-icon>&nbsp; 版本管理
            </el-button>
        </div>
        <!-- 數據呈現 table -->

        
        <div class="table-container">
            <el-table :data="DeptList" style="width: 100%" class="custom-table" >
                <el-table-column type="index" label="編號" width="100" align="center"/>
                <el-table-column prop="name" label="部門名稱" width="150" align="center"/>
                <el-table-column prop="updateTime" label="最後操作時間" width="250" align="center"/>
                <el-table-column label="操作" align="center" >
                    <template #default="scope">
                        <el-button type="primary"  @click="handleEdit(scope.row)"> <el-icon><EditPen /></el-icon>  &nbsp; 編輯</el-button>
                        <el-button type="danger"  @click="handleDelete(scope.row)"><el-icon><Delete /></el-icon> &nbsp; 刪除</el-button>
                    </template>
                </el-table-column>
                <template #empty>
                    <div class="py-8 flex flex-col items-center justify-center text-slate-500">
                        <el-icon size="40" class="mb-2"><img src="" alt="" />&nbsp;&nbsp; ☁️&nbsp; &nbsp; </el-icon>
                        <span>目前沒有版本資料</span>
                    </div>
                </template>
            </el-table>
        </div>
    
        <!-- 新增部門(add) dialog 對話框 -->
        <el-dialog v-model="addDialogVisible" :title="formTitle" width="30%" >
            <el-form :model="DeptaddForm" :rules="rules" ref="DeptFormRef" label-width="100px">
                <el-form-item label="部門名稱" prop="name">
                    <el-input v-model="DeptaddForm.name" placeholder="請輸入部門名稱"></el-input>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button type="primary" @click="submitDeptAdd">確認新增</el-button>
                    <el-button type="danger" @click="addDialogVisible = false">取消</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 新增編輯(Edit) dialog 對話框 -->
         <el-dialog v-model="editDialogVisible" :title="formTitle" width="30%">
            {{ editForm }}
            <el-form :model="editForm" :rules="rules" ref="DeptFormRef" label-width="100px">
                <el-form-item label="部門編號" prop="id">
                    <el-input v-model="editForm.id" disabled></el-input> <!-- disabled 禁止編輯 ID 編號 -->
                </el-form-item>

                <el-form-item label="部門名稱" prop="name">
                    <el-input v-model="editForm.name" placeholder="請輸入部門名稱"></el-input>
                </el-form-item>

            </el-form>
            
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="editDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="submitEdit">確認</el-button>
                </span>
            </template>
        </el-dialog>
    
</template>

<style scoped>

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