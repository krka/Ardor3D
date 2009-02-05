/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.swt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;

import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Control;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;
import com.ardor3d.input.KeyboardWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

/**
 * Keyboard wrapper for SWT input.
 */
@ThreadSafe
public class SwtKeyboardWrapper implements KeyboardWrapper, KeyListener {
    @GuardedBy("this")
    private final LinkedList<KeyEvent> _upcomingEvents;

    private final Control _control;

    @GuardedBy("this")
    private SwtKeyboardIterator _currentIterator = null;
    @GuardedBy("this")
    private int _lastKeyPressedCode = -1;

    public SwtKeyboardWrapper(final Control control) {
        _upcomingEvents = new LinkedList<KeyEvent>();
        _control = checkNotNull(control, "control");
    }

    public void init() {
        _control.addKeyListener(this);
    }

    public synchronized PeekingIterator<KeyEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new SwtKeyboardIterator();
        }

        return _currentIterator;
    }

    public synchronized void keyPressed(final org.eclipse.swt.events.KeyEvent event) {
        // System.out.println("keyPressed(" + SwtKey.findByCode(event.keyCode) + ")");
        if (event.keyCode == _lastKeyPressedCode) {
            // ignore if this is a repeat event
            return;
        }

        if (_lastKeyPressedCode != -1) {
            // if this is a different key to the last key that was pressed, then
            // add an 'up' even for the previous one - SWT doesn't send an 'up' event for the
            // first key in the below scenario:
            // 1. key 1 down
            // 2. key 2 down
            // 3. key 1 up
            _upcomingEvents.add(new KeyEvent(SwtKey.findByCode(_lastKeyPressedCode), KeyState.UP));
        }

        _lastKeyPressedCode = event.keyCode;
        _upcomingEvents.add(new KeyEvent(SwtKey.findByCode(event.keyCode), KeyState.DOWN));
    }

    public synchronized void keyReleased(final org.eclipse.swt.events.KeyEvent event) {
        // System.out.println("keyReleased(" + SwtKey.findByCode(event.keyCode) + ")");
        _upcomingEvents.add(new KeyEvent(SwtKey.findByCode(event.keyCode), KeyState.UP));
        _lastKeyPressedCode = -1;
    }

    private class SwtKeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {

        @Override
        protected KeyEvent computeNext() {
            synchronized (SwtKeyboardWrapper.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }
        }
    }
}
