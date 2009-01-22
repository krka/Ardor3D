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

import static com.google.common.base.Preconditions.checkNotNull;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;

/**
 * Focus Listener wrapper class for use with SWT.
 */
public class SwtFocusWrapper implements FocusWrapper, FocusListener {
    private volatile boolean focusLost = false;

    private final Control _control;

    public SwtFocusWrapper(final Control control) {
        _control = checkNotNull(control, "control");
    }

    public void focusGained(final FocusEvent focusEvent) {
    // nothing to do
    }

    public void focusLost(final FocusEvent focusEvent) {
        focusLost = true;
    }

    public boolean getAndClearFocusLost() {
        final boolean result = focusLost;

        focusLost = false;

        return result;
    }

    public void init() {
        _control.addFocusListener(this);
    }
}
