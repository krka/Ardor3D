/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.resource;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

/**
 * This class extends the behavior of the {@link SimpleResourceLocator} by appending different file extensions to the
 * resource name, if it cannot find a resource with the extension specified in the path name.
 */
public class MultiFormatResourceLocator extends SimpleResourceLocator {

    private final String[] _extensions;
    private boolean _trySpecifiedFormatFirst = false;

    public MultiFormatResourceLocator(final URI baseDir) {
        this(baseDir, ".dds", ".tga", ".png", ".jpg", ".gif");
    }

    public MultiFormatResourceLocator(final URL baseDir) throws URISyntaxException {
        this(baseDir, ".dds", ".tga", ".png", ".jpg", ".gif");
    }

    public MultiFormatResourceLocator(final URI baseDir, final String... extensions) {
        super(baseDir);

        if (extensions == null) {
            throw new NullPointerException("extensions can not be null.");
        }
        _extensions = extensions;
    }

    public MultiFormatResourceLocator(final URL baseDir, final String... extensions) throws URISyntaxException {
        this(baseDir.toURI(), extensions);
    }

    @Override
    public URL locateResource(final String resourceName) {
        if (_trySpecifiedFormatFirst) {
            final URL u = super.locateResource(resourceName);
            if (u != null) {
                return u;
            }
        }

        final String baseFileName = getBaseFileName(resourceName);
        for (final String extension : _extensions) {
            final URL u = super.locateResource(baseFileName + extension);
            if (u != null) {
                return u;
            }
        }

        if (!_trySpecifiedFormatFirst) {
            // If all else fails, just try the original name.
            return super.locateResource(resourceName);
        } else {
            return null;
        }
    }

    private String getBaseFileName(final String resourceName) {
        final File f = new File(resourceName);
        final String name = f.getPath();
        final int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name;
        } else {
            return name.substring(0, dot);
        }
    }

    public boolean isTrySpecifiedFormatFirst() {
        return _trySpecifiedFormatFirst;
    }

    public void setTrySpecifiedFormatFirst(final boolean trySpecifiedFormatFirst) {
        _trySpecifiedFormatFirst = trySpecifiedFormatFirst;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MultiFormatResourceLocator) {
            return _baseDir.equals(((MultiFormatResourceLocator) obj)._baseDir)
                    && Arrays.equals(_extensions, ((MultiFormatResourceLocator) obj)._extensions);
        }
        return super.equals(obj);
    }
}
