/**
 * Copyright (C) 2004 - 2009 Shopzilla, Inc. 
 * All rights reserved. Unauthorized disclosure or distribution is prohibited.
 */

package com.ardor3d.framework.lwjgl;

/**
 * TODO: document this class!
 *
 */
public enum LwjglLibraryPaths {
    MACOSX("Mac OS X", null, new String[] {
            "/macosx/libjinput-osx.jnilib",
            "/macosx/liblwjgl.jnilib",
            "/macosx/openal.dylib",
    }),
    WINDOWS_XP("Windows XP", null, new String[] {
            "/win32/jinput-dx8.dll",
            "/win32/jinput-raw.dll",
            "/win32/lwjgl.dll",
            "/win32/OpenAL32.dll",
    });
    
    private final String _operatingSystem;
    private final String _architecture;
    private final String[] _libraryPaths;


    LwjglLibraryPaths(String operatingSystem, String architecture, String[] libraryPaths) {
        _operatingSystem = operatingSystem;
        _architecture = architecture;
        _libraryPaths = libraryPaths;
    }

    public static String[] getLibraryPaths(String operatingSystem, String architecture) {
        for (LwjglLibraryPaths libraryPath : values()) {
            if (operatingSystem.equals(libraryPath._operatingSystem) &&
                    (libraryPath._architecture == null || architecture.equals(libraryPath._architecture))) {
                return libraryPath._libraryPaths;
            }
        }

        throw new IllegalStateException("No matching set of library paths found for " + operatingSystem + ", " + architecture);
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("os.name"));
        System.out.println(System.getProperty("os.arch"));

        System.getProperties();
    }
}
