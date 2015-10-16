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

public class ParallelServiceTasksGenerator extends AbstractGenerator {

    @Override
    public String getKey() {
        return "par";
    }

    @Override
    public boolean generateModel(String config) throws Exception {

        int numBranches = Integer.parseInt(c[2]);
        int numNodes = Integer.parseInt(c[3]);
        String serviceClass = c[4];

        Element start = addNode(startEvent, "startEvent");
        Element parallelSplit1 = addNode(parallelGateway, "psplit1");
        connect(start, parallelSplit1);
        Element parallelJoin1 = addNode(parallelGateway, "pjoin1");

        for (int i = 1; i <= numBranches; i++) {
            Element[] branch = generateSequence(serviceClass, i, numNodes);
            connect(parallelSplit1, branch[0]);
            connect(branch[1], parallelJoin1);
        }

        Element end = addNode(endEvent, "endEvent");
        connect(parallelJoin1, end);

        return true;
    }

    private Element[] generateSequence(String serviceClass, int seqNumber, int numNodes) {

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
}
