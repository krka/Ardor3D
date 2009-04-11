/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
package com.ardor3d.animations.reference;

import com.ardor3d.annotation.Immutable;

/**
 * TODO: This class may not be very useful, since it doesn't add anything on top of the Joint for now.
 * For debugging purposes, and to keep a registry of valid skeletons it has some benefit though,
 * so let's keep it for now.
 */
@Immutable
public class Skeleton {
    // TODO: the _id should probably be a combination
    // of <namespace (= source file/other source), name>. This is useful for debugging
    private final String _id;
    private final Joint _rootJoint;

    public Skeleton(String id, Joint rootJoint) {
        this._id = id;
        this._rootJoint = rootJoint;
    }

    @Override
    public String toString() {
        return "Skeleton{" +
                "_id='" + _id + '\'' +
                ", _rootJoint=" + _rootJoint +
                '}';
    }
}
