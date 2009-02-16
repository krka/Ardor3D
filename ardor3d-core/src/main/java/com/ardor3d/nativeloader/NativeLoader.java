/**
 * Copyright (C) 2004 - 2009 Shopzilla, Inc. 
 * All rights reserved. Unauthorized disclosure or distribution is prohibited.
 */

package com.ardor3d.nativeloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * TODO: document this class!
 * 
 */
public class NativeLoader {
    private static final Logger logger = Logger.getLogger(NativeLoader.class.toString());
    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    private static final String PATH_SEPARATOR = "path.separator";
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    public static void makeLibrariesAvailable(final String[] libraryPaths) {
        boolean fail = false;
        for (final String s : libraryPaths) {
            try {
                makeLibraryAvailable(s);
            } catch (final IOException e) {
                fail = true;
                // logger.log(Level.SEVERE, "Fail", e);
            }
        }

        if (fail) {
            throw new RuntimeException("Failed to make libraries available");
        }
    }

    private static void makeLibraryAvailable(final String libraryPath) throws IOException {
        // logger.info("Making library: " + libraryPath + " available");

        final URL resourceURL = NativeLoader.class.getResource(libraryPath);

        if (resourceURL == null) {
            throw new IOException("Unable to locate library resource '" + libraryPath + "' in classpath");
        }

        final File a3DNativeDirectory = getA3DNativeDirectory();
        final File outFile = createFileFor(a3DNativeDirectory, libraryPath);

        final InputStream in = resourceURL.openStream();
        final OutputStream out = new FileOutputStream(outFile);

        final byte[] buffer = new byte[65536];

        int totalBytesRead = 0;

        while (true) {
            final int bytesRead = in.read(buffer);

            if (bytesRead < 0) {
                break;
            }

            out.write(buffer, 0, bytesRead);

            totalBytesRead += bytesRead;
        }

        in.close();
        out.close();

        // logger.info("Copied " + totalBytesRead + " bytes into file: " + outFile);

        // addToLibraryPath(a3DNativeDirectory);
    }

    private static void addToLibraryPath(final File a3DNativeDirectory) {
        final String currentPath = System.getProperty(JAVA_LIBRARY_PATH);

        if (currentPath.contains(a3DNativeDirectory.toString())) {
            // logger.fine("a3d directory already included in " + JAVA_LIBRARY_PATH + ": " + currentPath);
            return;
        }

        System.setProperty(JAVA_LIBRARY_PATH, currentPath + System.getProperty(PATH_SEPARATOR)
                + a3DNativeDirectory.toString());
        // logger.info("Library path now: '" + System.getProperty(JAVA_LIBRARY_PATH));
    }

    private static File createFileFor(final File a3dNativeDirectory, final String libraryPath) throws IOException {
        // File result = new File(a3dNativeDirectory, getFilePart(libraryPath));
        final File result = new File((File) null, getFilePart(libraryPath));

        if (result.exists()) {
            if (!result.delete()) {
                throw new IOException("Unable to delete existing file: " + result);
            }
        }

        if (!result.createNewFile()) {
            throw new IOException("Unable to create file: " + result);
        }

        // flag this for deletion when the JVM terminates. This is a bit shaky; the file won't be deleted if
        // the JVM terminates unexpectedly
        result.deleteOnExit();

        return result;
    }

    private static File getA3DNativeDirectory() throws IOException {
        final File tempDir = new File(System.getProperty(JAVA_IO_TMPDIR));

        if (!(tempDir.exists() && tempDir.isDirectory())) {
            throw new IllegalStateException("Temporary directory: " + tempDir.toString()
                    + " doesn't exist or isn't a directory");
        }

        final File a3dnativeDirectory = new File(tempDir, "a3dnatives");

        if (!a3dnativeDirectory.exists()) {
            if (!a3dnativeDirectory.mkdir()) {
                throw new IOException("Unable to create native directory: " + a3dnativeDirectory);
            }
        }
        return a3dnativeDirectory;
    }

    private static String getFilePart(final String libraryPath) {
        final String[] parts = libraryPath.split("/");

        return parts[parts.length - 1];
    }
}
