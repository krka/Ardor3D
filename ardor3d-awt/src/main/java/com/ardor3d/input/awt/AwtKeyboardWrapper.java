/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.awt;

import java.awt.Component;
import java.awt.event.KeyListener;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;
import com.ardor3d.input.KeyboardWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;
import com.google.inject.Inject;

/**
 * Keyboard wrapper class for use with AWT.
 */
public class AwtKeyboardWrapper implements KeyboardWrapper, KeyListener {
    @GuardedBy("this")
    private final LinkedList<KeyEvent> _upcomingEvents = new LinkedList<KeyEvent>();

    @GuardedBy("this")
    private AwtKeyboardIterator _currentIterator = null;

    private final Component _component;

    @Inject
    public AwtKeyboardWrapper(final Component component) {
        _component = Preconditions.checkNotNull(component, "component");
    }

    public void init() {
        _component.addKeyListener(this);
    }

    public synchronized PeekingIterator<KeyEvent> getEvents() {
        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new AwtKeyboardIterator();
        }

        return _currentIterator;
    }

    public synchronized void keyTyped(final java.awt.event.KeyEvent e) {
    // ignore this event
    }

    public synchronized void keyPressed(final java.awt.event.KeyEvent e) {
        _upcomingEvents.add(new KeyEvent(AwtKey.findByCode(e.getKeyCode()), KeyState.DOWN, e.getKeyChar()));
    }

    public synchronized void keyReleased(final java.awt.event.KeyEvent e) {
        _upcomingEvents.add(new KeyEvent(AwtKey.findByCode(e.getKeyCode()), KeyState.UP, e.getKeyChar()));
    }

    private class AwtKeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {
        @Override
        protected KeyEvent computeNext() {
            synchronized (AwtKeyboardWrapper.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }
        }
    }
}
