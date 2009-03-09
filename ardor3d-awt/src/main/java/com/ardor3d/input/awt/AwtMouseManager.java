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

public class AwtMouseManager implements MouseManager {
    private final Component _component;

    @Inject
    public AwtMouseManager(final Component component) {
        this._component = component;
    }

    public void setCursor(MouseCursor cursor) {
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

    public void setGrabbed(GrabbedState grabbedState) {
        throw new UnsupportedOperationException();
    }

    public boolean isSetPositionSupported() {
        return false;  // not supported by AWT
    }

    public boolean isSetGrabbedSupported() {
        return false;  // not supported by AWT   
    }
}