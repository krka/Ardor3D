/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scene.state.jogl.util.JoglRendererUtil;
import com.ardor3d.util.geom.BufferUtils;

public class JoglFont {

    /**
     * Sets the style of the font to normal.
     */
    public static final int NORMAL = 0;

    /**
     * Sets the style of the font to italics.
     */
    public static final int ITALICS = 1;

    // display list offset.
    private int base;

    // buffer that holds the text.
    private ByteBuffer scratch;

    // Color to render the font.
    private final ColorRGBA fontColor;

    /**
     * Constructor instantiates a new JoglFont object. The initial color is set to white.
     * 
     */
    public JoglFont() {
        fontColor = new ColorRGBA(1, 1, 1, 1);
        scratch = BufferUtils.createByteBuffer(1);
        buildDisplayList();
    }

    /**
     * <code>deleteFont</code> deletes the current display list of font objects. The font will be useless until a call
     * to <code>buildDisplayLists</code> is made.
     */
    public void deleteFont() {
        final GL gl = GLU.getCurrentGL();

        gl.glDeleteLists(base, 256);
    }

    /**
     * <code>setColor</code> sets the RGBA values to render the font as. By default the color is white with no
     * transparency.
     * 
     * @param color
     *            the color to set.
     */
    public void setColor(final ColorRGBA color) {
        fontColor.set(color);
    }

    /**
     * <code>print</code> renders the specified string to a given (x,y) location. The x, y location is in terms of
     * screen coordinates. There are currently two sets of fonts supported: NORMAL and ITALICS.
     * 
     * @param r
     * 
     * @param x
     *            the x screen location to start the string render.
     * @param y
     *            the y screen location to start the string render.
     * @param text
     *            the String to render.
     * @param set
     *            the mode of font: NORMAL or ITALICS.
     */
    public void print(final Renderer r, final double x, final double y, final ReadOnlyVector3 scale,
            final StringBuffer text, int set) {
        final GL gl = GLU.getCurrentGL();

        final RendererRecord matRecord = ContextManager.getCurrentContext().getRendererRecord();
        if (set > 1) {
            set = 1;
        } else if (set < 0) {
            set = 0;
        }

        final boolean alreadyOrtho = r.isInOrthoMode();
        if (!alreadyOrtho) {
            r.setOrtho();
        } else {
            JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
        }
        gl.glTranslated(x, y, 0);
        gl.glScaled(scale.getX(), scale.getY(), scale.getZ());
        gl.glListBase(base - 32 + (128 * set));

        // Put the string into a "pointer"
        if (text.length() > scratch.capacity()) {
            scratch = BufferUtils.createByteBuffer(text.length());
        } else {
            scratch.clear();
        }

        final int charLen = text.length();
        for (int z = 0; z < charLen; z++) {
            scratch.put((byte) text.charAt(z));
        }
        scratch.flip();
        gl.glColor4f(fontColor.getRed(), fontColor.getGreen(), fontColor.getBlue(), fontColor.getAlpha());
        // call the list for each letter in the string.
        gl.glCallLists(scratch.limit(), GL.GL_BYTE, scratch); // TODO Check <count> and assumed <type> GL_BYTE
        // set color back to white
        gl.glColor4f(1, 1, 1, 1);

        if (!alreadyOrtho) {
            r.unsetOrtho();
        } else {
            JoglRendererUtil.switchMode(matRecord, GL.GL_MODELVIEW);
            gl.glPopMatrix();
        }
    }

    /**
     * <code>buildDisplayList</code> sets up the 256 display lists that are used to render each font character. Each
     * list quad is 16x16, as defined by the font image size.
     */
    public void buildDisplayList() {
        final GL gl = GLU.getCurrentGL();

        float cx;
        float cy;

        base = gl.glGenLists(256);

        for (int loop = 0; loop < 256; loop++) {
            cx = (loop % 16) / 16.0f;
            cy = (loop / 16) / 16.0f;

            gl.glNewList(base + loop, GL.GL_COMPILE);
            gl.glBegin(GL.GL_QUADS);
            gl.glTexCoord2f(cx, 1 - cy - 0.0625f);
            gl.glVertex2i(0, 0);
            gl.glTexCoord2f(cx + 0.0625f, 1 - cy - 0.0625f);
            gl.glVertex2i(16, 0);
            gl.glTexCoord2f(cx + 0.0625f, 1 - cy);
            gl.glVertex2i(16, 16);
            gl.glTexCoord2f(cx, 1 - cy);
            gl.glVertex2i(0, 16);
            gl.glEnd();
            gl.glTranslatef(10, 0, 0);
            gl.glEndList();
        }
    }

    @Override
    public String toString() {
        String string = super.toString();
        string += "\nColor: " + fontColor.toString();

        return string;
    }
}
