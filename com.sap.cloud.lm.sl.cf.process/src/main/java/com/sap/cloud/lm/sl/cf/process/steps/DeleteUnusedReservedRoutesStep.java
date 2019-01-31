package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.OccupiedPortsDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

@Component("deleteUnusedReservedRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteUnusedReservedRoutesStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.DELETING_UNUSED_RESERVED_ROUTES);
            boolean portBasedRouting = StepsUtil.getVariableOrDefault(execution.getContext(), Constants.VAR_PORT_BASED_ROUTING, false);

            String defaultDomain = getDefaultDomain(execution.getContext());

            if (portBasedRouting) {
                PortAllocator portAllocator = clientProvider.getPortAllocator(execution.getXsControllerClient(), defaultDomain);
                portAllocator.setAllocatedPorts(StepsUtil.getAllocatedPorts(execution.getContext()));

                Map<String, Set<Integer>> usedApplicationPorts = getUsedApplicationPorts(execution.getContext());
                getStepLogger().debug(Messages.USED_APPLICATION_PORTS, usedApplicationPorts);
                freeUnusedPortsAllocatedForModulesToDeploy(execution.getContext(), portAllocator, usedApplicationPorts);

                freeUnusedPortsForNotDeployedModules(execution.getContext(), portAllocator, usedApplicationPorts);
                StepsUtil.setAllocatedPorts(execution.getContext(), portAllocator.getAllocatedPorts());
                getStepLogger().debug(Messages.ALLOCATED_PORTS, portAllocator.getAllocatedPorts());
            }

            getStepLogger().debug(Messages.UNUSED_RESERVED_ROUTES_DELETED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DELETING_UNUSED_RESERVED_ROUTES);
            throw e;
        }
    }

    private void freeUnusedPortsAllocatedForModulesToDeploy(DelegateExecution context, PortAllocator portAllocator,
        Map<String, Set<Integer>> usedApplicationPorts) {
        for (String module : usedApplicationPorts.keySet()) {
            portAllocator.freeAllExcept(module, usedApplicationPorts.get(module));
        }
    }

    private void freeUnusedPortsForNotDeployedModules(DelegateExecution context, PortAllocator portAllocator,
        Map<String, Set<Integer>> usedApplicationPorts) {
        Set<String> allMtaModules = StepsUtil.getMtaModules(context);
        allMtaModules.removeAll(usedApplicationPorts.keySet());
        // Remove all allocated ports for modules which are not marked for deploy
        for (String module : allMtaModules) {
            portAllocator.freeAllExcept(module, Collections.emptySet());
        }
    }

    private String getDefaultDomain(DelegateExecution context) {
        Map<String, Object> xsPlaceholderReplacementValues = StepsUtil.getXsPlaceholderReplacementValues(context);
        return (String) xsPlaceholderReplacementValues.get(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER);
    }

    private Map<String, Set<Integer>> getUsedApplicationPorts(DelegateExecution context) {
        List<Module> modulesToDeploy = StepsUtil.getModulesToDeploy(context);
        Map<String, Set<Integer>> allocatedPorts = new HashMap<>();
        ApplicationsCloudModelBuilder applicationsCloudModelBuilder = StepsUtil.getApplicationsCloudModelBuilder(context);
        OccupiedPortsDetector occupiedPortsDetector = new OccupiedPortsDetector();

        for (Module module : modulesToDeploy) {
            if (!moduleToDeployHelper.isApplication(module)) {
                continue;
            }
            List<String> applicationUris = applicationsCloudModelBuilder.getApplicationUris(module);
            allocatedPorts.put(module.getName(), occupiedPortsDetector.detectOccupiedPorts(applicationUris));
        }

        return allocatedPorts;
    }

    // private Set<String> getServicesForApplications(DelegateExecution context) {
    // Set<String> servicesForApplications = new HashSet<>();
    // List<Module> modules = StepsUtil.getModulesToDeploy(context);
    // for (Module module : modules) {
    // if (!moduleToDeployHelper.isApplication(module)) {
    // continue;
    // }
    // servicesForApplications.addAll(StepsUtil.getServicesForApplication(context, module, moduleToDeployHelper));
    // }
    // return servicesForApplications;
    // }
}
