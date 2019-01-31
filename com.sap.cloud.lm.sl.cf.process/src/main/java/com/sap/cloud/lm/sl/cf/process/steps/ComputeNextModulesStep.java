package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

@Component("computeNextModulesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ComputeNextModulesStep extends SyncFlowableStep {

    protected SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        getStepLogger().debug(Messages.COMPUTING_NEXT_MODULES_FOR_PARALLEL_ITERATION);
        List<? extends Module> allModulesToDeploy = StepsUtil.getModulesToDeploy(execution.getContext());
        List<? extends Module> completedApplications = StepsUtil.getIteratedModulesInParallel(execution.getContext());
        List<String> completedModuleNames = completedApplications.stream()
            .map(Module::getName)
            .collect(Collectors.toList());

        // Set next iteration data
        List<Module> modulesForNextIteration = computeApplicationsForNextIteration(allModulesToDeploy,
            completedModuleNames);
        StepsUtil.setModulesToIterateInParallel(execution.getContext(), modulesForNextIteration);

        // Mark next iteration data as computed
        StepsUtil.setIteratedModulesInParallel(execution.getContext(), ListUtils.union(completedApplications, modulesForNextIteration));

        getStepLogger().debug(Messages.COMPUTED_NEXT_MODULES_FOR_PARALLEL_ITERATION, secureSerializer.toJson(modulesForNextIteration));
        return StepPhase.DONE;
    }

    private List<Module> computeApplicationsForNextIteration(List<? extends Module> allModulesToDeploy,
        List<String> completedModules) {
        allModulesToDeploy.removeIf(module -> completedModules.contains(module.getName()));
        return allModulesToDeploy.stream()
            .filter(module -> applicationHasAllDependenciesSatisfied(completedModules, module))
            .collect(Collectors.toList());
    }

    private boolean applicationHasAllDependenciesSatisfied(List<String> completedModules, Module module) {
        if (!(module instanceof com.sap.cloud.lm.sl.mta.model.v3.Module)) {
            return true;
        }
        com.sap.cloud.lm.sl.mta.model.v3.Module upcastedModule = (com.sap.cloud.lm.sl.mta.model.v3.Module) module;
        return upcastedModule.getDeployedAfter()
            .isEmpty() || completedModules.containsAll(upcastedModule.getDeployedAfter());
    }

}
