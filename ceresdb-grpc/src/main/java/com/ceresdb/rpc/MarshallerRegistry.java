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
package com.ceresdb.rpc;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.ceresdb.common.util.Requires;
import com.google.protobuf.Message;

/**
 * Marshaller registry for grpc service.
 *
 * @author jiachun.fjc
 */
public interface MarshallerRegistry {

    /**
     * Find method name by request class.
     *
     * @param reqCls request class
     * @param methodType grpc method type
     * @return method name
     */
    default String getMethodName(final Class<? extends Message> reqCls, //
                                 final io.grpc.MethodDescriptor.MethodType methodType) {
        return getMethodName(reqCls, supportedMethodType(methodType));
    }

    /**
     * Find method name by request class.
     *
     * @param reqCls request class
     * @param methodType method type
     * @return method name
     */
    String getMethodName(final Class<? extends Message> reqCls, final MethodDescriptor.MethodType methodType);

    /**
     * Get all registered method descriptor.
     *
     * @return method descriptor list
     */
    Set<MethodDescriptor> getAllMethodDescriptors();

    /**
     * Get all method's limit percent.
     *
     * @return method's limit percent
     */
    default Map<String, Double> getAllMethodsLimitPercent() {
        return getAllMethodDescriptors() //
                .stream() //
                .filter(mth -> mth.getLimitPercent() > 0) //
                .collect(Collectors.toMap(MethodDescriptor::getName, MethodDescriptor::getLimitPercent));
    }

    /**
     * Find default request instance by request class.
     *
     * @param reqCls request class
     * @return default request instance
     */
    Message getDefaultRequestInstance(final Class<? extends Message> reqCls);

    /**
     * Find default response instance by request class.
     *
     * @param reqCls request class
     * @return default response instance
     */
    Message getDefaultResponseInstance(final Class<? extends Message> reqCls);

    /**
     * Register default request instance.
     *
     * @param method         method name and type
     * @param reqCls         request class
     * @param defaultReqIns  default request instance
     * @param defaultRespIns default response instance
     */
    void registerMarshaller(final MethodDescriptor method, //
                            final Class<? extends Message> reqCls, //
                            final Message defaultReqIns, //
                            final Message defaultRespIns);

    default MethodDescriptor.MethodType supportedMethodType(final io.grpc.MethodDescriptor.MethodType mt) {
        switch (mt) {
            case UNARY:
                return MethodDescriptor.MethodType.UNARY;
            case CLIENT_STREAMING:
                return MethodDescriptor.MethodType.CLIENT_STREAMING;
            case SERVER_STREAMING:
                return MethodDescriptor.MethodType.SERVER_STREAMING;
            default:
                throw new UnsupportedOperationException(mt.name());
        }
    }

    enum DefaultMarshallerRegistry implements MarshallerRegistry {
        INSTANCE;

        private final Map<Class<? extends Message>, Map<MethodDescriptor.MethodType, MethodDescriptor>> methods   = new ConcurrentHashMap<>();
        private final Map<Class<? extends Message>, Message>                                            requests  = new ConcurrentHashMap<>();
        private final Map<Class<? extends Message>, Message>                                            responses = new ConcurrentHashMap<>();

        @Override
        public String getMethodName(final Class<? extends Message> reqCls,
                                    final MethodDescriptor.MethodType methodType) {
            final Map<MethodDescriptor.MethodType, MethodDescriptor> methods = this.methods.get(reqCls);
            Requires.requireNonNull(methods, "Could not find method by " + reqCls);
            final MethodDescriptor md = methods.get(methodType);
            Requires.requireNonNull(md, "Could not find method by " + reqCls + " and " + methodType);
            return md.getName();
        }

        @Override
        public Set<MethodDescriptor> getAllMethodDescriptors() {
            return this.methods.values().stream().flatMap(map -> map.values().stream()).collect(Collectors.toSet());
        }

        @Override
        public Message getDefaultRequestInstance(final Class<? extends Message> reqCls) {
            return Requires.requireNonNull(this.requests.get(reqCls), "Could not find request instance by " + reqCls);
        }

        @Override
        public Message getDefaultResponseInstance(final Class<? extends Message> reqCls) {
            return Requires.requireNonNull(this.responses.get(reqCls), "Could not find response instance by " + reqCls);
        }

        @Override
        public void registerMarshaller(final MethodDescriptor method, final Class<? extends Message> reqCls,
                                       final Message defaultReqIns, final Message defaultRespIns) {
            this.methods.computeIfAbsent(reqCls, cls -> new ConcurrentHashMap<>()).put(method.getType(), method);
            this.requests.put(reqCls, defaultReqIns);
            this.responses.put(reqCls, defaultRespIns);
        }
    }
}
