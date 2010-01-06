/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;

public class VertKey {

    private final Vector3 _vert;
    private Vector3 _norm;
    private ColorRGBA _color;
    private Vector2[] _texs;
    private final int _options;

    public VertKey(final Vector3 vert, final Vector3 norm, final ColorRGBA color, final Vector2[] texs,
            final int options) {
        _vert = vert;
        if ((options & GeometryTool.MV_SAME_NORMALS) != 0) {
            _norm = norm;
        }
        if ((options & GeometryTool.MV_SAME_COLORS) != 0) {
            _color = color;
        }
        if ((options & GeometryTool.MV_SAME_TEXS) != 0) {
            _texs = texs;
        }
        _options = options;
    }

    @Override
    public int hashCode() {
        int rez = _vert.hashCode();
        if ((_options & GeometryTool.MV_SAME_NORMALS) != 0 && _norm != null) {
            final long x = Double.doubleToLongBits(_norm.getX());
            rez += 31 * rez + (int) (x ^ (x >>> 32));

            final long y = Double.doubleToLongBits(_norm.getY());
            rez += 31 * rez + (int) (y ^ (y >>> 32));

            final long z = Double.doubleToLongBits(_norm.getZ());
            rez += 31 * rez + (int) (z ^ (z >>> 32));
        }
        if ((_options & GeometryTool.MV_SAME_COLORS) != 0 && _color != null) {
            final int r = Float.floatToIntBits(_color.getRed());
            rez += 31 * rez + r;

            final int g = Float.floatToIntBits(_color.getGreen());
            rez += 31 * rez + g;

            final int b = Float.floatToIntBits(_color.getBlue());
            rez += 31 * rez + b;

            final int a = Float.floatToIntBits(_color.getAlpha());
            rez += 31 * rez + a;
        }
        if ((_options & GeometryTool.MV_SAME_TEXS) != 0 && _texs != null) {
            for (int i = 0; i < _texs.length; i++) {
                if (_texs[i] != null) {
                    final long x = Double.doubleToLongBits(_texs[i].getX());
                    rez += 31 * rez + (int) (x ^ (x >>> 32));

                    final long y = Double.doubleToLongBits(_texs[i].getY());
                    rez += 31 * rez + (int) (y ^ (y >>> 32));
                }
            }
        }
        return rez;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof VertKey)) {
            return false;
        }

        final VertKey other = (VertKey) obj;

        if (other._options != _options) {
            return false;
        }
        if (!other._vert.equals(_vert)) {
            return false;
        }

        if ((_options & GeometryTool.MV_SAME_NORMALS) != 0) {
            if (_norm != null) {
                if (!_norm.equals(other._norm)) {
                    return false;
                }
            } else if (other._norm != null) {
                return false;
            }
        }

        if ((_options & GeometryTool.MV_SAME_COLORS) != 0) {
            if (_color != null) {
                if (!_color.equals(other._color)) {
                    return false;
                }
            } else if (other._color != null) {
                return false;
            }
        }

        if ((_options & GeometryTool.MV_SAME_TEXS) != 0) {
            if (_texs != null) {
                if (other._texs == null || other._texs.length != _texs.length) {
                    return false;
                }
                for (int x = 0; x < _texs.length; x++) {
                    if (_texs[x] != null) {
                        if (!_texs[x].equals(other._texs[x])) {
                            return false;
                        }
                    } else if (other._texs[x] != null) {
                        return false;
                    }
                }
            } else if (other._texs != null) {
                return false;
            }
        }

        return true;
    }
}
