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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import com.google.common.collect.Maps;

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

    private int[] _orders = null;

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
        _orders = new int[_joints.length];
        regenOrderArray();
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

    public int[] getJointOrders() {
        return _orders;
    }

    /**
     * Call this is you alter the parent Indices of the Joints after constructing the skeleton.
     */
    public void regenOrderArray() {
        final Joint[] sorted = new Joint[_joints.length];
        final Map<Joint, Integer> _indexMap = Maps.newHashMap();
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = _joints[i];
            _indexMap.put(_joints[i], i);
        }

        // Sort our new array by parent index.
        // Wish Java had a way to return original indices...
        Arrays.sort(sorted, new Comparator<Joint>() {
            public int compare(final Joint o1, final Joint o2) {
                if (isAncestorOf(o1, o2)) {
                    return -1;
                } else if (isAncestorOf(o2, o1)) {
                    return 1;
                } else {
                    return 0;
                }
            }

            private boolean isAncestorOf(final Joint o1, final Joint o2) {
                if (o1 == o2 || o2.getParentIndex() == Joint.NO_PARENT) {
                    return false;
                }
                final Joint o2Parent = _joints[o2.getParentIndex()];
                if (o1 == o2Parent) {
                    return true;
                } else {
                    return isAncestorOf(o1, o2Parent);
                }
            }
        });
        for (int i = 0; i < sorted.length; i++) {
            _orders[i] = _indexMap.get(sorted[i]);
        }
    }
}
