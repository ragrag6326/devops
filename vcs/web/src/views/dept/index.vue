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
    <h1>部門管理</h1>

    <el-button type="primary" @click="handleAddDept"> <el-icon><Plus /></el-icon>  &nbsp; 新增部門</el-button>

        <div class="el-table">
            <el-table :data="DeptList" style="width: 100%" border fit="true" >
                <el-table-column type="index" label="編號" width="100" align="center"/>
                <el-table-column prop="name" label="部門名稱" width="150" align="center"/>
                <el-table-column prop="updateTime" label="最後操作時間" width="250" align="center"/>
                <el-table-column label="操作" align="center" >
                    <template #default="scope">
                        <el-button type="primary"  @click="handleEdit(scope.row)"> <el-icon><EditPen /></el-icon>  &nbsp; 編輯</el-button>
                        <el-button type="danger"  @click="handleDelete(scope.row)"><el-icon><Delete /></el-icon> &nbsp; 刪除</el-button>
                    </template>
                </el-table-column>
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

.el-table {
    margin: 10px 0px;
}


</style>