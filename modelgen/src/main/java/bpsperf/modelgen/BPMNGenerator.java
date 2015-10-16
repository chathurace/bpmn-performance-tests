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
package bpsperf.modelgen;

import bpsperf.modelgen.generators.*;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class BPMNGenerator {

    public static final String ns = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    public static final String activitins = "http://activiti.org/bpmn";
    public static final String xsins = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String startEvent = "startEvent";
    public static final String endEvent = "endEvent";
    public static final String parallelGateway = "parallelGateway";
    public static final String xorGateway = "exclusiveGateway";
    public static final String userTask = "userTask";
    public static final String serviceTask = "serviceTask";
    private Element parent;
    private Document doc;
    private int numNodes = 1;
    private int numBranches = 2;
    private int nodesPerBranch = 5;
    private String processId = "xor_s-3v";
    private String restServiceURL = "http://10.0.3.1:9764/bps-bperf-services_1.0.0/services/service1/echo";
    private String serviceClass = "org.wso2.carbon.bps.perf.services.Service1V";

    public static String outPath = "/home/chathura/projects/bps-perf/modelgen/src/main/resources/bpmn/";
    private String confPath = "/home/chathura/projects/bps-perf/modelgen/src/main/resources/conf/modelgen.config";
    private List<AbstractGenerator> generators = new ArrayList<>();

    /**
     * BPMNGenerator out_file model_type [model type parameters]
     * @param args
     */
    public static void main(String[] args) {
        try {
            new BPMNGenerator().generateModels();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateModels() throws Exception {

        generators.add(new UserTaskSequenceGenerator());
        generators.add(new ServiceTaskSequenceGenerator());
        generators.add(new RestTaskSequenceGenerator());
        generators.add(new ParallelServiceTasksGenerator());
        generators.add(new XORModelGenerator());

        File configFile = new File(confPath);
        List<String> lines = FileUtils.readLines(configFile);
        for (String line : lines) {
            if (!line.startsWith("#")) {
                generate(line);
            }
        }
    }

    public void generate(String config) throws Exception {

        boolean processed = false;
        for (AbstractGenerator gen : generators) {
            processed = gen.generate(config);
            if (processed) {
                break;
            }
        }

        if (!processed) {
            System.out.println("No generator found for processing config line: " + config);
        }
    }

//        outPath = outPath + processId + ".bpmn20.xml";
//
//        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//        doc = docBuilder.newDocument();
//        Element rootElement = doc.createElementNS(ns, "definitions");
//        rootElement.setAttribute("targetNamespace", "http://www.activiti.org/test");
//        doc.appendChild(rootElement);
//
//        Element process = doc.createElementNS(ns, "process");
//        process.setAttribute("id", processId);
//        process.setAttribute("name", processId);
//        process.setAttribute("isExecutable", "true");
//        rootElement.appendChild(process);
//        this.parent = process;

//        generateUserTaskSequence(numNodes);
//        generateRESTTaskSequence(numNodes, restServiceURL);
//        generateServiceTaskSequence(numNodes, serviceClass);
//        generateParallelServiceTasks(numBranches, nodesPerBranch, serviceClass);
//        generateXORServiceTasks(3, serviceClass);
//        write(new File(outPath));
//    }

    public void generateUserTaskSequence(int numNodes) throws Exception {

        Element start = addNode(startEvent, "startEvent");
        Element utask1 = addNode(userTask, "ut1");
        utask1.setAttributeNS(activitins, "candidateUsers", "kermit,admin");
        connect(start, utask1);

        Element utaskS = utask1;
        Element utaskT = utask1;
        for (int i = 2; i <= numNodes; i++) {
            utaskT = addNode(userTask, "ut" + i);
            utaskT.setAttributeNS(activitins, "candidateUsers", "kermit,admin");
            connect(utaskS, utaskT);
            utaskS = utaskT;
        }

        Element end = addNode(endEvent, "endEvent");
        connect(utaskS, end);
    }

    public void generateServiceTaskSequence(int numNodes, String serviceClass) throws Exception {

        Element start = addNode(startEvent, "startEvent");
        Element rtaskS = null;
        for (int i = 1; i <= numNodes; i++) {

            Element rtask = addNode(serviceTask, "st" + i);
            rtask.setAttributeNS(activitins, "class", serviceClass);

            if (rtaskS == null) {
                connect(start, rtask);
            } else {
                connect(rtaskS, rtask);
            }
            rtaskS = rtask;
        }

        Element end = addNode(endEvent, "endEvent");
        connect(rtaskS, end);
    }

    public void generateXORServiceTasks(int numGateways, String serviceClass) throws Exception {

        Element start = addNode(startEvent, "startEvent");
        Element end = addNode(endEvent, "endEvent");
        Element xorS = null;
        for (int i = 1; i <= numGateways; i++) {

            Element xor = addNode(xorGateway, "xor" + i);

            Element task = addNode(serviceTask, "st" + i);
            task.setAttributeNS(activitins, "class", serviceClass);
            Element defaultFlow = connect(xor, task);
            xor.setAttribute("default", defaultFlow.getAttribute("id"));
            connect(task, end);

            if (xorS == null) {
                connect(start, xor);
            } else {
                Element conFlow = connect(xorS, xor);
                Element condition = addElement(conFlow, ns, "conditionExpression");
                condition.setAttributeNS(xsins, "type", "tFormalExpression");
                CDATASection expression = doc.createCDATASection("${v1 >= " + i + "}");
                condition.appendChild(expression);
            }
            xorS = xor;
        }

        Element conditionTask = addNode(serviceTask, "st_condition");
        conditionTask.setAttributeNS(activitins, "class", serviceClass);
        Element lastConFlow = connect(xorS, conditionTask);
        Element condition = addElement(lastConFlow, ns, "conditionExpression");
        condition.setAttributeNS(xsins, "type", "tFormalExpression");
        CDATASection expression = doc.createCDATASection("${v1 > 10000}");
        condition.appendChild(expression);
        connect(conditionTask, end);
    }

    public void generateParallelServiceTasks(int numBranches, int nodePerBranch, String serviceClass) throws Exception {

        Element start = addNode(startEvent, "startEvent");
        Element parallelSplit1 = addNode(parallelGateway, "psplit1");
        connect(start, parallelSplit1);
        Element parallelJoin1 = addNode(parallelGateway, "pjoin1");

        for (int i = 1; i <= numBranches; i++) {
            Element[] branch = generateSequence(serviceClass, i, 10);
            connect(parallelSplit1, branch[0]);
            connect(branch[1], parallelJoin1);
        }

        Element end = addNode(endEvent, "endEvent");
        connect(parallelJoin1, end);
    }

    private Element[] generateSequence(String serviceClass, int seqNumber, int length) {

        Element[] terminals = new Element[2];

        Element rtaskS = null;
        for (int i = 1; i <= numNodes; i++) {
            Element rtask = addNode(serviceTask, "st" + seqNumber + "_" + i);
            rtask.setAttributeNS(activitins, "class", serviceClass);

            if (rtaskS == null) {
                terminals[0] = rtask;
            } else {
                connect(rtaskS, rtask);
            }
            rtaskS = rtask;
        }
        terminals[1] = rtaskS;
        return terminals;
    }

    public void generateRESTTaskSequence(int numNodes, String serviceURL) throws Exception {

        Element start = addNode(startEvent, "startEvent");
        Element rtaskS = null;
        for (int i = 1; i <= numNodes; i++) {

            Element rtask = addNode(serviceTask, "rt" + i);
            rtask.setAttributeNS(activitins, "class", "org.wso2.carbon.bpmn.extensions.rest.SyncInvokeTask");
            Element extensions = addElement(rtask, ns, "extensionElements");

            Element serviceURLField = addElement(extensions, activitins, "field");
            serviceURLField.setAttribute("name", "serviceURL");
            Element expression1 = addElement(serviceURLField, activitins, "expression");
            expression1.setTextContent(serviceURL);

            Element methodField = addElement(extensions, activitins, "field");
            methodField.setAttribute("name", "method");
            Element expression2 = addElement(methodField, activitins, "string");
            CDATASection postData = doc.createCDATASection("POST");
            expression2.appendChild(postData);

            Element inputField = addElement(extensions, activitins, "field");
            inputField.setAttribute("name", "input");
            Element expression3 = addElement(inputField, activitins, "expression");
            expression3.setTextContent("Message sent from: rt" + i);

            Element voutField = addElement(extensions, activitins, "field");
            voutField.setAttribute("name", "vout");
            Element expression4 = addElement(voutField, activitins, "string");
            CDATASection outvarData = doc.createCDATASection("outvar1");
            expression4.appendChild(outvarData);

            if (rtaskS == null) {
                connect(start, rtask);
            } else {
                connect(rtaskS, rtask);
            }
            rtaskS = rtask;
        }

        Element end = addNode(endEvent, "endEvent");
        connect(rtaskS, end);
    }

    public void generateUserTaskParallelModel(int numBranches, int nodesPerBranch) throws Exception  {

        Element start = addNode(startEvent, "startEvent");
    }

    private void write(File outFile) throws Exception {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        System.out.println("XML IN String format is: \n" + writer.toString());
        System.out.println("Writing XML to: " + outFile.getAbsolutePath());
        FileUtils.write(outFile, writer.toString());
    }

    private Element addNode(String nodeType, String id) {
        return addNode(nodeType, id, id);
    }

    private Element addNode(String nodeType, String id, String name) {
        Element node = doc.createElementNS(ns, nodeType);
        node.setAttribute("id", id);
        node.setAttribute("name", name);
        parent.appendChild(node);
        return node;
    }

    private Element connect(Element sourceNode, Element targetNode) {
        String flowId = sourceNode.getAttribute("id") + "_" + targetNode.getAttribute("id");
        return connect(sourceNode.getAttribute("id"), targetNode.getAttribute("id"), flowId);
    }

    private Element connect(String sourceId, String targetId) {
        String flowId = sourceId + "_" + targetId;
        return connect(sourceId, targetId, flowId);
    }

    private Element connect(String sourceId, String targetId, String flowId) {
        Element flow = doc.createElementNS(ns, "sequenceFlow");
        flow.setAttribute("id", flowId);
        flow.setAttribute("sourceRef", sourceId);
        flow.setAttribute("targetRef", targetId);
        parent.appendChild(flow);
        return flow;
    }

    private Element addElement(Element parent, String namespace, String name) {
        Element element = doc.createElementNS(namespace, name);
        parent.appendChild(element);
        return element;
    }
}
