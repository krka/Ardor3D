/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain;

/**
 * Used to calculate clipmap block boundaries etc
 */
public class Region {
    private int x;
    private int y;
    private final int width;
    private final int height;

    private int left;
    private int right;
    private int top;
    private int bottom;

    public Region(final int x, final int y, final int width, final int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        left = x;
        right = x + width;
        top = y;
        bottom = y + height;
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * @param x
     *            the x to set
     */
    public void setX(final int x) {
        this.x = x;
        left = x;
        right = x + width;
    }

    /**
     * @param y
     *            the y to set
     */
    public void setY(final int y) {
        this.y = y;
        top = y;
        bottom = y + height;
    }

    /**
     * @return the left
     */
    public int getLeft() {
        return left;
    }

    /**
     * @return the right
     */
    public int getRight() {
        return right;
    }

    /**
     * @return the top
     */
    public int getTop() {
        return top;
    }

    /**
     * @return the bottom
     */
    public int getBottom() {
        return bottom;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }
}
