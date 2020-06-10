package org.cloudfoundry.multiapps.controller.web.configuration;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.processes.metering.EmptyMicrometerProcessesMetricsRecorder;
import org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants;
import org.cloudfoundry.multiapps.controller.processes.metering.MicrometerProcessesMetricsRecorder;
import org.cloudfoundry.multiapps.controller.processes.metering.MicrometerProcessesMetricsRecorderImpl;
import org.cloudfoundry.multiapps.controller.web.timers.MicrometerProcessesMetricsCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

@Configuration
public class MicrometerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicrometerConfiguration.class);
    public static final String DYNATRACE_SERVICE_NAME = "deploy-service-dynatrace";
    private static final String CLIENT_CONNECTIONS_METRICS_PREFIX = "reactor.netty.connection.provider.cloudfoundry-client.";

    @Inject
    @Bean
    public CompositeMeterRegistry compositeMeterRegistry(ApplicationConfiguration appConfig) {
        // Skip creation of registry when Dynatrace is not bound
        if (!dynatraceExist()) {
            LOGGER.info("Skipping registering JmxMeterRegistry");
            return null;
        }
        Integer stepInSeconds = appConfig.getMicrometerStepInSeconds();

        JmxConfig reactorNettyRegistryConfig = initJmxConfigRegistry(stepInSeconds, "reactor");

        JmxMeterRegistry reactorNettyRegistry = new JmxMeterRegistry(reactorNettyRegistryConfig, Clock.SYSTEM);
        reactorNettyRegistry.config()
                            .meterFilter(MeterFilter.acceptNameStartsWith(CLIENT_CONNECTIONS_METRICS_PREFIX))
                            // .meterFilter(MeterFilter.acceptNameStartsWith(MicrometerConstants.MULTIAPPS_METRICS_PREFIX))
                            .meterFilter(MeterFilter.deny());
        // Metrics.globalRegistry.add(reactorNettyRegistry);

        JmxConfig multiappsStartEventsRegistryConfig = initJmxConfigRegistry(stepInSeconds,
                                                                             MicrometerConstants.START_PROCESS_EVENT_MULTIAPPS_METRIC);
        JmxMeterRegistry multiappsStartEventsRegistry = new JmxMeterRegistry(multiappsStartEventsRegistryConfig, Clock.SYSTEM);
        multiappsStartEventsRegistry.config()
                            .meterFilter(MeterFilter.acceptNameStartsWith(MicrometerConstants.START_PROCESS_EVENT_MULTIAPPS_METRIC))
                            .meterFilter(MeterFilter.deny());
        JmxConfig multiappsErrorEventsRegistryConfig = initJmxConfigRegistry(stepInSeconds,
                                                                             MicrometerConstants.ERROR_PROCESS_EVENT_MULTIAPPS_METRIC);
        JmxMeterRegistry multiappsErrorEventsRegistry = new JmxMeterRegistry(multiappsErrorEventsRegistryConfig, Clock.SYSTEM);
        multiappsErrorEventsRegistry.config()
                            .meterFilter(MeterFilter.acceptNameStartsWith(MicrometerConstants.ERROR_PROCESS_EVENT_MULTIAPPS_METRIC))
                            .meterFilter(MeterFilter.deny());
        CompositeMeterRegistry registry = new CompositeMeterRegistry(Clock.SYSTEM,
                                                                     Arrays.asList(reactorNettyRegistry, multiappsStartEventsRegistry, multiappsErrorEventsRegistry));
        Metrics.globalRegistry.add(registry);
        return registry;
    }

    private JmxConfig initJmxConfigRegistry(Integer stepInSeconds, String domain) {
        return new JmxConfig() {

            @Override
            public Duration step() {
                if (stepInSeconds == null) {
                    return Duration.ofSeconds(15);
                }
                return Duration.ofSeconds(stepInSeconds);
            }

            @Override
            public String domain() {
                return domain;
            }

            @Override
            @Nullable
            public String get(String k) {
                return null;
            }
        };
    }

    @Bean
    public MicrometerProcessesMetricsRecorder micrometerProcessesMetricsRecorder() {
        if (!dynatraceExist()) {
            LOGGER.info("Missing dynatrace, EmptyMicrometerProcessesMetricsRecorder...");
            return new EmptyMicrometerProcessesMetricsRecorder();
        }
        return new MicrometerProcessesMetricsRecorderImpl();
    }

    @Bean
    public MicrometerProcessesMetricsCleaner micrometerProcessesMetricsCleaner() {
        if (!dynatraceExist()) {
            LOGGER.info("Missing dynatrace, MicrometerProcessesMetricsCleaner is not required...");
            return null;
        }
        return new MicrometerProcessesMetricsCleaner();
    }

    private boolean dynatraceExist() {
        try {
            CloudFactory cloudFactory = new CloudFactory();
            Cloud cloud = cloudFactory.getCloud();
            cloud.getServiceInfo(DYNATRACE_SERVICE_NAME);
            LOGGER.info("Dynatrace service instance found");
            return true;
        } catch (Exception e) {
            LOGGER.warn(MessageFormat.format("Failed to detect service info for service \"{0}\"!", DYNATRACE_SERVICE_NAME), e);
        }
        return false;
    }

}
