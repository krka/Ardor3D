/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Simple data class storing a buffer of bytes
 */
public class ByteBufferData extends IndexBufferData<ByteBuffer> implements Savable {

    /**
     * Instantiates a new ByteBufferData.
     */
    public ByteBufferData() {}

    /**
     * Instantiates a new ByteBufferData with a buffer of the given size.
     */
    public ByteBufferData(final int size) {
        this(BufferUtils.createByteBuffer(size));
    }

    /**
     * Creates a new ByteBufferData.
     * 
     * @param buffer
     *            Buffer holding the data. Must not be null.
     */
    public ByteBufferData(final ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer can not be null!");
        }

        _buffer = buffer;
    }

    public Class<? extends ByteBufferData> getClassTag() {
        return getClass();
    }

    public void read(final InputCapsule capsule) throws IOException {
        _buffer = capsule.readByteBuffer("buffer", null);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_buffer, "buffer", null);
    }

    @Override
    public int get() {
        return _buffer.get() & 0xFF;
    }

    @Override
    public int get(final int index) {
        return _buffer.get(index) & 0xFF;
    }

    @Override
    public ByteBufferData put(final int value) {
        if (value < 0 || value >= 256) {
            throw new IllegalArgumentException("Invalid value passed to byte buffer: " + value);
        }
        _buffer.put((byte) value);
        return this;
    }

    @Override
    public ByteBufferData put(final int index, final int value) {
        if (value < 0 || value >= 256) {
            throw new IllegalArgumentException("Invalid value passed to byte buffer: " + value);
        }
        _buffer.put(index, (byte) value);
        return this;
    }

    @Override
    public void put(final IndexBufferData<?> buf) {
        if (buf instanceof ByteBufferData) {
            _buffer.put((ByteBuffer) buf.getBuffer());
        } else {
            while (buf.getBuffer().hasRemaining()) {
                put(buf.get());
            }
        }
    }
}
