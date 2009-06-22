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

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.ardor3d.image.util.AWTImageUtil;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.MouseManager;
import com.google.inject.Inject;

/**
 * Implementation of the {@link com.ardor3d.input.MouseManager} interface for use with AWT windows. This implementation
 * supports the optional {@link #setGrabbed(com.ardor3d.input.GrabbedState)} and {@link #setPosition(int, int)} methods
 * if an AWT robot can be created on the current system. The constructor takes an AWT {@link java.awt.Component}
 * instance, for which the cursor is set. In a multi-canvas application, each canvas can have its own AwtMouseManager
 * instance, or it is possible to use a single one for the AWT container that includes the canvases.
 */
public class AwtMouseManager implements MouseManager {
    private static final Logger logger = Logger.getLogger(AwtMouseManager.class.getName());

    private static Cursor _transparentCursor;

    private final Component _component;
    private Robot _robot;

    @Inject
    public AwtMouseManager(final Component component) {
        _component = component;

        // Attempt to make
        try {
            _robot = new Robot();
        } catch (final AWTException ex) {
            logger.warning("Unable to create java.awt.Robot.  setPosition and setGrabbed will not be supported.");
        }
    }

    public void setCursor(final MouseCursor cursor) {
        if (cursor == MouseCursor.SYSTEM_DEFAULT) {
            _component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }

        final BufferedImage image = AWTImageUtil.convertToAWT(cursor.getImage()).get(0);

        // the hotSpot values must be less than the Dimension returned by getBestCursorSize
        final Dimension bestCursorSize = Toolkit.getDefaultToolkit().getBestCursorSize(cursor.getHotspotX(),
                cursor.getHotspotY());
        final Point hotSpot = new Point(Math.min(cursor.getHotspotX(), (int) bestCursorSize.getWidth() - 1), Math.min(
                cursor.getHotspotY(), (int) bestCursorSize.getHeight() - 1));

        final Cursor awtCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, hotSpot, cursor.getName());

        _component.setCursor(awtCursor);
    }

    public void setPosition(final int x, final int y) {
        if (!isSetPositionSupported()) {
            throw new UnsupportedOperationException();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Component c = _component;
                if (c instanceof Frame && ((Frame) c).getComponentCount() > 0) {
                    c = ((Frame) c).getComponent(0);
                }
                final Point p = new Point(x, c.getHeight() - y);
                SwingUtilities.convertPointToScreen(p, c);
                _robot.mouseMove(p.x, p.y);
            }
        });
    }

    private void recenterMouse() {
        if (!isSetPositionSupported()) {
            throw new UnsupportedOperationException();
        }

        Component c = _component;
        if (c instanceof Frame && ((Frame) c).getComponentCount() > 0) {
            c = ((Frame) c).getComponent(0);
        }
        setPosition(c.getWidth() / 2, c.getHeight() / 2);
    }

    public void setGrabbed(final GrabbedState grabbedState) {
        if (!isSetGrabbedSupported()) {
            throw new UnsupportedOperationException();
        }

        if (grabbedState == GrabbedState.GRABBED) {
            _component.setCursor(getTransparentCursor());
        } else {
            _component.setCursor(null);
            if (isSetPositionSupported()) {
                recenterMouse();
            }
        }
    }

    private static final Cursor getTransparentCursor() {
        if (_transparentCursor == null) {
            final BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            cursorImage.setRGB(0, 0, 0);
            _transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(0, 0),
                    "empty cursor");
        }
        return _transparentCursor;
    }

    public boolean isSetPositionSupported() {
        return _robot != null;
    }

    public boolean isSetGrabbedSupported() {
        return _robot != null;
    }
}