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
 * Custom logic for how a skin should react when it is told its pose has updated. This might include throttling skin
 * application, ignoring skin application when the skin is outside of the camera view, etc.
 */
public interface SkinPoseApplyLogic {

    void doApply(SkinnedMesh skinnedMesh, SkeletonPose pose);

}
