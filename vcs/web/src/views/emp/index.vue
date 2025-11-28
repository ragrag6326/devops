<script setup>
import { onMounted, ref , watch } from 'vue'
import { queryEmpPage , addempApi , queryEmpByIdApi , updateEmpApi , deleteEmpApi } from "@/api/emp";
import { queryDeptsApi } from "@/api/dept";
import { ElMessage , ElMessageBox } from 'element-plus'

// 原數據
const jobs = ref (
    [
        { name : "投資長" , value : "1"},
        { name : "多頭司令" , value : "2"},
        { name : "空軍一號", value : "3"},
        { name : "不只三分鐘", value : "4"},
        { name : "出貨分析師", value : "5"},
        { name : "老船長", value : "6"}
    ]
);

const token = ref ('');

// ----------------  查詢部門 ---------------- 
const DeptList = ref ([])
const searchDept = async () => {

    let result = await queryDeptsApi();
    
    if ( result.code ) { // JS 隱氏轉換 0 、 '' 、 undefined -> false  ， 其他都是 -> true
        DeptList.value = result.data;
    }
}

// ----------  搜索表單  ---------- 
const searchEmp = ref ({
    name : "" , gender: "" , date: [] , begin: "" , end: ""
})

// 監聽 date 屬性
watch (() => searchEmp.value.date, (newVal, oldVal) => {
    if (newVal.length == 2) {
        // console.log(newVal);
        searchEmp.value.begin = newVal[0];
        searchEmp.value.end = newVal[1];  
    } else {
        searchEmp.value.begin = '';
        searchEmp.value.end = '';
        return;
    }
})

// 查詢員工列表
const search = async () => {
    
 try {
        const result = await queryEmpPage(
            searchEmp.value.name,
            searchEmp.value.gender ,
            searchEmp.value.begin,
            searchEmp.value.end,
            currentPage.value,
            pageSize.value
        )
        console.log(result);
        if (result && result.code) {
            empList.value = result.data.rows;
            totalpage.value = result.data.total;
        }
    } catch (error) {
        console.error('Error fetching employee data:', error);
    }
}

// 清空
const clear = () => {
    for (let key in searchEmp.value) {
        searchEmp.value[key] = ''; 
    }
}


// ----------------- 員工數據列表 --------------------------------- 
const empList = ref([])


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

// ----------------- 新增 、 修改 員工 --------------------------------- 
const formTitle = ref('');
const addDialogVisible = ref(false); // 對話框默認隱藏
const EmpaddFormRef = ref(); // 表單驗證

const EmpaddForm = ref ({ 
    id : "",
    username : "",
    name : "" ,
    gender: "",
    image: "",
    deptId: "",
    deptName: "",
    job: "",
    phone: "",
    salary: "",
    entrydate: "",
    updateTime: "",
    exprList: []
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

const handleAvatarSuccess = ( response ) => {
    EmpaddForm.value.image = response.data;
}

// 文件上傳前 檢查圖片 格式、大小
const beforeAvatarUpload = (rawFile) => {
    if ( rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png' ) {
        ElMessage.error("只支援上傳圖片")
        return false;
    } else if ( rawFile.size / 1024 / 1024 > 10 ) {
        ElMessage.error("圖片大小不能超過 10MB")
        return false;
    }
    return true
}
// 增加工作經歷
const addExperience = () => {
    EmpaddForm.value.exprList.push({
        company: '' ,
        job: '' ,
        exprDate: [],
        begin: '',
        end: ''
    });
}

// 刪除工作經歷
const deleteExperience = () => { 
    if (EmpaddForm.value.exprList.length > 0) {
        EmpaddForm.value.exprList.pop(); // 移除最後一個元素
    }
}

// 監聽 EmpaddForm 中的 exprDate 屬性
watch(() => EmpaddForm.value.exprList, (newVal, oldVal) => {
    if ( EmpaddForm.value.exprList && EmpaddForm.value.exprList.length > 0 ) {
        EmpaddForm.value.exprList.forEach( (expr) => {
            if (expr.exprDate && expr.exprDate.length === 2) {
                expr.begin = expr.exprDate[0];
                expr.end = expr.exprDate[1];
            } else {
                expr.begin = '';
                expr.end = '';
            }
        });
    }
} , {deep: true} )



// 保存前表單驗證
const rules = {
    username: [
        { required: true, message: '請輸入用戶名', trigger: 'blur' },
        { min: 2, max: 20, message: '長度在 2 到 20 個字符', trigger: 'blur' }
    ] ,
    name: [
        { required: true, message: '請輸入姓名', trigger: 'blur' },
        { min: 2, max: 10, message: '長度在 2 到 10 個字符', trigger: 'blur' }
    ] ,
    gender: [
        { required: true, message: '請選擇性別', trigger: 'change' }
    ] ,
    phone: [
        { required: true, message: '請輸入手機或電話號碼', trigger: 'blur' },
        { pattern: /^0\d{9}$/ , message: '請輸入有效的手機或電話號碼', trigger: 'blur' }
    ] 
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
    <h1>員工管理</h1>

    <!-- 搜索欄 -->
    <div id="container">
        <!-- {{ searchEmp }} -->
        <el-form :inline="true" :model="searchEmp" class="demo-form-inline">
            <el-form-item label="姓名">
                <el-input v-model="searchEmp.name" placeholder="請輸入姓名" clearable />
            </el-form-item>
            <el-form-item label="性別" >
                <el-select v-model="searchEmp.gender" style="width: 100px" placeholder="性別" clearable @clear="searchEmp.gender = ''">
                <el-option label="男" value="1" />
                <el-option label="女" value="2" />
            </el-select>
            </el-form-item>
            <el-form-item label="日期">
                <el-date-picker
                    v-model="searchEmp.date"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="入職開始日期"
                    end-placeholder="入職結束日期"
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

    <!-- 新增 、批量員工按鈕 -->
    <div class="contanier">
        <el-button el-button type="primary" @click="AddEmp">新增員工</el-button>
        <el-button el-button type="danger"  @click="DeleteEmps"> 批量刪除</el-button>
    </div><br>
    
    <!-- {{ selectedEmployees }} -->

    <!-- 數據表格顯示 -->
    <el-table :data="empList" border style="width: 100%" @selection-change="SelectionChange">
        <el-table-column type="selection" width="35" align="center" />

        <!-- <el-table-column prop="username" label="用戶名" width="80" /> -->
        <el-table-column prop="name" label="姓名" width="120" align="center" />

        <el-table-column label="性別" width="60" align="center" >
            <template #default="scope">
                {{ scope.row.gender == 1 ? '男' : '女' }}
            </template>
        </el-table-column>

        <el-table-column label="圖片" width="120" align="center">
            <template #default="scope">
                <img :src=scope.row.image height="100px">
            </template>
        </el-table-column>

        <el-table-column prop="deptName" label="部門" width="120" align="center" />
        <el-table-column prop="job" label="職稱" width="120" align="center">
            <template #default="scope">
                <span v-if="scope.row.job == 1">空軍1號</span>
                <span v-else-if="scope.row.job == 2">多頭總司令</span>
                <span v-else-if="scope.row.job == 3">分析分析師的分析師</span>
                <span v-else-if="scope.row.job == 4">永不停損</span>
                <span v-else>股海愚夫</span>

            </template>
        </el-table-column>

        
        <el-table-column prop="entrydate" label="入職時間" width="120" align="center" />
        <el-table-column prop="updateTime" label="更新時間" width="200" align="center" />
        <el-table-column label="操作" align="center" >
            <template #default="scope">
                <el-button type="primary"  @click="handleEdit(scope.row)"> <el-icon><EditPen /></el-icon>  &nbsp; 編輯</el-button>
                <el-button type="danger"  @click="handleDelete(scope.row)"><el-icon><Delete /></el-icon> &nbsp; 刪除</el-button>
            </template>
        </el-table-column>
    </el-table><br>


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
        <!-- {{ EmpaddForm }} -->
        <el-form :model="EmpaddForm" :rules="rules" ref="EmpaddFormRef" label-width="80px">
            <!-- 第一行 -->
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="用戶名" prop="username">
                        <el-input v-model="EmpaddForm.username" placeholder="請輸入用戶名" />
                    </el-form-item>
                </el-col>

                <el-col :span="12">
                    <el-form-item label="姓名" prop="name">
                        <el-input v-model="EmpaddForm.name" placeholder="請輸入姓名" />
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- 第二行 -->
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="性別" prop="gender">
                        <el-select v-model="EmpaddForm.gender" placeholder="性別" style="width: 75px" clearable>
                            <el-option label="男" value="1"></el-option>
                            <el-option label="女" value="2"></el-option>
                        </el-select>
                    </el-form-item>
                </el-col>

                <el-col :span="12">
                    <el-form-item label="手機號碼" prop="phone">
                        <el-input v-model="EmpaddForm.phone" placeholder="請輸入手機號碼" />
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- 第三行 -->  
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="職位" >
                        <el-select v-model="EmpaddForm.job" placeholder="請輸入職位" style="width: 150px">
                            <!-- <el-option v-for="job in jobs" :key="job.value" :lable="job.name" :value="job.value" ></el-option> -->
                            <el-option v-for="job in jobs" :key="job.value" :label="job.name" :value="job.value"></el-option>
                        
                        </el-select>
                    </el-form-item>
                </el-col>

                <el-col :span="12">
                    <el-form-item label="薪水" prop="salary">
                        <el-input v-model="EmpaddForm.salary" placeholder="請輸入薪資" />
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- 第四行 -->  
            <el-row :gutter="20">
                <el-col :span="12">
                    <el-form-item label="投顧單位" prop="deptid">
                        <!-- {{ DeptList }} -->
                        <!-- <el-select v-model="EmpaddForm.deptId" placeholder="請輸入單位" style="width: 150px" clearable>
                            <el-option label="魔鬼投顧" value="1"></el-option>
                            <el-option label="華信投顧" value="2"></el-option>
                            <el-option label="啟發投顧" value="3"></el-option>
                            <el-option label="倫元投顧" value="4"></el-option>
                            <el-option label="浦惠投顧" value="5"></el-option>
                        </el-select> -->
                        <el-select placeholder="請輸入單位" style="width: 150px" v-model="EmpaddForm.deptId" clearable>
                             <el-option v-for="Dept  in DeptList" :key="Dept.id" :label="Dept.name" :value="Dept.id" ></el-option>
                        </el-select>

                    </el-form-item>
                </el-col>

                <el-col :span="12">
                    <el-form-item label="入職日期" prop="entrydate">
                        <el-date-picker
                            v-model="EmpaddForm.entrydate"
                            type="date"
                            placeholder="選擇入職日期"
                            value-format="YYYY-MM-DD">
                        </el-date-picker>
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- 第五行 -->  
            <!-- el-upload .name 為啥要改成 image 因為在後端 upload 設定接收的參數為 MultipartFile "image" -->
            <el-row :gutter="20">
                <el-col :span="24">
                    <el-form-item label="頭像" prop="image" >
                        <el-upload class="avatar-uploader"
                            name = "image"
                            action="api/upload"
                            :headers="{'token': token}"
                            :show-file-list="false"
                            :on-success="handleAvatarSuccess"
                            :before-upload="beforeAvatarUpload">
                            <img v-if="EmpaddForm.image" :src="EmpaddForm.image" class="avatar">
                            <!-- 沒有圖片時顯示帶虛線外框的 User 圖標 -->
                            <el-icon v-else class="avatar-uploader-icon"><User /></el-icon>

                        </el-upload>
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- 第六行 -->
            <el-row :gutter="20" >
                <el-col :span="24">
                    <el-form-item label="工作經歷"  >
                        <el-button type="success" @click="addExperience"> + 新增工作經歷</el-button>
                    </el-form-item>
                </el-col>
            </el-row>

            <!-- 第七行 -->
            <el-row :gutter="1" v-for="expr ,index in EmpaddForm.exprList" :key="index">
                {{ index }} {{ expr }}
                <el-col :span="12">
                    <el-form-item label="時間"  lable-width="80px" >
                        <el-date-picker
                            type="daterange"
                            v-model="expr.exprDate"
                            range-separator="至"
                            start-placeholder="開始"
                            end-placeholder="結束"
                            value-format="YYYY-MM-DD"
                            clearable
                        />
                    </el-form-item>
                </el-col>

                <el-col :span="6">
                    <el-form-item label="公司" lable-width="80px">
                        <el-input placeholder="請輸入公司名稱"  v-model="expr.company"/>
                    </el-form-item>
                </el-col>

                <el-col :span="6">
                    <el-form-item label="職位" lable-width="80px">
                        <el-input placeholder="請輸入職位" v-model="expr.job" />
                    </el-form-item>
                </el-col>
            </el-row>
            
            <el-form-item size="small" lable-width="0px">
                <el-button type="danger" @click="deleteExperience">清除</el-button>
            </el-form-item>


        </el-form>

        <template #footer>
            <span class="dialog-footer">
                <el-button type="primary" @click="submitempAdd">確認新增</el-button>
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


</style>