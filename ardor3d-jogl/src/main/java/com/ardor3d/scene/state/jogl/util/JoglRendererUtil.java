/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl.util;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.renderer.state.record.RendererRecord;

public class JoglRendererUtil {

    public static void switchMode(final RendererRecord rendRecord, final int mode) {
        final GL gl = GLU.getCurrentGL();

        if (!rendRecord.isMatrixValid() || rendRecord.getMatrixMode() != mode) {
            gl.glMatrixMode(mode);
            rendRecord.setMatrixMode(mode);
            rendRecord.setMatrixValid(true);
        }
    }

    public static void setBoundVBO(final RendererRecord rendRecord, final int id) {
        final GL gl = GLU.getCurrentGL();

        if (!rendRecord.isVboValid() || rendRecord.getCurrentVboId() != id) {
            gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, id);
            rendRecord.setCurrentVboId(id);
            rendRecord.setVboValid(true);
        }
    }

    public static void setBoundElementVBO(final RendererRecord rendRecord, final int id) {
        final GL gl = GLU.getCurrentGL();

        if (!rendRecord.isElementVboValid() || rendRecord.getCurrentElementVboId() != id) {
            gl.glBindBufferARB(GL.GL_ELEMENT_ARRAY_BUFFER_ARB, id);
            rendRecord.setCurrentElementVboId(id);
            rendRecord.setElementVboValid(true);
        }
    }
}
