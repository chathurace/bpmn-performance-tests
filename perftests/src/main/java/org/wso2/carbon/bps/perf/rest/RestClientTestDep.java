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
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RestClientTestDep {

    private static Logger log = Logger.getLogger(RestClientTestDep.class);

    private static int instanceCount = 1;
    private static String processKey = "seq_u2";
    private static String processId = "seq_u2";
    private static int numTreads = 1;
    private static String serverURL = "http://localhost:8080/activiti-rest/service/runtime/";
    private static String outPath = "bps-perf-output.csv";
    private static String logFilePath = "log4j.properties";
    private static String configFilePath = "perf.properties";

    private static Properties config;
//    private List<String> processIds = new ArrayList<>();

    public static void main(String[] args) {
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

            if (args.length >= 3) {
                String processPart = args[0];
                if (processPart.contains(":")) {
                    processId = processPart;
                    processKey = null;
                } else {
                    processKey = args[0];
                    processId = null;
                }
                instanceCount = Integer.parseInt(args[1]);
                numTreads = Integer.parseInt(args[2]);

                if (args.length == 4) {
                    serverURL = args[3];
                }
            }
            log.info("Starting tests - Process key: " + processKey + ", Instance count: " + instanceCount + ", Threads: " + numTreads);
            new RestClientTestDep().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute() throws Exception {

        instanceCount = Integer.parseInt(config.getProperty("instances"));
        serverURL = config.getProperty("serverURL");
        numTreads = Integer.parseInt(config.getProperty("threads"));
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
        summaryBuffer.append("Number of threads," + numTreads + "\n\n\n");
        summaryBuffer.append("Process ID,Total time,TPS,Average execution time\n\n");
        FileUtils.write(testReportFile, summaryBuffer.toString());

        List<ProcessConfig> processConfigs = new ArrayList<>();
        boolean processFound = true;
        String processRef = "process";
        int processNum = 1;
        while (processFound) {
            String processProp = config.getProperty(processRef + processNum);
            if (processProp == null) {
                break;
            }
            ProcessConfig processConfig = new ProcessConfig(processProp, null);
            String[] processParts = processProp.split("\\|");
            processConfig.setKey(processParts[0].trim());
            if (processParts.length > 1) {
                String[] varParts = processParts[1].split(";");
                for (String varPart : varParts) {
                    String name = varPart.split(",")[0];
                    String value = varPart.split(",")[1];
                    processConfig.addVariable(name, value);
                }
            }
            processConfigs.add(processConfig);
        }

        for (ProcessConfig processConfig : processConfigs) {
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
            double externalTPS = (double) instanceCount * 1000 / (double) (etime - stime);
            double totalDuration = 0;
            buf.append("Instance durations\n");
            for (RestProcessExecutor processExecutor : processExecutors) {
                buf.append(processExecutor.getExternalDuration() + "\n");
                totalDuration += processExecutor.getExternalDuration();
            }
            double avgExeTimeEngine = totalDuration / instanceCount;

            log.info("Process " + processConfig.getId() + " completed with duration: " + (etime - stime) + " ms | TPS: " + externalTPS + " | Average execution time: " + avgExeTimeEngine);
            String processRecord = processConfig.getId() + "," + (etime - stime) + "," + externalTPS + "," + avgExeTimeEngine + "\n";
            FileWriter fileWriter = new FileWriter(testReportFile, true);
            fileWriter.write(processRecord);
            fileWriter.close();

            buf.append("\n\nTPS," + externalTPS + "\n\n");

            File processReportFile = new File(outFolder, processConfig.getId() + ".csv");
            FileUtils.write(processReportFile, buf.toString());
            client.close();
        }

//        Map<String, Object> vars = new HashMap<String, Object>();
//        vars.put("testCount", new Long(1));
//
//        List<RestProcessExecutor> processExecutors = new ArrayList<>(instanceCount);
//        ExecutorService executorService = Executors.newFixedThreadPool(numTreads);
//
//        long stime = System.currentTimeMillis();
//        for (int i = 0; i < instanceCount; i++) {
//            RestProcessExecutor processExecutor = new RestProcessExecutor(processKey, processId, vars, client, i);
//            processExecutors.add(processExecutor);
//            executorService.execute(processExecutor);
//        }
//
//        executorService.shutdown();
//        try {
//            executorService.awaitTermination(1, TimeUnit.HOURS);
//        } catch (InterruptedException e) {
//            String msg = "Error occurred while waiting for executors to terminate.";
//            log.error(msg, e);
//        }
//
//        long etime = System.currentTimeMillis();
//
//        List<Long> startTimes = new ArrayList<Long>();
//        List<Long> endTimes = new ArrayList<Long>();

//        HistoryService h = engine.getHistoryService();
//        for (HistoricProcessInstance hp : h.createHistoricProcessInstanceQuery().list()) {
//            long startTime = hp.getStartTime().getTime();
//            long endTime = hp.getStartTime().getTime();
//            startTimes.add(startTime);
//            endTimes.add(endTime);
//            long duration = endTime - startTime;
//            System.out.println("Duration: " + duration + " ms");
//        }
//        Collections.sort(startTimes);
//        Collections.sort(endTimes);
//
//        long testStartTime = startTimes.get(0);
//        long testEndTime = endTimes.get(endTimes.size() - 1);
//        System.out.println("Test duration: " + (testEndTime - testStartTime));
//        double throughput = (double) instanceCount * 1000 / (double) (testEndTime - testStartTime);
//        System.out.println("TPS: " + throughput);

//        StringBuffer buf = new StringBuffer();
//        log.info("External duration: " + (etime - stime));
//        double externalTPS = (double) instanceCount * 1000 / (double) (etime - stime);
//        log.info("External TPS: " + externalTPS);
//        buf.append("TPS," + externalTPS + "\n\n");
//
//        double totalDuration = 0;
//
//        buf.append("Instance duration\n");
//        for (RestProcessExecutor processExecutor : processExecutors) {
//            buf.append(processExecutor.getExternalDuration() + "\n");
//            totalDuration += processExecutor.getExternalDuration();
//        }
//        log.info("Total duration: " + totalDuration);
//        double avgExeTimeEngine = totalDuration / instanceCount;
//        log.info("Average execution time (External): " + avgExeTimeEngine);
//
//        FileUtils.write(new File(outPath), buf.toString());
//        client.close();

    }

    private HttpClient createClient(int concurrency) throws Exception {
        CloseableHttpClient httpclient = null;

//        SSLContext sslContext = SSLContext.getInstance("SSL");
//        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
//            public X509Certificate[] getAcceptedIssuers() {
//                return null;
//            }
//
//            public void checkClientTrusted(X509Certificate[] certs,
//                                           String authType) {
//            }
//
//            public void checkServerTrusted(X509Certificate[] certs,
//                                           String authType) {
//            }
//        }}, new SecureRandom());
//
//        SSLSocketFactory sf = new SSLSocketFactory(sslContext);
//        Scheme httpsScheme = new Scheme("https", 443, sf);
//        SchemeRegistry schemeRegistry = new SchemeRegistry();
//        schemeRegistry.register(httpsScheme);

//        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(schemeRegistry);
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(concurrency);
        cm.setMaxTotal(concurrency + 10);
        httpclient = HttpClientBuilder.create().setConnectionManager(cm).build();
//        httpclient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
//        httpclient.getCredentialsProvider().setCredentials(
//                AuthScope.ANY,
//                new UsernamePasswordCredentials("admin", "admin"));
        return httpclient;
    }
}
