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

public class RestTaskSequenceGenerator extends AbstractGenerator {

    @Override
    public String getKey() {
        return "rest";
    }

    @Override
    public boolean generateModel(String config) throws Exception {

        int numNodes = Integer.parseInt(c[2]);
        String serviceURL = c[3];

        Element start = addNode(startEvent, "startEvent");
        Element rtaskS = null;
        for (int i = 1; i <= numNodes; i++) {

            Element rtask = addNode(serviceTask, "rt" + i);
            rtask.setAttributeNS(activitins, "class", "org.wso2.carbon.bpmn.extensions.rest.RESTTask");
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
            voutField.setAttribute("name", "outputVariable");
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

        return true;
    }


}
