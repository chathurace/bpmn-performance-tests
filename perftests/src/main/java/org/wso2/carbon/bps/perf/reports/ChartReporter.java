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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class ChartReporter {

    private String summaryPath = "/home/chathura/projects/bps-perf/artifacts/results5/summary.csv";
    private String chartPath = "/home/chathura/projects/bps-perf/artifacts/results5/charts.csv";

    public static void main(String[] args) {
        try {
            new ChartReporter().generateCharts(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateCharts(String[] args) throws Exception {

        if (args.length > 0) {
            if (args.length == 3) {
                File summaryFile = new File(args[1]);
                File chartsFile = new File(args[2]);
                summaryPath = summaryFile.getAbsolutePath();
                chartPath = chartsFile.getAbsolutePath();
            } else {
                String workingPath = System.getProperty("user.dir");
                System.out.println("Usage: ChartReporter <summary-path> <output-path>.\n Current working directory is " + workingPath);
                return;
            }

        }
        System.out.println("Summary file location: " + summaryPath);
        System.out.println("Charts file location: " + chartPath);

        ChartTableHolder chartTableHolder = new ChartTableHolder();

        List<String> data = FileUtils.readLines(new File(summaryPath));
        boolean resultsStarted = false;
        for (String result : data) {
            if (resultsStarted && result.length() > 0) {
                ChartTable table = chartTableHolder.getChartTable(result);
                table.populate(result);
            }

            if (!resultsStarted && result.startsWith("Process ID")) {
                resultsStarted = true;
            }
        }

        StringBuffer chartData = new StringBuffer();
        Collection<ChartTable> chartTables = chartTableHolder.getChartTables();
        for (ChartTable chartTable : chartTables) {
            chartData.append("\n\n");
            chartData.append(chartTable.getTPSChart());
            chartData.append("\n\n");
            chartData.append(chartTable.getDurationsChart());
        }

        FileUtils.write(new File(chartPath), chartData.toString());
    }


}
