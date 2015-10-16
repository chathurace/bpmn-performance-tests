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


import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class RestProcessExecutor implements Runnable {

    private static Logger log = Logger.getLogger(RestProcessExecutor.class);

    private String executorPrefix;
    private String processKey;
    private String processId;
    private Map<String, Object> variables;
    private long externalDuration;
    private ActivitiRestClient client;
    private boolean failed = false;

    public RestProcessExecutor(String processKey, String processId, Map<String, Object> vars, ActivitiRestClient client, int executorNumber) {
        this.executorPrefix = "E_" + executorNumber + ": ";
        this.processKey = processKey;
        this.processId = processId;
        this.variables = vars;
        this.client = client;
    }

    @Override
    public void run() {

        long processStartTime = System.currentTimeMillis();

        JSONObject processInstance = null;
        JSONObject tasksResponse = null;
        try {

            if (processKey != null) {
                if (log.isDebugEnabled()) {
                    log.debug(executorPrefix + "Starting process " + processKey);
                }
                processInstance = client.startProcessInstance(processKey, variables);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(executorPrefix + "Starting process " + processId);
                }
                processInstance = client.startProcessInstanceById(processId, variables);
            }
            if (log.isTraceEnabled()) {
                log.trace(executorPrefix + "Start process instance response payload: " + processInstance.toString());
            }
            String processInstanceId = (String) processInstance.get("id");

            if (!processInstance.has("statusCode")) {
                tasksResponse = client.getTasks(processInstanceId);
                if (log.isTraceEnabled()) {
                    log.trace(executorPrefix + "Available tasks response payload: " + tasksResponse.toString());
                }
                org.json.JSONArray tasks = tasksResponse.getJSONArray("data");

                while (tasks != null && tasks.length() > 0) {
                    for (int i = 0; i < tasks.length(); i++) {
                        JSONObject task = tasks.getJSONObject(i);
                        String taskId = task.getString("id");

                        if (log.isTraceEnabled()) {
                            log.trace(executorPrefix + "Completing task - Task ID: " + taskId + ", Task name: " + task.getString("name"));
                        }
                        client.completeTask(taskId);
                    }
                    tasksResponse = client.getTasks(processInstanceId);
                    if (log.isTraceEnabled()) {
                        log.trace(executorPrefix + "Available tasks response payload: " + tasksResponse.toString());
                    }
                    tasks = tasksResponse.getJSONArray("data");
                }
            }
            log.debug(executorPrefix + "Completed process instance: " + processInstanceId);
        } catch (Exception e) {
            failed = true;
            String msg = executorPrefix + "Failed to complete process instance execution.";
            log.error(msg, e);
            if (processInstance != null) log.error("Last process creation response: " + processInstance.toString());
            if (tasksResponse != null) log.error("Last available tasks response: " + tasksResponse.toString());
        }
        externalDuration = System.currentTimeMillis() - processStartTime;
    }

    public long getExternalDuration() {
        return externalDuration;
    }

    public boolean isFailed() {
        return failed;
    }
}
