/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.core;

import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeNode extends DaeTreeNode {
    private DaeAsset asset;
    private DaeList<DaeTransform> transforms;
    private DaeList<DaeInstanceCamera> instanceCameras;
    private DaeList<DaeInstanceController> instanceControllers;
    private DaeList<DaeInstanceGeometry> instanceGeometries;
    private DaeList<DaeInstanceLight> instanceLights;
    private DaeList<DaeInstanceNode> instanceNodes;
    private DaeList<DaeNode> nodes;

    /**
     * @return the asset
     */
    public DaeAsset getAsset() {
        return asset;
    }

    /**
     * @return the transforms
     */
    public DaeList<DaeTransform> getTransforms() {
        return transforms;
    }

    /**
     * @return the instanceGeometries
     */
    public DaeList<DaeInstanceGeometry> getInstanceGeometries() {
        return instanceGeometries;
    }

    /**
     * @return the instanceCameras
     */
    public DaeList<DaeInstanceCamera> getInstanceCameras() {
        return instanceCameras;
    }

    /**
     * @return the instanceNodes
     */
    public DaeList<DaeInstanceNode> getInstanceNodes() {
        return instanceNodes;
    }

    /**
     * @return the nodes
     */
    public DaeList<DaeNode> getNodes() {
        return nodes;
    }

    /**
     * @return the instanceControllers
     */
    public DaeList<DaeInstanceController> getInstanceControllers() {
        return instanceControllers;
    }

    /**
     * @return the instanceLights
     */
    public DaeList<DaeInstanceLight> getInstanceLights() {
        return instanceLights;
    }
}
