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
import java.util.logging.Logger;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Simple data class storing a buffer of floats and a number that indicates how many floats to group together to make up
 * a texture coordinate "tuple"
 */
public class FloatBufferData implements Savable {

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(FloatBufferData.class.getName());

    /** Buffer holding the data. */
    private FloatBuffer buffer;

    /** Specifies the number of coordinates per vertex. Must be 1 - 4. */
    private int coordsPerVertex;

    /**
     * Instantiates a new FloatBufferData.
     */
    public FloatBufferData() {}

    /**
     * Creates a new FloatBufferData.
     * 
     * @param buffer
     *            Buffer holding the data
     * @param coordsPerVertex
     *            Specifies the number of coordinates per vertex. Must be 1 - 4.
     */
    public FloatBufferData(final FloatBuffer buffer, final int coordsPerVertex) {
        if (buffer == null) {
            logger.severe("Buffer can not be null!");
        }
        if (coordsPerVertex < 1 || coordsPerVertex > 4) {
            logger.severe("Number of coordinates per vertex must be 1 - 4");
        }

        this.buffer = buffer;
        this.coordsPerVertex = coordsPerVertex;
    }

    /**
     * Gets the count.
     * 
     * @return the count
     */
    public int getCount() {
        if (buffer != null) {
            return buffer.limit() / coordsPerVertex;
        }

        return 0;
    }

    /**
     * Get the buffer holding the coordinate data.
     * 
     * @return the buffer
     */
    public FloatBuffer getBuffer() {
        return buffer;
    }

    /**
     * Set the buffer holding the coordinate data. This method should only be used internally.
     * 
     * @param buffer
     *            the buffer to set
     */
    void setBuffer(final FloatBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Gets number of coordinates per vertex
     * 
     * @return number of coordinates per vertex
     */
    public int getCoordsPerVertex() {
        return coordsPerVertex;
    }

    /**
     * Set number of coordinates per vertex. This method should only be used internally.
     * 
     * @param coordsPerVertex
     */
    void setCoordsPerVertex(final int coordsPerVertex) {
        this.coordsPerVertex = coordsPerVertex;
    }

    public Class<? extends FloatBufferData> getClassTag() {
        return getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule cap = im.getCapsule(this);
        buffer = cap.readFloatBuffer("buffer", null);
        coordsPerVertex = cap.readInt("coordsPerVertex", 0);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule cap = ex.getCapsule(this);
        cap.write(buffer, "buffer", null);
        cap.write(coordsPerVertex, "coordsPerVertex", 0);
    }
}
