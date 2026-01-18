<script setup>
import { onMounted, ref , watch , nextTick , onUnmounted } from 'vue'
import { AnsiUp } from 'ansi_up';

import { queryVersionPage , queryVersionById , deleteVersionById  , editRemark , getLatestSuccessVersion , getNextVersion , checkDeployable , updateJenkinsBuildId} from "@/api/version";
import { triggerJenkinsBackendBuild , getJenkinsConsoleLog , getJenkinsPiplineNumber } from "@/api/jenkins";
import { deploying } from "@/api/deploy";

import { ElMessage , ElMessageBox , ElLoading, sliderContextKey } from 'element-plus'
import axios from 'axios';

const token = ref ('');

// ---------------- 版本數據列表 ----------------
const versionList = ref([])
const loading = ref(false)

// 搜索欄
const projectNameOptions = [
    { label: "tkbgoapi", value: "tkbgoapi" },
    { label: "tkbtv", value: "tkbtv" },
    { label: "go_nuxt", value: "go_nuxt" },
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

// ---------------- 版本數據列表 ----------------
const multipleSelection = ref([]);


// --- jenkins 日誌視窗相關變數 ---
const logDialogVisible = ref(false);
const logContent = ref("");
const logLoading = ref(false);
const currentLogTitle = ref("");
const currentLogInfo = ref({
    env: '',
    version: '',
    buildId: '',
    jobName: '',
    url: '' ,
    pipeline_url: ''
});

// --- 輪詢控制變數 ---
let logTimer = null;         // 計時器 ID
const isPolling = ref(false); // 是否正在輪詢中


// 2. 實例化
const ansiUp = new AnsiUp();

// 監聽 dialog 關閉，確保停止輪詢
watch(logDialogVisible, (val) => {
    if (!val) {
        stopPolling();
    }
});

// 組件銷毀時也要確保停止
onUnmounted(() => {
    stopPolling();
});

// 停止輪詢的函數
const stopPolling = () => {
    if (logTimer) {
        clearTimeout(logTimer);
        logTimer = null;
    }
    isPolling.value = false;
};


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
const editDialogVisible = ref(false);  // 對話框默認隱藏
const versionFormRef = ref();          // 表單驗證

// (新增、修改) 版號表單 數據回顯
const versionForm = ref ({ 
    id: "" ,
    name : "" ,
    env: "",
    version: "",
    branch: "",
    remark: "",
    jenkinsBuildId: ""
})

// 監聽表單中 env 變化
watch(() => versionForm.value.env, async (newEnv) => {
    
    // 防呆：如果沒選專案，先不動作
    if (!versionForm.value.name) return;
    
    const projectName = versionForm.value.name;

    try {
        // ===============
        // 如選擇的是 Dev 
        // ===============
        if (newEnv === 'dev') {
            // 呼叫 /next 接口，取得 Dev 的下一個版號 (e.g. 1.0.20 -> 1.0.21)
            const res = await getNextVersion(projectName , 'dev'); // 假設這是您的 /api/version/next
            if (res.code === 1) {
                versionForm.value.version = res.data; // 自動填入 1.0.21
                versionForm.value.branch = 'develop'; // 自動帶入分支
                ElMessage.success(`已自動帶入 dev 部屬成功版本: ${res.data}`);
            }
        } 
        // ================
        // 如選擇的是 Prod
        // ================
        else if (newEnv === 'prod') {
            // Prod 的版號來源，必須是 "Dev 最後成功的版本"
            // 這裡發送請求查 Dev 的 latest-success
            const devLatestRes = await getNextVersion(projectName, 'dev');
            const devVer = devLatestRes.data; //  1.0.20

            if (!devVer) {
                ElMessage.error("Dev 尚無版本，無法部署 Prod");
                versionForm.value.version = '';
                return;
            }

            // 防呆：檢查 Prod 是否已經跟上這個版本了
            const prodLatestRes = await getNextVersion(projectName, 'prod');
            const prodVer = prodLatestRes.data; // 1.0.20 或 1.0.19

            // 如果 Dev (1.0.20) == Prod (1.0.20)
            // if (prodVer && devVer === prodVer) {
            //     ElMessageBox.alert(
            //         `目前 Prod 已是最新版本 (${prodVer})，與 Dev 同步。\n請先更新 Dev 環境後再執行此操作。`,
            //         '無法更新',
            //         { type: 'warning' }
            //     );
            //     // 清空版號，並建議讓確認按鈕 disable
            //     versionForm.value.version = '';
            //     return;
            // }

            // 通過檢查，自動填入 Dev 的版號 (e.g. 1.0.20)
            versionForm.value.version = prodVer;
            versionForm.value.branch = 'develop'; // Prod 通常固定分支
            ElMessage.success(`已自動帶入 Prod 部屬成功版本: ${prodVer}`);
        }

    } catch (e) {
        console.error(e);
    }
});

// 版號表單驗證規則
const rules = {
    name: [{ required: true, message: "請選擇專案", trigger: "change" }],
    env: [{ required: true, message: "請選擇環境", trigger: "change" }],
    branch: [{ required: true, message: "請輸入分支", trigger: "change" }],
    version: [
        { required: true, message: "請輸版號 格式: 1.0.0", trigger: "change" },
        { pattern: /^[\d]{1}\.[\d]+\.[\d]+$/ , message: '請輸入有效的版號 範例: 1.0.0', trigger: 'blur'}
    ]
}

// 版號表單初始化
const InitAddForm = () => {

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

    formTitle.value = '修改版號';
    editDialogVisible.value = true;
}

// 點開(編輯)版號表單 - 數據回顯
const handleEdit = async (row) => {
    InitEditForm();

    const result = await queryVersionById( row.id ) ;
    if ( result.code ) {
        versionForm.value = result.data;
    } else {
        // 處理錯誤情況 (可選)
        console.error('查詢失敗:', result.msg);
    }
}

// 移除操作
const handleDelete = async (row) => {

    ElMessageBox.confirm(`確定是否刪除 專案:${row.projectName} 環境:${row.projectEnv} 版號:${row.version} `, '提示', {
        cancelButtonText: '取消',
        confirmButtonText: '確定刪除',
        type: 'warning', 
        customClass: 'glass-confirm'
    }).then(async () => {
        const result = await deleteVersionById(row.id)
        console.log('handleDelete API回傳結果:', result);
        
        if ( result.code ) {
            ElMessage.success('刪除成功');
            search(); 
        } else {
            ElMessage.error('移除失敗:' + result.msg);
        }
    }).catch(() => {
        ElMessage.info('已取消');
    })
}

// -------------------------------------------------------------------------------
// 批量移除操作 監聽表格勾選事件 (Element Plus 自動傳入 val，即所有選中的行物件)
const handleSelectionChange = (val) => {
    multipleSelection.value = val;
};

// 批量移除操作
const handleBatchDelete = async () => {

    if (multipleSelection.value.length === 0) {
        return;
    }

    // 取出所有 ID 變成陣列，例如：[101, 102, 103]
    const ids = multipleSelection.value.map(item => item.id);
    console.log(ids);

    ElMessageBox.confirm(`確定要刪除這 ${ids.length} 筆資料嗎？此操作無法恢復。`, '警告', {
        confirmButtonText: '確定刪除',
        cancelButtonText: '取消',
        type: 'warning', 
        customClass: 'glass-confirm'
    }).then(async () => {
        try {
            const result = await deleteVersionById(ids)
            console.log('handleDelete API回傳結果:', result);
            
            if ( result.code ) {
                ElMessage.success('批量刪除成功');
                search(); 
                multipleSelection.value = [];
            } else {
                ElMessage.error(result.msg || '刪除失敗');
            }
        } catch (error){
            console.error('刪除錯誤:', error);
            ElMessage.error('系統發生錯誤');
        }
    }).catch(() => {
        ElMessage.info('已取消');
    })
}


// 輔助函數：將 Jenkins 回傳的絕對路徑轉為 Proxy 相對路徑
const getQueueApiUrl = (absoluteUrl) => {
    // 假設後端回傳的是 http://192.168.1.35:8088/queue/item/319/
    // 需要把它變成 /jenkins-proxy/queue/item/319/api/json
    // 使用正則表達式去掉 http://IP:PORT 部分
    const relativePath = absoluteUrl.replace(/^http:\/\/[^/]+/, '');
    // 確保路徑乾淨並加上 api/json
    return `/jenkins-proxy${relativePath.replace(/\/$/, '')}/api/json`;
};

// 輔助函數：延遲 (Sleep)
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

// 確認 版號表單 ( 新增 、 修改 )
const submitVersionAddandEdit = async () => {

    // 表單驗證
    if (!versionFormRef.value) return;

    versionFormRef.value.validate( async (valid) => { // valid 表示是否校驗通過 : true 通過、false 未通過
        if (valid) {

            let result ;
            
            if ( versionForm.value.id ) { 
                // 修改
                result = await editRemark(versionForm.value);
                console.log('API回傳結果:', result);
                
                if ( result.code ) {
                    editDialogVisible.value = false;
                    search();
                    ElMessage.success('修改成功');
                } else {
                    ElMessage.error('修改失敗:' + result.msg);
                }
            } else { 
                ElMessageBox.confirm(`此操作將進行環境更新 是否繼續? `, '提示', {
                    cancelButtonText: '取消',
                    confirmButtonText: '確定',
                    type: 'warning', // 跳出的樣式 
                    customClass: 'glass-confirm' // 自定義樣式類名
                }).then(async () => {
                    // 1. 開啟全螢幕 Loading，防止使用者重複點擊
                    const loadingInstance = ElLoading.service({
                        lock: true,
                        text: '正在進行版本檢查與部署請求...',
                        background: 'rgba(0, 0, 0, 0.7)',
                    });
                    let recordId = null;

                    try{
                        // 解構賦值
                        const { name: projectName, env: projectEnv, version } = versionForm.value;
                        
                        // ==============================
                        // 步驟 1: 檢查是否可部署 (Check)
                        // =============================
                        const checkResult = await checkDeployable(projectName, projectEnv, version);
                        console.log('checkDeployable API回傳結果:', checkResult);

                        if (!checkResult.code) {
                            throw new Error(checkResult.msg || "版號未通過檢查，請確認版本規則");
                        }

                        // ====================================
                        // 步驟 2: 標記部署狀態 (Mark Deploying)
                        // ====================================

                        // 構建 DTO (Data Transfer Object)
                        const deployParams = {
                            projectName,
                            projectEnv,
                            version,
                            user: 'Web-UI',
                        };
                        const deployResult = await deploying(deployParams);
                        console.log('deploying API回傳結果:', deployResult);

                        
                        if (!deployResult.code) {
                            throw new Error(deployResult.msg || "無法標記部署狀態");
                        }
                        recordId = deployResult.data;
                        console.log('部署紀錄 ID:', recordId);

                        versionForm.value.id = recordId;
                        const editRemarkResult = await editRemark(versionForm.value);
                        console.log('部署備註編輯結果:', editRemarkResult);

                        // ElMessage.success("通過版號檢查並標記部署狀態");

                        // ==========================================
                        // 步驟 3: 觸發 Jenkins (Trigger Jenkins)
                        // ==========================================
                        const jenkinsResult = await triggerJenkinsBackendBuild(projectName , projectEnv , versionForm.value.branch )

                        if (jenkinsResult.status !== 201) {
                            throw new Error(jenkinsResult.msg || "Jenkins 觸發失敗");
                        }

                        // 更新 Loading 文字
                        loadingInstance.setText('請求發送成功，等待 Jenkins 排程...');
                        //ElMessage.success('Jenkins 部署請求發送成功！');

                        // ==========================================
                        // 步驟 4: 輪詢 Queue 直到拿到 Build Number
                        // ==========================================
                        const queueLocation = jenkinsResult.headers['location'];
                        console.log('Queue URL:', queueLocation);

                        if (!queueLocation) throw new Error("Jenkins 未回傳 Queue 位置");

                        let buildNumber = null;
                        let attempts = 0;
                        const maxAttempts = 30; // 最大嘗試次數 (例如 30次 * 2秒 = 60秒)


                        // 開始輪詢
                        while (!buildNumber && attempts < maxAttempts) {
                            attempts++;
                            loadingInstance.setText(`等待 Jenkins 執行中... (${attempts}/${maxAttempts})`);
                            
                            try {
                                const queueApiUrl = getQueueApiUrl(queueLocation);
                                // 帶上 Auth Header
                                const qRes = await axios.get(queueApiUrl, {
                                    auth: { username: "admin", password: "11a7af399de1d45513f9eb13e394ebe1f9" } 
                                });

                                if (qRes.data.executable && qRes.data.executable.number) {
                                    buildNumber = qRes.data.executable.number;
                                } else if (qRes.data.cancelled) {
                                    throw new Error("部署任務在 Jenkins 佇列中被取消");
                                } else {
                                    // 還沒開始，等待 2 秒再試
                                    await sleep(2000);
                                }
                            } catch (qErr) {
                                console.warn("查詢 Queue 失敗，稍後重試", qErr);
                                await sleep(2000); // 失敗也等待一下
                            }
                        }

                        if (!buildNumber) {
                            throw new Error("等待 Jenkins 建置超時，請稍後檢查 Jenkins 狀態");
                        }

                        // ========================
                        // 成功拿到 Build Number
                        // ========================
                        console.log(`Jenkins 建置開始！Build ID: ${buildNumber}`);
                        ElMessage.success(`部署成功啟動！Jenkins Build #${buildNumber}`);
                        
                        
                        const BuildIdParams = {
                            id: recordId,
                            jenkinsBuildId: buildNumber,
                        };

                        const updateResult = await updateJenkinsBuildId(BuildIdParams);
                        console.log('JenkinsBuildId更新結果:', updateResult);

                        addDialogVisible.value = false;
                        search()

                    } catch (error) {
                        console.error('部署流程發生錯誤:', error);

                        if (recordId) {
                            try {
                                const deleteDeployRecordId = await deleteVersionById(recordId);
                                console.log('部署紀錄 ID ' + deleteDeployRecordId + ' 已刪除 (回滾)');
                            } catch (deleteError) {
                                console.error('回滾刪除失敗:', deleteError);
                            }
                        }

                        ElMessage.error(error.message || "系統發生未預期錯誤，請稍後再試");
                        search()
                    } finally {
                        // 無論成功或失敗，最後要關閉 Loading
                        loadingInstance.close();
                    }
                }).catch(() => {
                    ElMessage.info('已取消');
                })
            }
            
        } else {
            ElMessage.error("表單驗證未通過 .... 請重新確認")
        }
    })
}

// --- 查看日誌的函數 ---
const handleViewLog = async (row) => {


    // 1. 設定 Dialog 標題
    currentLogTitle.value = `專案: ${row.projectName} 環境: ${row.projectEnv} 版號: ${row.version}`;

    const buildId = row.jenkinsBuildId || row.jenkins_build_id; 
    let jobName = "";
    let pipelineName = "";
    let safePipelineUrl = 'Not yet generate pipeline_url , try again later when job finished';
    let pipeline_link = ""


    if ( row.projectName == 'go_nuxt') {
        jobName="frontend-"
    } else {
        jobName="backend-"
    }

    if ( row.projectEnv == 'dev') {
        jobName=`${jobName}dev`
    } else {
        pipelineName=`${jobName}pipeline`
        jobName=`${jobName}prod`
        pipeline_link = await getJenkinsPiplineNumber(pipelineName , jobName , buildId);
    }

    if (pipeline_link && pipeline_link.url) {
        safePipelineUrl = pipeline_link.url + 'pipeline-overview/';
    }
    const console_url = `http://192.168.1.35:8088/job/${jobName}/${buildId}/console`


    
    // 1. 設定顯示資訊
    currentLogInfo.value = {
        env: row.projectEnv || 'Unknown Env', // 環境
        version: row.version || 'Unknown Ver', // 版本
        buildId: row.jenkinsBuildId || row.jenkins_build_id || '-', // Build ID
        jobName: jobName || 'Unknown Job' ,// 專案/Job 名稱
        url: console_url || 'Unknown console_url' ,
        pipeline_url: safePipelineUrl
    };

    //console.log(jobName);
    
    if (!buildId) {
        ElMessage.warning("尚未生成 Jenkins Build ID，請稍後再試或是確認部署狀態");
        return;
    }

    if (!jobName) {
        ElMessage.error("找不到專案名稱 (Job Name)，無法跳轉");
        return;
    }

    openLogWindow(jobName, buildId);
}

// 取得 jenkins log
const openLogWindow = async (env, buildNumber) => {
    logDialogVisible.value = true;
    logContent.value = "正在讀取日誌...";
    stopPolling(); // 防止重複開啟

    isPolling.value = true;

    await pullLogRecursive(env, buildNumber, 0);
}

// --- 核心：遞迴讀取日誌 ---
// startOffset: Jenkins API 支援從某個 byte 開始讀取 (增量讀取)，
// 如果您的 API 封裝不支援 start 參數，傳 0 每次讀全部也可以 (但日誌大時會變慢)
const pullLogRecursive = async (env, buildNumber, startOffset = 0) => {
    // 1. 如果視窗關閉了，就停止執行
    if (!logDialogVisible.value || !isPolling.value) return;

    try {
        // 呼叫 API (假設 getJenkinsConsoleLog 支援第三個參數 start)
        // 如果您的 API 不支援 start，就只傳 env, buildNumber，但每次都會拿全部
        const res = await getJenkinsConsoleLog(env, buildNumber);
        
        const rawLog = res.data; // 這次拿到的文字片段
        
        // --- 判斷是否還有新資料 ---
        // Jenkins 通常會在 Header 回傳 X-More-Data: true 代表還在跑
        // 或是我們簡單判斷：如果這次拿到的 rawLog 為空，且 build 狀態還沒結束，就繼續等
        const hasMoreData = res.headers && res.headers['x-more-data'] === 'true';
        
        // 或是透過日誌內容暴力判斷是否結束 (Jenkins 標準結尾)
        const isFinished = rawLog.includes('Finished: SUCCESS') || rawLog.includes('Finished: FAILURE') || rawLog.includes('Finished: ABORTED');

        // --- 處理畫面顯示 ---
        if (rawLog) {
            // 轉換顏色
            const htmlFragment = ansiUp.ansi_to_html(rawLog);

            // 如果是增量讀取 (Offset > 0)，我們要用「追加」的方式
            // 如果是全量讀取 (每次都拿全部)，則是「覆蓋」
            if (startOffset > 0) {
                 logContent.value += htmlFragment;
            } else {
                 // 如果您的 API 每次都回傳整包，這裡直接覆蓋
                 // 注意：每次覆蓋畫面會閃爍，建議後端支援 start 參數
                 logContent.value = htmlFragment;
            }

            // 捲動到底部
            nextTick(() => {
                const terminal = document.getElementById('terminal-content');
                if (terminal) {
                    terminal.scrollTop = terminal.scrollHeight;
                }
            });
        }

        // --- 決定下一動作 ---
        if (isFinished) {
            // A. 已結束：停止輪詢
            stopPolling();
            // 補上最後的提示 (可選)
            logContent.value += '<br/><span style="color:#aaa">---日誌結束---</span>';
        } else {
            // B. 未結束：計算新的 offset 並繼續輪詢
            
            // Jenkins Header 會回傳 X-Text-Size 告訴你目前總大小，下次從這裡開始抓
            let nextOffset = 0;
            if (res.headers && res.headers['x-text-size']) {
                nextOffset = parseInt(res.headers['x-text-size'], 10);
            } else {
                // 如果沒有 header，簡單做法是全量重抓 (offset 維持 0)
                // 或者自己計算長度 (不精準，建議用 API Header)
                nextOffset = 0; 
            }

            // 設置計時器，2秒後再抓一次
            logTimer = setTimeout(() => {
                pullLogRecursive(env, buildNumber, nextOffset);
            }, 2000); 
        }

    } catch (error) {
        console.error("Log Polling Error:", error);
        
        // 遇到錯誤 (例如 404 剛開始還沒生成 Log)，不要馬上死掉，可以 retry 幾次
        // 這裡簡單做：如果還在開啟狀態，就休息 3 秒再試一次 (可能是網路波動)
        if (logDialogVisible.value) {
             logTimer = setTimeout(() => {
                pullLogRecursive(env, buildNumber, 0); // 失敗重試通常從頭抓比較保險
            }, 3000);
        }
    }
}


    // try {
    //     // 直接從前端發送請求
    //     const res = await getJenkinsConsoleLog(env, buildNumber);
        
    //     // 轉換 ANSI 編碼為 HTML
    //     const rawLog = res.data;
    //     // 將 ANSI 轉為 HTML，並將換行符 \n 轉為 HTML 換行 (如果不是用 <pre> 標籤的話需要)
    //     const htmlLog = ansiUp.ansi_to_html(rawLog);

    //     logContent.value = htmlLog;

    //     // 使用 nextTick 確保 DOM 已經更新完 HTML 內容後再捲動
    //     nextTick(() => {
    //         const terminal = document.getElementById('terminal-content');
    //         if (terminal) {
    //             terminal.scrollTop = terminal.scrollHeight;
    //         }
    //     });

    //     // 自動捲動到底部
    //     // setTimeout(() => {
    //     //     const terminal = document.getElementById('terminal-content');
    //     //     if (terminal) terminal.scrollTop = terminal.scrollHeight;
    //     // }, 100);

    // } catch (error) {
    //     console.error(error);
    //     if (error.response && error.response.status === 404) {
    //         logContent.value = '<span style="color: #ff5f56;">找不到該 Build ID 的日誌，可能已被刪除或尚未開始。</span>';
    //     } else {
    //         logContent.value = '<span style="color: #ff5f56;">讀取失敗，請確認 Jenkins 狀態。</span>';
    //     }
    // }
//}


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

        <el-button el-button type="primary" @click="InitAddForm">新增版號</el-button>
        <el-button el-button type="danger" :disabled="multipleSelection.length === 0" @click="handleBatchDelete"> 批量刪除</el-button>

    </div>

    
    <!-- 數據表格顯示 -->
    <div class="table-container">

        <el-table :data="versionList" border style="width:100%" v-loading="loading" @selection-change="handleSelectionChange">
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
                <el-table-column prop="remark" label="備註" min-width="50"/>
                <el-table-column prop="createdTime" label="建立時間" min-width="60"/>
                <!-- <el-table-column prop="updatedTime" label="更新時間" min-width="60" /> -->

                <el-table-column label="操作" min-width="60" >
                    <template #default="scope">
                        <el-button type="primary"  @click="handleEdit(scope.row)"> <el-icon><EditPen /></el-icon>  &nbsp; 編輯</el-button>
                        <el-button type="danger"  @click="handleDelete(scope.row)"><el-icon><Delete /></el-icon> &nbsp; 刪除</el-button>
                    </template>
                </el-table-column>

                <el-table-column label="查看jenkins操作日誌" min-width="35" align="center">
                    <template #default="scope">
                        <el-tooltip content="查看建置日誌" placement="top">
                            <el-button circle type="info" plain :disabled="!scope.row.jenkinsBuildId" @click="handleViewLog(scope.row)">
                                <el-icon ><Document /></el-icon>
                            </el-button>
                        </el-tooltip>
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
    <el-dialog v-model="addDialogVisible" :title="formTitle" width="600px" class="custom-edit-dialog">

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

            <el-form-item label="版號" prop="version">
                <el-input v-model="versionForm.version" placeholder="" style="width:50%"/>
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

    <!-- 修改版號(edit) dialog -->
    <el-dialog v-model="editDialogVisible" :title="formTitle" width="600px" class="custom-edit-dialog">

        <el-form :model="versionForm" :rules="rules" ref="versionFormRef" label-width="90px">

            <el-form-item label="備註">
                <el-input type="textarea" v-model="versionForm.remark" />
            </el-form-item>

        </el-form>

        <template #footer>
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitVersionAddandEdit()">確認</el-button>
        </template>
    </el-dialog>


    <el-dialog 
        v-model="logDialogVisible" 
        :title="'建置日誌: ' + currentLogTitle" 
        width="900px" 
        class="terminal-dialog"
        destroy-on-close
        top="5vh"
    >
        <div class="log-info-bar">
            <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="專案環境">
                    <el-tag type="success" size="small">{{ currentLogInfo.env }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="版本號">
                    <el-tag type="primary" size="small">{{ currentLogInfo.version }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="Jenkins Build ID">
                    <span style="font-family: monospace; font-weight: bold;">
                        #{{ currentLogInfo.buildId }}
                    </span>
                </el-descriptions-item>

                <el-descriptions-item label="console 連結" >
                    <el-link :href="currentLogInfo.url" target="_blank" :underline="false">
                        <el-tag type="primary" size="small">{{ currentLogInfo.url }}</el-tag>
                    </el-link>
                </el-descriptions-item>

                <el-descriptions-item label="pipeline 連結" v-if="currentLogInfo.env == 'prod'" >
                    <el-link :href="currentLogInfo.pipeline_url" target="_blank" :underline="false">
                        <el-tag type="primary" size="small">{{ currentLogInfo.pipeline_url }}</el-tag>
                    </el-link>
                </el-descriptions-item>

            </el-descriptions>
        </div>

        <div class="terminal-window" v-loading="logLoading" element-loading-background="rgba(0, 0, 0, 0.8)">
            <div class="terminal-header">
                <!-- <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span> -->
                <pre  id="terminal-content" class="terminal-body" v-html="logContent"></pre>
            </div>
            <!-- <pre id="terminal-content" class="terminal-body">{{ logContent }}</pre> -->
        </div>
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


.glass-confirm .el-button--primary {
    /* 確保按鈕使用您定義的漸層和發光效果 */
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
    border: none !important;
    color: #fff !important; /* 文字顏色 */
    box-shadow: 0 4px 10px rgba(99, 102, 241, 0.4);
    font-weight: 600 !important;
    transition: all 0.3s ease;
}

.glass-confirm .el-button--primary:hover {
    opacity: 0.9;
    transform: translateY(-1px);
}


/* 2. ❌ 取消按鈕 (Cancel Button) - 玻璃透明/淡白色邊框 */
.glass-confirm .el-button--default,
.glass-confirm .el-button--info {
    /* 移除預設的白色背景 */
    background: rgba(255, 255, 255, 0.1) !important; 
    
    /* 增加透明邊框和柔和文字顏色 */
    border: 1px solid rgba(255, 255, 255, 0.2) !important;
    color: var(--text-main, #f1f5f9) !important; 
    font-weight: 500 !important;
}

.glass-confirm .el-button--default:hover,
.glass-confirm .el-button--info:hover {
    /* 懸停時背景輕微加亮 */
    background: rgba(255, 255, 255, 0.2) !important;
    border-color: rgba(255, 255, 255, 0.3) !important;
}

/* 3. 調整警告圖標顏色 (選填，使警告色更柔和) */
.glass-confirm .el-icon-warning {
    color: #fbbf24 !important; /* 柔和的黃色/琥珀色 */
}
</style>

<style>
/* 注意：這裡沒有 scoped
   因為 Dialog 被掛載到 body，scoped 樣式無法觸及
*/

/* 1. 針對這個 Dialog 內的 "確認" 按鈕 (Primary) */
.custom-edit-dialog .el-dialog__footer .el-button--primary {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); /* 紫色系漸層 */
    border: none;
    box-shadow: 0 2px 6px rgba(99, 102, 241, 0.4);
    color: white;
    transition: all 0.3s;
}

.custom-edit-dialog .el-dialog__footer .el-button--primary:hover {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.5);
}

/* 2. 針對這個 Dialog 內的 "取消" 按鈕 (Default) */
/* 排除 primary 和 danger，剩下的就是普通按鈕 */
.custom-edit-dialog .el-dialog__footer .el-button:not(.el-button--primary):not(.el-button--danger) {
    background-color: #f3f4f6; /* 淡淡的灰色背景 */
    border: 1px solid #e5e7eb;
    color: #4b5563;
}

.custom-edit-dialog .el-dialog__footer .el-button:not(.el-button--primary):not(.el-button--danger):hover {
    background-color: #e5e7eb;
    color: #111827;
    border-color: #d1d5db;
}
</style>

<style>
/* 終端機 Dialog 樣式 (放在 scoped 外或 global) */
.terminal-dialog .el-dialog__body {
    padding: 0 !important;
    background-color: #1e1e1e;
}
.terminal-dialog .el-dialog__header {
    background-color: #2d2d2d;
    margin-right: 0;
    padding-bottom: 15px;
}
.terminal-dialog .el-dialog__title {
    color: #ccc;
    font-family: monospace;
}
</style>

<style scoped>
.log-info-bar {
    margin-bottom: 15px;
}

/* 終端機視窗本體 */
.terminal-window {
    background-color: #1e1e1e; /* VSCode 深色背景 */
    color: #d4d4d4;             /* 淺灰文字 */
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    border-radius: 0 0 4px 4px;
    overflow: hidden;
}

.terminal-window {
    background-color: #1e1e1e;
    border-radius: 6px;
    box-shadow: 0 4px 10px rgba(0,0,0,0.3);
    overflow: hidden;
    font-family: 'Consolas', 'Monaco', monospace;
}

/* 模擬 Mac 視窗紅黃綠燈 */
.terminal-header {
    background-color: #252526;
    padding: 8px 15px;
    display: flex;
    gap: 8px;
    border-bottom: 1px solid #333;
}
.dot { width: 12px; height: 12px; border-radius: 50%; }
.dot.red { background-color: #ff5f56; }
.dot.yellow { background-color: #ffbd2e; }
.dot.green { background-color: #27c93f; }

/* Log 內容區 */
.terminal-body {
    background-color: #1e1e1e; /* 深色背景 */
    color: #f0f0f0;            /* 預設文字顏色 (白色/淺灰) */
    padding: 15px;
    margin: 0;
    white-space: pre-wrap;     /* 保留換行格式 */
    word-break: break-all;
    max-height: 500px;
    overflow-y: auto;
    font-family: 'Consolas', 'Monaco', monospace; /* 等寬字體 */
    font-size: 13px;
    line-height: 1.5;
}

/* 自定義捲軸 */
.terminal-body::-webkit-scrollbar {
    width: 10px;
}
.terminal-body::-webkit-scrollbar-track {
    background: #1e1e1e; 
}
.terminal-body::-webkit-scrollbar-thumb {
    background: #444; 
    border-radius: 5px;
}
.terminal-body::-webkit-scrollbar-thumb:hover {
    background: #555; 
}
</style>