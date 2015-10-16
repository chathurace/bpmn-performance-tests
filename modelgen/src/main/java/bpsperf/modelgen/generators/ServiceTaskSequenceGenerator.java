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
import org.w3c.dom.Element;

public class ServiceTaskSequenceGenerator extends AbstractGenerator {

    @Override
    public String getKey() {
        return "java";
    }

    @Override
    public boolean generateModel(String config) throws Exception {

        int numNodes = Integer.parseInt(c[2]);
        String serviceClass = c[3];

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

        return true;
    }
}
