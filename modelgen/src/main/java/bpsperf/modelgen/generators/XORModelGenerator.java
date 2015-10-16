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
package bpsperf.modelgen.generators;

import bpsperf.modelgen.AbstractGenerator;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

public class XORModelGenerator extends AbstractGenerator {

    @Override
    public String getKey() {
        return "xor";
    }

    @Override
    public boolean generateModel(String config) throws Exception {

        int numGateways = Integer.parseInt(c[2]);
        String serviceClass = c[3];

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

        return true;
    }
}
