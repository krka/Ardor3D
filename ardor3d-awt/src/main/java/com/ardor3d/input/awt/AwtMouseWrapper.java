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

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.MouseWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.PeekingIterator;
import com.google.inject.Inject;

/**
 * Mouse wrapper class for use with AWT.
 */
public class AwtMouseWrapper implements MouseWrapper, MouseListener, MouseWheelListener, MouseMotionListener {
    @GuardedBy("this")
    private final LinkedList<MouseState> _upcomingEvents = new LinkedList<MouseState>();

    @GuardedBy("this")
    private AwtMouseIterator _currentIterator = null;

    @GuardedBy("this")
    private MouseState _lastState = null;

    private final Component _component;

    private final Multiset<MouseButton> _clicks = Multisets.newEnumMultiset(MouseButton.class);
    private final EnumMap<MouseButton, Long> _lastClickTime = Maps.newEnumMap(MouseButton.class);
    private final EnumSet<MouseButton> _clickArmed = EnumSet.noneOf(MouseButton.class);

    @Inject
    public AwtMouseWrapper(final Component component) {
        _component = checkNotNull(component, "component");
        for (final MouseButton mb : MouseButton.values()) {
            _lastClickTime.put(mb, 0L);
        }
    }

    public void init() {
        _component.addMouseListener(this);
        _component.addMouseMotionListener(this);
    }

    public synchronized PeekingIterator<MouseState> getEvents() {
        expireClickEvents();

        if (_currentIterator == null || !_currentIterator.hasNext()) {
            _currentIterator = new AwtMouseIterator();
        }

        return _currentIterator;
    }

    private void expireClickEvents() {
        if (!_clicks.isEmpty()) {
            for (final MouseButton mb : MouseButton.values()) {
                if (System.currentTimeMillis() - _lastClickTime.get(mb) > MouseState.CLICK_TIME_MS) {
                    _clicks.removeAllOccurrences(mb);
                }
            }
        }
    }

    public synchronized void mouseClicked(final MouseEvent e) {
    // Yes, we could use the click count here, but in the interests of this working the same way as SWT and Native, we
    // will do it the same way they do it.
    }

    public synchronized void mousePressed(final MouseEvent e) {
        final MouseButton b = getButtonForEvent(e);
        if (_clickArmed.contains(b)) {
            _clicks.removeAllOccurrences(b);
        }
        _clickArmed.add(b);
        _lastClickTime.put(b, System.currentTimeMillis());

        initState(e);

        final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

        setStateForButton(e, buttons, ButtonState.DOWN);

        addNewState(e, buttons, null);
    }

    public synchronized void mouseReleased(final MouseEvent e) {
        initState(e);

        final EnumMap<MouseButton, ButtonState> buttons = _lastState.getButtonStates();

        setStateForButton(e, buttons, ButtonState.UP);

        final MouseButton b = getButtonForEvent(e);
        if (_clickArmed.contains(b) && (System.currentTimeMillis() - _lastClickTime.get(b) <= MouseState.CLICK_TIME_MS)) {
            _clicks.add(b); // increment count of clicks for button b.
            // XXX: Note the double event add... this prevents sticky click counts, but is it the best way?
            addNewState(e, buttons, Multisets.newEnumMultiset(_clicks));
        } else {
            _clicks.removeAllOccurrences(b); // clear click count for button b.
        }
        _clickArmed.remove(b);

        addNewState(e, buttons, null);
    }

    public synchronized void mouseEntered(final MouseEvent e) {
    // ignore this
    }

    public synchronized void mouseExited(final MouseEvent e) {
    // ignore this
    }

    public synchronized void mouseDragged(final MouseEvent e) {
        mouseMoved(e);
    }

    public synchronized void mouseMoved(final MouseEvent e) {
        _clickArmed.clear();
        _clicks.clear();

        initState(e);

        addNewState(e, _lastState.getButtonStates(), null);
    }

    public void mouseWheelMoved(final MouseWheelEvent e) {
        initState(e);

        addNewState(e, _lastState.getButtonStates(), null);
    }

    private void initState(final MouseEvent mouseEvent) {
        if (_lastState == null) {
            _lastState = new MouseState(mouseEvent.getX(), _component.getHeight() - mouseEvent.getY(), 0, 0, 0, null,
                    null);
        }
    }

    private void addNewState(final MouseEvent mouseEvent, final EnumMap<MouseButton, ButtonState> enumMap,
            final Multiset<MouseButton> clicks) {
        // changing the y value, since for AWT, y = 0 at the top of the screen
        final int fixedY = _component.getHeight() - mouseEvent.getY();

        final MouseState newState = new MouseState(mouseEvent.getX(), fixedY, mouseEvent.getX() - _lastState.getX(),
                fixedY - _lastState.getY(), (mouseEvent instanceof MouseWheelEvent ? ((MouseWheelEvent) mouseEvent)
                        .getWheelRotation() : 0), enumMap, clicks);

        _upcomingEvents.add(newState);
        _lastState = newState;
    }

    private void setStateForButton(final MouseEvent e, final EnumMap<MouseButton, ButtonState> buttons,
            final ButtonState buttonState) {
        final MouseButton button = getButtonForEvent(e);
        buttons.put(button, buttonState);
    }

    private MouseButton getButtonForEvent(final MouseEvent e) {
        MouseButton button;
        switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                button = MouseButton.LEFT;
                break;
            case MouseEvent.BUTTON2:
                button = MouseButton.MIDDLE;
                break;
            case MouseEvent.BUTTON3:
                button = MouseButton.RIGHT;
                break;
            default:
                throw new RuntimeException("unknown button: " + e.getButton());
        }
        return button;
    }

    private class AwtMouseIterator extends AbstractIterator<MouseState> implements PeekingIterator<MouseState> {
        @Override
        protected MouseState computeNext() {
            synchronized (AwtMouseWrapper.this) {
                if (_upcomingEvents.isEmpty()) {
                    return endOfData();
                }

                return _upcomingEvents.poll();
            }

        }
    }
}
