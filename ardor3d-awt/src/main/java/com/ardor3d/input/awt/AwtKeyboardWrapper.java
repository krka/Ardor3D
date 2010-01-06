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
import java.awt.event.KeyListener;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.input.Key;
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

    private final EnumSet<Key> _pressedList = EnumSet.noneOf(Key.class);

    @Inject
    public AwtKeyboardWrapper(final Component component) {
        _component = Preconditions.checkNotNull(component, "component");
    }

    public void init() {
        _component.addKeyListener(this);
        _component.addFocusListener(new FocusListener() {
            public void focusLost(final FocusEvent e) {}

            public void focusGained(final FocusEvent e) {
                _pressedList.clear();
            }
        });
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
        final Key pressed = AwtKey.findByCode(e.getKeyCode());
        if (!_pressedList.contains(pressed)) {
            _upcomingEvents.add(new KeyEvent(pressed, KeyState.DOWN, e.getKeyChar()));
            _pressedList.add(pressed);
        }
    }

    public synchronized void keyReleased(final java.awt.event.KeyEvent e) {
        final Key released = AwtKey.findByCode(e.getKeyCode());
        _upcomingEvents.add(new KeyEvent(released, KeyState.UP, e.getKeyChar()));
        _pressedList.remove(released);
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
