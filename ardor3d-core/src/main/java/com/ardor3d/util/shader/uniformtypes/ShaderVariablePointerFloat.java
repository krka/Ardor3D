/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.shader.uniformtypes;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.shader.ShaderVariable;

/** ShaderVariablePointerFloat */
public class ShaderVariablePointerFloat extends ShaderVariable {
    /**
     * Specifies the number of values for each element of the generic vertex attribute array. Must be 1, 2, 3, or 4.
     */
    public int size;
    /**
     * Specifies the byte offset between consecutive attribute values. If stride is 0 (the initial value), the attribute
     * values are understood to be tightly packed in the array.
     */
    public int stride;
    /**
     * Specifies whether fixed-point data values should be normalized (true) or converted directly as fixed-point values
     * (false) when they are accessed.
     */
    public boolean normalized;
    /** The data for the attribute value */
    public FloatBuffer data;

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);

        capsule.write(size, "size", 0);
        capsule.write(stride, "stride", 0);
        capsule.write(normalized, "normalized", false);
        capsule.write(data, "data", null);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);

        size = capsule.readInt("size", 0);
        stride = capsule.readInt("stride", 0);
        normalized = capsule.readBoolean("normalized", false);
        data = capsule.readFloatBuffer("data", null);
    }
}
