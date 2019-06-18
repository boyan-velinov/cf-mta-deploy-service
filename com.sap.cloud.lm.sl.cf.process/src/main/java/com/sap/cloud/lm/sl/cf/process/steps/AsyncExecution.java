package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.common.SLException;

public interface AsyncExecution {

    AsyncExecutionState execute(ExecutionWrapper execution);
    
    void onPollingError(ExecutionWrapper execution, Exception e) throws SLException;

}
