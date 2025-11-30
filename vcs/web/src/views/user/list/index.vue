<script setup>
import { onMounted, ref } from 'vue'
import { queryUserPage, addUserApi, updateUserApi, deleteUserApi  } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Delete } from '@element-plus/icons-vue'

// token（如果之後請求要帶可以用）
const token = ref('')

// ---------- 搜索表單 ----------
const searchUserForm = ref({
  name: ''
})

// 使用者列表 & 分頁
const userList = ref([])
const currentPage = ref(1)
const pageSize = ref(5)
const totalPage = ref(0)
const background = ref(true)

// 查詢使用者列表
const search = async () => {
  try {
    const result = await queryUserPage(
      searchUserForm.value.name,
      currentPage.value,
      pageSize.value
    )
    console.log('queryUserPage result:', result)

    if (result && result.code) {
      userList.value = result.data.rows || []
      totalPage.value = result.data.total || 0
    } else {
      ElMessage.error(result?.msg || '查詢失敗')
    }
  } catch (error) {
    console.error('Error fetching user data:', error)
    ElMessage.error('查詢使用者失敗')
  }
}

// 清空搜尋表單
const clear = () => {
  for (const key in searchUserForm.value) {
    searchUserForm.value[key] = ''
  }
  // 可選：清空後重新查詢
  search()
}

// 分頁事件
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  search()
}
const handleCurrentChange = (val) => {
  currentPage.value = val
  search()
}

// ----------------- 新增 / 修改 使用者 -----------------
const formTitle = ref('')
const addDialogVisible = ref(false)
const userFormRef = ref()

const userForm = ref({
  id: '',
  username: '',
  name: '',
  password: ''
})

// 打開「新增」對話框
const addUser = () => {
  formTitle.value = '新增使用者'

  // 清空表單
  for (const key in userForm.value) {
    userForm.value[key] = ''
  }

  // 重置表單驗證
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }

  addDialogVisible.value = true
}

// 表單驗證規則
const rules = {
  username: [
    { required: true, message: '請輸入使用者名', trigger: 'blur' },
    { min: 2, max: 20, message: '長度在 2 到 20 個字符', trigger: 'blur' }
  ],
  password: [
    {
      required: true,
      message: '請輸入密碼',
      trigger: 'blur',
      // 只有新增時必填，編輯時可以略過
      validator: (rule, value, callback) => {
        if (!userForm.value.id && !value) {
          callback(new Error('請輸入密碼'))
        } else {
          callback()
        }
      }
    },
    { min: 6, max: 20, message: '密碼長度在 6 到 20 個字符', trigger: 'blur' }
  ]
}

// 送出表單（新增 / 修改）
const submitUser = async () => {
  if (!userFormRef.value) return

  userFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.error('表單驗證未通過，請重新確認')
      return
    }

    let result
    try {
      if (userForm.value.id) {
        // 修改
        result = await updateUserApi(userForm.value)
        if (result.code) {
          ElMessage.success('使用者修改成功')
        } else {
          ElMessage.error('使用者修改失敗: ' + result.msg)
        }
      } else {
        // 新增
        result = await addUserApi(userForm.value)
        if (result.code) {
          ElMessage.success('新增使用者成功')
        } else {
          ElMessage.error('新增使用者失敗: ' + result.msg)
        }
      }

      addDialogVisible.value = false
      search()
    } catch (e) {
      console.error(e)
      ElMessage.error('操作失敗，請稍後再試')
    }
  })
}

// ----------------- 編輯使用者 -----------------
const handleEdit = (row) => {
  formTitle.value = '編輯使用者'

  // 先清空再塞入，避免殘留舊值
  for (const key in userForm.value) {
    userForm.value[key] = ''
  }
  Object.assign(userForm.value, row)

  if (userFormRef.value) {
    userFormRef.value.clearValidate()
  }

  addDialogVisible.value = true
}

// ----------------- 刪除使用者 -----------------
const selectedUsers = ref([])

// 單筆刪除
const handleDelete = (user) => {
  ElMessageBox.confirm(`此操作將永久刪除 "${user.name}" 使用者，是否繼續?`, '提示', {
    cancelButtonText: '取消',
    confirmButtonText: '確定刪除',
    type: 'warning'
  }).then(async () => {
    try {
      const result = await deleteUserApi(user.id)
      if (result.code) {
        ElMessage.success('刪除成功')
        search()
      } else {
        ElMessage.error('刪除失敗: ' + result.msg)
      }
    } catch (e) {
      console.error(e)
      ElMessage.error('刪除失敗')
    }
  }).catch(() => {
    ElMessage.info('已取消刪除')
  })
}

// 多選變化
const selectionChange = (selection) => {
  selectedUsers.value = selection.map((u) => u.id)
}

// 批量刪除
const deleteUsers = () => {
  if (!selectedUsers.value || selectedUsers.value.length === 0) {
    ElMessage.warning('請先勾選要刪除的使用者')
    return
  }

  ElMessageBox.confirm(`此操作將永久刪除選中的使用者，是否繼續?`, '提示', {
    cancelButtonText: '取消',
    confirmButtonText: '確定刪除',
    type: 'warning'
  }).then(async () => {
    try {
      const result = await deleteUserApi(selectedUsers.value)
      if (result.code) {
        ElMessage.success('刪除成功')
        search()
      } else {
        ElMessage.error('刪除失敗: ' + result.msg)
      }
    } catch (e) {
      console.error(e)
      ElMessage.error('刪除失敗')
    }
  }).catch(() => {
    ElMessage.info('已取消刪除')
  })
}

// 清空整個新增/編輯表單（dialog 裡的「清除表單」按鈕）
const resetUserForm = () => {
  for (const key in userForm.value) {
    userForm.value[key] = ''
  }
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}

// 取得 token（如果未來需要帶在 header 可以從這裡取）
const getToken = () => {
  token.value = localStorage.getItem('jwt_token') || ''
}

// 初始化
onMounted(() => {
  getToken()
  search()
})
</script>


<template>
  <h1>使用者管理</h1>

  <!-- 搜索欄 -->
  <div class="container">
    <el-form :inline="true" :model="searchUserForm" class="demo-form-inline">
      <el-form-item label="使用者名稱">
        <el-input v-model="searchUserForm.name" placeholder="請輸入姓名" clearable />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="search">查詢</el-button>
        <el-button type="info" @click="clear">清空</el-button>
      </el-form-item>
    </el-form>
  </div>

  <!-- 新增 、批量刪除 -->
  <div class="container">
    <el-button type="primary" @click="addUser">新增使用者</el-button>
    <el-button type="danger" @click="deleteUsers">批量刪除</el-button>
  </div>

  <!-- 使用者列表 -->
  <div class="table-container">
    <el-table :data="userList" style="width: 100%" class="custom-table" @selection-change="selectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column type="index" label="編號" width="80" align="center" />
      <el-table-column prop="username" label="帳號" width="150" align="center" />
      <el-table-column prop="name" label="姓名" width="150" align="center" />

      <el-table-column prop="role" label="角色" width="120" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'ADMIN' ? 'success' : 'info'" size="small">
            {{ scope.row.role === 'ADMIN' ? '管理員' : '一般使用者' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createdTime" label="創建時間" min-width="180" align="center"/>

      <el-table-column label="操作" min-width="180" align="center">
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
          <span class="text-lg">目前沒有使用者資料</span>
        </div>
      </template>
    </el-table>
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

  <!-- 新增 / 編輯 使用者 dialog -->
  <el-dialog v-model="addDialogVisible" :title="formTitle">
    <el-form
      :model="userForm"
      :rules="rules"
      ref="userFormRef"
      label-width="90px"
    >
      <el-row>
        <el-col :span="12">
          <el-form-item label="使用者名稱" prop="username">
            <el-input v-model="userForm.username" placeholder="請輸入用戶名" />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="userForm.name" placeholder="請輸入姓名" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 只有新增時顯示密碼欄位 -->
      <el-row v-if="!userForm.id">
        <el-col :span="12">
          <el-form-item label="密碼" prop="password">
            <el-input
              v-model="userForm.password"
              placeholder="請輸入密碼"
              type="password"
              show-password
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item>
        <el-button type="danger" @click="resetUserForm">清除表單</el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button type="primary" @click="submitUser">
          確認{{ userForm.id ? '修改' : '新增' }}
        </el-button>
        <el-button type="danger" @click="addDialogVisible = false">取消</el-button>
      </span>
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
/*    分頁組件樣式 (Pagination)     */
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