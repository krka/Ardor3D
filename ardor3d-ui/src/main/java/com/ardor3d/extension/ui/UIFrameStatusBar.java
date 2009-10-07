/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.input.InputState;
import com.ardor3d.math.Vector3;

/**
 * This panel extension defines a frame status bar (used at the bottom of a frame) with a text label and resize handle.
 */
public class UIFrameStatusBar extends UIPanel {

    /** Our text label. */
    private final UILabel _statusLabel;

    /** Resize handle, used to drag out this content's size when the frame is set as resizeable. */
    private final FrameResizeButton _resizeButton;

    /** A drag listener used to perform resize operations on this frame. */
    private final ResizeListener _resizeListener = new ResizeListener();

    /**
     * Construct a new status bar
     */
    public UIFrameStatusBar() {
        super(new BorderLayout());

        _statusLabel = new UILabel("");
        _statusLabel.setLayoutData(BorderLayoutData.CENTER);
        add(_statusLabel);

        _resizeButton = new FrameResizeButton();
        _resizeButton.setLayoutData(BorderLayoutData.EAST);
        add(_resizeButton);
    }

    public FrameResizeButton getResizeButton() {
        return _resizeButton;
    }

    public UILabel getStatusLabel() {
        return _statusLabel;
    }

    @Override
    public void attachedToHud() {
        super.attachedToHud();
        final UIHud hud = getHud();
        if (hud != null) {
            hud.addDragListener(_resizeListener);
        }
    }

    @Override
    public void detachedFromHud() {
        super.detachedFromHud();
        final UIHud hud = getHud();
        if (hud != null) {
            hud.removeDragListener(_resizeListener);
        }
    }

    private final class ResizeListener implements DragListener {
        private int _oldX = 0;
        private int _oldY = 0;

        public void startDrag(final int mouseX, final int mouseY) {
            final Vector3 vec = Vector3.fetchTempInstance();
            vec.set(mouseX, mouseY, 0);
            getWorldTransform().applyInverse(vec);

            _oldX = Math.round(vec.getXf());
            _oldY = Math.round(vec.getYf());
            Vector3.releaseTempInstance(vec);
        }

        public void drag(final int mouseX, final int mouseY) {
            final Vector3 vec = Vector3.fetchTempInstance();
            vec.set(mouseX, mouseY, 0);
            getWorldTransform().applyInverse(vec);

            final int x = Math.round(vec.getXf());
            final int y = Math.round(vec.getYf());

            final UIFrame frame = UIFrame.findParentFrame(UIFrameStatusBar.this);

            // Set the new width to the current width + the change in mouse x position.
            int newWidth = frame.getComponentWidth() + x - _oldX;
            if (newWidth < UIFrame.MIN_FRAME_WIDTH) {
                // don't let us get smaller than min size
                newWidth = UIFrame.MIN_FRAME_WIDTH;
            }

            // Set the new height to the current width + the change in mouse y position.
            int heightDif = y - _oldY;
            int newHeight = frame.getComponentHeight() + _oldY - y;
            if (newHeight < UIFrame.MIN_FRAME_HEIGHT) {
                // don't let us get smaller than min size
                newHeight = UIFrame.MIN_FRAME_HEIGHT;
                heightDif = frame.getComponentHeight() - newHeight;
            }

            frame.setComponentSize(newWidth, newHeight);

            vec.set(0, heightDif, 0);
            getWorldTransform().applyForwardVector(vec);
            frame.addTranslation(vec);
            Vector3.releaseTempInstance(vec);

            frame.layout();

            _oldX = x;
            _oldY = y - heightDif;
        }

        public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {}

        public boolean isDragHandle(final UIComponent component, final int mouseX, final int mouseY) {
            return component == _resizeButton;
        }
    }

    class FrameResizeButton extends UIButton {

        public FrameResizeButton() {
            super("...");
            _pressedState = new MyPressedState();
            _defaultState = new MyDefaultState();
            _mouseOverState = new MyMouseOverState();
            switchState(_defaultState);
        }

        @Override
        protected void applySkin() {
            ; // keep this from happening by default
        }

        class MyPressedState extends UIButton.PressedState {
            @Override
            public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
                super.mouseDeparted(mouseX, mouseY, state);
                // TODO: Reset mouse cursor.
            }
        }

        class MyDefaultState extends UIButton.DefaultState {
            @Override
            public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
                super.mouseEntered(mouseX, mouseY, state);
                // TODO: Set mouse cursor to resize.
            }

            @Override
            public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
                super.mouseDeparted(mouseX, mouseY, state);
                // TODO: Reset mouse cursor.
            }
        }

        class MyMouseOverState extends UIButton.MouseOverState {
            @Override
            public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
                super.mouseDeparted(mouseX, mouseY, state);
                // TODO: Reset mouse cursor.
            }
        }
    }
}
