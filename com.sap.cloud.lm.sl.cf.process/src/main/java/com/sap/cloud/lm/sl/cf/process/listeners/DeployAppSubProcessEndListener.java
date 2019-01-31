package com.sap.cloud.lm.sl.cf.process.listeners;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

@Component("deployAppSubProcessEndListener")
public class DeployAppSubProcessEndListener extends AbstractProcessExecutionListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeployAppSubProcessEndListener.class);

    @Override
    protected void notifyInternal(DelegateExecution context) throws Exception {
        ServiceUrl serviceUrl = StepsUtil.getServiceUrlToRegister(context);
        
        if (serviceUrl == null) {
            return;
        }
        
        StepsUtil.setVariableInParentProcess(context, Constants.VAR_APP_SERVICE_URL_VAR_PREFIX, serviceUrl);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
