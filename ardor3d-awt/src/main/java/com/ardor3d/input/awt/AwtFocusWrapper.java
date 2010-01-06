/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.awt;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import com.ardor3d.input.FocusWrapper;
import com.google.inject.Inject;

/**
 * Focus listener class for use with AWT.
 */
public class AwtFocusWrapper implements FocusWrapper, FocusListener {
    private volatile boolean _focusLost = false;

    private final Component _component;

    @Inject
    public AwtFocusWrapper(final Component component) {
        _component = component;
    }

    public void focusGained(final FocusEvent e) {
    // do nothing
    }

    public void focusLost(final FocusEvent e) {
        _focusLost = true;
    }

    public boolean getAndClearFocusLost() {
        final boolean result = _focusLost;

        _focusLost = false;

        return result;
    }

    public void init() {
        _component.addFocusListener(this);
    }
}
