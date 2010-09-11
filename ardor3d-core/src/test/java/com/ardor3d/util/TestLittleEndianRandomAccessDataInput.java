/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import org.junit.Test;

/**
 * These tests are fairly brittle, since they rely on the implementation of BufferedInputStream. It is necessary for the
 * bytes available to always be larger than the buffer size of the buffered input stream for the tests to work. This
 * size is currently 8192, but if it changes, or if the implementation changes, these tests can break.
 */
public class TestLittleEndianRandomAccessDataInput {

    @Test
    public void testReadUint() throws Exception {
        // test reading of uint vs int.
        final byte[] data = new byte[4];
        data[0] = (byte) 0xff;
        data[1] = (byte) 0xff;
        data[2] = (byte) 0xff;
        data[3] = (byte) 0xff;

        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final LittleEndianRandomAccessDataInput littleEndien = new LittleEndianRandomAccessDataInput(bais);

        final long val = littleEndien.readUnsignedInt();
        assertTrue(val == 4294967295L);

        littleEndien.seek(0);
        final int val2 = littleEndien.readInt();
        assertTrue(val2 == -1);
    }
}
