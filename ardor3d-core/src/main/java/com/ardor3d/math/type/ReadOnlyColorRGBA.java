/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.ColorRGBA;

public interface ReadOnlyColorRGBA {

    public float getRed();

    public float getGreen();

    public float getBlue();

    public float getAlpha();

    public float getValue(final int index);

    public float[] toArray(float[] store);

    public ColorRGBA clamp(final ColorRGBA store);

    public int asIntARGB();

    public int asIntRGBA();

    public ColorRGBA add(final float r, final float g, final float b, final float a, final ColorRGBA store);

    public ColorRGBA add(final ReadOnlyColorRGBA source, final ColorRGBA store);

    public ColorRGBA subtract(final float r, final float g, final float b, final float a, final ColorRGBA store);

    public ColorRGBA subtract(final ReadOnlyColorRGBA source, final ColorRGBA store);

    public ColorRGBA multiply(final float scalar, final ColorRGBA store);

    public ColorRGBA multiply(final ReadOnlyColorRGBA scale, final ColorRGBA store);

    public ColorRGBA divide(final float scalar, final ColorRGBA store);

    public ColorRGBA divide(final ReadOnlyColorRGBA scale, final ColorRGBA store);

    public ColorRGBA lerp(final ReadOnlyColorRGBA endColor, final float scalar, final ColorRGBA store);

}
