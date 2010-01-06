/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.resource;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager class for locator utility classes used to find various assets. (XXX: Needs more documentation)
 */
public class ResourceLocatorTool {
    private static final Logger logger = Logger.getLogger(ResourceLocatorTool.class.getName());

    public static final String TYPE_TEXTURE = "texture";
    public static final String TYPE_MODEL = "model";
    public static final String TYPE_PARTICLE = "particle";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_SHADER = "shader";

    private static final Map<String, List<ResourceLocator>> _locatorMap = new HashMap<String, List<ResourceLocator>>();

    public static ResourceSource locateResource(final String resourceType, String resourceName) {
        if (resourceName == null) {
            return null;
        }

        try {
            resourceName = URLDecoder.decode(resourceName, "UTF-8");
        } catch (final UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        synchronized (_locatorMap) {
            final List<ResourceLocator> bases = _locatorMap.get(resourceType);
            if (bases != null) {
                for (int i = bases.size(); --i >= 0;) {
                    final ResourceLocator loc = bases.get(i);
                    final ResourceSource rVal = loc.locateResource(resourceName);
                    if (rVal != null) {
                        return rVal;
                    }
                }
            }
            // last resort...
            try {
                final URL u = ResourceLocatorTool.class.getResource(resourceName);
                if (u != null) {
                    return new URLResourceSource(u);
                }
            } catch (final Exception e) {
                logger.logp(Level.WARNING, ResourceLocatorTool.class.getName(), "locateResource(String, String)", e
                        .getMessage(), e);
            }

            logger.warning("Unable to locate: " + resourceName);
            return null;
        }
    }

    public static void addResourceLocator(final String resourceType, final ResourceLocator locator) {
        if (locator == null) {
            return;
        }
        synchronized (_locatorMap) {
            List<ResourceLocator> bases = _locatorMap.get(resourceType);
            if (bases == null) {
                bases = new ArrayList<ResourceLocator>();
                _locatorMap.put(resourceType, bases);
            }

            if (!bases.contains(locator)) {
                bases.add(locator);
            }
        }
    }

    public static boolean removeResourceLocator(final String resourceType, final ResourceLocator locator) {
        synchronized (_locatorMap) {
            final List<ResourceLocator> bases = _locatorMap.get(resourceType);
            if (bases == null) {
                return false;
            }
            return bases.remove(locator);
        }
    }
}
