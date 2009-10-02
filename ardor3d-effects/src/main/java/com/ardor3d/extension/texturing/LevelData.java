
package com.ardor3d.extension.texturing;

import java.nio.ByteBuffer;

public class LevelData {
    public int unit;
    public int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE;
    public int offsetX, offsetY;

    public ByteBuffer sliceData;

    public LevelData(final int unit) {
        this.unit = unit;
    }
}
