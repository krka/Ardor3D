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

import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.Spatial;

public class AttachmentPoint implements PoseListener {

    private int _jointIndex;
    private Spatial _attachment;
    private final Transform _offset = new Transform();
    private final Transform _store = new Transform();

    public AttachmentPoint() {}

    public AttachmentPoint(final int jointIndex, final Spatial attachment, final ReadOnlyTransform offset) {
        setJointIndex(jointIndex);
        setAttachment(attachment);
        setOffset(offset);
    }

    public Spatial getAttachment() {
        return _attachment;
    }

    public void setAttachment(final Spatial attachment) {
        _attachment = attachment;
    }

    public int getJointIndex() {
        return _jointIndex;
    }

    public void setJointIndex(final int jointIndex) {
        _jointIndex = jointIndex;
    }

    public ReadOnlyTransform getOffset() {
        return _offset;
    }

    public void setOffset(final ReadOnlyTransform offset) {
        _offset.set(offset);
    }

    public void poseUpdated(final SkeletonPose pose) {
        final Transform t = pose.getGlobalJointTransforms()[_jointIndex];
        t.multiply(_offset, _store);
        _attachment.setTransform(_store);
    }
}
