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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * This locator takes a base URL for finding resources specified with a relative path. If it cannot find the path
 * relative to the URL, it successively omits the starting components of the relative path until it can find a resources
 * with such a trimmed path. If no resource is found with this method null is returned.
 */
public class SimpleResourceLocator implements ResourceLocator {

    protected URI baseDir;

    public SimpleResourceLocator(final URI baseDir) {
        if (baseDir == null) {
            throw new NullPointerException("baseDir can not be null.");
        }
        this.baseDir = baseDir;
    }

    public SimpleResourceLocator(final URL baseDir) throws URISyntaxException {
        if (baseDir == null) {
            throw new NullPointerException("baseDir can not be null.");
        }
        this.baseDir = baseDir.toURI();
    }

    public URL locateResource(String resourceName) {
        // Trim off any prepended local dir.
        while (resourceName.startsWith("./") && resourceName.length() > 2) {
            resourceName = resourceName.substring(2);
        }
        while (resourceName.startsWith(".\\") && resourceName.length() > 2) {
            resourceName = resourceName.substring(2);
        }

        // Try to locate using resourceName as is.
        try {
            String spec = URLEncoder.encode(resourceName, "UTF-8");
            // this fixes a bug in JRE1.5 (file handler does not decode "+" to spaces)
            spec = spec.replaceAll("\\+", "%20");

            final URL rVal = new URL(baseDir.toURL(), spec);
            // open a stream to see if this is a valid resource
            // XXX: Perhaps this is wasteful? Also, what info will determine validity?
            rVal.openStream().close();
            return rVal;
        } catch (final IOException e) {
            // URL wasn't valid in some way, so try up a path.
        } catch (final IllegalArgumentException e) {
            // URL wasn't valid in some way, so try up a path.
        }

        resourceName = trimResourceName(resourceName);
        if (resourceName == null) {
            return null;
        } else {
            return locateResource(resourceName);
        }
    }

    protected String trimResourceName(final String resourceName) {
        // we are sure this is part of a URL so using slashes only is fine:
        final int firstSlashIndex = resourceName.indexOf('/');
        if (firstSlashIndex >= 0 && firstSlashIndex < resourceName.length() - 1) {
            return resourceName.substring(firstSlashIndex + 1);
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof SimpleResourceLocator) {
            return baseDir.equals(((SimpleResourceLocator) obj).baseDir);
        }
        return false;
    }
}
