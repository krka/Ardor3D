/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import com.ardor3d.annotation.Immutable;

/**
 * The total input state of the devices that are being handled.
 */
@Immutable
public class InputState {
    public static final InputState LOST_FOCUS = new InputState(KeyboardState.NOTHING, MouseState.NOTHING);
    public static final InputState EMPTY = new InputState(KeyboardState.NOTHING, MouseState.NOTHING);

    private final KeyboardState keyboardState;
    private final MouseState mouseState;

    /**
     * Creates a new instance.
     * 
     * @param keyboardState
     *            a non-null KeyboardState instance
     * @param mouseState
     *            a non-null MouseState instance
     * @throws NullPointerException
     *             if either parameter is null
     */
    public InputState(final KeyboardState keyboardState, final MouseState mouseState) {
        if (keyboardState == null) {
            throw new NullPointerException("Keyboard state");
        }

        if (mouseState == null) {
            throw new NullPointerException("Mouse state");
        }

        this.keyboardState = keyboardState;
        this.mouseState = mouseState;
    }

    public KeyboardState getKeyboardState() {
        return keyboardState;
    }

    public MouseState getMouseState() {
        return mouseState;
    }

    @Override
    public String toString() {
        return "InputState{" +
                "keyboardState=" + keyboardState +
                ", mouseState=" + mouseState +
                '}';
    }
}
