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

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessExecutor implements Runnable {

    private static final Logger log = Logger.getLogger(ProcessExecutor.class);

    private String processDefinitionKey;
    private ProcessEngine processEngine;
    private long duration;
    private long externalDuration;
    private Map<String, Object> variables;

    public ProcessExecutor(String processDefinitionKey, ProcessEngine processEngine) {
        this.processDefinitionKey = processDefinitionKey;
        this.processEngine = processEngine;
        this.variables = new HashMap<String, Object>();
    }

    public ProcessExecutor(String processDefinitionKey, Map<String, Object> variables, ProcessEngine processEngine) {
        this.processDefinitionKey = processDefinitionKey;
        this.processEngine = processEngine;
        this.variables = variables;
    }

    public void run() {
        long processStartTime = System.currentTimeMillis();
        ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey(processDefinitionKey, variables);

        duration = System.currentTimeMillis() - processStartTime;

        if(!processInstance.isEnded())
        {
            List<Task> currentTasks = processEngine.getTaskService()
                    .createTaskQuery().processInstanceId(processInstance.getId())
                    .list();
            while(currentTasks != null && currentTasks.size() > 0)  {
                long start = System.currentTimeMillis();

                // Complete all open tasks
                for(Task task : currentTasks)
                {
                    processEngine.getTaskService().complete(task.getId());
                    if (log.isDebugEnabled()) {
                        log.debug("Completed task: " + task.getName() + " of instance: " + processInstance.getId());
                    }
                }
                // Only measure the actual time it took to complete the tasks, not including querying for them
                duration += (System.currentTimeMillis() - start);

                // Check for possible new tasks
                currentTasks = processEngine.getTaskService()
                        .createTaskQuery().processInstanceId(processInstance.getId())
                        .list();
            }
        }

        externalDuration = System.currentTimeMillis() - processStartTime;
    }

    public long getDuration() {
        return duration;
    }

    public long getExternalDuration() {
        return externalDuration;
    }
}
