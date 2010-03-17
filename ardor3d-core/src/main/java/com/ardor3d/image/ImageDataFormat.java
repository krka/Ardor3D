/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image;

public enum ImageDataFormat {
    RGB(3, false), //
    RGBA(4, false), //
    BGR(3, false), //
    BGRA(4, false), //
    Luminance(1, false), //
    LuminanceAlpha(2, false), //
    Alpha(1, false), //
    Intensity(1, false), //
    Red(1, false), //
    Green(1, false), //
    Blue(1, false), //
    StencilIndex(1, false), //
    ColorIndex(1, false), //
    Depth(1, false), //
    PrecompressedDXT1(1, true), //
    PrecompressedDXT1A(1, true), //
    PrecompressedDXT3(2, true), //
    PrecompressedDXT5(2, true), //
    PrecompressedLATC_L(1, true), //
    PrecompressedLATC_LA(2, true);

    private final int _components;
    private final boolean _compressed;

    ImageDataFormat(final int components, final boolean isCompressed) {
        _components = components;
        _compressed = isCompressed;
    }

    public int getComponents() {
        return _components;
    }

    public boolean isCompressed() {
        return _compressed;
    }
}
