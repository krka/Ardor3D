/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.MainThread;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.InputState;
import com.ardor3d.input.PhysicalLayer;
import com.google.inject.Inject;

/**
 * Implementation of a logical layer on top of the physical one, to be able to more easily trigger certain commands for
 * certain combination of user input.
 */
@ThreadSafe
public final class LogicalLayer {
    private final Set<InputSource> inputs = new CopyOnWriteArraySet<InputSource>();
    private final Set<InputTrigger> triggers = new CopyOnWriteArraySet<InputTrigger>();

    @Inject
    public LogicalLayer() {}

    public void registerInput(final Canvas source, final PhysicalLayer physicalLayer) {
        inputs.add(new InputSource(source, physicalLayer));
    }

    /**
     * Register a trigger for evaluation when the {@link #checkTriggers(double)} method is called.
     * 
     * @param inputTrigger
     *            the trigger to check
     */
    public void registerTrigger(final InputTrigger inputTrigger) {
        triggers.add(inputTrigger);
    }

    /**
     * Deregister a trigger for evaluation when the {@link #checkTriggers(double)} method is called.
     * 
     * @param inputTrigger
     *            the trigger to stop checking
     */
    public void deregisterTrigger(final InputTrigger inputTrigger) {
        triggers.remove(inputTrigger);
    }

    /**
     * Check all registered triggers to see if their respective conditions are met. For every trigger whose condition is
     * true, perform the associated action.
     * 
     * @param tpf
     *            time per frame in seconds
     */
    @MainThread
    public synchronized void checkTriggers(final double tpf) {
        for (final InputSource is : inputs) {
            is.physicalLayer.readState();

            final List<InputState> newStates = is.physicalLayer.drainAvailableStates();

            if (newStates.isEmpty()) {
                checkAndPerformTriggers(is.source, new TwoInputStates(is.lastState, is.lastState), tpf);
            } else {
                for (final InputState inputState : newStates) {
                    // no trigger is valid in the LOST_FOCUS state, so don't bother checking them
                    if (inputState != InputState.LOST_FOCUS) {
                        checkAndPerformTriggers(is.source, new TwoInputStates(is.lastState, inputState), tpf);
                    }

                    is.lastState = inputState;
                }
            }
        }
    }

    private void checkAndPerformTriggers(final Canvas source, final TwoInputStates states, final double tpf) {
        for (final InputTrigger trigger : triggers) {
            trigger.performIfValid(source, states, tpf);
        }
    }

    private static class InputSource {
        private final Canvas source;
        private final PhysicalLayer physicalLayer;
        @GuardedBy("LogicalLayer.this")
        private InputState lastState;

        public InputSource(final Canvas source, final PhysicalLayer physicalLayer) {
            this.source = source;
            this.physicalLayer = physicalLayer;
            lastState = InputState.EMPTY;
        }
    }
}
