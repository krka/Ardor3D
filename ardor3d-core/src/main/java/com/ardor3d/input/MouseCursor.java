/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import com.ardor3d.image.Image;
import com.ardor3d.math.Vector2;
import com.ardor3d.annotation.Immutable;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * An immutable representation of a mouse cursor. A mouse cursor consists of an image and a hotspot where clicking is
 * done.
 *
 */
@Immutable
public class MouseCursor {
    private final String _name;
    private final Image _image;
    private final int _hotspotX;
    private final int _hotspotY;

    public MouseCursor(String name, Image image, final int hotspotX, final int hotspotY) {
        _name = name;
        _image = image;
        _hotspotX = hotspotX;
        _hotspotY = hotspotY;

        checkArgument(hotspotX >= 0 && hotspotX < image.getWidth(), "hotspot X is out of bounds: 0 <= %s  < " + image.getWidth(), hotspotX);
        checkArgument(hotspotY >= 0 && hotspotY < image.getHeight(), "hotspot Y is out of bounds: 0 <= %s  < " + image.getHeight(), hotspotY);
    }

    public String getName() {
        return _name;
    }

    public Image getImage() {
        return _image;
    }

    public int getWidth() {
        return _image.getWidth();
    }

    public int getHeight() {
        return _image.getHeight();
    }

    public int getHotspotX() {
        return _hotspotX;
    }

    public int getHotspotY() {
        return _hotspotY;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MouseCursor that = (MouseCursor) o;

        if (_hotspotX != that._hotspotX) return false;
        if (_hotspotY != that._hotspotY) return false;
        if (_image != null ? !_image.equals(that._image) : that._image != null) return false;
        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _name != null ? _name.hashCode() : 0;
        result = 31 * result + (_image != null ? _image.hashCode() : 0);
        result = 31 * result + _hotspotX;
        result = 31 * result + _hotspotY;
        return result;
    }
}
