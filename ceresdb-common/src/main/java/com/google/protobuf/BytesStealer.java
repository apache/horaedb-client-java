/*
 * Copyright 2023 CeresDB Project Authors. Licensed under Apache-2.0.
 */
package com.google.protobuf;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Try to gets the bytes form ByteString with no copy.
 *
 */
public class BytesStealer extends ByteOutput {

    private byte[]  value;
    private boolean valid = false;

    public byte[] value() {
        return value;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void write(final byte value) {
        this.valid = false;
    }

    @Override
    public void write(final byte[] value, final int offset, final int length) {
        doWrite(value, offset, length);
    }

    @Override
    public void writeLazy(final byte[] value, final int offset, final int length) {
        doWrite(value, offset, length);
    }

    @Override
    public void write(final ByteBuffer value) throws IOException {
        this.valid = false;
    }

    @Override
    public void writeLazy(final ByteBuffer value) {
        this.valid = false;
    }

    private void doWrite(final byte[] value, final int offset, final int length) {
        if (this.value != null) {
            this.valid = false;
            return;
        }
        if (offset == 0 && length == value.length) {
            this.value = value;
            this.valid = true;
        }
    }
}
