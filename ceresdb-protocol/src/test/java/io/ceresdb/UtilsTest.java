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
package io.ceresdb;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import io.ceresdb.common.Endpoint;
import io.ceresdb.models.Err;
import io.ceresdb.models.Point;
import io.ceresdb.models.Result;
import io.ceresdb.models.WriteOk;
import io.ceresdb.util.TestUtil;
import io.ceresdb.util.Utils;

/**
 *
 * @author xvyang.xy
 */
public class UtilsTest {

    @Test
    public void combineOkResultTest() {
        final Result<WriteOk, Err> ok1 = WriteOk.ok(100, 1, null).mapToResult();
        final Result<WriteOk, Err> ok2 = WriteOk.ok(200, 0, new HashSet<>(Collections.singletonList("t1")))
                .mapToResult();
        final Result<WriteOk, Err> r = Utils.combineResult(ok1, ok2);

        Assert.assertEquals(new Integer(300), r.mapOr(0, WriteOk::getSuccess));
        Assert.assertEquals(new Integer(1), r.mapOr(0, WriteOk::getFailed));
    }

    @Test
    public void combineErrResultTest() {
        final Result<WriteOk, Err> err1 = Err.writeErr(400, "err1", Endpoint.of("127.0.0.1", 9001), null).mapToResult();
        final Result<WriteOk, Err> err2 = Err.writeErr(401, "err2", Endpoint.of("127.0.0.2", 9001), null).mapToResult();
        final Result<WriteOk, Err> r = Utils.combineResult(err1, err2);

        Assert.assertEquals(400, r.getErr().getCode());
        Assert.assertEquals(Endpoint.of("127.0.0.1", 9001), r.getErr().getErrTo());
        final Optional<Err> next = r.getErr().stream().skip(1).findFirst();
        Assert.assertTrue(next.isPresent());
        Assert.assertEquals(401, next.get().getCode());
        Assert.assertEquals(Endpoint.of("127.0.0.2", 9001), next.get().getErrTo());
    }

    @Test
    public void combineErrAndOkResultTest() {
        final Result<WriteOk, Err> err = Err.writeErr(400, "err1", Endpoint.of("127.0.0.1", 9001), null).mapToResult();
        final Result<WriteOk, Err> ok1 = WriteOk.ok(200, 0, null).mapToResult();
        final Result<WriteOk, Err> ok2 = WriteOk.ok(300, 0, null).mapToResult();
        final Result<WriteOk, Err> r1 = Utils.combineResult(ok1, err);

        Assert.assertFalse(r1.isOk());
        Assert.assertEquals(Endpoint.of("127.0.0.1", 9001), r1.getErr().getErrTo());
        Assert.assertEquals(200, r1.getErr().getSubOk().getSuccess());

        final Result<WriteOk, Err> r2 = Utils.combineResult(ok2, r1);
        Assert.assertFalse(r2.isOk());
        Assert.assertEquals(500, r2.getErr().getSubOk().getSuccess());
    }

    @Test
    public void splitDataBySingleRouteTest() {
        final List<Point> data = TestUtil.newMultiTablePoints("t1", "t2", "t3");
        final Map<String, Route> routes = new HashMap<>();
        final Endpoint ep = Endpoint.of("127.0.0.1", 9001);
        routes.put("t1", Route.of("t1", ep));
        routes.put("t2", Route.of("t2", ep));
        routes.put("t3", Route.of("t3", ep));

        final Map<Endpoint, List<Point>> result = Utils.splitDataByRoute(data, routes);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(ep, result.keySet().stream().findFirst().orElse(null));
        Assert.assertEquals(6, result.values().stream().findFirst().orElse(Collections.emptyList()).size());
    }

    @Test
    public void splitDataByMultiRouteTest() {
        final List<Point> data = TestUtil.newMultiTablePoints("t1", "t2", "t3");
        final Map<String, Route> routes = new HashMap<>();
        final Endpoint ep1 = Endpoint.of("127.0.0.1", 9001);
        final Endpoint ep2 = Endpoint.of("127.0.0.2", 9001);
        routes.put("t1", Route.of("t1", ep1));
        routes.put("t2", Route.of("t2", ep1));
        routes.put("t3", Route.of("t3", ep2));

        final Map<Endpoint, List<Point>> result = Utils.splitDataByRoute(data, routes);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(4, result.get(ep1).size());
        Assert.assertEquals(2, result.get(ep2).size());
    }
}
