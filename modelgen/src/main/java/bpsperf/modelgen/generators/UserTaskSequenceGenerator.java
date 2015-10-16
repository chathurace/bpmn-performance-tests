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

public class UserTaskSequenceGenerator extends AbstractGenerator {

    @Override
    public String getKey() {
        return "user";
    }

    @Override
    public boolean generateModel(String config) throws Exception {

        int numNodes = Integer.parseInt(c[2]);

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

        return true;
    }
}
