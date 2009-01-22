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
 * Describes the state of a key - either it has been pressed or it has been released.
 */
@Immutable
public class KeyEvent {
    private final Key key;
    private final KeyState state;

    public KeyEvent(final Key key, final KeyState state) {
        this.key = key;
        this.state = state;
    }

    public Key getKey() {
        return key;
    }

    public KeyState getState() {
        return state;
    }
}
