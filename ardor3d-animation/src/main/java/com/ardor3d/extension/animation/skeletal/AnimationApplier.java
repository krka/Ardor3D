/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;


/**
 * Describes a class that can take information from a manager and its current layers and state and apply it to a given
 * SkeletonPose. The class should not update or modify the manager, but should merely request current state (usually via
 * <i>manager.getCurrentSourceData();</i>)
 */
public interface AnimationApplier {

    /**
     * Apply the current status of the manager to our SkeletonPose.
     * 
     * @param applyToPose
     *            the pose to apply to
     * @param manager
     *            the animation manager to pull state from.
     */
    void applyTo(SkeletonPose applyToPose, AnimationManager manager);

}
