package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

@Component("buildCloudUndeployModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildCloudUndeployModelStep extends SyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationSubscriptionDao dao;
    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.BUILDING_CLOUD_UNDEPLOY_MODEL);
        try {
            DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
            if (deployedMta == null) {
                setComponentsToUndeploy(execution.getContext(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                return StepPhase.DONE;
            }

            List<ConfigurationSubscription> subscriptionsToCreate = StepsUtil.getSubscriptionsToCreate(execution.getContext());
            Set<String> mtaModules = StepsUtil.getMtaModules(execution.getContext());
            List<String> appNames = StepsUtil.getAppsToDeploy(execution.getContext(), moduleToDeployHelper);
            List<CloudApplication> deployedApps = StepsUtil.getDeployedApps(execution.getContext());

            getStepLogger().debug(Messages.MTA_MODULES, mtaModules);

            List<DeployedMtaModule> modulesToUndeploy = computeModulesToUndeploy(deployedMta, mtaModules, appNames);
            getStepLogger().debug(Messages.MODULES_TO_UNDEPLOY, secureSerializer.toJson(modulesToUndeploy));

            List<DeployedMtaModule> modulesToKeep = computeModulesToKeep(modulesToUndeploy, deployedMta);
            getStepLogger().debug(Messages.MODULES_TO_KEEP, secureSerializer.toJson(modulesToKeep));

            List<ConfigurationSubscription> subscriptionsToDelete = computeSubscriptionsToDelete(subscriptionsToCreate, deployedMta,
                StepsUtil.getSpaceId(execution.getContext()));
            getStepLogger().debug(Messages.SUBSCRIPTIONS_TO_DELETE, secureSerializer.toJson(subscriptionsToDelete));

            Set<String> servicesForApplications = getServicesForApplications(execution.getContext());
            List<String> servicesToDelete = computeServicesToDelete(modulesToKeep, deployedMta.getServices(), servicesForApplications);
            getStepLogger().debug(Messages.SERVICES_TO_DELETE, servicesToDelete);

            List<CloudApplication> appsToUndeploy = computeAppsToUndeploy(modulesToUndeploy, deployedApps);
            getStepLogger().debug(Messages.APPS_TO_UNDEPLOY, secureSerializer.toJson(appsToUndeploy));

            setComponentsToUndeploy(execution.getContext(), servicesToDelete, appsToUndeploy, subscriptionsToDelete);

            getStepLogger().debug(Messages.CLOUD_UNDEPLOY_MODEL_BUILT);
            return StepPhase.DONE;
        } catch (Exception e) {
            getStepLogger().error(e, Messages.ERROR_BUILDING_CLOUD_UNDEPLOY_MODEL);
            throw e;
        }
    }

    private Set<String> getServicesForApplications(DelegateExecution context) {
        Set<String> servicesForApplications = new HashSet<>();
        List<Module> modules = StepsUtil.getModulesToDeploy(context);
        for (Module module : modules) {
            if (!moduleToDeployHelper.isApplication(module)) {
                continue;
            }
            servicesForApplications.addAll(StepsUtil.getServicesForApplication(context, module, moduleToDeployHelper));
        }
        return servicesForApplications;
    }

    private List<DeployedMtaModule> computeModulesToKeep(List<DeployedMtaModule> modulesToUndeploy, DeployedMta deployedMta) {
        return deployedMta.getModules()
            .stream()
            .filter(existingModule -> shouldKeepExistingModule(modulesToUndeploy, existingModule))
            .collect(Collectors.toList());
    }

    private boolean shouldKeepExistingModule(List<DeployedMtaModule> modulesToUndeploy, DeployedMtaModule existingModule) {
        String existingModuleName = existingModule.getModuleName();
        return modulesToUndeploy.stream()
            .map(DeployedMtaModule::getModuleName)
            .noneMatch(moduleName -> existingModuleName.equals(moduleName));
    }

    private void setComponentsToUndeploy(DelegateExecution context, List<String> services, List<CloudApplication> apps,
        List<ConfigurationSubscription> subscriptions) {
        StepsUtil.setSubscriptionsToDelete(context, subscriptions);
        StepsUtil.setServicesToDelete(context, services);
        StepsUtil.setAppsToUndeploy(context, apps);
    }

    private List<String> computeServicesToDelete(List<DeployedMtaModule> modulesToKeep, Set<String> existingServices,
        Set<String> servicesForApplications) {
        return existingServices.stream()
            .filter(service -> shouldDeleteService(modulesToKeep, service, servicesForApplications))
            .collect(Collectors.toList());
    }

    private boolean shouldDeleteService(List<DeployedMtaModule> modulesToKeep, String service, Set<String> servicesForApplications) {
        return modulesToKeep.stream()
            .map(DeployedMtaModule::getServices)
            .noneMatch(moduleToKeepService -> moduleToKeepService.contains(service)) && servicesForApplications.contains(service);
    }

    private List<DeployedMtaModule> computeModulesToUndeploy(DeployedMta deployedMta, Set<String> mtaModules, List<String> appsToDeploy) {
        return deployedMta.getModules()
            .stream()
            .filter(deployedModule -> shouldBeCheckedforUndeployment(deployedModule, mtaModules))
            .filter(deployedModule -> shouldUndeployModule(deployedModule, mtaModules, appsToDeploy))
            .collect(Collectors.toList());
    }

    private boolean shouldBeCheckedforUndeployment(DeployedMtaModule deployedModule, Set<String> mtaModules) {
        return mtaModules.contains(deployedModule.getModuleName());
    }

    private boolean shouldUndeployModule(DeployedMtaModule deployedModule, Set<String> mtaModules, List<String> appsToDeploy) {
        // The deployed module may be in the list of MTA modules, but the actual application that was created from it may have a
        // different name:
        return !appsToDeploy.contains(deployedModule.getAppName());
    }

    private List<CloudApplication> computeAppsToUndeploy(List<DeployedMtaModule> modulesToUndeploy, List<CloudApplication> deployedApps) {
        return deployedApps.stream()
            .filter(app -> shouldUndeployApp(modulesToUndeploy, app))
            .collect(Collectors.toList());
    }

    private boolean shouldUndeployApp(List<DeployedMtaModule> modulesToUndeploy, CloudApplication app) {
        return modulesToUndeploy.stream()
            .anyMatch(module -> module.getAppName()
                .equals(app.getName()));
    }

    private List<ConfigurationSubscription> computeSubscriptionsToDelete(List<ConfigurationSubscription> subscriptionsToCreate,
        DeployedMta deployedMta, String spaceId) {
        String mtaId = deployedMta.getMetadata()
            .getId();
        List<ConfigurationSubscription> existingSubscriptions = dao.findAll(mtaId, null, spaceId, null);
        return existingSubscriptions.stream()
            .filter(subscription -> !willBeCreatedOrUpdated(subscription, subscriptionsToCreate))
            .collect(Collectors.toList());
    }

    private boolean willBeCreatedOrUpdated(ConfigurationSubscription existingSubscription,
        List<ConfigurationSubscription> createdOrUpdatedSubscriptions) {
        return createdOrUpdatedSubscriptions.stream()
            .anyMatch(subscription -> areEqual(subscription, existingSubscription));
    }

    protected boolean areEqual(ConfigurationSubscription subscription1, ConfigurationSubscription subscription2) {
        return Objects.equals(subscription1.getAppName(), subscription2.getAppName())
            && Objects.equals(subscription1.getSpaceId(), subscription2.getSpaceId()) && Objects.equals(subscription1.getResourceDto()
                .getName(),
                subscription2.getResourceDto()
                    .getName());
    }

}
