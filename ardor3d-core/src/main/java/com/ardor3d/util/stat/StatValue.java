/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat;

public class StatValue {
    public double _val;
    public long _iterations;
    public double _average = 0;

    public StatValue(final double val, final long iterations) {
        _val = val;
        _iterations = iterations;
    }
}
