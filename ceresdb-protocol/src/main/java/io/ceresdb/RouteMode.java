/**
 * Alipay.com Inc. Copyright (c) 2004-2017 All Rights Reserved. CreatedBy: RouteMode.java CreatedDate: 2023年01月17日 14:23 ticketNumber: (aone
 * link) comments: Description
 * <p>
 * codeReviewBy: codeReviewDate: comments:
 * <p>
 * modifiedBy: modifiedDate: comments:
 */
package io.ceresdb;

/**
 *  define route mode
 * @author lee
 * @version : RouteMode.java, v 0.1 2023年01月17日 14:23 lee Exp $
 */
public enum RouteMode{
    /**
     * In this mode, client request to a server  directly, and the server proxy the request to the correct server.
     */
    STANDALONE,

    /**
     * In this mode, the client will find the correct server first, and then request to the server.
     */
    CLUSTER
}