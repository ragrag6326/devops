import request from "@/utils/request";

const triggerJenkinsBackendBuild = (projectName, env ,branch) => {
  const JENKINS_URL = "http://192.168.1.35:8088";
  const JOB_NAME = `backend-${env}`;
  const TOKEN = "dev-yjjnoXvHXUE16TAmBzP4";
  const USER = "admin";
  const API_TOKEN = "11a7af399de1d45513f9eb13e394ebe1f9";

  return request.post(
    `${JENKINS_URL}/job/${JOB_NAME}/buildWithParameters`, null,
    {
      params: {
        token: TOKEN,
        PROJECT_NAME: projectName,
        BRANCH: branch,
      },
      auth: {
        username: USER,
        password: API_TOKEN,
      },
    }
  );
};



export {
    triggerJenkinsBackendBuild
}