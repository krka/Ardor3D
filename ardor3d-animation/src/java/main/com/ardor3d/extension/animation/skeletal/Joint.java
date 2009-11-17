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

import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;

/**
 * Representation of a Joint in a Skeleton. Meant to be used within a specific Skeleton object.
 */
public class Joint {

    /** The inverse transform of this Joint in its bind position. */
    protected ReadOnlyTransform _inverseBindPose = Transform.IDENTITY;

    /** A name, for display or debugging purposes. */
    private final String _name;

    /** Index of our parent Joint, or Short.MAX_VALUE if we are the root. */
    protected short _parentIndex;

    /**
     * Construct a new Joint object using the given name.
     * 
     * @param name
     *            the name
     */
    public Joint(final String name) {
        _name = name;
    }

    /**
     * @return the inverse of the joint space -> model space transformation.
     */
    public ReadOnlyTransform getInverseBindPose() {
        return _inverseBindPose;
    }

    public void setInverseBindPose(final ReadOnlyTransform inverseBindPose) {
        _inverseBindPose = inverseBindPose;
    }

    /**
     * @return the human-readable name of this joint.
     */
    public String getName() {
        return _name;
    }

    /**
     * Set the index of this joint's parent within the containing Skeleton's joint array.
     * 
     * @param parentIndex
     *            the index, or Short.MAX_VALUE if this Joint is root (has no parent)
     */
    public void setParentIndex(final short parentIndex) {
        _parentIndex = parentIndex;
    }

    public short getParentIndex() {
        return _parentIndex;
    }
}
