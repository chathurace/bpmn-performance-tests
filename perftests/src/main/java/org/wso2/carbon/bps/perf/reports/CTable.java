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

public class CTable {

    private Map<Integer, Map<Integer, Double>> table = new HashMap<>();

    public void add(int row, int column, double value) {
        Map<Integer, Double> columnMap = table.get(row);
        if (columnMap == null) {
            columnMap = new HashMap<>();
            table.put(row, columnMap);
        }
        columnMap.put(column, value);
    }

    public Double get(int row, int column) {
        Map<Integer, Double> columnMap = table.get(row);
        if (columnMap == null) {
            return null;
        }
        return columnMap.get(column);
    }

    public Map<Integer, Map<Integer, Double>> getTable() {
        return table;
    }

    public int getNumRows() {
        return table.keySet().size();
    }

    public Set<Integer> getRowValues() {
        return table.keySet();
    }

    public Set<Integer> getColumnValues() {
        Set<Integer> columnValues = null;
        Collection<Map<Integer, Double>> rows = table.values();
        for (Map<Integer, Double> row : rows) {
            columnValues = row.keySet();
            break;
        }
        return columnValues;
    }
}
