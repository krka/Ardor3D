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

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;

/**
 * Defines an action to be performed when a given input condition is true.
 */
public interface TriggerAction {
    /**
     * Implementing classes should implementing this method to take whatever action is desired. This method will always
     * be called on the main GL thread.
     * 
     * @param source
     *            the Canvas that was the source of the current input
     * @param inputState
     *            the current state of the input systems when the action was triggered
     * @param tpf
     *            the time per frame in seconds
     */
    @MainThread
    public void perform(Canvas source, TwoInputStates inputState, double tpf);
}
