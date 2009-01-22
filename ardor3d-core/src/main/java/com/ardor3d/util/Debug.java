/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
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
public class Debug {

    public static final boolean debug = (!"FALSE".equalsIgnoreCase(System.getProperty("ardor3d.debug")));

    public static final boolean stats = (System.getProperty("ardor3d.stats") != null);

    public static final boolean infoLogging = (System.getProperty("ardor3d.info") != null) ? (!"FALSE"
            .equalsIgnoreCase(System.getProperty("ardor3d.info"))) : true;

    public static boolean updateGraphs = false;

    public static final boolean trackDirectMemory = (System.getProperty("ardor3d.trackDirect") != null);

    public static final boolean useMathPools = (System.getProperty("ardor3d.useMathPools") == null);

}
