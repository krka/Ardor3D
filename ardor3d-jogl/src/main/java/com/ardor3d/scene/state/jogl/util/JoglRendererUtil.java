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

import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.math.Rectangle2;
import com.ardor3d.renderer.state.record.RendererRecord;

public abstract class JoglRendererUtil {

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

    public static void applyScissors(final RendererRecord rendRecord) {
        final GL gl = GLU.getCurrentGL();
        final Stack<Rectangle2> clips = rendRecord.getScissorClips();

        if (clips.size() > 0) {
            gl.glEnable(GL.GL_SCISSOR_TEST);

            Rectangle2 init = null;
            for (final Rectangle2 r : clips) {
                if (init == null) {
                    init = new Rectangle2(r);
                } else {
                    init.intersect(r, init);
                }
                if (init.getWidth() <= 0 || init.getHeight() <= 0) {
                    init.setWidth(0);
                    init.setHeight(0);
                    break;
                }
            }
            gl.glScissor(init.getX(), init.getY(), init.getWidth(), init.getHeight());
        } else {
            // no clips, so disable
            gl.glDisable(GL.GL_SCISSOR_TEST);
        }
    }

    public static void setClippingEnabled(final RendererRecord rendRecord, final boolean enabled) {
        final GL gl = GLU.getCurrentGL();

        if (enabled && (!rendRecord.isClippingTestValid() || !rendRecord.isClippingTestEnabled())) {
            gl.glEnable(GL.GL_SCISSOR_TEST);
            rendRecord.setClippingTestEnabled(true);
        } else if (!enabled && (!rendRecord.isClippingTestValid() || rendRecord.isClippingTestEnabled())) {
            gl.glDisable(GL.GL_SCISSOR_TEST);
            rendRecord.setClippingTestEnabled(false);
        }
        rendRecord.setClippingTestValid(true);
    }
}
