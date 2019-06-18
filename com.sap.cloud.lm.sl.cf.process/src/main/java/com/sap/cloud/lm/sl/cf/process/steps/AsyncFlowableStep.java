package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;

public abstract class AsyncFlowableStep extends SyncFlowableStep {

    private static final Integer DEFAULT_STEP_EXECUTION_INDEX = 0;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        StepPhase stepPhase = StepsUtil.getStepPhase(execution.getContext());
        if (stepPhase == StepPhase.POLL) {
            return executeStepExecution(execution);
        }
        execution.getContext()
            .setVariable(Constants.ASYNC_STEP_EXECUTION_INDEX, DEFAULT_STEP_EXECUTION_INDEX);
        return executeAsyncStep(execution);
    }

    @Override
    protected void onError(ExecutionWrapper execution, Exception e) throws Exception {
        StepPhase stepPhase = StepsUtil.getStepPhase(execution.getContext());
        if (stepPhase == StepPhase.POLL) {
            if (e instanceof MonitoringException) {
                throw e;
            }
            onPollingError(execution, e);
        } else {
            onStepError(execution.getContext(), e);
        }
    }

    private StepPhase executeStepExecution(ExecutionWrapper execution) throws Exception {
        List<AsyncExecution> stepExecutions = getAsyncStepExecutions(execution);

        AsyncExecutionState stepExecutionStatus = getStepExecution(execution, stepExecutions).execute(execution);
        return handleStepExecutionStatus(execution, stepExecutionStatus, stepExecutions);
    }

    private void onPollingError(ExecutionWrapper execution, Exception e) throws Exception {
        List<AsyncExecution> stepExecutions = getAsyncStepExecutions(execution);
        getStepExecution(execution, stepExecutions).onPollingError(execution, e);
    }

    private AsyncExecution getStepExecution(ExecutionWrapper execution, List<AsyncExecution> stepOperations) {
        Integer operationIndex = getStepExecutionIndex(execution.getContext());
        return stepOperations.get(operationIndex);
    }

    private Integer getStepExecutionIndex(DelegateExecution context) {
        return (Integer) context.getVariable(Constants.ASYNC_STEP_EXECUTION_INDEX);
    }

    private StepPhase handleStepExecutionStatus(ExecutionWrapper execution, AsyncExecutionState stepExecutionState,
        List<AsyncExecution> stepExecutions) {
        if (stepExecutionState == AsyncExecutionState.FINISHED) {
            StepsUtil.incrementVariable(execution.getContext(), Constants.ASYNC_STEP_EXECUTION_INDEX);
        }

        if (stepExecutionState == AsyncExecutionState.ERROR) {
            return StepPhase.RETRY;
        }

        if (stepExecutionState == AsyncExecutionState.RUNNING) {
            return StepPhase.POLL;
        }
        return determineStepPhase(execution.getContext(), stepExecutions);
    }

    private StepPhase determineStepPhase(DelegateExecution context, List<AsyncExecution> stepExecutions) {
        Integer stepExecutionIndex = getStepExecutionIndex(context);
        if (stepExecutionIndex >= stepExecutions.size()) {
            return StepPhase.DONE;
        }

        return StepPhase.POLL;
    }

    @Override
    protected StepPhase getInitialStepPhase(ExecutionWrapper execution) {
        StepPhase currentStepPhase = StepsUtil.getStepPhase(execution.getContext());
        if (currentStepPhase == StepPhase.DONE || currentStepPhase == StepPhase.RETRY) {
            return StepPhase.EXECUTE;
        }
        return currentStepPhase;
    }

    protected abstract StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception;

    protected abstract List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution);

}
