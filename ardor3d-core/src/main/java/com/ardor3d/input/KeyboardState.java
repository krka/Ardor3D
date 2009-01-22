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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.ardor3d.annotation.Immutable;

/**
 * A keyboard state at some point in time.
 */
@Immutable
public class KeyboardState {
    private final EnumSet<Key> keysDown;
    private final Set<Key> keysDownView;
    public static final KeyboardState NOTHING = new KeyboardState(EnumSet.noneOf(Key.class));

    public KeyboardState(final EnumSet<Key> keysDown) {
        // keeping the keysDown as an EnumSet rather than as an unmodifiableSet in order to get
        // the performance benefit of working with the fast implementations of contains(),
        // removeAll(), etc., in EnumSet. The intention is that the keysDown set should never change.
        this.keysDown = keysDown;
        keysDownView = Collections.unmodifiableSet(keysDown);
    }

    public boolean isDown(final Key key) {
        return keysDown.contains(key);
    }

    public Set<Key> getKeysDown() {
        return keysDownView;
    }

    public EnumSet<Key> getKeysReleasedSince(final KeyboardState previous) {
        final EnumSet<Key> result = EnumSet.copyOf(previous.keysDown);

        result.removeAll(keysDown);

        return result;
    }

    public EnumSet<Key> getKeysPressedSince(final KeyboardState previous) {
        final EnumSet<Key> result = EnumSet.copyOf(keysDown);

        result.removeAll(previous.keysDown);

        return result;

    }

    @Override
    public String toString() {
        return "KeyboardState{" +
                "keysDown=" + keysDown +
                '}';
    }
}
