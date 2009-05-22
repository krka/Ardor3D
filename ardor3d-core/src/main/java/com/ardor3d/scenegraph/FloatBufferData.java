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
import java.nio.FloatBuffer;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Simple data class storing a buffer of floats and a number that indicates how many floats to group together to make up
 * a "tuple"
 */
public class FloatBufferData extends AbstractBufferData<FloatBuffer> implements Savable {

    /** Specifies the number of coordinates per vertex. Must be 1 - 4. */
    private int _valuesPerTuple;

    /**
     * Instantiates a new FloatBufferData.
     */
    public FloatBufferData() {}

    /**
     * Creates a new FloatBufferData.
     * 
     * @param buffer
     *            Buffer holding the data. Must not be null.
     * @param valuesPerTuple
     *            Specifies the number of values per tuple. Can not be < 1.
     */
    public FloatBufferData(final FloatBuffer buffer, final int valuesPerTuple) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer can not be null!");
        }

        if (valuesPerTuple < 1) {
            throw new IllegalArgumentException("valuesPerTuple must be greater than 1.");
        }

        _buffer = buffer;
        _valuesPerTuple = valuesPerTuple;
    }

    public int getTupleCount() {
        // TODO Auto-generated method stub
        return getBufferLimit() / _valuesPerTuple;
    }

    /**
     * @return number of values per tuple
     */
    public int getValuesPerTuple() {
        return _valuesPerTuple;
    }

    /**
     * Set number of values per tuple. This method should only be used internally.
     * 
     * @param valuesPerTuple
     *            number of values per tuple
     */
    void setValuesPerTuple(final int valuesPerTuple) {
        _valuesPerTuple = valuesPerTuple;
    }

    public Class<? extends FloatBufferData> getClassTag() {
        return getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule cap = im.getCapsule(this);
        _buffer = cap.readFloatBuffer("buffer", null);
        _valuesPerTuple = cap.readInt("valuesPerTuple", 0);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule cap = ex.getCapsule(this);
        cap.write(_buffer, "buffer", null);
        cap.write(_valuesPerTuple, "valuesPerTuple", 0);
    }
}
