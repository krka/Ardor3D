/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
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
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.shader.ShaderVariable;

/** ShaderVariableMatrix2 */
public class ShaderVariableMatrix2 extends ShaderVariable {
    public FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(4);
    public boolean rowMajor;

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);

        capsule.write(matrixBuffer, "matrixBuffer", null);
        capsule.write(rowMajor, "rowMajor", false);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);

        matrixBuffer = capsule.readFloatBuffer("matrixBuffer", null);
        rowMajor = capsule.readBoolean("rowMajor", false);
    }
}
