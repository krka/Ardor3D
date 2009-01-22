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

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.PeekingIterator;
import com.google.inject.Inject;

/**
 * Provides access to the physical layer of the input system. This is done via one method that polls the input system,
 * causing it to track which states it has been in {@link #readState()}, and one method that fetches the list of states
 * that are new {@link #drainAvailableStates()}.
 */
public class PhysicalLayer {

    private static final Logger logger = Logger.getLogger(PhysicalLayer.class.getName());

    private final BlockingQueue<InputState> stateQueue;
    private final KeyboardWrapper keyboardWrapper;
    private final MouseWrapper mouseWrapper;
    private final FocusWrapper focusWrapper;

    private KeyboardState currentKeyboardState;
    private MouseState currentMouseState;

    private boolean inited = false;

    private static final long MAX_INPUT_POLL_TIME = TimeUnit.SECONDS.toNanos(2);
    private static final List<InputState> EMPTY_LIST = ImmutableList.of();

    @Inject
    public PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper,
            final FocusWrapper focusWrapper) {
        this.keyboardWrapper = keyboardWrapper;
        this.mouseWrapper = mouseWrapper;
        this.focusWrapper = focusWrapper;
        stateQueue = new LinkedBlockingQueue<InputState>();

        currentKeyboardState = KeyboardState.NOTHING;
        currentMouseState = MouseState.NOTHING;
    }

    /**
     * Causes a poll of the input devices to happen, making any updates to input states available via the
     * {@link #drainAvailableStates()} method.
     * 
     * @throws IllegalStateException
     *             if too many state changes have happened since the last call to this method
     */
    public void readState() {
        if (!inited) {
            init();
        }

        KeyboardState oldKeyState = currentKeyboardState;
        MouseState oldMouseState = currentMouseState = new MouseState(currentMouseState.getX(), currentMouseState
                .getY(), 0, 0, 0, currentMouseState.getButtonStates(), currentMouseState.getClickCounts());

        final long loopExitTime = System.nanoTime() + MAX_INPUT_POLL_TIME;

        while (true) {
            readKeyboardState();
            readMouseState();

            // if there is no new input, exit the loop. Otherwise, add a new input state to the queue, and
            // see if there is even more input to read.
            if (oldKeyState.equals(currentKeyboardState) && oldMouseState.equals(currentMouseState)) {
                break;
            }

            stateQueue.add(new InputState(currentKeyboardState, currentMouseState));

            oldKeyState = currentKeyboardState;
            oldMouseState = currentMouseState;

            if (System.nanoTime() > loopExitTime) {
                logger.severe("Spent too long collecting input data, this is probably an input system bug");
                break;
            }
        }

        if (focusWrapper.getAndClearFocusLost()) {
            lostFocus();
        }
    }

    private void readMouseState() {
        final PeekingIterator<MouseState> eventIterator = mouseWrapper.getEvents();

        if (eventIterator.hasNext()) {
            currentMouseState = eventIterator.next();
        }
    }

    private void readKeyboardState() {
        EnumSet<Key> keysDown = null;
        EnumSet<Key> keysChanged = null;

        final PeekingIterator<KeyEvent> eventIterator = keyboardWrapper.getEvents();

        while (eventIterator.hasNext()) {
            // only initialising these variables if we actually have to use them; this
            // initialisation will be done during the first loop iteration.
            if (keysDown == null) {
                // EnumSet.copyOf fails if the collection is empty, since it needs at least one object to
                // figure out which type of enum to deal with. Hence the check below.
                if (currentKeyboardState.getKeysDown().isEmpty()) {
                    keysDown = EnumSet.noneOf(Key.class);
                } else {
                    keysDown = EnumSet.copyOf(currentKeyboardState.getKeysDown());
                }

                keysChanged = EnumSet.noneOf(Key.class);
            }

            final KeyEvent keyEvent = eventIterator.peek();

            if (keysChanged.contains(keyEvent.getKey())) {
                // this key has already changed once in this keyboard state, so we need a new state.
                // exit this loop and return.
                break;
            }

            eventIterator.next();

            // add this to the changed keys and keep track of whether it is now down or not
            keysChanged.add(keyEvent.getKey());

            if (keyEvent.getState() == KeyState.DOWN) {
                keysDown.add(keyEvent.getKey());
            } else {
                keysDown.remove(keyEvent.getKey());
            }
        }

        // check if the current keyboard state should be updated
        if (keysChanged != null && !keysChanged.isEmpty()) {
            currentKeyboardState = new KeyboardState(keysDown);
        }
    }

    /**
     * Fetches any new <code>InputState</code>s since the last call to this method. If no input system changes have been
     * made since the last call (no mouse movements, no keys pressed or released), an empty list is returned.
     * 
     * @return the list of new <code>InputState</code>, or an empty list if there have been no changes in input
     */
    public List<InputState> drainAvailableStates() {
        // returning a reusable empty list to avoid object creation if there is no new
        // input available. There is a race condition here (input might become available right after
        // the check of isEmpty()) but that's OK, it won't do any harm if that is picked up next frame.
        if (stateQueue.isEmpty()) {
            return EMPTY_LIST;
        }

        final LinkedList<InputState> result = new LinkedList<InputState>();

        stateQueue.drainTo(result);

        return result;
    }

    private void lostFocus() {
        stateQueue.add(InputState.LOST_FOCUS);
        currentKeyboardState = KeyboardState.NOTHING;
        currentMouseState = MouseState.NOTHING;
    }

    private void init() {
        inited = true;

        keyboardWrapper.init();
        mouseWrapper.init();
        focusWrapper.init();
    }
}
