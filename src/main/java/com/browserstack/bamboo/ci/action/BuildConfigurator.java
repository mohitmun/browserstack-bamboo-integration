package com.browserstack.bamboo.ci.action;

import com.atlassian.bamboo.variable.VariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.bamboo.variable.VariableType;
import com.atlassian.bamboo.build.CustomPreBuildAction;
import com.atlassian.bamboo.v2.build.BaseConfigurableBuildPlugin;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.browserstack.bamboo.ci.BStackEnvVars;
import com.atlassian.spring.container.ContainerManager;
import org.apache.commons.lang.StringUtils;
import com.atlassian.bamboo.plan.Plan;
import com.browserstack.bamboo.ci.BStackConfigManager;
import com.browserstack.bamboo.ci.singletons.BrowserStackLocalSingleton;
import com.browserstack.bamboo.ci.local.BambooBrowserStackLocal;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bandana.BandanaManager;
import java.util.Arrays;
import java.util.Map;
import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.exception.AppAutomateException;
import java.io.FileNotFoundException;
import com.browserstack.automate.exception.InvalidFileExtensionException;

/*
  This configures the Job level BrowserStack settings. And starts the Local Binary if required.
*/

/**
 * @author Pulkit Sharma
 */

public class BuildConfigurator extends BaseConfigurableBuildPlugin implements CustomPreBuildAction {

    private AdministrationConfigurationAccessor administrationConfigurationAccessor;
    private BStackConfigManager configManager;
    private BandanaManager bandanaManager;



    /* 
      Invoked when Build is about to start.
    */
    @Override
    public BuildContext call() {
      this.configManager = new BStackConfigManager(administrationConfigurationAccessor.getAdministrationConfiguration(), buildContext.getBuildDefinition().getCustomConfiguration(), bandanaManager);

      if(configManager.hasCredentials()) {
        if(configManager.localEnabled()) {
          startLocal();
        }
        if(configManager.isAppAutomateEnabled()) {
          enableAppAutomate();
        }
      }
      
      return buildContext;
    }

    private void startLocal() {
      BuildLoggerManager buildLoggerManager = (BuildLoggerManager) ContainerManager.getComponent("buildLoggerManager");
      final BuildLogger buildLogger = buildLoggerManager.getLogger(buildContext.getResultKey());
      
      //Setting BambooBrowserStackLocal instance to null because updated configuration was not updated in the Singleton instance(because it was already in memory ??)
      BrowserStackLocalSingleton.reset();
      String accessKey = configManager.get(BStackEnvVars.BSTACK_ACCESS_KEY);
      String localArgs = configManager.get(BStackEnvVars.BSTACK_LOCAL_ARGS);
      BambooBrowserStackLocal browserStackLocal = BrowserStackLocalSingleton.getBrowserStackLocal(accessKey, localArgs);

      if(StringUtils.isNotBlank(localArgs)) {
        localArgs = "with args " + localArgs;
      }


      try {
        buildLogger.addBuildLogEntry("Starting BrowserStackLocal Binary " + localArgs);
        browserStackLocal.start();
        //Add Sleep Here ? Got 'browserstack.local is set to true but BrowserStackLocal binary is not connected error.'
        buildLogger.addBuildLogEntry("BrowserStackLocal Binary started successfully. LocalIdentifier: " + browserStackLocal.getLocalIdentifier());
        injectVariable(buildContext, BStackEnvVars.BSTACK_LOCAL_IDENTIFIER,browserStackLocal.getLocalIdentifier());
      } catch (Exception e) {
        buildLogger.addBuildLogEntry("Exception while starting the BrowserStackLocal Binary : " + e.toString());
        throw new RuntimeException("Exception while starting the BrowserStackLocal Binary : " + e.toString());
      }
  }

  private void enableAppAutomate(){
    BuildLoggerManager buildLoggerManager = (BuildLoggerManager) ContainerManager.getComponent("buildLoggerManager");
    BuildLogger buildLogger = buildLoggerManager.getLogger(buildContext.getResultKey());
    buildLogger.addBuildLogEntry("App automate support is enabled");
    if (configManager.hasAppAutomateBuildPath()) {
      String buildPath = configManager.getAppBuildPath();
      buildLogger.addBuildLogEntry("App build path:" + buildPath);
      try{
        String app_id = uploadFile(buildPath);
        buildLogger.addBuildLogEntry("Browsestack binary uploaded app_id:" + app_id);
        injectVariable(buildContext, BStackEnvVars.BSTACK_APP_ID, app_id);
      }catch(AppAutomateException appAutomateException){
        buildLogger.addBuildLogEntry("AppAutomateException occured");
      }catch(FileNotFoundException fileNotFoundException){
        buildLogger.addBuildLogEntry("FileNotFoundException occured");
      }catch(InvalidFileExtensionException invalidFileExtensionException){
        buildLogger.addBuildLogEntry("InvalidFileExtensionException occured");
      }
    } else {
      buildLogger.addBuildLogEntry("App build path is not available");
    }
  }
 
    public String uploadFile(String appPath) throws AppAutomateException, FileNotFoundException, InvalidFileExtensionException {
        String userName = configManager.get(BStackEnvVars.BSTACK_USERNAME);
        String accessKey = configManager.get(BStackEnvVars.BSTACK_ACCESS_KEY);
        AppAutomateClient appAutomateClient = new AppAutomateClient(userName, accessKey);
        return appAutomateClient.uploadApp(appPath).getAppUrl();
    }

    private void injectVariable(BuildContext buildContext, String key, String value) {
        VariableContext variableContext =  buildContext.getVariableContext();
        variableContext.addLocalVariable(key, value);
        VariableDefinitionContext variableDefinitionContext = variableContext.getEffectiveVariables().get(key);
        if (variableDefinitionContext != null)
        {
          variableDefinitionContext.setVariableType(VariableType.ENVIRONMENT);
        }
    }


    /*
      Populate variables for the template.
    */
    @Override
    protected void populateContextForEdit(final Map<String, Object> context, final BuildConfiguration buildConfiguration, final Plan build) {
      String contextPrefix = "custom.browserstack.";

      context.put("browserstack_username_key", contextPrefix + BStackEnvVars.BSTACK_USERNAME);
      context.put("browserstack_access_key_key", contextPrefix + BStackEnvVars.BSTACK_ACCESS_KEY);
      context.put("browserstack_local_enabled_key", contextPrefix + BStackEnvVars.BSTACK_LOCAL_ENABLED);
      context.put("browserstack_local_args_key", contextPrefix + BStackEnvVars.BSTACK_LOCAL_ARGS); 
      context.put("browserstack_disable_env_vars", contextPrefix + BStackEnvVars.BSTACK_DISABLE_ENV_VARS);
    }

    public AdministrationConfigurationAccessor getAdministrationConfigurationAccessor() {
        return administrationConfigurationAccessor;
    }

    public void setAdministrationConfigurationAccessor(AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }

    public void setBandanaManager(BandanaManager bandanaManager)
    {
      this.bandanaManager = bandanaManager;
    }
}
