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
package org.wso2.carbon.bps.perf.reports;

import java.util.*;

public class ChartTable {

    private String modelName;

    // Structures for [num threads][model size]
    private CTable tps = new CTable();
    private CTable durations = new CTable();

//    private double[][] tps;
//    private double[][] duration;

    public ChartTable(String modelName) {
        this.modelName = modelName;
    }

    public void populate(String data) {

        String[] parts = data.split(",");
        String processId = parts[0];
        String processKey = processId.split(":")[0];
        int modelSize = Integer.parseInt(processKey.split("_")[1]);
        int numThreads = Integer.parseInt(parts[1]);

        double tpsValue = Double.parseDouble(parts[3]);
        double durationValue = Double.parseDouble(parts[4]);

        tps.add(numThreads, modelSize, tpsValue);
        durations.add(numThreads, modelSize, durationValue);
    }

    public String getTPSChart() {
        StringBuffer chart = new StringBuffer();
        chart.append("TPS of Model," + modelName + "\n");
        chart.append("Threads,Model sizes\n");

        List<Integer> rows = new ArrayList<>(tps.getRowValues());
        Collections.sort(rows);
        List<Integer> columns = new ArrayList<>(tps.getColumnValues());
        Collections.sort(columns);

        for (int columnValue : columns) {
            chart.append("," + columnValue);
        }
        chart.append("\n");

        for (int rowValue : rows) {
            chart.append(rowValue + ",");
            for (int column : columns) {
                double v = tps.get(rowValue, column);
                chart.append(v + ",");
            }
            chart.append("\n");
        }
        return chart.toString();
    }

    public String getDurationsChart() {
        StringBuffer chart = new StringBuffer();
        chart.append("Execution times of Model," + modelName + "\n");
        chart.append("Threads,Model sizes\n");

        List<Integer> rows = new ArrayList<>(durations.getRowValues());
        Collections.sort(rows);
        List<Integer> columns = new ArrayList<>(durations.getColumnValues());
        Collections.sort(columns);

        for (int columnValue : columns) {
            chart.append("," + columnValue);
        }
        chart.append("\n");

        for (int rowValue : rows) {
            chart.append(rowValue + ",");
            for (int column : columns) {
                double v = durations.get(rowValue, column);
                chart.append(v + ",");
            }
            chart.append("\n");
        }
        return chart.toString();
    }
}
