/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import java.util.EnumSet;
import java.util.concurrent.Callable;

import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.renderer.Camera;
import com.ardor3d.util.GameTaskQueueManager;

/**
 * A component similar to an inner frame in Swing. It can be dragged around the screen, minimized, expanded, closed, and
 * resized. Frames can also have their opacity individually assigned which will affect all elements drawn within them.
 */
public class UIFrame extends UIContainer {
    /** Minimum height we'll allow during manual resize */
    public static int MIN_FRAME_HEIGHT = 60;
    /** Minimum width we'll allow during manual resize */
    public static int MIN_FRAME_WIDTH = 100;

    /** The main panel containing the contents panel and status bar of the frame. */
    private final UIPanel _basePanel;
    /** The panel meant to hold the contents of the frame. */
    private UIPanel _contentPanel;
    /** The top title bar of the frame, part of the frame's "chrome" */
    private final UIFrameBar _titleBar;
    /** The bar running along the bottom of the frame. */
    private final UIFrameStatusBar _statusBar;

    /** If true, show our title and status bars. */
    private boolean _decorated = true;

    /** The drag listener responsible for allowing repositioning of the frame by dragging the title label. */
    private final FrameDragListener _dragListener = new FrameDragListener();

    /** If true, show a resize handle on this frame and allow its use. */
    private boolean _resizeable = true;

    /**
     * Construct a new UIFrame with the given title and default buttons (CLOSE).
     * 
     * @param title
     *            the text to display on the title bar of this frame
     */
    public UIFrame(final String title) {
        this(title, EnumSet.of(FrameButtons.CLOSE));
    }

    /**
     * Construct a new UIFrame with the given title and button.
     * 
     * @param title
     *            the text to display on the title bar of this frame
     * @param buttons
     *            which buttons we should show in the frame bar.
     */
    public UIFrame(final String title, final EnumSet<FrameButtons> buttons) {
        setLayout(new BorderLayout());

        _basePanel = new UIPanel(new BorderLayout());
        _basePanel.setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));
        _basePanel.setLayoutData(BorderLayoutData.CENTER);
        add(_basePanel);

        _contentPanel = new UIPanel();
        _contentPanel.setLayoutData(BorderLayoutData.CENTER);
        _basePanel.add(_contentPanel);

        _titleBar = new UIFrameBar(buttons);
        _titleBar.setLayoutData(BorderLayoutData.NORTH);
        setTitle(title);
        add(_titleBar);

        _statusBar = new UIFrameStatusBar();
        _statusBar.setLayoutData(BorderLayoutData.SOUTH);
        _basePanel.add(_statusBar);

        applySkin();
    }

    /**
     * @param decorated
     *            true to show the title and status bars. False to remove both. Undecorated frames have no resize or
     *            drag handles, or close buttons, etc.
     */
    public void setDecorated(final boolean decorated) {
        _decorated = decorated;
        if (!_decorated) {
            remove(_titleBar);
        } else {
            add(_titleBar);
        }

        if (!_decorated) {
            _basePanel.remove(_statusBar);
        } else {
            _basePanel.add(_statusBar);
        }
    }

    /**
     * @return true if this frame is decorated.
     */
    public boolean isDecorated() {
        return _decorated;
    }

    /**
     * @param resizeable
     *            true if we should allow resizing of this frame via a resize handle in the status bar. This does not
     *            stop programmatic resizing of this frame.
     */
    public void setResizeable(final boolean resizeable) {
        if (_resizeable != resizeable) {
            _resizeable = resizeable;

            if (!_resizeable) {
                _statusBar.remove(_statusBar.getResizeButton());
            } else {
                _statusBar.add(_statusBar.getResizeButton());
            }
            _statusBar.updateMinimumSizeFromContents();
            _statusBar.layout();
        }
    }

    /**
     * @return true if this frame allows manual resizing.
     */
    public boolean isResizeable() {
        return _resizeable;
    }

    /**
     * Remove this frame from the hud it is attached to.
     * 
     * @throws IllegalStateException
     *             if frame is not currently attached to a hud.
     */
    public void close() {
        final UIHud hud = getHud();
        if (hud == null) {
            throw new IllegalStateException("UIFrame is not attached to a hud.");
        }

        // Remove our drag listener
        hud.removeDragListener(_dragListener);

        // When a frame closes, close any open tooltip
        hud.getTooltip().setVisible(false);

        // clear any resources for standin
        clearStandin();

        hud.remove(this);
        _parent = null;
    }

    /**
     * Centers this frame on the location of the given component.
     * 
     * @param comp
     *            the component to center on.
     */
    public void setLocationRelativeTo(final UIComponent comp) {
        final Rectangle2 rectA = comp.getRelativeComponentBounds(null);
        final Rectangle2 rectB = getRelativeComponentBounds(null);
        int x = (rectA.getWidth() - rectB.getWidth()) / 2;
        int y = (rectA.getHeight() - rectB.getHeight()) / 2;
        x += comp.getHudX() - rectA.getX() + rectB.getX();
        y += comp.getHudY() - rectA.getY() + rectB.getY();
        setHudXY(x, y);
        updateGeometricState(0);
    }

    /**
     * Centers this frame on the view of the camera
     * 
     * @param cam
     *            the camera to center on.
     */
    public void setLocationRelativeTo(final Camera cam) {
        final Rectangle2 rectA = getRelativeComponentBounds(null);
        int x = (cam.getWidth() - rectA.getWidth()) / 2;
        int y = (cam.getHeight() - rectA.getHeight()) / 2;
        x -= rectA.getX();
        y -= rectA.getY();
        setHudXY(x, y);
        updateGeometricState(0);
    }

    /**
     * @return this frame's title bar
     */
    public UIFrameBar getTitleBar() {
        return _titleBar;
    }

    /**
     * @return this frame's status bar
     */
    public UIFrameStatusBar getStatusBar() {
        return _statusBar;
    }

    /**
     * @return the center content panel of this frame.
     */
    public UIPanel getContentPanel() {
        return _contentPanel;
    }

    /**
     * @return the base panel of this frame which holds the content panel and status bar.
     */
    public UIPanel getBasePanel() {
        return _basePanel;
    }

    /**
     * Replaces the content panel of this frame with a new one.
     * 
     * @param panel
     *            the new content panel.
     */
    public void setContentPanel(final UIPanel panel) {
        _basePanel.remove(_contentPanel);
        _contentPanel = panel;
        panel.setLayoutData(BorderLayoutData.CENTER);
        _basePanel.add(panel);
    }

    /**
     * @return the current title of this frame
     */
    public String getTitle() {
        if (_titleBar != null) {
            return _titleBar.getTitleLabel().getText();
        }

        return null;
    }

    /**
     * Sets the title of this frame
     * 
     * @param title
     *            the new title
     */
    public void setTitle(final String title) {
        if (_titleBar != null) {
            _titleBar.getTitleLabel().setText(title);
            _titleBar.layout();
        }
    }

    @Override
    public void attachedToHud() {
        super.attachedToHud();
        // add our drag listener to the hud
        getHud().addDragListener(_dragListener);
    }

    @Override
    public void detachedFromHud() {
        super.detachedFromHud();

        // Remove our drag listener from the hud
        if (getHud() != null) {
            getHud().removeDragListener(_dragListener);
        }
    }

    /**
     * Resize the frame to fit the minimum size of its content panel.
     */
    public void pack() {
        _contentPanel.updateMinimumSizeFromContents();
        pack(_contentPanel.getMinimumLocalComponentWidth(), _contentPanel.getMinimumLocalComponentHeight());
    }

    /**
     * Resize the frame to fit its content panel to the given dimensions
     * 
     * @param contentWidth
     *            our desired content panel width
     * @param contentHeight
     *            our desired content panel height
     */
    public void pack(final int contentWidth, final int contentHeight) {
        // grab the desired width and height of the frame.
        final int width = contentWidth + _basePanel.getTotalLeft() + _basePanel.getTotalRight();
        int height = contentHeight + _basePanel.getTotalTop() + _basePanel.getTotalBottom();

        // add in our frame chrome, if it is enabled.
        if (isDecorated()) {
            height += _statusBar.getLocalComponentHeight() + _titleBar.getLocalComponentHeight();
        }

        // Set our size, obeying min sizes.
        setLocalComponentSize(Math.max(width, UIFrame.MIN_FRAME_WIDTH), Math.max(height, UIFrame.MIN_FRAME_HEIGHT));

        // Layout the panel
        layout();
    }

    /**
     * Causes our shared texture renderer - used to draw cached versions of all frames - to be recreated on the next
     * render loop.
     */
    public static void resetTextureRenderer(final Object queueKey) {
        final Callable<Void> exe = new Callable<Void>() {
            public Void call() {
                if (UIContainer._textureRenderer != null) {
                    UIContainer._textureRenderer.cleanup();
                }
                UIContainer._textureRenderer = null;
                return null;
            }
        };
        GameTaskQueueManager.getManager(queueKey).render(exe);
    }

    /**
     * Recursive convenience method for locating the first UIFrame above a given component.
     * 
     * @param component
     *            the component to look above.
     * @return the first UIFrame found above the given component, or null if none.
     */
    public static UIFrame findParentFrame(final UIComponent component) {
        if (component.getParent() instanceof UIFrame) {
            return (UIFrame) component.getParent();
        } else if (component.getParent() instanceof UIComponent) {
            return UIFrame.findParentFrame((UIComponent) component.getParent());
        } else {
            return null;
        }
    }

    /**
     * The drag listener responsible for allowing a frame to be moved around the screen with the mouse.
     */
    private final class FrameDragListener implements DragListener {
        int oldX = 0;
        int oldY = 0;

        public void startDrag(final int mouseX, final int mouseY) {
            oldX = mouseX;
            oldY = mouseY;
        }

        public void drag(final int mouseX, final int mouseY) {
            // check if we are off the edge... if so, flag for redraw (part of the frame may have been hidden)
            if (!smallerThanWindow()) {
                fireComponentDirty();
            }

            addTranslation(mouseX - oldX, mouseY - oldY, 0);
            oldX = mouseX;
            oldY = mouseY;

            // check if we are off the edge now... if so, flag for redraw (part of the frame may have been hidden)
            if (!smallerThanWindow()) {
                fireComponentDirty();
            }
        }

        /**
         * @return true if this frame can be fully contained by the hud.
         */
        public boolean smallerThanWindow() {
            final int dispWidth = getHud().getWidth();
            final int dispHeight = getHud().getHeight();
            final Rectangle2 rect = getRelativeComponentBounds(null);
            return rect.getWidth() <= dispWidth && rect.getHeight() <= dispHeight;
        }

        public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {}

        public boolean isDragHandle(final UIComponent component, final int mouseX, final int mouseY) {
            return component == _titleBar.getTitleLabel();
        }
    }

    /**
     * Enumeration of possible frame chrome buttons.
     */
    public enum FrameButtons {
        CLOSE, MINIMIZE, MAXIMIZE, HELP;
    }
}
