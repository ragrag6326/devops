package com.tkb.service;

import com.tkb.dto.DeployDTO;

public interface DeployService  {

    void markDeploying(DeployDTO dto);

    void markSuccess(DeployDTO dto);

    void markFail(DeployDTO dto);

    void markRollback(DeployDTO dto);
}
