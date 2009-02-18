/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.fx;

import com.ardor3d.extension.model.collada.binding.DaeList;
import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeSurface extends DaeTreeNode {

    public enum DaeSurfaceType {
        ONE_D, TWO_D, THREE_D, RECT, CUBE, DEPTH, UNTYPED;

        public static DaeSurfaceType fromColladaString(final String s) {
            if ("1D".equals(s)) {
                return ONE_D;
            } else if ("2D".equals(s)) {
                return TWO_D;
            } else if ("3D".equals(s)) {
                return THREE_D;
            } else if ("RECT".equals(s)) {
                return RECT;
            } else if ("CUBE".equals(s)) {
                return CUBE;
            } else if ("DEPTH".equals(s)) {
                return DEPTH;
            } else if ("UNTYPED".equals(s)) {
                return UNTYPED;
            } else {
                throw new IllegalArgumentException("Unknown type: " + s);
            }
        }

        public String toColladaString() {
            switch (this) {
                case ONE_D:
                    return "1D";
                case TWO_D:
                    return "2D";
                case THREE_D:
                    return "3D";
                case RECT:
                    return "RECT";
                case CUBE:
                    return "CUBE";
                case DEPTH:
                    return "DEPTH";
                case UNTYPED:
                    return "UNTYPED";
                default:
                    throw new IllegalArgumentException("Unhandled type: " + this);
            }
        }
    }

    private DaeList<DaeInitFrom> initFroms;
    private DaeSurfaceType type;

    /**
     * @return the initFroms
     */
    public DaeList<DaeInitFrom> getInitFroms() {
        return initFroms;
    }

    /**
     * @return the type
     */
    public DaeSurfaceType getType() {
        return type;
    }

    public void setTypeFromCollada(final String typeString) {
        type = DaeSurfaceType.fromColladaString(typeString);
    }

    public String getTypeAsCollada() {
        return type.toColladaString();
    }
}
