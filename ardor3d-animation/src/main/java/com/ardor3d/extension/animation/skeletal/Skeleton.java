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
 * Describes a collection of Joints. This class represents the hierarchy of a Skeleton and its original aspect (via the
 * Joint class). This does not support posing the joints in any way... Use with a SkeletonPose to describe a skeleton in
 * a specific pose.
 */
public class Skeleton {

    /**
     * An array of Joints associated with this Skeleton.
     */
    private final Joint[] _joints;

    /** A name, for display or debugging purposes. */
    private final String _name;

    /**
     * 
     * @param name
     *            A name, for display or debugging purposes
     * @param joints
     *            An array of Joints associated with this Skeleton.
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

    /**
     * 
     * @param jointName
     *            name of the joint to locate. Case sensitive.
     * @return the index of the joint, if found, or -1 if not.
     */
    public int findJointByName(final String jointName) {
        for (int i = 0; i < _joints.length; i++) {
            if (jointName.equals(_joints[i].getName())) {
                return i;
            }
        }
        return -1;
    }
}
