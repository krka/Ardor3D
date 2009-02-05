/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import com.ardor3d.scenegraph.Mesh;

public class VertMap {

    private int[] _lookupTable;

    public VertMap(final Mesh mesh) {
        setupTable(mesh);
    }

    private void setupTable(final Mesh mesh) {
        _lookupTable = new int[mesh.getMeshData().getVertexCount()];
        for (int x = 0; x < _lookupTable.length; x++) {
            _lookupTable[x] = x;
        }
    }

    public int getNewIndex(final int oldIndex) {
        return _lookupTable[oldIndex];
    }

    public void replaceIndex(final int oldIndex, final int newIndex) {
        for (int x = 0; x < _lookupTable.length; x++) {
            if (_lookupTable[x] == oldIndex) {
                _lookupTable[x] = newIndex;
            }
        }
    }

    public void decrementIndices(final int above) {
        for (int x = _lookupTable.length; --x >= 0;) {
            if (_lookupTable[x] >= above) {
                _lookupTable[x]--;
            }
        }
    }

}
