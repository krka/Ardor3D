/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.queue;

import java.util.Comparator;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;

public class OrthoRenderBucket extends AbstractRenderBucket {

    public OrthoRenderBucket(final Renderer renderer) {
        super(renderer);

        _comparator = new OrthoComparator();
    }

    @Override
    public void render() {
        if (_currentListSize > 0) {
            _renderer.setOrtho();
            for (int i = 0; i < _currentListSize; i++) {
                _currentList[i].draw(_renderer);
            }
            _renderer.unsetOrtho();
        }
    }

    private class OrthoComparator implements Comparator<Spatial> {
        public int compare(final Spatial o1, final Spatial o2) {
            if (o2.getZOrder() == o1.getZOrder()) {
                return 0;
            } else if (o2.getZOrder() < o1.getZOrder()) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
