/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
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

    private final Vector3 vert;
    private Vector3 norm;
    private ColorRGBA color;
    private Vector2[] texs;
    private final int options;

    public VertKey(final Vector3 vert, final Vector3 norm, final ColorRGBA color, final Vector2[] texs,
            final int options) {
        this.vert = vert;
        if ((options & GeometryTool.MV_SAME_NORMALS) != 0) {
            this.norm = norm;
        }
        if ((options & GeometryTool.MV_SAME_COLORS) != 0) {
            this.color = color;
        }
        if ((options & GeometryTool.MV_SAME_TEXS) != 0) {
            this.texs = texs;
        }
        this.options = options;
    }

    @Override
    public int hashCode() {
        int rez = vert.hashCode();
        if ((options & GeometryTool.MV_SAME_NORMALS) != 0 && norm != null) {
            final long x = Double.doubleToLongBits(norm.getX());
            rez += 31 * rez + (int) (x ^ (x >>> 32));

            final long y = Double.doubleToLongBits(norm.getY());
            rez += 31 * rez + (int) (y ^ (y >>> 32));

            final long z = Double.doubleToLongBits(norm.getZ());
            rez += 31 * rez + (int) (z ^ (z >>> 32));
        }
        if ((options & GeometryTool.MV_SAME_COLORS) != 0 && color != null) {
            final int r = Float.floatToIntBits(color.getRed());
            rez += 31 * rez + r;

            final int g = Float.floatToIntBits(color.getGreen());
            rez += 31 * rez + g;

            final int b = Float.floatToIntBits(color.getBlue());
            rez += 31 * rez + b;

            final int a = Float.floatToIntBits(color.getAlpha());
            rez += 31 * rez + a;
        }
        if ((options & GeometryTool.MV_SAME_TEXS) != 0 && texs != null) {
            for (int i = 0; i < texs.length; i++) {
                if (texs[i] != null) {
                    final long x = Double.doubleToLongBits(texs[i].getX());
                    rez += 31 * rez + (int) (x ^ (x >>> 32));

                    final long y = Double.doubleToLongBits(texs[i].getY());
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

        if (other.options != options) {
            return false;
        }
        if (!other.vert.equals(vert)) {
            return false;
        }

        if ((options & GeometryTool.MV_SAME_NORMALS) != 0) {
            if (norm != null) {
                if (!norm.equals(other.norm)) {
                    return false;
                }
            } else if (other.norm != null) {
                return false;
            }
        }

        if ((options & GeometryTool.MV_SAME_COLORS) != 0) {
            if (color != null) {
                if (!color.equals(other.color)) {
                    return false;
                }
            } else if (other.color != null) {
                return false;
            }
        }

        if ((options & GeometryTool.MV_SAME_TEXS) != 0) {
            if (texs != null) {
                if (other.texs == null || other.texs.length != texs.length) {
                    return false;
                }
                for (int x = 0; x < texs.length; x++) {
                    if (texs[x] != null) {
                        if (!texs[x].equals(other.texs[x])) {
                            return false;
                        }
                    } else if (other.texs[x] != null) {
                        return false;
                    }
                }
            } else if (other.texs != null) {
                return false;
            }
        }

        return true;
    }
}
