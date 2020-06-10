package org.cloudfoundry.multiapps.controller.processes.metering;

import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.flowable.engine.delegate.DelegateExecution;

public interface MicrometerProcessesMetricsRecorder {

    public void recordOverallTime(DelegateExecution execution, State state, long processDurationInMillis);

    public void recordStartProcessEvent(DelegateExecution execution);

    public void recordEndProcessEvent(DelegateExecution execution);

    public void recordErrorProcessEvent(DelegateExecution execution, String errorMessage);
    
}
