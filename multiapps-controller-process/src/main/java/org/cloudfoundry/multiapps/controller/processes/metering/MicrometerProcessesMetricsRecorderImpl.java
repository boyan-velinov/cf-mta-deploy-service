package org.cloudfoundry.multiapps.controller.processes.metering;

import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.DEFAULT_TAG_VALUE;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.DYNATRACE_DIMENSION_MAX_VALUE_LENGTH;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.END_PROCESS_EVENT_MULTIAPPS_METRIC;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.ERROR_PROCESS_EVENT_MULTIAPPS_METRIC;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.MTA_ID_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.OVERALL_PROCESS_TIMER_MULTIAPPS_METRIC;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.SPACE_ID_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.START_PROCESS_EVENT_MULTIAPPS_METRIC;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

public class MicrometerProcessesMetricsRecorderImpl implements MicrometerProcessesMetricsRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerProcessesMetricsRecorderImpl.class);

    @Override
    public void recordOverallTime(DelegateExecution execution, State state, long processDurationInMillis) {
//        Map<String, String> additionalTags = new HashMap<String, String>();
//        additionalTags.put(OPERATION_STATE_TAG, state.name());
        recordProcessTimer(execution, OVERALL_PROCESS_TIMER_MULTIAPPS_METRIC, processDurationInMillis);
    }

    @Override
    public void recordStartProcessEvent(DelegateExecution execution) {
        recordProcessEvent(execution, START_PROCESS_EVENT_MULTIAPPS_METRIC);
    }

    @Override
    public void recordEndProcessEvent(DelegateExecution execution) {
        recordProcessEvent(execution, END_PROCESS_EVENT_MULTIAPPS_METRIC);
    }

    @Override
    public void recordErrorProcessEvent(DelegateExecution execution, String errorMessage) {
//        Map<String, String> additionalTags = new HashMap<String, String>();
//        additionalTags.put(PROCESS_MESSAGE_TAG, errorMessage);
        recordProcessEvent(execution, ERROR_PROCESS_EVENT_MULTIAPPS_METRIC);
    }
//
//    @Override
//    public void clearRegistry() {
//        LOGGER.warn("clearRegistry start");
//        Metrics.globalRegistry.forEachMeter(meter -> removeIfNecessary(meter));
//    }
//
//    private void removeIfNecessary(Meter meter) {
//        if(meter.getId().getName().startsWith(MULTIAPPS_METRICS_PREFIX)) {
//            LOGGER.warn("removeIfNecessary: " + meter.getId());
//            Metrics.globalRegistry.remove(meter);
//        }
//    }

    private void recordProcessEvent(DelegateExecution execution, String meterName) {
        Set<Tag> tags = buildStandardTags(execution);
        Counter counter = Metrics.globalRegistry.counter(meterName, tags);
        counter.increment();
    }

    private void recordProcessTimer(DelegateExecution execution, String meterName, long durationInMillis) {
        Set<Tag> tags = buildStandardTags(execution);
//        if (!MapUtils.isEmpty(additionalTags)) {
//            addAdditionalTags(tags, additionalTags);
//        }
        Timer timer = Metrics.globalRegistry.timer(meterName, tags);
        timer.record(durationInMillis, TimeUnit.MILLISECONDS);
    }

    private Set<Tag> buildStandardTags(DelegateExecution execution) {
        Set<Tag> tags = new HashSet<>();
//        addTagIfNull(tags, CORRELATION_ID_TAG, VariableHandling.get(execution, Variables.CORRELATION_ID));
        addTagIfNull(tags, SPACE_ID_TAG, VariableHandling.get(execution, Variables.SPACE_GUID));
//        addTagIfNull(tags, SPACE_NAME_TAG, VariableHandling.get(execution, Variables.SPACE_NAME));
//        addTagIfNull(tags, ORG_ID_TAG, VariableHandling.get(execution, Variables.ORGANIZATION_GUID));
//        addTagIfNull(tags, ORG_NAME_TAG, VariableHandling.get(execution, Variables.ORGANIZATION_NAME));
        addTagIfNull(tags, MTA_ID_TAG, VariableHandling.get(execution, Variables.MTA_ID));
//        safeExecutor.execute(() -> addTagIfNull(tags, PROCESS_TYPE_TAG, processTypeParser.getProcessType(execution)
//                                                                                         .getName()));
        return tags;
    }

//    private void addAdditionalTags(Set<Tag> tags, Map<String, String> additionalTags) {
//        additionalTags.forEach((tagKey, tagValue) -> addTagIfNull(tags, tagKey, tagValue));
//    }

    private void addTagIfNull(Set<Tag> tags, String tagKey, String tagValue) {
        if (tagValue == null) {
            tagValue = DEFAULT_TAG_VALUE;
        }
        tags.add(Tag.of(tagKey, correctTagValue(tagValue)));
    }

    private String correctTagValue(String tagValue) {
        return StringUtils.substring(tagValue, 0, DYNATRACE_DIMENSION_MAX_VALUE_LENGTH);
    }

}
