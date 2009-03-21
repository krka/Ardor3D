/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.input.awt;

import com.ardor3d.image.util.AWTImageUtil;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;
import com.google.inject.Inject;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Implementation of the {@link com.ardor3d.input.MouseManager} interface for use with AWT windows.
 * This implementation does not support the optional {@link #setPosition(int, int)} and {@link #setGrabbed(com.ardor3d.input.GrabbedState)}
 * methods. The constructor takes an AWT {@link java.awt.Component} instance, for which the cursor is set. In
 * a multi-canvas application, each canvas can have its own AwtMouseManager instance, or it is
 * possible to use a single one for the AWT container that includes the canvases.
 */
public class AwtMouseManager implements MouseManager {
    private final Component _component;

    @Inject
    public AwtMouseManager(final Component component) {
        this._component = component;
    }

    public void setCursor(final MouseCursor cursor) {
        if (cursor == MouseCursor.SYSTEM_DEFAULT) {
            _component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        
        final BufferedImage image = AWTImageUtil.convertToAWT(cursor.getImage()).get(0);
        final Point hotSpot = new Point(cursor.getHotspotX(), cursor.getHotspotY());

        Cursor awtCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, hotSpot, cursor.getName());

        _component.setCursor(awtCursor);
    }

    public void setPosition(int x, int y) {
        throw new UnsupportedOperationException();
    }

    public void setGrabbed(final GrabbedState grabbedState) {
        throw new UnsupportedOperationException();
    }

    public boolean isSetPositionSupported() {
        return false;  // not supported by AWT
    }

    public boolean isSetGrabbedSupported() {
        return false;  // not supported by AWT   
    }
}