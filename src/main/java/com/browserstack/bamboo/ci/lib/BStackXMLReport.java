package com.browserstack.bamboo.ci.lib;

/*
 * Represents generated XML report which contains session id and projectType
 */

public class BStackXMLReport{
  
  public String sessionId;
  public String projectType;

  public BStackXMLReport(String sessionId, String projectType){
    this.sessionId = sessionId;
    this.projectType = projectType;
  }

  public boolean isAutomateReport(){
    if (projectType == null){
      return true;
    }
    if (projectType.equals("AUTOMATE")){
      return true;
    }
    return false;
  }
}

