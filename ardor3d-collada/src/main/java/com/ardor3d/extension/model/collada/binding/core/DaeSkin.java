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

public class DaeSkin extends DaeTreeNode {

    private String source;
    private DaeSimpleFloatArray bindShapeMatrix;
    private DaeList<DaeSource> sources;
    private DaeJoints joints;
    private DaeVertexWeights vertexWeights;

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the bindShapeMatrix
     */
    public DaeSimpleFloatArray getBindShapeMatrix() {
        return bindShapeMatrix;
    }

    /**
     * @return the sources
     */
    public DaeList<DaeSource> getSources() {
        return sources;
    }

    /**
     * @return the joints
     */
    public DaeJoints getJoints() {
        return joints;
    }

    /**
     * @return the vertexWeights
     */
    public DaeVertexWeights getVertexWeights() {
        return vertexWeights;
    }
}
