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
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;
import com.google.inject.Inject;

/**
 * Keyboard wrapper class for use with AWT.
 */
public class AwtKeyboardWrapper implements KeyboardWrapper, KeyListener {
    @GuardedBy("this")
    private final LinkedList<KeyEvent> upcomingEvents = new LinkedList<KeyEvent>();

    @GuardedBy("this")
    private AwtKeyboardIterator currentIterator = null;

    private final Component component;

    @Inject
    public AwtKeyboardWrapper(final Component component) {
        this.component = component;
    }

    public void init() {
        component.addKeyListener(this);
    }

    public synchronized PeekingIterator<KeyEvent> getEvents() {
        if (currentIterator == null || !currentIterator.hasNext()) {
            currentIterator = new AwtKeyboardIterator();
        }

        return currentIterator;
    }

    public synchronized void keyTyped(final java.awt.event.KeyEvent e) {
    // ignore this event
    }

    public synchronized void keyPressed(final java.awt.event.KeyEvent e) {
        upcomingEvents.add(new KeyEvent(AwtKey.findByCode(e.getKeyCode()), KeyState.DOWN));
    }

    public synchronized void keyReleased(final java.awt.event.KeyEvent e) {
        upcomingEvents.add(new KeyEvent(AwtKey.findByCode(e.getKeyCode()), KeyState.UP));
    }

    private class AwtKeyboardIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {
        @Override
        protected KeyEvent computeNext() {
            synchronized (AwtKeyboardWrapper.this) {
                if (upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return upcomingEvents.poll();
            }
        }
    }
}
