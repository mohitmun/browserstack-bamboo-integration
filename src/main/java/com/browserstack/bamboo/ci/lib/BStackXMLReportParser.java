package com.browserstack.bamboo.ci.lib;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import com.browserstack.bamboo.ci.lib.BStackDirectoryScanner;
/*
    Inspired by https://github.com/jenkinsci/browserstack-integration-plugin/blob/master/src/main/java/com/browserstack/automate/ci/common/report/XmlReporter.java
    This is used here to parse the browserstack report files generated by test suites which will contain testcases -> BrowserStack session ID mappings.
*/

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 * @author Pulkit Sharma
 */
public class BStackXMLReportParser {

    private String baseDir;
    private Map<String, String> testSessionMap;
    private static final String pattern = "**/browserstack-reports/REPORT-*.xml";

    public BStackXMLReportParser(String baseDir) {
        this.baseDir = baseDir;
        this.testSessionMap = new HashMap<String, String>();
    }

    public void process() {

        String[] bStackreports = BStackDirectoryScanner.findFilesMatchingPattern(baseDir,pattern);

        if(bStackreports == null || bStackreports.length == 0) {
            System.out.println("Unable to find any BrowserStack reports, make sure you have correctly set your pom.xml");
            return;
        }

        for(String filePath : bStackreports) {
            Map<String, String> parsedIds = null;

            try {
                parsedIds = parse(filePath);
            } catch (IOException e) {
                System.out.println("Error while parsing BStackReports : " + e.toString());
            }

            if(parsedIds != null && !parsedIds.isEmpty()) {
                testSessionMap.putAll(parsedIds);
            }
        }
    }

    private Map<String, String> parse(String pathToFile) throws IOException {
        File f = new File(baseDir + "/" + pathToFile);

        Map<String, String> testSessionMap = new HashMap<String, String>();
        Document doc;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(f);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        Element documentElement = doc.getDocumentElement();
        NodeList testCaseNodes = documentElement.getElementsByTagName("testcase");

        for (int i = 0; i < testCaseNodes.getLength(); i++) {
            Node n = testCaseNodes.item(i);

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) n;
                if (el.hasAttribute("id") && el.hasChildNodes()) {
                    String testId = el.getAttribute("id");
                    NodeList sessionNode = el.getElementsByTagName("session");
                    if (sessionNode.getLength() > 0 && sessionNode.item(0).getNodeType() == Node.ELEMENT_NODE) {
                        testSessionMap.put(testId, sessionNode.item(0).getTextContent());
                    }
                }
            }
        }
        return testSessionMap;
    }

    public Map<String,String> getTestSessionMap() {
        return testSessionMap;
    }
}
