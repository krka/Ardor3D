/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.util;

/**
 * A class used by the UI system to track the 2D bounds of a rotated component.
 */
public class BoundingRectangle {

    private int _x;
    private int _y;
    private int _width;
    private int _height;

    /**
     * Constructs a new 0 x 0 BoundingRectangle.
     */
    public BoundingRectangle() {
        set(0, 0, 0, 0);
    }

    /**
     * Constructs a new BoundingRectangle using the given dimensions and lower left handle origin.
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public BoundingRectangle(final int x, final int y, final int width, final int height) {
        set(x, y, width, height);
    }

    /**
     * Constructs a new BoundingRectangle using the given source.
     * 
     * @param source
     */
    public BoundingRectangle(final BoundingRectangle source) {
        set(source);
    }

    /**
     * Set the dimensions and lower left handle origin of this BoundingRectangle.
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void set(final int x, final int y, final int width, final int height) {
        _x = x;
        _y = y;
        _width = width;
        _height = height;
    }

    public BoundingRectangle set(final BoundingRectangle other) {
        _x = other.getX();
        _y = other.getY();
        _width = other.getWidth();
        _height = other.getHeight();
        return this;
    }

    public int getX() {
        return _x;
    }

    public int getY() {
        return _y;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public void setX(final int x) {
        _x = x;
    }

    public void setY(final int y) {
        _y = y;
    }

    public void setWidth(final int width) {
        _width = width;
    }

    public void setHeight(final int height) {
        _height = height;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof BoundingRectangle) {
            final BoundingRectangle other = (BoundingRectangle) obj;
            return _x == other._x && _y == other._y && _width == other._width && _height == other._height;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * result + getX();
        result += 31 * result + getY();
        result += 31 * result + getWidth();
        result += 31 * result + getHeight();
        return result;
    }
}
