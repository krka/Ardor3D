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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.event.DragAndDropListener;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.KeyboardState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.BasicTriggersApplier;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.google.common.base.Predicate;

/**
 * UIHud represents a "Heads Up Display" or the base of a game UI scenegraph. Various UI Input, dragging, events, etc.
 * are handled through this class.
 */
public class UIHud extends Node {
    private static final Logger _logger = Logger.getLogger(UIHud.class.getName());

    /**
     * The logical layer used by this UI to receive input events.
     */
    private final LogicalLayer _logicalLayer = new LogicalLayer();

    /**
     * The single tooltip used by this hud.
     */
    private final UITooltip _ttip = new UITooltip();

    /**
     * Internal flag indicating whether the last input event was consumed by the UI. This is used to decide if we will
     * forward the event to the next LogicalLayer.
     */
    private boolean _inputConsumed;

    /** Which button is used for drag operations. Defaults to LEFT. */
    private MouseButton _dragButton = MouseButton.LEFT;

    /** Tracks the previous component our mouse was over so we can properly handle mouse entered and departed events. */
    private UIComponent _lastMouseOverComponent;

    /**
     * Tracks the previous component our mouse button was pressed on so we can properly handle mouse pressed and
     * released events.
     */
    private UIComponent _mouseDownComponent;

    /** Tracks last mouse location so we can detect movement. */
    private int _lastMouseX, _lastMouseY;

    /**
     * List of potential drag and drop handlers. When a drag operation is detected, we will offer it to each item in the
     * list until one accepts it.
     */
    protected final List<WeakReference<DragAndDropListener>> _dndListeners = new ArrayList<WeakReference<DragAndDropListener>>();

    /** Our current drag listener. When a drop occurs, this is set back to null. */
    private DragAndDropListener _dragListener = null;

    /** The component that currently has key focus - key events will be sent to this component. */
    private UIComponent _focusedComponent = null;

    /**
     * Construct a new UIHud
     */
    public UIHud() {
        setName("UIHud");

        getSceneHints().setCullHint(CullHint.Never);
        getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);

        final ZBufferState zstate = new ZBufferState();
        zstate.setEnabled(false);
        zstate.setWritable(false);
        setRenderState(zstate);

        setupLogicalLayer();
    }

    /**
     * @return the last detected x position of the mouse.
     */
    public int getLastMouseX() {
        return _lastMouseX;
    }

    /**
     * @return the last detected y position of the mouse.
     */
    public int getLastMouseY() {
        return _lastMouseY;
    }

    /**
     * @return this hud's associated tooltip object.
     */
    public UITooltip getTooltip() {
        return _ttip;
    }

    /**
     * Add the given frame to this hud.
     * 
     * @param frame
     *            the frame to add
     */
    public void add(final UIFrame frame) {
        attachChild(frame);
        frame.attachedToHud();
    }

    /**
     * Remove the given frame from the hud
     * 
     * @param frame
     *            the frame to remove
     */
    public void remove(final UIFrame frame) {
        frame.detachedFromHud();
        detachChild(frame);
    }

    /**
     * Overridden to force frame detachments to go through {@link #remove(UIFrame)}
     */
    @Override
    public void detachAllChildren() {
        if (getNumberOfChildren() > 0) {
            for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
                final Spatial spat = getChild(i);
                if (spat instanceof UIFrame) {
                    remove((UIFrame) spat);
                } else {
                    detachChildAt(i);
                }
            }
        }
    }

    /**
     * Reorder the frames so that the given frame is drawn last and is therefore "on top" of any others.
     * 
     * @param frame
     *            the frame to bring to front
     */
    public void bringToFront(final UIFrame frame) {
        getChildren().remove(frame);
        getChildren().add(frame);
    }

    /**
     * Look for a UIComponent at the given screen coordinates. If no pickable component is at that location, null is
     * returned.
     * 
     * @param x
     *            the x screen coordinate
     * @param y
     *            the y screen coordinate
     * @return the picked component or null if nothing pickable was found at the given coordinates.
     */
    public UIComponent getUIComponent(final int x, final int y) {
        UIComponent ret = null;
        UIComponent found = null;

        for (int i = 0; i < getNumberOfChildren(); i++) {
            final Spatial s = getChild(i);
            if (s instanceof UIComponent) {
                final UIComponent comp = (UIComponent) s;

                ret = comp.getUIComponent(x, y);

                if (ret != null) {
                    found = ret;
                }
            }
        }

        return found;
    }

    /**
     * @return the component that currently has key focus or null if none.
     */
    public UIComponent getFocusedComponent() {
        return _focusedComponent;
    }

    /**
     * @param compomponent
     *            the component that should now have key focus. If this component has a focus target, that will be used
     *            instead.
     */
    public void setFocusedComponent(final UIComponent compomponent) {
        // If we already have a different focused component, tell it that it has lost focus.
        if (_focusedComponent != null && _focusedComponent != compomponent) {
            _focusedComponent.lostFocus();
        }

        // Set our focused component to the given component (or its focus target)
        if (compomponent != null) {
            if (compomponent.getKeyFocusTarget() != null) {
                // set null so we don't re-tell it that it lost focus.
                _focusedComponent = null;
                // recurse down to target.
                setFocusedComponent(compomponent.getKeyFocusTarget());
            } else {
                _focusedComponent = compomponent;
                // let our new focused component know it has focus
                _focusedComponent.gainedFocus();
            }
        } else {
            _focusedComponent = null;
        }
    }

    /**
     * @return the MouseButton that triggers drag operations
     */
    public MouseButton getDragButton() {
        return _dragButton;
    }

    /**
     * @param dragButton
     *            set the MouseButton that triggers drag operations
     */
    public void setDragButton(final MouseButton dragButton) {
        _dragButton = dragButton;
    }

    /**
     * Override to force setting ortho before drawing and to specifically handle draw order of frames and tool tip.
     */
    @Override
    public void draw(final Renderer r) {
        r.setOrtho();
        try {
            Spatial child;
            for (int i = 0, max = getNumberOfChildren(); i < max; i++) {
                child = getChild(i);
                if (child != null) {
                    child.onDraw(r);
                }
            }
            if (_ttip != null && _ttip.isVisible()) {
                _ttip.onDraw(r);
            }
        } catch (final Exception e) {
            UIHud._logger.logp(Level.SEVERE, getClass().getName(), "draw(Renderer)", "Exception", e);
        } finally {
            if (r.isInOrthoMode()) {
                r.unsetOrtho();
            }
            r.clearClips();
        }
    }

    /**
     * Add the given drag and drop listener to this hud.
     * 
     * @param listener
     *            the listener to add
     */
    public void addDnDListener(final DragAndDropListener listener) {
        _dndListeners.add(new WeakReference<DragAndDropListener>(listener));
    }

    /**
     * Remove the given drag and drop listener from this hud.
     * 
     * @param listener
     *            the listener to remove
     * @return true if it was found in the pool of listeners and removed.
     */
    public boolean removeDnDListener(final DragAndDropListener listener) {
        return _dndListeners.remove(new WeakReference<DragAndDropListener>(listener));
    }

    /**
     * @return the logical layer associated with this hud. When chaining UI logic to game logic, this LogicalLayer is
     *         the one to call checkTriggers on.
     */
    public LogicalLayer getLogicalLayer() {
        return _logicalLayer;
    }

    /**
     * Convenience method for setting up the UI's connection to the Ardor3D input system, along with a forwarding
     * address for input events that the UI does not care about.
     * 
     * @param canvas
     *            the canvas to register with
     * @param physicalLayer
     *            the physical layer to register with
     * @param forwardTo
     *            a LogicalLayer to send unconsumed (by the UI) input events to.
     */
    public void setupInput(final NativeCanvas canvas, final PhysicalLayer physicalLayer, final LogicalLayer forwardTo) {
        // Set up this logical layer to listen for events from the given canvas and PhysicalLayer
        _logicalLayer.registerInput(canvas, physicalLayer);

        // Set up forwarding for events not consumed.
        if (forwardTo != null) {
            _logicalLayer.setApplier(new BasicTriggersApplier() {

                @Override
                public void checkAndPerformTriggers(final Set<InputTrigger> triggers, final Canvas source,
                        final TwoInputStates states, final double tpf) {
                    super.checkAndPerformTriggers(triggers, source, states, tpf);

                    if (!_inputConsumed) {
                        forwardTo.getApplier().checkAndPerformTriggers(forwardTo.getTriggers(), source, states, tpf);
                    }
                    _inputConsumed = false;
                }
            });
        }
    }

    /**
     * Set up our logical layer with a trigger that hands input to the UI and saves whether it was "consumed".
     */
    private void setupLogicalLayer() {
        _logicalLayer.registerTrigger(new InputTrigger(new Predicate<TwoInputStates>() {
            public boolean apply(final TwoInputStates arg0) {
                // always trigger this.
                return true;
            }
        }, new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                _inputConsumed = offerInputToUI(inputStates);
            }
        }));
    }

    /**
     * Parse a given set of input states for UI events and pass these events to the UI frames contained in this hud.
     * 
     * @param inputStates
     *            our two InputState objects, detailing a before and after snapshot of the input system.
     * @return true if a UI element consumed the event described by inputStates.
     */
    private boolean offerInputToUI(final TwoInputStates inputStates) {
        boolean consumed = false;
        final InputState current = inputStates.getCurrent();

        // Mouse checks.
        {
            final MouseState previousMState = inputStates.getPrevious().getMouseState();
            final MouseState currentMState = current.getMouseState();
            if (previousMState != currentMState) {

                // Check for presses.
                if (currentMState.hasButtonState(ButtonState.DOWN)) {
                    final EnumSet<MouseButton> pressed = currentMState.getButtonsPressedSince(previousMState);
                    if (!pressed.isEmpty()) {
                        for (final MouseButton button : pressed) {
                            consumed |= fireMouseButtonPressed(button, current);
                        }
                    }
                }

                // Check for releases.
                if (previousMState.hasButtonState(ButtonState.DOWN)) {
                    final EnumSet<MouseButton> released = currentMState.getButtonsReleasedSince(previousMState);
                    if (!released.isEmpty()) {
                        for (final MouseButton button : released) {
                            consumed |= fireMouseButtonReleased(button, current);
                        }
                    }
                }

                // Check for mouse movement
                if (currentMState.getDx() != 0 || currentMState.getDy() != 0) {
                    consumed |= fireMouseMoved(currentMState.getX(), currentMState.getY(), current);
                }

                // Check for wheel change
                if (currentMState.getDwheel() != 0) {
                    consumed |= fireMouseWheelMoved(currentMState.getDwheel(), current);
                }
            }
        }

        // Keyboard checks
        {
            final KeyboardState previousKState = inputStates.getPrevious().getKeyboardState();
            final KeyboardState currentKState = current.getKeyboardState();
            if (!currentKState.getKeysDown().isEmpty()) {
                // new presses
                final EnumSet<Key> pressed = currentKState.getKeysPressedSince(previousKState);
                if (!pressed.isEmpty()) {
                    for (final Key key : pressed) {
                        consumed |= fireKeyPressedEvent(key, current);
                    }
                }

                // repeats
                final EnumSet<Key> repeats = currentKState.getKeysHeldSince(previousKState);
                if (!repeats.isEmpty() && _focusedComponent != null) {
                    // TODO: handle repeats better once we have text fields.
                    consumed = true;
                }
            }

            // key releases
            if (!previousKState.getKeysDown().isEmpty()) {
                final EnumSet<Key> released = currentKState.getKeysReleasedSince(previousKState);
                if (!released.isEmpty()) {
                    for (final Key key : released) {
                        consumed |= fireKeyReleasedEvent(key, current);
                    }
                }
            }
        }

        return consumed;
    }

    /**
     * Handle mouse presses.
     * 
     * @param button
     *            the button that was pressed.
     * @param currentIS
     *            the current input state.
     * @return true if this event is consumed.
     */
    public boolean fireMouseButtonPressed(final MouseButton button, final InputState currentIS) {
        boolean consumed = false;
        final int mouseX = currentIS.getMouseState().getX(), mouseY = currentIS.getMouseState().getY();
        final UIComponent over = getUIComponent(mouseX, mouseY);
        _mouseDownComponent = over;

        setFocusedComponent(over);

        if (over == null) {
            return false;
        } else {
            consumed |= over.mousePressed(button, currentIS);
        }

        // Check if the component we are pressing on is "draggable"
        for (final WeakReference<DragAndDropListener> ref : _dndListeners) {
            final DragAndDropListener listener = ref.get();
            if (listener == null) {
                continue;
            }

            if (listener.isDragHandle(over, mouseX, mouseY)) {
                listener.startDrag(mouseX, mouseY);
                _dragListener = listener;
                consumed = true;
                break;
            }
        }

        // bring any clicked frames to front
        final UIFrame frame = over.getParentFrame();
        if (frame != null && frame.isAttachedToHUD()) {
            bringToFront(frame);
        }
        return consumed;
    }

    /**
     * Handle mouse releases.
     * 
     * @param button
     *            the button that was release.
     * @param currentIS
     *            the current input state.
     * @return true if this event is consumed.
     */
    public boolean fireMouseButtonReleased(final MouseButton button, final InputState currentIS) {
        boolean consumed = false;
        final int mouseX = currentIS.getMouseState().getX(), mouseY = currentIS.getMouseState().getY();
        final UIComponent over = getUIComponent(mouseX, mouseY);

        // if we're over a pickable component, send it the mouse release
        if (over != null && _mouseDownComponent == over) {
            consumed |= over.mouseReleased(button, currentIS);
        }

        if (button == _dragButton && _dragListener != null) {
            _dragListener.drop(over, mouseX, mouseY);
            _dragListener = null;
            consumed = true;
        }

        return consumed;
    }

    /**
     * Handle movement events.
     * 
     * @param mouseX
     *            the new x position of the mouse
     * @param mouseY
     *            the new y position of the mouse
     * @param currentIS
     *            the current input state.
     * @return if this event is consumed.
     */
    public boolean fireMouseMoved(final int mouseX, final int mouseY, final InputState currentIS) {
        _lastMouseX = mouseX;
        _lastMouseY = mouseY;

        // Check for drag movements.
        if (currentIS.getMouseState().getButtonState(_dragButton) == ButtonState.DOWN) {
            if (_dragListener != null) {
                _dragListener.drag(mouseX, mouseY);
                return true;
            }
        }

        // grab the component the mouse is over, if any
        final UIComponent over = getUIComponent(mouseX, mouseY);

        // Are we over a component? let it know we moved inside it.
        if (over != null) {
            over.mouseMoved(mouseX, mouseY, currentIS);
        }

        // component points to a different UIComponent than before, so mark departed
        if (over == null || over != _lastMouseOverComponent) {
            if (_lastMouseOverComponent != null) {
                _lastMouseOverComponent.mouseDeparted(mouseX, mouseY, currentIS);
            }
            if (over != null) {
                over.mouseEntered(mouseX, mouseY, currentIS);
            }
        }
        _lastMouseOverComponent = over;

        return false;
    }

    /**
     * Handle wheel events.
     * 
     * @param wheelDx
     *            the change in wheel position.
     * @param currentIS
     *            the current input state.
     * @return if this event is consumed.
     */
    public boolean fireMouseWheelMoved(final int wheelDx, final InputState currentIS) {
        final UIComponent over = getUIComponent(currentIS.getMouseState().getX(), currentIS.getMouseState().getY());

        if (over == null) {
            return false;
        }

        return over.mouseWheel(wheelDx, currentIS);
    }

    /**
     * Handle key presses.
     * 
     * @param key
     *            the pressed key
     * @param currentIS
     *            the current input state.
     * @return if this event is consumed.
     */
    public boolean fireKeyPressedEvent(final Key key, final InputState currentIS) {
        if (_focusedComponent != null) {
            return _focusedComponent.keyPressed(key, currentIS);
        } else {
            return false;
        }
    }

    /**
     * Handle key releases.
     * 
     * @param key
     *            the released key
     * @param currentIS
     *            the current input state.
     * @return if this event is consumed.
     */
    public boolean fireKeyReleasedEvent(final Key key, final InputState currentIS) {
        if (_focusedComponent != null) {
            return _focusedComponent.keyReleased(key, currentIS);
        } else {
            return false;
        }
    }
}
