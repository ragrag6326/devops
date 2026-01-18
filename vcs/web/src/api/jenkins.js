import axios from 'axios';


const PROXY_URL = "/jenkins-proxy";
//const PROXY_URL = "http://192.168.1.35:8088";
const USER = "admin";
const API_TOKEN = "11a7af399de1d45513f9eb13e394ebe1f9";


// 1. 獲取 CSRF Crumb 的函數
const getJenkinsCrumb = () => {
  return axios.get(`${PROXY_URL}/crumbIssuer/api/json`, {
    auth: {
      username: USER,
      password: API_TOKEN
    }
  });
};

const triggerJenkinsBackendBuild = async (projectName, env, branch) => {
  const JOB_NAME = `backend-${env}`;
  const JOB_TOKEN = `${env}-yjjnoXvHXUE16TAmBzP4`; // 這是 Job 配置裡的 Token

  try {
    // 第一步：先拿 Crumb
    const crumbRes = await getJenkinsCrumb();
    const crumbField = crumbRes.data.crumbRequestField; // 通常是 "Jenkins-Crumb"
    const crumbValue = crumbRes.data.crumb;

    // 第二步：帶著 Crumb 發送 Build 請求
    return axios.post(
      `${PROXY_URL}/job/${JOB_NAME}/buildWithParameters`,
      null,
      {
        params: {
          token: JOB_TOKEN, 
          PROJECT_NAME: projectName,
          BRANCH: branch
        },
        auth: {
          username: USER,
          password: API_TOKEN
        },
        headers: {
          // 重點：將 Crumb 加入 Header
          [crumbField]: crumbValue 
        }
      }
    );
  } catch (error) {
    console.error("Jenkins Trigger Error:", error);
    throw error;
  }
};

/**
 * 前端直接獲取 Jenkins Log
 * @param {string} jobname  Job Name
 * @param {number|string} buildNumber  Jenkins 的建置編號 (例如 123)
 */
const getJenkinsConsoleLog = (jobname, buildNumber) => {

  // const JOB_NAME = `${jobname}`;

  // Jenkins 獲取純文字 Log 的官方 API 路徑:
  // /job/{jobName}/{buildNumber}/consoleText
  const url = `${PROXY_URL}/job/${jobname}/${buildNumber}/consoleText`;

  return axios.get(url, {
    baseURL: '', // 確保不會被全域 baseURL 覆蓋
    auth: {
      username: USER,
      password: API_TOKEN
    },
    // 回傳的是純文字，不是 JSON，所以要設定 responseType
    responseType: 'text' 
  });
};

/**
 * 前端取得 Jenkins pipeline 的
 * @param {string} pipelineName  Job Name
 * @param {number|string} buildNumber  Jenkins 的建置編號 (例如 123)
 */
const getJenkinsPiplineNumber = async (pipelineName , upstreamJobName , upstreamBuildNumber) => {

  const url = `${PROXY_URL}/job/${pipelineName}/api/json?tree=builds[number,url,actions[causes[upstreamProject,upstreamBuild]]]`;

  try {
    const res = await axios.get(url, {
      baseURL: '',
      auth: {
        username: USER,
        password: API_TOKEN
      }
      // axios 預設就會把 JSON parse 成物件，所以不用擔心 responseType
    });

    // --- 解析邏輯開始 ---
    const builds = res.data.builds || [];

    // 使用 find 遍歷所有建置紀錄
    const foundBuild = builds.find(build => {
      // actions 是一個陣列，我們要找裡面含有 causes 的那個物件
      if (!build.actions) return false;

      // 檢查每一個 action
      return build.actions.some(action => {
        // 如果這個 action 有 causes 屬性
        if (action.causes) {
          // 檢查 causes 陣列裡是否包含我們要找的上游資訊
          return action.causes.some(cause => 
            cause.upstreamProject === upstreamJobName && 
            cause.upstreamBuild === parseInt(upstreamBuildNumber)
          );
        }
        return false;
      });
    });

    if (foundBuild) {
      //console.log(`🎉 找到對應的下游建置: #${foundBuild.number}`);
      return {
        number: foundBuild.number,
        url: foundBuild.url,
        result: foundBuild.result // SUCCESS, FAILURE, null (進行中)
      };
    } else {
      console.log('⚠️ 尚未找到對應的下游建置 (可能還在 Queue 中或未觸發)');
      return null;
    }

  } catch (error) {
    console.error('API 呼叫失敗', error);
    return null;
  }
};


export {
    triggerJenkinsBackendBuild,
    getJenkinsConsoleLog,
    getJenkinsPiplineNumber
};