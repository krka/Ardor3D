/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.lwjgl;

import com.ardor3d.input.MouseManager;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.math.Vector2;
import com.ardor3d.image.Image;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.annotation.MainThread;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Cursor;
import org.lwjgl.LWJGLException;

import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * LWJGL-specific implementation of the {@link com.ardor3d.input.MouseManager} interface. No methods in this
 * class should be called before the LWJGL Display has been initialized, since this class is dependent on being
 * able to initialize the {@link org.lwjgl.input.Mouse} class.
 */
public class LwjglMouseManager implements MouseManager {
    private boolean _inited = false;


    private void init() {
        if (!_inited) {
            try {
                Mouse.create();
            } catch (LWJGLException e) {
                throw new RuntimeException("Unable to initialise mouse manager", e);
            }
            _inited = true;
        }
    }

    @MainThread
    public void setCursor(final MouseCursor cursor) {
        init();

        try {
            final Cursor lwjglCursor = createLwjglCursor(cursor);

            if (!lwjglCursor.equals(Mouse.getNativeCursor())) {
                Mouse.setNativeCursor(lwjglCursor);
            }
        } catch (LWJGLException e) {
            throw new RuntimeException("Unable to set cursor", e);
        }
    }

    private Cursor createLwjglCursor(final MouseCursor cursor) throws LWJGLException {
        boolean eightBitAlpha = (Cursor.getCapabilities() & Cursor.CURSOR_8_BIT_ALPHA) != 0;

        Image image = cursor.getImage();

        boolean isRgba = image.getFormat() == Image.Format.RGBA8;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        ByteBuffer imageData = image.getData(0);
        imageData.rewind();
        IntBuffer imageDataCopy = BufferUtils.createIntBuffer(imageWidth * imageHeight);

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int index = y * imageWidth + x;

                int r = imageData.get() & 0xff;
                int g = imageData.get() & 0xff;
                int b = imageData.get() & 0xff;
                int a = 0xff;
                if (isRgba) {
                    a = imageData.get() & 0xff;
                    if (!eightBitAlpha) {
                        if (a < 0x7f) {
                            a = 0x00;
                            // small hack to prevent triggering "reverse screen" on windows.
                            r = g = b = 0;
                        }
                        else {
                            a = 0xff;
                        }
                    }
                }

                imageDataCopy.put(index, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }


        return new Cursor(imageWidth, imageHeight, cursor.getHotspotX(), cursor.getHotspotY(), 1, imageDataCopy, null);
    }

    public void setPosition(final Vector2 position) {
        init();

        Mouse.setCursorPosition((int) position.getX(), (int) position.getY());
    }

    public void setGrabbed(final GrabbedState grabbedState) {
        init();

        switch (grabbedState) {
            case GRABBED:
                Mouse.setGrabbed(true);
                break;
            case NOT_GRABBED:
                Mouse.setGrabbed(false);
                break;
            default:
                throw new IllegalStateException("Unhandled GrabbedState: " + grabbedState);
        }
    }
}
