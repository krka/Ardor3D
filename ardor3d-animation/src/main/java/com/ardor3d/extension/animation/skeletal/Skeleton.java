/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

/**
 * Describes a collection of Joints. This class represents the hierarchy of a Skeleton and its original aspect (via the
 * Joint class). This does not support posing the joints in any way... Use with a SkeletonPose to describe a skeleton in
 * a specific pose. Please take note of the requirement that Joints MUST be ordered such that a joint's parent comes
 * before the child joint in the joints array.
 */
public class Skeleton {

    /**
     * An array of Joints associated with this Skeleton. Joints must be ordered such that a joint's parent comes before
     * the child joint in the array.
     */
    private final Joint[] _joints;

    /** A name, for display or debugging purposes. */
    private final String _name;

    /**
     * 
     * @param name
     *            A name, for display or debugging purposes
     * @param joints
     *            An array of Joints associated with this Skeleton. Joints MUST be ordered such that a joint's parent
     *            comes before the child joint in the array.
     */
    public Skeleton(final String name, final Joint[] joints) {
        _name = name;
        _joints = joints;
    }

    /**
     * @return the human-readable name of this skeleton.
     */
    public String getName() {
        return _name;
    }

    /**
     * @return the array of Joints that make up this skeleton.
     */
    public Joint[] getJoints() {
        return _joints;
    }
}
