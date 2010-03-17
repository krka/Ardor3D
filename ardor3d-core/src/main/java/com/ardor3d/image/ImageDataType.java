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

// XXX: Possibly support other types... eg. UnsignedByte_3_3_2
public enum ImageDataType {
    UnsignedByte(1), Byte(1), UnsignedShort(2), Short(2), UnsignedInt(4), Int(4), Float(4), HalfFloat(2);

    final float _bytesPerComponent;

    ImageDataType(final float bytes) {
        _bytesPerComponent = bytes;
    }

    public float getBytesPerComponent() {
        return _bytesPerComponent;
    }
}
