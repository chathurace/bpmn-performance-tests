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

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;

public abstract class AbstractGenerator {

    public static final String ns = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    public static final String activitins = "http://activiti.org/bpmn";
    public static final String xsins = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String startEvent = "startEvent";
    public static final String endEvent = "endEvent";
    public static final String parallelGateway = "parallelGateway";
    public static final String xorGateway = "exclusiveGateway";
    public static final String userTask = "userTask";
    public static final String serviceTask = "serviceTask";

    protected Element parent;
    protected Document doc;
    protected String[] c;

    public abstract String getKey();

    public abstract boolean generateModel(String config) throws Exception;

    public boolean generate(String config) throws Exception {

        c = config.split(",");
        for (int i = 0; i < c.length; i++) {
            c[i] = c[i].trim();
        }

        if (!c[1].equals(getKey())) {
            return false;
        }

        String processId = c[0];
        generateSkeleton(processId);
        generateModel(config);

        String outPath = BPMNGenerator.outPath + processId + ".bpmn20.xml";
        write(new File(outPath));
        return true;
    }

    protected void generateSkeleton(String processId) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        doc = docBuilder.newDocument();
        Element rootElement = doc.createElementNS(ns, "definitions");
        rootElement.setAttribute("targetNamespace", "http://www.activiti.org/test");
        doc.appendChild(rootElement);

        Element process = doc.createElementNS(ns, "process");
        process.setAttribute("id", processId);
        process.setAttribute("name", processId);
        process.setAttribute("isExecutable", "true");
        rootElement.appendChild(process);
        this.parent = process;
    }

    protected void write(File outFile) throws Exception {
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

    protected Element addNode(String nodeType, String id) {
        return addNode(nodeType, id, id);
    }

    protected Element addNode(String nodeType, String id, String name) {
        Element node = doc.createElementNS(ns, nodeType);
        node.setAttribute("id", id);
        node.setAttribute("name", name);
        parent.appendChild(node);
        return node;
    }

    protected Element connect(Element sourceNode, Element targetNode) {
        String flowId = sourceNode.getAttribute("id") + "_" + targetNode.getAttribute("id");
        return connect(sourceNode.getAttribute("id"), targetNode.getAttribute("id"), flowId);
    }

    protected Element connect(String sourceId, String targetId) {
        String flowId = sourceId + "_" + targetId;
        return connect(sourceId, targetId, flowId);
    }

    protected Element connect(String sourceId, String targetId, String flowId) {
        Element flow = doc.createElementNS(ns, "sequenceFlow");
        flow.setAttribute("id", flowId);
        flow.setAttribute("sourceRef", sourceId);
        flow.setAttribute("targetRef", targetId);
        parent.appendChild(flow);
        return flow;
    }

    protected Element addElement(Element parent, String namespace, String name) {
        Element element = doc.createElementNS(namespace, name);
        parent.appendChild(element);
        return element;
    }
}
