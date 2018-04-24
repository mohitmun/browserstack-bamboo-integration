package com.browserstack.bamboo.ci;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.browserstack.bamboo.ci.BStackEnvVars;
import org.apache.commons.lang.StringUtils;
import java.util.Map;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.bamboo.bandana.PlanAwareBandanaContext;

/*
  Handles the BrowserStack Configuration. Gives the appropriate values based on the Global Config(Admin Configuration) or the Job Configuration(Build Configuration)
*/

/**
 * @author Pulkit Sharma
*/
public class BStackConfigManager {

  private AdministrationConfiguration adminConfig;
  private Map<String, String> buildConfig;
  private boolean overrideAdmin;
  private BandanaManager bandanaManager;

  public BStackConfigManager(AdministrationConfiguration adminConfig, Map<String, String> buildConfig, BandanaManager bandanaManager) {
    this.adminConfig = adminConfig;
    this.buildConfig = buildConfig;
    this.bandanaManager = bandanaManager;

    if (StringUtils.isNotBlank(buildConfig.get("custom.browserstack.override")) && buildConfig.get("custom.browserstack.override").equals("true")) {
      this.overrideAdmin = true;
    } else {
      this.overrideAdmin = false;
    }
  }

  public boolean hasCredentials() {
    return (StringUtils.isNotBlank(get(BStackEnvVars.BSTACK_USERNAME)) && StringUtils.isNotBlank(get(BStackEnvVars.BSTACK_ACCESS_KEY)));
  }

  public boolean localEnabled() {
    return (get(BStackEnvVars.BSTACK_LOCAL_ENABLED) != null && get(BStackEnvVars.BSTACK_LOCAL_ENABLED).equals("true"));
  }

  public String getUsername() {
    return get(BStackEnvVars.BSTACK_USERNAME);
  }

  public String getAccessKey(){
    return get(BStackEnvVars.BSTACK_ACCESS_KEY); 
  }
  
  public Boolean disableEnvVar(){
	String disableEnv = get(BStackEnvVars.BSTACK_DISABLE_ENV_VARS);
	if (disableEnv != null && disableEnv.equals("true"))
	  return true;
	else
      return false;
  }

  public String get(String key) {
    String adminValue = adminConfig.getSystemProperty(key);
    String buildValue = buildConfig.get("custom.browserstack." + key);
    Object bandanaValue = bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, "com.browserstack.bamboo.ci:" + key);

    if (overrideAdmin) {
      return (buildValue == null) ? null : buildValue.trim();
    } else {
      return (adminValue == null) ? ((bandanaValue == null) ? null : ((String)bandanaValue).trim()) : adminValue.trim();
    }
  }
}