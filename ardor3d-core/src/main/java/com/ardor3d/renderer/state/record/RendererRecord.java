/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import java.util.Stack;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.renderer.DrawBufferTarget;

public class RendererRecord extends StateRecord {
    private int _matrixMode = -1;
    private int _currentElementVboId = -1, _currentVboId = -1;
    private boolean _matrixValid;
    private boolean _vboValid;
    private boolean _elementVboValid;
    private boolean _clippingTestValid;
    private boolean _clippingTestEnabled;
    private transient final ColorRGBA _tempColor = new ColorRGBA();
    private DrawBufferTarget _drawBufferTarget = null;
    private final Stack<Rectangle2> _clips = new Stack<Rectangle2>();

    @Override
    public void invalidate() {
        invalidateMatrix();
        invalidateVBO();
        _drawBufferTarget = null;
        _clippingTestValid = false;
    }

    @Override
    public void validate() {
    // ignore - validate per item or locally
    }

    public void invalidateMatrix() {
        _matrixValid = false;
        _matrixMode = -1;
    }

    public void invalidateVBO() {
        _vboValid = false;
        _elementVboValid = false;
        _currentElementVboId = _currentVboId = -1;
    }

    public int getMatrixMode() {
        return _matrixMode;
    }

    public void setMatrixMode(final int matrixMode) {
        _matrixMode = matrixMode;
    }

    public int getCurrentElementVboId() {
        return _currentElementVboId;
    }

    public void setCurrentElementVboId(final int currentElementVboId) {
        _currentElementVboId = currentElementVboId;
    }

    public int getCurrentVboId() {
        return _currentVboId;
    }

    public void setCurrentVboId(final int currentVboId) {
        _currentVboId = currentVboId;
    }

    public boolean isMatrixValid() {
        return _matrixValid;
    }

    public void setMatrixValid(final boolean matrixValid) {
        _matrixValid = matrixValid;
    }

    public boolean isVboValid() {
        return _vboValid;
    }

    public void setVboValid(final boolean vboValid) {
        _vboValid = vboValid;
    }

    public boolean isElementVboValid() {
        return _elementVboValid;
    }

    public void setElementVboValid(final boolean elementVboValid) {
        _elementVboValid = elementVboValid;
    }

    public ColorRGBA getTempColor() {
        return _tempColor;
    }

    public DrawBufferTarget getDrawBufferTarget() {
        return _drawBufferTarget;
    }

    public void setDrawBufferTarget(final DrawBufferTarget target) {
        _drawBufferTarget = target;
    }

    public Stack<Rectangle2> getScissorClips() {
        return _clips;
    }

    public boolean isClippingTestEnabled() {
        return _clippingTestEnabled;
    }

    public void setClippingTestEnabled(final boolean enabled) {
        _clippingTestEnabled = enabled;
    }

    public boolean isClippingTestValid() {
        return _clippingTestValid;
    }

    public void setClippingTestValid(final boolean valid) {
        _clippingTestValid = valid;
    }
}
