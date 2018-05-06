package com.browserstack.bamboo.ci.lib;

import com.browserstack.bamboo.ci.lib.BStackSession;
import com.browserstack.bamboo.ci.lib.JUnitReport;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.HashMap;
import com.browserstack.automate.model.Session;
import com.browserstack.bamboo.ci.lib.BStackDirectoryScanner;
import com.browserstack.automate.AutomateClient;
import com.browserstack.appautomate.AppAutomateClient;
import com.browserstack.automate.exception.AutomateException;
import com.browserstack.automate.exception.SessionNotFound;
import com.browserstack.bamboo.ci.lib.BStackDirectoryScanner;
import com.browserstack.automate.exception.AppAutomateException;


/*
  Used for assosciating TestCases found in the surefire-reports directory with the TestCases -> SessionID mapping (browserstack report).
*/

/**
 * @author Pulkit Sharma
 */
public class BStackJUnitSessionMapper {

  private String baseDir;
  private Map<String, BStackXMLReport> testSessionMap;
  private AutomateClient automateClient;
  private AppAutomateClient appAutomateClient;
  public List<BStackSession> bStackSessions;
  private static final String pattern = "**/surefire-reports/TEST-*.xml";

  public BStackJUnitSessionMapper(String baseDir, Map<String, BStackXMLReport> testSessionMap, AutomateClient automateClient, AppAutomateClient appAutomateClient) {
    this.baseDir = baseDir;
    this.testSessionMap = testSessionMap;
    this.bStackSessions = new ArrayList<BStackSession>();
    this.automateClient = automateClient;
    this.appAutomateClient = appAutomateClient;
  }

  public List<BStackSession> parseAndMapJUnitXMLReports() {

    if(testSessionMap.isEmpty()) {
      System.out.println("BrowserStack reports not found, Aborting.");
      return bStackSessions;
    }

    String[] reportFilePaths = BStackDirectoryScanner.findFilesMatchingPattern(baseDir, pattern);

    if(reportFilePaths == null || reportFilePaths.length == 0) {
      System.out.println("Unable to find any JUnit Test reports, make sure you have correctly set your pom.xml");
      return bStackSessions;
    }

    Map<String, Long> testCaseIndices = new HashMap<String, Long>();

    for(String reportFilePath : reportFilePaths) {
      List<JUnitReport> testCases = new ArrayList<JUnitReport>();
      try {

        testCases = parseReport(reportFilePath);

      } catch (IOException e) {
        System.out.println("Error Parsing JUnit Test Reports : " + e.toString());
      }

      for(JUnitReport testCase : testCases) {
        String testCaseName = testCase.fullStrippedName();

        Long testIndex = testCaseIndices.containsKey(testCaseName) ? testCaseIndices.get(testCaseName) : -1L;
        testCaseIndices.put(testCaseName, ++testIndex);
        System.out.println(testCaseName + " / " + testCaseName + " <=> " + testIndex);

        String testId = String.format("%s{%d}", testCaseName, testIndex);

        if (testSessionMap.containsKey(testId)) {
            Session activeSession = null;
            BStackXMLReport bStackXMLReport = testSessionMap.get(testId);
            String exceptionEncountered = "";

            try {
              if(bStackXMLReport.isAutomateReport()){
                activeSession = automateClient.getSession(bStackXMLReport.sessionId);
              }else{
                activeSession = appAutomateClient.getSession(bStackXMLReport.sessionId);
              }
            } catch (AutomateException aex) {
              exceptionEncountered = aex.toString();
            } catch (AppAutomateException appAutomateException) {
              exceptionEncountered = appAutomateException.toString();
            } catch (SessionNotFound snfEx) {
              exceptionEncountered = snfEx.toString();
            }
           bStackSessions.add(new BStackSession(testCase, bStackXMLReport.sessionId, activeSession, exceptionEncountered));
        }
      }
    }

    return bStackSessions;
  }


  private List<JUnitReport> parseReport(String reportPath) throws IOException {

    File reportFile = new File(baseDir + "/" + reportPath);

    Document doc = null;
    List<JUnitReport> testCases = new ArrayList<JUnitReport>();

    try {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        doc = dBuilder.parse(reportFile);
    } catch (Exception e) {
        throw new IOException(e.getMessage());
    }

    if (doc != null) {
      Element documentElement = doc.getDocumentElement();
      NodeList testCaseNodes = documentElement.getElementsByTagName("testcase");

      for (int i = 0; i < testCaseNodes.getLength(); i++) {
          Node n = testCaseNodes.item(i);

          if (n.getNodeType() == Node.ELEMENT_NODE) {
          Element el = (Element) n;

          String name = el.hasAttribute("name") ? el.getAttribute("name") : "";
          String classname = el.hasAttribute("classname") ? el.getAttribute("classname") : "";
          String duration = el.hasAttribute("time") ? el.getAttribute("time") : "";
          String status = "Pass";

          NodeList failureNodes = el.getElementsByTagName("failure");
          NodeList errorNodes = el.getElementsByTagName("error");

          if (failureNodes.getLength() > 0 && failureNodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
              status = "Fail";
          }

          if (errorNodes.getLength() > 0 && errorNodes.item(0).getNodeType() == Node.ELEMENT_NODE) {
              status = "Error";
          }

          testCases.add(new JUnitReport(classname, name, duration, status));
        }
      }
    }
    return testCases;
  }
}
