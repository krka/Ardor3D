/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

/**
 * Callback interface for logic to execute when a Trigger from a TriggerChannel is encountered.
 */
public interface TriggerCallback {

    /**
     * Called once per encounter of a TriggerParam. Not guaranteed to be called if, for example, the window defined in
     * the TriggerParam is very small and/or the frame rate is really bad.
     */
    void doTrigger();

}