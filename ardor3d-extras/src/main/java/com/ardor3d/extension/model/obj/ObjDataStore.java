/**
 * Copyright (c) 2008-20010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.obj;

import java.util.List;

import com.ardor3d.math.Vector3;
import com.google.common.collect.Lists;

public class ObjDataStore {
    private final List<Vector3> _vertices = Lists.newArrayList();
    private final List<Vector3> _normals = Lists.newArrayList();
    private final List<Vector3> _generatedNormals = Lists.newArrayList();
    private final List<Vector3> _uvs = Lists.newArrayList();

    List<Vector3> getVertices() {
        return _vertices;
    }

    List<Vector3> getNormals() {
        return _normals;
    }

    List<Vector3> getGeneratedNormals() {
        return _generatedNormals;
    }

    List<Vector3> getUvs() {
        return _uvs;
    }
}
