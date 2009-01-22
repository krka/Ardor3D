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

import java.util.EnumMap;
import java.util.EnumSet;

import com.ardor3d.annotation.Immutable;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

/**
 * Describes the mouse state at some point in time.
 */
@Immutable
public class MouseState {
    public static final MouseState NOTHING = new MouseState(0, 0, 0, 0, 0, null, null);
    public static long CLICK_TIME_MS = 500;

    private final int _x;
    private final int _y;
    private final int _dx;
    private final int _dy;
    private final int _dwheel;
    private final EnumMap<MouseButton, ButtonState> _buttonStates = Maps.newEnumMap(MouseButton.class);
    private final Multiset<MouseButton> _clickCounts = Multisets.newEnumMultiset(MouseButton.class);

    /**
     * Constructs a new MouseState instance.
     * 
     * @param x
     *            the mouse's x position
     * @param y
     *            the mouse's y position
     * @param dx
     *            the delta in the mouse's x position since the last update
     * @param dy
     *            the delta in the mouse's y position since the last update
     * @param dwheel
     *            the delta in the mouse's wheel movement since the last update
     * @param buttonStates
     *            the states of the various given buttons.
     * @param clicks
     *            the number of times each button has been clicked
     */
    public MouseState(final int x, final int y, final int dx, final int dy, final int dwheel,
            final EnumMap<MouseButton, ButtonState> buttonStates, final Multiset<MouseButton> clicks) {
        _x = x;
        _y = y;
        _dx = dx;
        _dy = dy;
        _dwheel = dwheel;
        if (buttonStates != null) {
            _buttonStates.putAll(buttonStates);
        }
        if (clicks != null) {
            _clickCounts.addAll(clicks);
        }
    }

    public int getX() {
        return _x;
    }

    public int getY() {
        return _y;
    }

    public int getDx() {
        return _dx;
    }

    public int getDy() {
        return _dy;
    }

    public int getDwheel() {
        return _dwheel;
    }

    /**
     * Returns all the buttons' states. It could be easier for most classes to use the
     * {@link #getButtonState(MouseButton)} methods, and that also results in less object creation.
     * 
     * @return a defensive copy of the states of all the buttons at this point in time.
     */
    public EnumMap<MouseButton, ButtonState> getButtonStates() {
        return _buttonStates.clone();
    }

    /**
     * Returns the current state for the supplied button, or UP if no state for that button is
     * registered.
     *
     * @param button the mouse button to check
     * @return the button's state, or {@link ButtonState#UP} if no button state registered.
     */
    public ButtonState getButtonState(final MouseButton button) {
        if (_buttonStates.containsKey(button)) {
            return _buttonStates.get(button);
        }

        return ButtonState.UP;
    }

    public EnumSet<MouseButton> getButtonsReleasedSince(final MouseState previous) {
        final EnumSet<MouseButton> result = EnumSet.noneOf(MouseButton.class);
        for (final MouseButton button : MouseButton.values()) {
            if (previous.getButtonState(button) == ButtonState.DOWN) {
                if (getButtonState(button) != ButtonState.DOWN) {
                    result.add(button);
                }
            }
        }

        return result;
    }

    public EnumSet<MouseButton> getButtonsPressedSince(final MouseState previous) {
        final EnumSet<MouseButton> result = EnumSet.noneOf(MouseButton.class);
        for (final MouseButton button : MouseButton.values()) {
            if (getButtonState(button) == ButtonState.DOWN) {
                if (previous.getButtonState(button) != ButtonState.DOWN) {
                    result.add(button);
                }
            }
        }

        return result;
    }

    /**
     * Returns all the buttons' states. It could be easier for most classes to use the
     * {@link #getClickCount(MouseButton)} method, and that also results in less object creation.
     * 
     * @return a defensive copy of the click counts of all the buttons at this point in time.
     */
    public Multiset<MouseButton> getClickCounts() {
        if (_clickCounts.isEmpty()) {
            return Multisets.newEnumMultiset(MouseButton.class);
        } else {
            return Multisets.newEnumMultiset(_clickCounts);
        }
    }

    /**
     * Returns the click count of a mouse button as of this frame. Click counts are non-zero only for frames when the
     * mouse button is released. A double-click sequence, for instance, could show up like this:
     * <nl>
     *  <li>Frame 1, mouse button pressed - click count == 0</li>
     *  <li>Frame 2, mouse button down - click count == 0</li>
     *  <li>Frame 3, mouse button released  - click count == 1</li>
     *  <li>Frame 4, mouse button up - click count == 0</li>
     *  <li>Frame 5, mouse button pressed - click count == 0</li>
     *  <li>Frame 6, mouse button down - click count == 0</li>
     *  <li>Frame 7, mouse button released  - click count == 2</li>
     * </nl>
     *
     * Whether or not a mouse press/release sequence counts as a click (or double-click) depends on the time passed
     * between them. See {@link #CLICK_TIME_MS}.
     *
     * 
     * @param button the button to check for clicks
     * @return the click count in this frame
     */
    public int getClickCount(final MouseButton button) {
        return _clickCounts.count(button);
    }

    /**
     * Returns a new EnumSet of all buttons that were clicked this frame.
     *
     * @return every mouse button whose click count this frame is > 0
     */
    public EnumSet<MouseButton> getButtonsClicked() {
        final EnumSet<MouseButton> result = EnumSet.noneOf(MouseButton.class);
        for (final MouseButton button : MouseButton.values()) {
            if (getClickCount(button) != 0) {
                result.add(button);
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "MouseState{" + "x=" + _x + ", y=" + _y + ", dx=" + _dx + ", dy=" + _dy + ", dwheel=" + _dwheel
                + ", buttonStates=" + _buttonStates.toString() + ", clickCounts=" + _clickCounts.toString() + '}';
    }
}
