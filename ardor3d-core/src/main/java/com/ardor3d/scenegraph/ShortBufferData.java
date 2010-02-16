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
import java.nio.ShortBuffer;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Simple data class storing a buffer of shorts
 */
public class ShortBufferData extends IndexBufferData<ShortBuffer> implements Savable {

    /**
     * Instantiates a new ShortBufferData.
     */
    public ShortBufferData() {}

    /**
     * Instantiates a new ShortBufferData with a buffer of the given size.
     */
    public ShortBufferData(final int size) {
        this(BufferUtils.createShortBuffer(size));
    }

    /**
     * Creates a new ShortBufferData.
     * 
     * @param buffer
     *            Buffer holding the data. Must not be null.
     */
    public ShortBufferData(final ShortBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer can not be null!");
        }

        _buffer = buffer;
    }

    public Class<? extends ShortBufferData> getClassTag() {
        return getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule cap = im.getCapsule(this);
        _buffer = cap.readShortBuffer("buffer", null);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule cap = ex.getCapsule(this);
        cap.write(_buffer, "buffer", null);
    }

    @Override
    public int get() {
        return _buffer.get();
    }

    @Override
    public int get(final int index) {
        return _buffer.get(index);
    }

    @Override
    public ShortBufferData put(final int value) {
        _buffer.put((short) (value & 0xFFFF));
        return this;
    }

    @Override
    public ShortBufferData put(final int index, final int value) {
        _buffer.put(index, (short) (value & 0xFFFF));
        return this;
    }

    @Override
    public void put(final IndexBufferData<?> buf) {
        if (buf instanceof ShortBufferData) {
            _buffer.put((ShortBuffer) buf.getBuffer());
        } else {
            while (buf.getBuffer().hasRemaining()) {
                put(buf.get());
            }
        }
    }
}
