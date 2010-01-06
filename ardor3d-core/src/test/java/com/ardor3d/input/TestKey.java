/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestKey {
    @Test
    public void testFindByCode1() throws Exception {
        final Key a = Key.findByCode(Key.A.getCode());

        assertEquals("a found", Key.A, a);
    }

    @Test(expected = KeyNotFoundException.class)
    public void testFindByCode2() throws Exception {
        Key.findByCode(-14);
    }
}
