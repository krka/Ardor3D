/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.queue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.SortUtil;

public class AbstractRenderBucket implements RenderBucket {

    protected final Renderer _renderer;
    protected Comparator<Spatial> _comparator;

    protected Spatial[] _currentList, _tempList;
    protected int _currentListSize;

    protected Stack<Spatial[]> _listStack = new Stack<Spatial[]>();
    protected Stack<Spatial[]> _listStackPool = new Stack<Spatial[]>();
    protected Stack<Integer> _listSizeStack = new Stack<Integer>();

    public AbstractRenderBucket(final Renderer renderer) {
        _renderer = renderer;

        _currentList = new Spatial[32];
    }

    public void add(final Spatial spatial) {
        if (_currentListSize == _currentList.length) {
            final Spatial[] temp = new Spatial[_currentListSize * 2];
            System.arraycopy(_currentList, 0, temp, 0, _currentListSize);
            _currentList = temp;
        }
        _currentList[_currentListSize++] = spatial;
    }

    public void remove(final Spatial spatial) {
        int index = 0;
        for (int i = 0; i < _currentListSize; i++) {
            if (_currentList[index] == spatial) {
                break;
            }
            index++;
        }
        for (int i = index; i < _currentListSize - 1; i++) {
            _currentList[i] = _currentList[i + 1];
        }

        _currentListSize--;
    }

    public void clear() {
        for (int i = 0; i < _currentListSize; i++) {
            _currentList[i] = null;
        }
        if (_tempList != null) {
            Arrays.fill(_tempList, null);
        }
        _currentListSize = 0;
    }

    public void render() {
        for (int i = 0; i < _currentListSize; i++) {
            _currentList[i].draw(_renderer);
        }
    }

    public void sort() {
        if (_currentListSize > 1) {
            // resize or populate our temporary array as necessary
            if (_tempList == null || _tempList.length != _currentList.length) {
                _tempList = _currentList.clone();
            } else {
                System.arraycopy(_currentList, 0, _tempList, 0, _currentList.length);
            }
            // now merge sort tlist into list
            SortUtil.msort(_tempList, _currentList, 0, _currentListSize, _comparator);
        }
    }

    public void pushBucket() {
        _listStack.push(_currentList);
        if (_listStackPool.isEmpty()) {
            _currentList = new Spatial[32];
        } else {
            _currentList = _listStackPool.pop();
        }

        _listSizeStack.push(_currentListSize);
        _currentListSize = 0;
    }

    public void popBucket() {
        if (_currentList != null) {
            _listStackPool.push(_currentList);
        }
        _currentList = _listStack.pop();
        _currentListSize = _listSizeStack.pop();
    }

    /**
     * Calculates the distance from a spatial to the camera. Distance is a squared distance.
     * 
     * @param spat
     *            Spatial to check distance.
     * @return Distance from Spatial to current context's camera.
     */
    protected double distanceToCam(final Spatial spat) {
        // this optimization should not be stored in the spatial
        // if (spat.queueDistance != Double.NEGATIVE_INFINITY) {
        // return spat.queueDistance;
        // }

        final Camera cam = Camera.getCurrentCamera();

        // spat.queueDistance = 0;

        ReadOnlyVector3 spatPosition;
        if (spat.getWorldBound() != null && Vector3.isValid(spat.getWorldBound().getCenter())) {
            spatPosition = spat.getWorldBound().getCenter();
        } else {
            spatPosition = spat.getWorldTranslation();
            if (!Vector3.isValid(spatPosition)) {
                return Double.NEGATIVE_INFINITY;
            }
        }

        return cam.distanceToCam(spatPosition);
        // return spat.queueDistance;
    }
}
