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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;

/**
 * A SerializingExecutor is a queue of tasks that run in sequence.
 *
 * Refer to <a href="https://github.com/grpc/grpc-java/blob/master/api/src/main/java/io/grpc/SynchronizationContext.java">SynchronizationContext</a>
 * Refer to <a href="https://github.com/grpc/grpc-java/blob/master/core/src/main/java/io/grpc/internal/SerializingExecutor.java">SerializingExecutor</a>
 */
public class SerializingExecutor implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(SerializingExecutor.class);

    private static final int QUEUE_SIZE_THRESHOLD = 512;

    private final String                          name;
    private final Timer                           singleTaskTimer;
    private final Timer                           drainTimer;
    private final Histogram                       drainNumHis;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private final Queue<Runnable>         queue          = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Thread> drainingThread = new AtomicReference<>();

    public SerializingExecutor(String name) {
        this(name, LogUncaughtExceptionHandler.INSTANCE);
    }

    public SerializingExecutor(String name, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.name = name;
        this.singleTaskTimer = MetricsUtil.timer("serializing_executor_single_task_timer", name);
        this.drainTimer = MetricsUtil.timer("serializing_executor_drain_timer", name);
        this.drainNumHis = MetricsUtil.histogram("serializing_executor_drain_num", name);
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    /**
     * Adds a task that will be run when {@link #drain} is called.
     *
     * This is useful for cases where you want to enqueue a task while
     * under a lock of your own, but don't want the tasks to be run under
     * your lock (for fear of deadlock).  You can call this method in the
     * lock, and call {@link #drain} outside the lock.
     */
    public final void executeLater(final Runnable task) {
        this.queue.add(Requires.requireNonNull(task, "task"));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public final void execute(final Runnable task) {
        executeLater(task);
        drain();
    }

    /**
     * If no other thread is running this method, run all tasks in the queue in
     * the current thread, otherwise do nothing.
     */
    public final void drain() {
        this.drainTimer.time(this::drain0);
    }

    private void drain0() {
        int drained = 0;
        do {
            if (!this.drainingThread.compareAndSet(null, Thread.currentThread())) {
                return;
            }

            try {
                Runnable task;
                while ((task = this.queue.poll()) != null) {
                    drained++;
                    final long startCall = Clock.defaultClock().getTick();
                    try {
                        task.run();
                    } catch (final Throwable t) {
                        this.uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                    } finally {
                        this.singleTaskTimer.update(Clock.defaultClock().duration(startCall), TimeUnit.MILLISECONDS);
                    }
                }
            } finally {
                this.drainingThread.set(null);
            }
            // must check queue again here to catch any added prior to clearing drainingThread
        } while (!this.queue.isEmpty());

        if (drained > 0) {
            this.drainNumHis.update(drained);
        }
        if (drained > QUEUE_SIZE_THRESHOLD) {
            LOG.warn("There were too many task [{}] in the queue [{}].", drained, this);
        }
    }

    private enum LogUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        INSTANCE;

        @Override
        public void uncaughtException(final Thread t, final Throwable err) {
            LOG.error("Uncaught exception in thread {}.", t, err);
        }
    }

    @Override
    public String toString() {
        return "SerializingExecutor{" + "name='" + name + '\'' + '}';
    }
}
