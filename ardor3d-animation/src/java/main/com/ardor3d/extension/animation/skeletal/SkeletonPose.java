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

import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;

/**
 * Joins a Skeleton with an array of joint poses. This allows the skeleton to exist and be reused between multiple
 * instances of poses.
 */
public class SkeletonPose {

    /** The skeleton being "posed". */
    private final Skeleton _skeleton;

    /** Local transforms for the joints of the associated skeleton. */
    private final Transform[] _localTransforms;

    /** Global transforms for the joints of the associated skeleton. */
    private final Transform[] _globalTransforms;

    /**
     * A pallete of matrices used in skin deformation - basically the global transform X the inverse bind pose
     * transform.
     */
    private final Matrix4[] _matrixPallete;

    /**
     * Construct a new SkeletonPose using the given Skeleton.
     * 
     * @param skeleton
     *            the skeleton to use.
     */
    public SkeletonPose(final Skeleton skeleton) {
        assert skeleton != null : "skeleton must not be null.";

        _skeleton = skeleton;
        final int jointCount = _skeleton.getJoints().length;

        // init local transforms
        _localTransforms = new Transform[jointCount];
        for (int i = 0; i < jointCount; i++) {
            _localTransforms[i] = new Transform();
        }

        // init global transforms
        _globalTransforms = new Transform[jointCount];
        for (int i = 0; i < jointCount; i++) {
            _globalTransforms[i] = new Transform();
        }

        // init pallete
        _matrixPallete = new Matrix4[jointCount];
        for (int i = 0; i < jointCount; i++) {
            _matrixPallete[i] = new Matrix4();
        }

        // start off in bind pose.
        setToBindPose();
    }

    /**
     * @return the skeleton posed by this object.
     */
    public Skeleton getSkeleton() {
        return _skeleton;
    }

    /**
     * @return an array of local space transforms for each of the skeleton's joints.
     */
    public Transform[] getLocalJointTransforms() {
        return _localTransforms;
    }

    /**
     * @return an array of global space transforms for each of the skeleton's joints.
     */
    public Transform[] getGlobalJointTransforms() {
        return _globalTransforms;
    }

    /**
     * @return an array of global space transforms for each of the skeleton's joints.
     */
    public Matrix4[] getMatrixPallete() {
        return _matrixPallete;
    }

    /**
     * Update the global and pallete transforms of our posed joints based on the current local joint transforms.
     */
    public void updateTransforms() {
        final Transform temp = Transform.fetchTempInstance();
        // we go in array order, which ensures parent global transforms are updated before child.
        for (int i = 0; i < _globalTransforms.length; i++) {
            // find our parent
            final short parentIndex = _skeleton.getJoints()[i].getParentIndex();
            if (parentIndex != Short.MAX_VALUE) {
                // we have a parent, so take us from local->parent->model space by multiplying by parent's local->model
                // space transform.
                _globalTransforms[parentIndex].multiply(_localTransforms[i], _globalTransforms[i]);
            } else {
                // no parent so just set global to the local transform
                _globalTransforms[i].set(_localTransforms[i]);
            }

            // at this point we have a local->model space transform for this joint, for skinning we multiply this by the
            // joint's inverse bind pose (joint->model space, inverted). This gives us a transform that can take a
            // vertex from bind pose (model space) to current pose (model space).
            _globalTransforms[i].multiply(_skeleton.getJoints()[i].getInverseBindPose(), temp);
            temp.getHomogeneousMatrix(_matrixPallete[i]);
        }
        Transform.releaseTempInstance(temp);
    }

    /**
     * Update our local joint transforms so that they reflect the skeleton in bind pose.
     */
    public void setToBindPose() {
        final Transform temp = Transform.fetchTempInstance();
        // go through our local transforms
        for (int i = 0; i < _localTransforms.length; i++) {
            // Set us to the bind pose
            _localTransforms[i].set(_skeleton.getJoints()[i].getInverseBindPose());
            // then invert.
            _localTransforms[i].invert(_localTransforms[i]);

            // At this point we are in model space, so we need to remove our parent's transform (if we have one.)
            final short parentIndex = _skeleton.getJoints()[i].getParentIndex();
            if (parentIndex != Short.MAX_VALUE) {
                // We remove the parent's transform simply by multiplying by its inverse bind pose. Done! :)
                _skeleton.getJoints()[parentIndex].getInverseBindPose().multiply(_localTransforms[i], temp);
                _localTransforms[i].set(temp);
            }
        }
        Transform.releaseTempInstance(temp);
    }
}
