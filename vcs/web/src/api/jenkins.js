import axios from 'axios';


const triggerJenkinsBackendBuild = (projectName, env , branch) => {
  const PROXY_URL = "/jenkins-proxy";
  //const JENKINS_URL = "http://192.168.1.35:8088";
  const JOB_NAME = `backend-${env}`;
  const TOKEN = `${env}-yjjnoXvHXUE16TAmBzP4`;
  const USER = "admin";
  const API_TOKEN = "11a7af399de1d45513f9eb13e394ebe1f9";

  return axios.post(
    `${PROXY_URL}/job/${JOB_NAME}/buildWithParameters`, 
    null ,
    {
      baseURL: '',
      params: {
        token: TOKEN, // Job Token
        PROJECT_NAME: projectName,
        BRANCH: branch
      },
      auth: {
        username: USER,      // 這裡填 admin
        password: API_TOKEN  // 這裡填 User API Token
      }
    }
  );
};



export {
    triggerJenkinsBackendBuild
}