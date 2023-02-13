/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package io.ceresdb.models;

import java.util.List;

public class WriteRequest {
    private RequestContext reqCtx;
    private List<Point>    points;

    public WriteRequest(List<Point> points) {
        this.points = points;
    }

    public WriteRequest(RequestContext reqCtx, List<Point> points) {
        this.reqCtx = reqCtx;
        this.points = points;
    }

    public RequestContext getReqCtx() {
        return reqCtx;
    }

    public void setReqCtx(RequestContext reqCtx) {
        this.reqCtx = reqCtx;
    }

    public List<Point> getPoints() {
        return points;
    }
}
