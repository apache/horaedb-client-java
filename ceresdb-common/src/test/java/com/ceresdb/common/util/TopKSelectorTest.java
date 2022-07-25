/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ceresdb.common.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author jiachun.fjc
 */
public class TopKSelectorTest {

    @Test
    public void selectTopKTest() {
        final Map<String, Integer> map = new HashMap<String, Integer>() {
            private static final long serialVersionUID = 2125026175853916643L;

            {
                put("a", 12);
                put("b", 11);
                put("c", 10);
                put("d", 9);
                put("e", 8);
                put("f", 7);
                put("g", 6);
                put("h", 5);
                put("i", 4);
                put("j", 3);
                put("k", 2);
                put("l", 1);
            }
        };

        final List<String> top5Keys = TopKSelector
                .selectTopK(map.entrySet(), 3, (o1, o2) -> -o1.getValue().compareTo(o2.getValue())) //
                .map(Map.Entry::getKey) //
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("j", "k", "l"), top5Keys);
    }
}
