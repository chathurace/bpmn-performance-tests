/*
 * Copyright 2005-2014 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.bps.perf.rest;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.bps.perf.reports.ChartReporter;
import org.wso2.carbon.bps.perf.util.DBCleaner;
import org.wso2.carbon.bps.perf.util.UMath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RestClientTest {

    private static Logger log = Logger.getLogger(RestClientTest.class);

    private int instanceCount = 1;
    private int sleepTime = 1000;
    private String serverURL = "http://localhost:8080/activiti-rest/service/runtime/";
    private String outPath = "bps-perf-output.csv";
    private static String logFilePath = "log4j.properties";
    private static String configFilePath = "perf.properties";
    private Map<String, String> processKeytoId = new HashMap<>();

    private static Properties config;
//    private List<String> processIds = new ArrayList<>();

    public static void main(String[] args) {

        if (args.length > 0) {
            if ("chart".equals(args[0])) {
                try {
                    new ChartReporter().generateCharts(args);
                } catch (Exception e) {
                    log.error("Failed to generate charts.", e);
                }
            } else if ("dbclean".equals(args[0])) {
                new DBCleaner().dropTables(args);
            } else if ("help".equals(args[0])) {
                System.out.println("Command line parameter options: \n" +
                        "chart <summary path> <chart path>\n" +
                        "dbclean <ip>\n" +
                        "No parameters: Runs performance tests according to the configuration given in perf.properties");
            }
            return;
        }

        long testSuiteStartTime = System.currentTimeMillis();
        try {
            Properties p = new Properties();
            File log4jFile = new File(logFilePath);
            if (log4jFile.exists()) {
                System.out.println("Loading log4j configuration from: " + log4jFile.getAbsolutePath());
                p.load(new FileInputStream(log4jFile));
                PropertyConfigurator.configure(p);
            } else {
                System.out.println("log4j configuration not found. log4j configuration should be given in " + log4jFile.getAbsolutePath());
                return;
            }

            config = new Properties();
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                log.info("Loading configuration from: " + configFile.getAbsolutePath());
                config.load(new FileInputStream(configFile));
            } else {
                log.error("Configuration file not found. Configuration should be given in " + configFile.getAbsolutePath());
                return;
            }

            new RestClientTest().execute();
        } catch (Exception e) {
            log.error("Error occurred during performance tests.", e);
        }
        long testSuiteEndTime = System.currentTimeMillis();
        double duration = testSuiteEndTime - testSuiteStartTime;
        double durationSeconds = UMath.round(duration / (1000), 2);
        double durationMinutes = UMath.round(duration / (1000 * 60), 2);
        double durationHours = UMath.round(duration / (1000 * 60 * 60), 2);
        log.info("All tests completed in " + durationMinutes + " minutes (" + durationHours + " hours)");
    }

    public void execute() throws Exception {

        serverURL = config.getProperty("serverURL");
        ActivitiRestClient pretestClient = new ActivitiRestClient(serverURL, 1);
        JSONObject processDefs = pretestClient.getProcessDefinitions();
        try {
            JSONArray defs = processDefs.getJSONArray("data");
            for (int defNumber = 0; defNumber < defs.length(); defNumber++) {
                JSONObject def = defs.getJSONObject(defNumber);
                String pid = def.getString("id");
                String pkey = def.getString("key");
                processKeytoId.put(pkey, pid);
            }
        } catch (JSONException e) {
            log.error("Failed to get process definitions from the server: " + serverURL + ". Process definitions response: " + processDefs.toString());
        }

        instanceCount = Integer.parseInt(config.getProperty("instances"));

        List<Integer> threadNumbers = new ArrayList<>();
        String threadsProp = config.getProperty("threads");
        String[] threadParts = threadsProp.split(",");
        for (String threadPart : threadParts) {
            int threadCount = Integer.parseInt(threadPart.trim());
            threadNumbers.add(threadCount);
        }

        sleepTime = Integer.parseInt(config.getProperty("sleep"));
        outPath = config.getProperty("results");
        File outFolder = new File(outPath);
        if (!outFolder.exists()) {
            log.info("Results folder " + outFolder.getAbsolutePath() + " does not exist. Creating a new folder...");
            outFolder.mkdirs();
        }
        File testReportFile = new File(outFolder, "summary.csv");
        StringBuffer summaryBuffer = new StringBuffer();
        summaryBuffer.append("Server URL," + serverURL + "\n");
        summaryBuffer.append("Number of process instances," + instanceCount + "\n");
        summaryBuffer.append("Number of threads," + Arrays.toString(threadNumbers.toArray()) + "\n\n\n");
        log.info("Test configuration - \n" + summaryBuffer.toString());
        summaryBuffer.append("Process ID,Threads,Total time,TPS,Average execution time\n\n");
        FileUtils.write(testReportFile, summaryBuffer.toString());

        List<ProcessConfig> processConfigs = new ArrayList<>();
        String processRef = "process";
        Set<String> processPropsNames = config.stringPropertyNames();
        for (String processPropName : processPropsNames) {
            if (processPropName.startsWith(processRef)) {
                String processProp = config.getProperty(processPropName);
                ProcessConfig processConfig = new ProcessConfig(processProp, processKeytoId);
                processConfigs.add(processConfig);
                log.info("Test configuration created for the process " + processConfig.toString());
            }
        }

        boolean testFailures = false;
        long allTestsStartTime = System.currentTimeMillis();
        int numTotalTests = processConfigs.size() * threadNumbers.size();
        int numCompletedTests = 0;

        List<String> completedProcessNames = new ArrayList<>();
        log.info("Starting performance tests...");
        for (ProcessConfig processConfig : processConfigs) {
            log.info("Starting tests for process " + processConfig.getId());

            for (int numTreads : threadNumbers) {
                log.info("Starting test for process " + processConfig.getId() + " with " + numTreads + " threads...");
                ActivitiRestClient client = new ActivitiRestClient(serverURL, numTreads);

                List<RestProcessExecutor> processExecutors = new ArrayList<>(instanceCount);
                ExecutorService executorService = Executors.newFixedThreadPool(numTreads);

                long stime = System.currentTimeMillis();
                for (int i = 0; i < instanceCount; i++) {
                    RestProcessExecutor processExecutor = new RestProcessExecutor(null, processConfig.getId(), processConfig.getStartupVariables(), client, i);
                    processExecutors.add(processExecutor);
                    executorService.execute(processExecutor);
                }

                executorService.shutdown();
                try {
                    executorService.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException e) {
                    String msg = "Error occurred while waiting for executors to terminate.";
                    log.error(msg, e);
                }
                long etime = System.currentTimeMillis();


                StringBuffer buf = new StringBuffer();
                double totalDuration = 0;
                buf.append("Instance durations for process: " + processConfig.getId() + "\n");
                for (RestProcessExecutor processExecutor : processExecutors) {
                    testFailures = processExecutor.isFailed();
                    if (testFailures) {
                        break;
                    }

                    buf.append(processExecutor.getExternalDuration() + "\n");
                    totalDuration += processExecutor.getExternalDuration();
                }

                if (!testFailures) {
                    double externalTPS = (double) instanceCount * 1000 / (double) (etime - stime);
                    externalTPS = UMath.round(externalTPS, 3);

                    double avgExeTime = totalDuration / instanceCount;
                    avgExeTime = UMath.round(avgExeTime, 3);

                    log.info("Test for process " + processConfig.getId() + " with " + numTreads + " threads completed with duration: " + (etime - stime) + " ms | TPS: " + externalTPS + " | Average execution time: " + avgExeTime);
                    String processRecord = processConfig.getId() + "," + numTreads + "," + (etime - stime) + "," + externalTPS + "," + avgExeTime + "\n";
                    FileWriter fileWriter = new FileWriter(testReportFile, true);
                    fileWriter.write(processRecord);
                    fileWriter.close();

                    buf.append("\n\nTPS," + externalTPS + "\n\n");
                    buf.append("\n\nAverage execution time," + avgExeTime + " ms\n\n");

                    File processReportFile = new File(outFolder, processConfig.getId() + ".csv");
                    FileUtils.write(processReportFile, buf.toString());
                    client.close();

                    numCompletedTests++;
                    double testingTime = System.currentTimeMillis() - allTestsStartTime;
                    double testingTimeMinutes = UMath.round(testingTime / (1000 * 60), 2);
                    double testingTimeHours = UMath.round(testingTime / (1000 * 60 * 60), 2);

                    double remainingTime = (testingTime / numCompletedTests) * (numTotalTests - numCompletedTests);
                    double remainingTimeMinutes = UMath.round(remainingTime / (1000 * 60), 2);
                    double remainingTimeHours = UMath.round(remainingTime / (1000 * 60 * 60), 2);
                    log.info("Completed test for process " + processConfig.getId() + " with " + numTreads + " threads.");
                    log.info(numCompletedTests + " out of " + numTotalTests + " completed in " + testingTimeMinutes + " minutes (" + testingTimeHours +
                            " hours). Estimated remaining time: " + remainingTimeMinutes + " minutes (" + remainingTimeHours + " hours)");

//                    client.undeploy();
//                    client.deploy();

                    completedProcessNames.add("Process: " + processConfig.getId() + " | Threads: " + numTreads);
                    log.info("Waiting " + sleepTime + " ms before the next test");
                    Thread.sleep(sleepTime);
                } else {
                    log.error("Test for process " + processConfig.getId() + " with " + numTreads + " failed. See client and server logs for more information.");
                    break; // terminate tests for this process with other threads
                }
            }

            if (!testFailures) {
                log.info("Completed tests for process " + processConfig.getId());
            } else {
                log.error("At least one test for the process " + processConfig.getId() + " has failed. Test suite will be terminated.");
                StringBuffer retryMessage = new StringBuffer();
                retryMessage.append("Below tests were completed successfully:\n");
                for (String completedProcessName : completedProcessNames) {
                    retryMessage.append(completedProcessName + "\n");
                }
                log.info(retryMessage.toString());
                break; // terminate tests for other processes
            }
        }
    }
}
