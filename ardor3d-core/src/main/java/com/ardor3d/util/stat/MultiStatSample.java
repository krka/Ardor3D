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

import java.util.HashMap;

public class MultiStatSample {
    public HashMap<StatType, StatValue> _values = new HashMap<StatType, StatValue>();
    public double _actualTime = 0.0;

    public static MultiStatSample createNew(final HashMap<StatType, StatValue> current) {
        final MultiStatSample rVal = new MultiStatSample();
        final double frames = current.containsKey(StatType.STAT_FRAMES) ? current.get(StatType.STAT_FRAMES)._val : 0;
        for (final StatType type : current.keySet()) {
            final StatValue entry = current.get(type);
            // only count values we've seen at
            // least 1 time from this sample set.
            if (entry._iterations != 0) {
                final StatValue store = new StatValue(entry._val, entry._iterations);
                if (frames > 0) {
                    store._average = store._val / frames;
                } else {
                    store._average = store._val / store._iterations;
                }
                rVal._values.put(type, store);
            }
        }
        return rVal;
    }

}
