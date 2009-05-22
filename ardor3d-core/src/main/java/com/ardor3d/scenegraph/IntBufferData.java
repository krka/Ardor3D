/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.IntBuffer;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Simple data class storing a buffer of ints
 */
public class IntBufferData extends AbstractBufferData<IntBuffer> implements Savable {

    /**
     * Instantiates a new IntBufferData.
     */
    public IntBufferData() {}

    /**
     * Creates a new IntBufferData.
     * 
     * @param buffer
     *            Buffer holding the data. Must not be null.
     */
    public IntBufferData(final IntBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer can not be null!");
        }

        _buffer = buffer;
    }

    public Class<? extends IntBufferData> getClassTag() {
        return getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule cap = im.getCapsule(this);
        _buffer = cap.readIntBuffer("buffer", null);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule cap = ex.getCapsule(this);
        cap.write(_buffer, "buffer", null);
    }
}
