/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import org.jdom.Element;

import com.ardor3d.math.Matrix4;

/**
 * Utility methods for converting Collada information to Ardor3D math objects / concepts.
 */
public class ColladaMathUtils {
    /**
     * Converting an Element's contents to a float based Matrix4.
     * 
     * @param element
     *            the Collada element. We'll read the text value under this element and convert it to a float array.
     * @return a Matrix4 using the contents of the parsed float array.
     */
    public static Matrix4 toMatrix4(final Element element) {
        final float[] array = ColladaDOMUtil.parseFloatArray(element);

        return new Matrix4(array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8],
                array[9], array[10], array[11], array[12], array[13], array[14], array[15]);
    }
}
