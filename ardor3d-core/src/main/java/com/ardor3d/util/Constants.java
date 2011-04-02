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

/**
 * Just a simple flag holder for runtime stripping of various ardor3d logging and debugging features.
 */
public class Constants {

    public static boolean updateGraphs = false;

    public static final boolean stats;

    public static final boolean trackDirectMemory;

    public static final boolean useMultipleContexts;

    public static final boolean useMathPools;

    public static final boolean useFastMath;

    public static final boolean storeSavableImages;

    public static final int maxPoolSize;

    public static final boolean useValidatingTransform;

    static {
        boolean hasPropertyAccess = true;
        try {
            System.getSecurityManager().checkPropertiesAccess();
        } catch (final SecurityException e) {
            hasPropertyAccess = false;
        }

        if (hasPropertyAccess) {
            stats = (System.getProperty("ardor3d.stats") != null);
            trackDirectMemory = (System.getProperty("ardor3d.trackDirect") != null);
            useMultipleContexts = (System.getProperty("ardor3d.useMultipleContexts") != null);
            useMathPools = (System.getProperty("ardor3d.noMathPools") == null);
            useFastMath = (System.getProperty("ardor3d.useFastMath") != null);
            storeSavableImages = (System.getProperty("ardor3d.storeSavableImages") != null);
            maxPoolSize = (System.getProperty("ardor3d.maxPoolSize") != null ? Integer.parseInt(System
                    .getProperty("ardor3d.maxPoolSize")) : 11);

            useValidatingTransform = (System.getProperty("ardor3d.disableValidatingTransform") == null);
        } else {
            stats = false;
            trackDirectMemory = false;
            useMultipleContexts = false;
            useMathPools = true;
            useFastMath = false;
            storeSavableImages = false;
            maxPoolSize = 11;
            useValidatingTransform = true;
        }
    }
}
