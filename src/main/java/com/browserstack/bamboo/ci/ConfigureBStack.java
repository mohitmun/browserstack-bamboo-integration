package com.browserstack.bamboo.ci;

import com.atlassian.bamboo.bandana.PlanAwareBandanaContext;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.configuration.AdministrationConfigurationManager;
import com.atlassian.bamboo.configuration.AdministrationConfigurationPersister;
import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.sal.api.component.ComponentLocator;

/*
 Global BrowserStack configuration. Available in Bamboo Administration section.
*/

/**
 * @author Pulkit Sharma
 */
public class ConfigureBStack extends BambooActionSupport implements GlobalAdminSecurityAware
{

    private String username;
    private String accessKey;
    private String browserstackLocal;
    private String browserstackLocalArgs;
    private String disableEnvironmentVariables;
    private BandanaManager bandanaManager;


    public ConfigureBStack(){
      super();
      setAdministrationConfigurationAccessor(ComponentLocator.getComponent(AdministrationConfigurationAccessor.class));
      setAdministrationConfigurationManager(ComponentLocator.getComponent(AdministrationConfigurationManager.class));
      setAdministrationConfigurationPersister(ComponentLocator.getComponent(AdministrationConfigurationPersister.class));
    }

    public String doEdit() {
      setUsername(getValue(BStackEnvVars.BSTACK_USERNAME));
      setAccessKey(getValue(BStackEnvVars.BSTACK_ACCESS_KEY));
      setBrowserstackLocal(getValue(BStackEnvVars.BSTACK_LOCAL_ENABLED));
      setBrowserstackLocalArgs(getValue(BStackEnvVars.BSTACK_LOCAL_ARGS));
      setDisableEnvironmentVariables(getValue(BStackEnvVars.BSTACK_DISABLE_ENV_VARS));
      return INPUT;
    }

    public String doSave(){
      setValue(BStackEnvVars.BSTACK_USERNAME,getUsername());
      setValue(BStackEnvVars.BSTACK_ACCESS_KEY,getAccessKey());
      setValue(BStackEnvVars.BSTACK_LOCAL_ENABLED,getBrowserstackLocal());
      setValue(BStackEnvVars.BSTACK_LOCAL_ARGS,getBrowserstackLocalArgs());
      setValue(BStackEnvVars.BSTACK_DISABLE_ENV_VARS,getDisableEnvironmentVariables());
      addActionMessage(getText("config.updated"));

      return SUCCESS;
    }

    private String getValue(String key) {
      final AdministrationConfiguration adminConfig = this.getAdministrationConfiguration();
      String value = null;
      if(adminConfig.getSystemProperty(key) != null) {
        value = adminConfig.getSystemProperty(key);
      } else {
        value = getFromBandana(key);
        if (value != null) {
          setValue(key, value);
        }
      }
      return value;
    }


    private void setValue(String key, String value) {
      final AdministrationConfiguration adminConfig = this.getAdministrationConfiguration();
      adminConfig.setSystemProperty(key,value);
      setInBandana(key,value);
    }

    public String getAccessKey()
    {
        return accessKey;
    }

    public void setAccessKey(String accesskey)
    {
        this.accessKey = accesskey;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setBrowserstackLocal(String value)
    {
        this.browserstackLocal = value;
    }

    public String getBrowserstackLocal()
    {
        return browserstackLocal;
    }

    public void setBrowserstackLocalArgs(String value)
    {
        this.browserstackLocalArgs = value;
    }

    public String getBrowserstackLocalArgs()
    {
        return browserstackLocalArgs;
    }

    public void setBandanaManager(BandanaManager bandanaManager)
    {
        this.bandanaManager = bandanaManager;
    }

    private String getFromBandana(String key) {
      Object value = bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, "com.browserstack.bamboo.ci:" + key);
      return (String) value;
    }

    private void setInBandana(String key, String value) {
      bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, "com.browserstack.bamboo.ci:" + key, value);
    }

	public String getDisableEnvironmentVariables() {
		return disableEnvironmentVariables;
	}

	public void setDisableEnvironmentVariables(String disableEnvironmentVariables) {
		this.disableEnvironmentVariables = disableEnvironmentVariables;
	}
}