/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.shader;

import java.io.IOException;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * An utily class to store shader's uniform variables content.
 */
public class ShaderVariable implements Savable {
    /** Name of the uniform variable. * */
    public String name;

    /** ID of uniform. * */
    public int variableID = -1;

    /** Needs to be refreshed */
    public boolean needsRefresh = true;

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ShaderVariable) {
            final ShaderVariable temp = (ShaderVariable) obj;
            if (name.equals(temp.name)) {
                return true;
            }
        }
        return false;
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);

        capsule.write(name, "name", "");
        capsule.write(variableID, "variableID", -1);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);

        name = capsule.readString("name", "");
        variableID = capsule.readInt("variableID", -1);
    }

    public Class<? extends ShaderVariable> getClassTag() {
        return this.getClass();
    }
}