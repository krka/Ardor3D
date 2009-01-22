/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import com.ardor3d.renderer.state.GLSLShaderObjectsState;

public class ShaderObjectsStateRecord extends StateRecord {
    // XXX NOTE: This is non-standard. Due to the fact that shader implementations
    // XXX will be changed this record simply makes use of the old reference
    // XXX checking system.
    GLSLShaderObjectsState reference = null;

    public GLSLShaderObjectsState getReference() {
        return reference;
    }

    public void setReference(final GLSLShaderObjectsState reference) {
        this.reference = reference;
    }

    @Override
    public void invalidate() {
        super.invalidate();

        reference = null;
    }
}
