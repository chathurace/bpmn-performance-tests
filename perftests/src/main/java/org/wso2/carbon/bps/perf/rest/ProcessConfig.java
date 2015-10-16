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

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ProcessConfig {

    private static final Logger log = Logger.getLogger(ProcessConfig.class);

    private String key;
    private String id;
    private Map<String, String> keyToId;
    private Map<String, Object> startupVariables = new HashMap<>();

    public ProcessConfig(String processProp, Map<String, String> keyToId) {
        this.keyToId = keyToId;
        String[] processParts = processProp.split("\\|");
        setKey(processParts[0].trim());
        if (processParts.length > 1) {
            String[] varParts = processParts[1].split(";");
            for (String varPart : varParts) {
                String name = varPart.split(",")[0];
                String value = varPart.split(",")[1];
                try {
                    Long longValue = Long.parseLong(value);
                    addVariable(name, longValue);
                } catch (NumberFormatException e) {}

                if (!getStartupVariables().containsKey(name)) {
                    addVariable(name, value);
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setKey(String key) {
        this.key = key;
        this.id = keyToId.get(key);

        if (id == null) {
            log.error("Process ID not found for the process Key: " + key);
        }
    }

    public Map<String, Object> getStartupVariables() {
        return startupVariables;
    }

    public void setStartupVariables(Map<String, Object> startupVariables) {
        this.startupVariables = startupVariables;
    }

    public void addVariable(String name, Object value) {
        startupVariables.put(name, value);
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("Process ID: " + id + " | ");
        for (String name : startupVariables.keySet()) {
            Object value = startupVariables.get(name);
            b.append(name + " -> " + value + "; ");
        }
        return b.toString();
    }
}
