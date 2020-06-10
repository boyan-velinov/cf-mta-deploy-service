package org.cloudfoundry.multiapps.controller.web.timers;

import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.MULTIAPPS_METRICS_PREFIX;

import java.util.TimerTask;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;

public class MicrometerProcessesMetricsCleaner implements RegularTimer {

    @Inject
    protected ApplicationConfiguration configuration;

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerProcessesMetricsCleaner.class);

    @Override
    public TimerTask getTimerTask() {
        LOGGER.warn("getTimerTask entering");
        return new TimerTask() {

            @Override
            public void run() {
                LOGGER.info("Micrometer cleaner started");
                clearRegistry();
                LOGGER.info("Micrometer cleaner ended");

            }
        };
    }

    @Override
    public long getDelay() {
        return configuration.getPeriodForMicrometerRegistryCleanup();
    }

    @Override
    public long getPeriod() {
        return configuration.getPeriodForMicrometerRegistryCleanup();
    }
    
    public void clearRegistry() {
        LOGGER.warn("clearRegistry start");
        Metrics.globalRegistry.forEachMeter(meter -> removeIfNecessary(meter));
    }

    private void removeIfNecessary(Meter meter) {
        if(meter.getId().getName().startsWith(MULTIAPPS_METRICS_PREFIX)) {
            LOGGER.warn("removeIfNecessary: " + meter.getId());
            Metrics.globalRegistry.remove(meter);
        }
    }

}
