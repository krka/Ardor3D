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
        private int _initialX;
        private int _initialY;
        private int _initialLocalComponentWidth;
        private int _initialLocalComponentHeight;

        public void startDrag(final int mouseX, final int mouseY) {
            final Vector3 vec = Vector3.fetchTempInstance();
            vec.set(mouseX, mouseY, 0);
            getWorldTransform().applyInverse(vec);

            _initialX = Math.round(vec.getXf());
            _initialY = Math.round(vec.getYf());
            Vector3.releaseTempInstance(vec);

            final UIFrame frame = UIFrame.findParentFrame(UIFrameStatusBar.this);
            _initialLocalComponentWidth = frame.getLocalComponentWidth();
            _initialLocalComponentHeight = frame.getLocalComponentHeight();
        }

        public void drag(final int mouseX, final int mouseY) {
            resizeFrameByPosition(mouseX, mouseY);
        }

        public void endDrag(final UIComponent component, final int mouseX, final int mouseY) {
            resizeFrameByPosition(mouseX, mouseY);
        }

        private void resizeFrameByPosition(final int mouseX, final int mouseY) {
            final Vector3 vec = Vector3.fetchTempInstance();
            vec.set(mouseX, mouseY, 0);
            getWorldTransform().applyInverse(vec);

            final int x = Math.round(vec.getXf());
            final int y = Math.round(vec.getYf());

            final UIFrame frame = UIFrame.findParentFrame(UIFrameStatusBar.this);

            // Set the new width to the initial width + the change in mouse x position.
            int newWidth = _initialLocalComponentWidth + (x - _initialX);
            if (newWidth < UIFrame.MIN_FRAME_WIDTH) {
                // don't let us get smaller than min size
                newWidth = UIFrame.MIN_FRAME_WIDTH;
            }
            if (newWidth < frame.getMinimumLocalComponentWidth()) {
                // don't let us get smaller than frame min size
                newWidth = frame.getMinimumLocalComponentWidth();
            }
            if (newWidth > frame.getMaximumLocalComponentWidth()) {
                // don't let us get bigger than frame max size
                newWidth = frame.getMaximumLocalComponentWidth();
            }

            // Set the new height to the initial height + the change in mouse y position.
            int newHeight = _initialLocalComponentHeight - (y - _initialY);
            if (newHeight < UIFrame.MIN_FRAME_HEIGHT) {
                // don't let us get smaller than absolute min size
                newHeight = UIFrame.MIN_FRAME_HEIGHT;
            }
            if (newHeight < frame.getMinimumLocalComponentHeight()) {
                // don't let us get smaller than frame min size
                newHeight = frame.getMinimumLocalComponentHeight();
            }
            if (newHeight > frame.getMaximumLocalComponentHeight()) {
                // don't let us get bigger than frame max size
                newHeight = frame.getMaximumLocalComponentHeight();
            }

            int heightDiff = newHeight - frame.getLocalComponentHeight();
            _initialY += heightDiff;
            
            frame.setLocalComponentSize(newWidth, newHeight);

            vec.set(0, -heightDiff, 0);
            getWorldTransform().applyForwardVector(vec);
            frame.addTranslation(vec);
            Vector3.releaseTempInstance(vec);

            frame.layout();
        }

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
