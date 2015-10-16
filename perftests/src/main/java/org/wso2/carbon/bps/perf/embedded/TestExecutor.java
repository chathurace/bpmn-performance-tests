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
package org.wso2.carbon.bps.perf.embedded;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.activiti.engine.repository.Deployment;
import org.apache.log4j.PropertyConfigurator;

public class TestExecutor {

    private final String activitiConfigPath = "/home/chathura/projects/bps-perf/perftests/src/main/resources/activiti.xml";
    private static int instanceCount = 1;
    private final String bpmnPath = "/home/chathura/projects/bps-perf/perftests/src/main/resources/bpmn";
    private static String processKey = "par_s-2";
    private static int numTreads = 1;

    private static String logFilePath = "/home/chathura/projects/bps-perf/perftests/src/main/java/log4j.properties";
    private ProcessEngine engine;

    public static void main(String[] args) {

        try {
            Properties p = new Properties();
            p.load(new FileInputStream(new File(logFilePath)));
            PropertyConfigurator.configure(p);

            if (args.length == 3) {
                processKey = args[0];
                instanceCount = Integer.parseInt(args[1]);
                numTreads = Integer.parseInt(args[2]);
            }

            new TestExecutor().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute() throws FileNotFoundException {

        File activitiConfigFile = new File(activitiConfigPath);
        ProcessEngineConfigurationImpl processEngineConfigurationImpl =
                (ProcessEngineConfigurationImpl) ProcessEngineConfiguration.createProcessEngineConfigurationFromInputStream(new FileInputStream(activitiConfigFile));

        // we have to build the process engine first to initialize session factories.
        engine = processEngineConfigurationImpl.buildProcessEngine();
        RepositoryService repositoryService = engine.getRepositoryService();

        File bpmnFolder = new File(bpmnPath);
        for (File bpmnFile : bpmnFolder.listFiles()) {
            repositoryService.createDeployment().addInputStream(bpmnPath, new FileInputStream(bpmnFile)).deploy();
            System.out.println("Process deployed: " + bpmnFile.getAbsolutePath());
        }

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("testCount", new Long(1));

        List<ProcessExecutor> processExecutors = new ArrayList<>(instanceCount);
        ExecutorService executorService = Executors.newFixedThreadPool(numTreads);

        long stime = System.currentTimeMillis();
        for (int i = 0; i < instanceCount; i++) {
            ProcessExecutor processExecutor = new ProcessExecutor(processKey, vars, engine);
            processExecutors.add(processExecutor);
            executorService.execute(processExecutor);
//            engine.getRuntimeService().startProcessInstanceByKey("seq_s2", vars);
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long etime = System.currentTimeMillis();

        List<Long> startTimes = new ArrayList<Long>();
        List<Long> endTimes = new ArrayList<Long>();

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

        double engineDuration = 0;
        for (ProcessExecutor processExecutor : processExecutors) {
            System.out.println("Process instance duration: " + processExecutor.getDuration() +  " | " + processExecutor.getExternalDuration());
            engineDuration += processExecutor.getDuration();
        }
        System.out.println("Engine duration: " + engineDuration);
        double avgExeTimeEngine = engineDuration / instanceCount;
        System.out.println("Average execution time (Engine): " + avgExeTimeEngine);


        System.out.println("External duration: " + (etime - stime));
        double externalTPS = (double) instanceCount * 1000 / (double) (etime - stime);
        System.out.println("External TPS: " + externalTPS);

        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            System.out.println("Undeploying " + deployment.getId());
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

        engine.close();
    }
}
