/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.binding.core;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;

public class DaeSource extends DaeTreeNode {
    private DaeNameArray nameArray;
    private DaeFloatArray floatArray;
    private DaeDoubleArray doubleArray;
    private DaeSimpleIntegerArray intArray;
    private DaeAccessor commonAccessor;

    public DaeSource() {}

    public DaeNameArray getNameArray() {
        return nameArray;
    }

    public DaeFloatArray getFloatArray() {
        return floatArray;
    }

    public DaeDoubleArray getDoubleArray() {
        return doubleArray;
    }

    public DaeSimpleIntegerArray getIntArray() {
        return intArray;
    }

    public DaeAccessor getCommonAccessor() {
        return commonAccessor;
    }

    public static DaeSource createNameArraySource(final DaeNameArray nameArray) {
        final DaeSource result = new DaeSource();

        result.nameArray = nameArray;

        return result;
    }

    public static DaeSource createFloatArraySource(final DaeFloatArray floatArray) {
        final DaeSource result = new DaeSource();

        result.floatArray = floatArray;

        return result;
    }
}
