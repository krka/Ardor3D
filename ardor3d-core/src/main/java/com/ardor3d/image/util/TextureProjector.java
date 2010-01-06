/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import com.ardor3d.image.Texture;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.renderer.Camera;

public class TextureProjector extends Camera {

    private final static ReadOnlyMatrix4 BIAS = new Matrix4( //
            0.5, 0.0, 0.0, 0.0, //
            0.0, 0.5, 0.0, 0.0, //
            0.0, 0.0, 0.5, 0.0, //
            0.5, 0.5, 0.5, 1.0);

    public TextureProjector() {
        super(1, 1);
    }

    public void updateTextureMatrix(final Texture texture) {
        update();
        final ReadOnlyMatrix4 projectorView = getModelViewMatrix();
        final ReadOnlyMatrix4 projectorProjection = getProjectionMatrix();
        final Matrix4 texMat = Matrix4.fetchTempInstance();
        texMat.set(projectorView).multiplyLocal(projectorProjection).multiplyLocal(BIAS);
        texture.setTextureMatrix(texMat);
        Matrix4.releaseTempInstance(texMat);
    }
}
