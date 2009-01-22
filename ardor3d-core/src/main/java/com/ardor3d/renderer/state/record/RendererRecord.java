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

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.ColorRGBA;

public class RendererRecord extends StateRecord {
    private int matrixMode = -1;
    private int currentElementVboId = -1, currentVboId = -1;
    private boolean matrixValid;
    private boolean vboValid;
    private boolean elementVboValid;
    private transient final ColorRGBA tempColor = new ColorRGBA();
    private final List<Integer> vboCleanupCache = new ArrayList<Integer>();

    @Override
    public void invalidate() {
        invalidateMatrix();
        invalidateVBO();
    }

    @Override
    public void validate() {
    // ignore - validate per item or locally
    }

    public void invalidateMatrix() {
        matrixValid = false;
        matrixMode = -1;
    }

    public void invalidateVBO() {
        vboValid = false;
        elementVboValid = false;
        currentElementVboId = currentVboId = -1;
    }

    public int getMatrixMode() {
        return matrixMode;
    }

    public void setMatrixMode(final int matrixMode) {
        this.matrixMode = matrixMode;
    }

    public int getCurrentElementVboId() {
        return currentElementVboId;
    }

    public void setCurrentElementVboId(final int currentElementVboId) {
        this.currentElementVboId = currentElementVboId;
    }

    public int getCurrentVboId() {
        return currentVboId;
    }

    public void setCurrentVboId(final int currentVboId) {
        this.currentVboId = currentVboId;
    }

    public boolean isMatrixValid() {
        return matrixValid;
    }

    public void setMatrixValid(final boolean matrixValid) {
        this.matrixValid = matrixValid;
    }

    public boolean isVboValid() {
        return vboValid;
    }

    public void setVboValid(final boolean vboValid) {
        this.vboValid = vboValid;
    }

    public boolean isElementVboValid() {
        return elementVboValid;
    }

    public void setElementVboValid(final boolean elementVboValid) {
        this.elementVboValid = elementVboValid;
    }

    public ColorRGBA getTempColor() {
        return tempColor;
    }

    public List<Integer> getVboCleanupCache() {
        return vboCleanupCache;
    }
}
