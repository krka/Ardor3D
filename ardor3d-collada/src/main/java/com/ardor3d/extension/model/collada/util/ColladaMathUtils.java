package com.ardor3d.extension.model.collada.util;

import com.ardor3d.math.Matrix4;
import com.ardor3d.extension.model.collada.binding.core.DaeSimpleFloatArray;

/**
 * TODO: document this class!
 *
 */
public class ColladaMathUtils {
    public static Matrix4 toMatrix4(DaeSimpleFloatArray floatArray) {
        float[] array = floatArray.getData();

        return new Matrix4(
                array[0], array[1], array[2], array[3],
                array[4], array[5], array[6], array[7],
                array[8], array[9], array[10], array[11],
                array[12], array[13], array[14], array[15]);
    }
}
