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

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ActivitiRestClient {

    private static Logger log = Logger.getLogger(ActivitiRestClient.class);
    private CloseableHttpClient httpClient;
    private String serverURL;

    public static void main(String[] args) {
        try {
            ActivitiRestClient client = new ActivitiRestClient("http://localhost:8080/activiti-rest/service/runtime/", 2);
            Map<String, Object> vars = new HashMap<>();
            vars.put("rvar1", "rval1");
            JSONObject processInstance = client.startProcessInstance("seq_u2", vars);
            System.out.println(processInstance.toString());
            String processInstanceId = (String) processInstance.get("id");

            while (!processInstance.has("statusCode")) {
                JSONObject tasksResponse = client.getTasks(processInstanceId);
                JSONArray tasks = tasksResponse.getJSONArray("data");
                for (int i = 0; i < tasks.length(); i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    String taskId = task.getString("id");
                    System.out.println("Task ID: " + taskId + ", Task: " + task.getString("name"));
                    client.completeTask(taskId);
                }
                processInstance = client.getProcessInstance(processInstanceId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ActivitiRestClient(String serverURL, int concurrency) throws Exception {
        this.httpClient = createClient(concurrency);
        this.serverURL = serverURL;
    }

    public JSONObject startProcessInstance(String processKey, Map<String, Object> variables) throws Exception {
        String url = serverURL + "runtime/process-instances";
        JSONObject payload = new JSONObject();
        payload.put("processDefinitionKey", processKey);
        JSONArray vars = new JSONArray();
        for (String varName : variables.keySet()) {
            Object varValue = variables.get(varName);
            if (varValue instanceof Long) {
                vars.put(new JSONObject().put("name", varName).put("value", varValue).put("type", "long"));
            } else {
                vars.put(new JSONObject().put("name", varName).put("value", varValue));
            }
        }
        payload.put("variables", vars);
        JSONObject result = invokePOST(url, payload);
        return result;
    }

    public JSONObject startProcessInstanceById(String processId, Map<String, Object> variables) throws Exception {
        String url = serverURL + "runtime/process-instances";
        JSONObject payload = new JSONObject();
        payload.put("processDefinitionId", processId);
        JSONArray vars = new JSONArray();
        for (String varName : variables.keySet()) {
            Object varValue = variables.get(varName);
            if (varValue instanceof Long) {
                vars.put(new JSONObject().put("name", varName).put("value", varValue).put("type", "long"));
            } else {
                vars.put(new JSONObject().put("name", varName).put("value", varValue));
            }
        }
        payload.put("variables", vars);
        JSONObject result = invokePOST(url, payload);
        return result;
    }

    public JSONObject getProcessInstance(String processInstanceId) throws Exception {
        String url = serverURL + "runtime/process-instances/" + processInstanceId;
        JSONObject result = invokeGET(url);
        return result;
    }

    public JSONObject getProcessDefinitions() throws Exception {
        String url = serverURL + "repository/process-definitions?size=10000";
        JSONObject result = invokeGET(url);
        return result;
    }

    public JSONObject getTasks(String processInstanceId) throws Exception {
        String url = serverURL + "runtime/tasks?processInstanceId=" + processInstanceId;
        JSONObject result = invokeGET(url);
        return result;
    }

    public void completeTask(String taskId) throws Exception {
        String url = serverURL + "runtime/tasks/" + taskId;
        JSONObject payload = new JSONObject();
        payload.put("action", "complete");
        invokePOST(url, payload);
    }

    private JSONObject invokePOST(String url, JSONObject payload) throws Exception {
        HttpPost post = new HttpPost(url);
        try {
            post.addHeader("Authorization", "Basic a2VybWl0Omtlcm1pdA==");
            StringEntity instanceEntity = new StringEntity(payload.toString());
            instanceEntity.setContentType("application/json");
            post.setEntity(instanceEntity);
            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(post);

            if (response.getEntity() != null) {
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));

                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                JSONObject responsePayload = new JSONObject(result.toString());
                EntityUtils.consume(response.getEntity());
                response.close();
                if (log.isTraceEnabled()) {
                    log.trace("Invoked POST on " + url + " with payload: " + payload.toString() + ". Response: " + responsePayload.toString());
                }
                return responsePayload;
            } else {
                return null;
            }

        } finally {
            post.releaseConnection();
        }
    }

    private JSONObject invokeGET(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        try {
            get.addHeader("Authorization", "Basic a2VybWl0Omtlcm1pdA==");
            CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(get);

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            JSONObject responsePayload = new JSONObject(result.toString());
            EntityUtils.consume(response.getEntity());
            response.close();

            if (log.isTraceEnabled()) {
                log.trace("Invoked GET on " + url + ". Response: " + responsePayload.toString());
            }
            return responsePayload;

        } finally {
            get.releaseConnection();
        }
    }

    public void close() throws IOException {
        httpClient.close();
    }

    private CloseableHttpClient createClient(int concurrency) throws Exception {
        log.info("Creating HTTP client with concurrency: " + concurrency);
        CloseableHttpClient httpClient = null;
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(concurrency);
        cm.setMaxTotal(concurrency + 10);
        httpClient = HttpClientBuilder.create().setConnectionManager(cm).build();
        return httpClient;
    }
}
