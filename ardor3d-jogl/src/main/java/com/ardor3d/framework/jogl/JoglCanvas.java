/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.ardor3d.renderer.jogl.JoglPbufferTextureRenderer;
import com.ardor3d.util.Ardor3dException;
import com.google.inject.Inject;

/**
 * A canvas implementation for use with native JOGL windows.
 */
public class JoglCanvas extends Frame implements NativeCanvas {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(JoglCanvas.class.getName());

    private final JoglCanvasRenderer _canvasRenderer;

    private final DisplaySettings _settings;
    private boolean _inited = false;
    // private Frame frame;
    private boolean _isClosing = false;

    private GLCanvas _glCanvas;

    // private PhysicalLayer physicalLayer;
    // private final boolean hasFocus = false;

    @Inject
    public JoglCanvas(final JoglCanvasRenderer canvasRenderer, final DisplaySettings settings) {
        _canvasRenderer = canvasRenderer;
        _settings = settings;
    }

    @Override
    public void addKeyListener(final KeyListener l) {
        _glCanvas.addKeyListener(l);
    }

    @Override
    public void addMouseListener(final MouseListener l) {
        _glCanvas.addMouseListener(l);
    }

    @Override
    public void addMouseMotionListener(final MouseMotionListener l) {
        _glCanvas.addMouseMotionListener(l);
    }

    @Override
    public void addMouseWheelListener(final MouseWheelListener l) {
        _glCanvas.addMouseWheelListener(l);
    }

    @Override
    public void addFocusListener(final FocusListener l) {
        _glCanvas.addFocusListener(l); // To change body of overridden methods use File | Settings | File Templates.
    }

    @MainThread
    public void init() {
        privateInit();
    }

    @MainThread
    protected void privateInit() {
        if (_inited) {
            return;
        }

        // Validate window dimensions.
        if (_settings.getWidth() <= 0 || _settings.getHeight() <= 0) {
            throw new Ardor3dException("Invalid resolution values: " + _settings.getWidth() + " "
                    + _settings.getHeight());
        }

        // Validate bit depth.
        if ((_settings.getColorDepth() != 32) && (_settings.getColorDepth() != 16) && (_settings.getColorDepth() != 24)) {
            throw new Ardor3dException("Invalid pixel depth: " + _settings.getColorDepth());
        }

        // Create the OpenGL canvas, and place it within a frame.
        // frame = new Frame();

        // Create the singleton's status.
        final GLCapabilities caps = new GLCapabilities();
        caps.setHardwareAccelerated(true);
        caps.setDoubleBuffered(true);
        caps.setAlphaBits(_settings.getAlphaBits());
        caps.setDepthBits(_settings.getDepthBits());
        caps.setNumSamples(_settings.getSamples());
        caps.setStereo(_settings.isStereo());

        // Create the OpenGL canvas,
        _glCanvas = new GLCanvas(caps);

        _glCanvas.setFocusable(true);
        _glCanvas.requestFocus();
        _glCanvas.setSize(_settings.getWidth(), _settings.getHeight());
        _glCanvas.setIgnoreRepaint(true);
        _glCanvas.setAutoSwapBufferMode(false);

        final GLContext glContext = _glCanvas.getContext();
        _canvasRenderer.setContext(glContext);
        // hack
        JoglPbufferTextureRenderer._parentContext = glContext;

        this.add(_glCanvas);
        final boolean isDisplayModeModified;
        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        // Get the current display mode
        final DisplayMode previousDisplayMode = gd.getDisplayMode();
        // Handle full screen mode if requested.
        if (_settings.isFullScreen()) {
            setUndecorated(true);
            // Check if the full-screen mode is supported by the OS
            boolean isFullScreenSupported = gd.isFullScreenSupported();
            if (isFullScreenSupported) {
                gd.setFullScreenWindow(this);
                // Check if display mode changes are supported by the OS
                if (gd.isDisplayChangeSupported()) {
                    // Get all available display modes
                    final DisplayMode[] displayModes = gd.getDisplayModes();
                    DisplayMode multiBitsDepthSupportedDisplayMode = null;
                    DisplayMode refreshRateUnknownDisplayMode = null;
                    DisplayMode multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode = null;
                    DisplayMode matchingDisplayMode = null;
                    DisplayMode currentDisplayMode;
                    // Look for the display mode that matches with our parameters
                    // Look for some display modes that are close to these parameters
                    // and that could be used as substitutes
                    // On some machines, the refresh rate is unknown and/or multi bit
                    // depths are supported. If you try to force a particular refresh
                    // rate or a bit depth, you might find no available display mode
                    // that matches exactly with your parameters
                    for (int i = 0; i < displayModes.length && matchingDisplayMode == null; i++) {
                        currentDisplayMode = displayModes[i];
                        if (currentDisplayMode.getWidth() == _settings.getWidth()
                                && currentDisplayMode.getHeight() == _settings.getHeight()) {
                            if (currentDisplayMode.getBitDepth() == _settings.getColorDepth()) {
                                if (currentDisplayMode.getRefreshRate() == _settings.getFrequency()) {
                                    matchingDisplayMode = currentDisplayMode;
                                } else if (currentDisplayMode.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) {
                                    refreshRateUnknownDisplayMode = currentDisplayMode;
                                }
                            } else if (currentDisplayMode.getBitDepth() == DisplayMode.BIT_DEPTH_MULTI) {
                                if (currentDisplayMode.getRefreshRate() == _settings.getFrequency()) {
                                    multiBitsDepthSupportedDisplayMode = currentDisplayMode;
                                } else if (currentDisplayMode.getRefreshRate() == DisplayMode.REFRESH_RATE_UNKNOWN) {
                                    multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode = currentDisplayMode;
                                }
                            }
                        }
                    }
                    DisplayMode nextDisplayMode = null;
                    if (matchingDisplayMode != null) {
                        nextDisplayMode = matchingDisplayMode;
                    } else if (multiBitsDepthSupportedDisplayMode != null) {
                        nextDisplayMode = multiBitsDepthSupportedDisplayMode;
                    } else if (refreshRateUnknownDisplayMode != null) {
                        nextDisplayMode = refreshRateUnknownDisplayMode;
                    } else if (multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode != null) {
                        nextDisplayMode = multiBitsDepthSupportedAndRefreshRateUnknownDisplayMode;
                    } else {
                        isFullScreenSupported = false;
                    }
                    // If we have found a display mode that approximatively matches
                    // with the input parameters, use it
                    if (nextDisplayMode != null) {
                        gd.setDisplayMode(nextDisplayMode);
                        isDisplayModeModified = true;
                    } else {
                        isDisplayModeModified = false;
                    }
                } else {
                    isDisplayModeModified = false;
                    // Resize the canvas if the display mode cannot be changed
                    // and the screen size is not equal to the canvas size
                    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    if (screenSize.width != _settings.getWidth() || screenSize.height != _settings.getHeight()) {
                        _glCanvas.setSize(screenSize);
                    }
                }
            } else {
                isDisplayModeModified = false;
            }

            // Software windowed full-screen mode
            if (!isFullScreenSupported) {
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                // Resize the canvas
                _glCanvas.setSize(screenSize);
                // Resize the frame so that it occupies the whole screen
                this.setSize(screenSize);
                // Set its location at the top left corner
                this.setLocation(0, 0);
            }
        }
        // Otherwise, center the window on the screen.
        else {
            isDisplayModeModified = false;
            pack();

            int x, y;
            x = (Toolkit.getDefaultToolkit().getScreenSize().width - _settings.getWidth()) / 2;
            y = (Toolkit.getDefaultToolkit().getScreenSize().height - _settings.getHeight()) / 2;
            this.setLocation(x, y);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                _isClosing = true;
                // If required, restore the previous display mode
                if (isDisplayModeModified) {
                    gd.setDisplayMode(previousDisplayMode);
                }
                // If required, get back to the windowed mode
                if (gd.getFullScreenWindow() == JoglCanvas.this) {
                    gd.setFullScreenWindow(null);
                }
            }
        });

        // Make the window visible to realize the OpenGL surface.
        setVisible(true);

        _canvasRenderer.init(_settings, true); // true - do swap in renderer.
        _inited = true;
    }

    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            privateInit();
        }

        checkFocus();

        _canvasRenderer.draw();
        latch.countDown();
    }

    private void checkFocus() {
    // // TODO: might be better to just not do anything if there is no physical layer set as focus listener - possibly,
    // // people might not
    // // want to do any input at all.
    // if (physicalLayer == null) {
    // throw new IllegalStateException("no physical layer set as focus listener");
    // }
    //
    // final boolean newFocus = Display.isActive() && Display.isVisible();
    //
    // if (!hasFocus && newFocus) {
    // // didn't use to have focus, but now we do
    // // do nothing for now, just keep track of the fact that we have focus
    // hasFocus = newFocus;
    // } else if (hasFocus && !newFocus) {
    // // had focus, but don't anymore - notify the physical input layer
    // physicalLayer.lostFocus();
    // hasFocus = newFocus;
    // }
    }

    public CanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    public void close() {
        try {
            if (GLContext.getCurrent() != null) {
                // Release the OpenGL resources.
                GLContext.getCurrent().release();
            }
        } catch (final GLException releaseFailure) {
            logger.log(Level.WARNING, "Failed to release OpenGL Context: " + _glCanvas, releaseFailure);
        }

        // Dispose of any window resources.
        dispose();
    }

    @Override
    public boolean isActive() {
        return hasFocus();
    }

    public boolean isClosing() {
        return _isClosing;
    }

    public void moveWindowTo(final int locX, final int locY) {
        setLocation(locX, locY);
    }

    public void setIcon(final Image[] iconImages) {
    // TODO Auto-generated method stub
    }

    public void setVSyncEnabled(final boolean enabled) {
        if (GLContext.getCurrent() != null) {
            // Release the OpenGL resources.
            GLContext.getCurrent().getGL().setSwapInterval(enabled ? 1 : 0);
        }
    }

    public void cleanup() {
        _canvasRenderer.cleanup();
    }

    // public void forward(final JoglCanvas canvas, final PhysicalLayer physicalLayer) {
    // this.physicalLayer = physicalLayer;
    // }
}
