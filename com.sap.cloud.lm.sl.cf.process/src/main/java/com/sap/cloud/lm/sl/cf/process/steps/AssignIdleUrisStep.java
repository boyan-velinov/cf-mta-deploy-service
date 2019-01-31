package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("assignIdleUrisStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AssignIdleUrisStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(getStartProgressMessage());
        StepsUtil.setUseIdleUris(execution.getContext(), true);
//        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(execution.getContext());
//        for (CloudApplicationExtended app : apps) {
//            assignUris(app);
//        }
//        StepsUtil.setAppsToDeploy(execution.getContext(), apps);
//        setAdditionalContextVariables(execution.getContext());

        getStepLogger().debug(getEndProgressMessage());
        return StepPhase.DONE;
    }

    private void reportAssignedUris(CloudApplication app) {
        for (String uri : app.getUris()) {
            getStepLogger().info(Messages.ASSIGNING_URI, uri, app.getName());
        }
    }

    private void assignUris(CloudApplicationExtended app) {
        List<String> newUris = getNewUris(app);
        if (newUris != null && !newUris.isEmpty()) {
            app.setUris(newUris);
            reportAssignedUris(app);
        }
    }

    protected String getStartProgressMessage() {
        return Messages.ASSIGNING_IDLE_URIS;
    }

    protected String getEndProgressMessage() {
        return Messages.IDLE_URIS_ASSIGNED;
    }

    protected List<String> getNewUris(CloudApplicationExtended app) {
        return app.getIdleUris();
    }

    protected void setAdditionalContextVariables(DelegateExecution context) {
        StepsUtil.setUseIdleUris(context, true);
    }
}
