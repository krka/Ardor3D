/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.image.Image;
import com.ardor3d.renderer.state.TextureState;

public abstract class ImageLoaderUtil {
    private static final Logger logger = Logger.getLogger(ImageLoaderUtil.class.getName());

    private static Map<String, ImageLoader> loaders = Collections.synchronizedMap(new HashMap<String, ImageLoader>());

    static {
        registerHandler(".DDS", new DdsLoader());
        registerHandler(".TGA", new TgaLoader());
    }

    public static Image loadImage(final URL file, final boolean flipped) {
        if (file == null) {
            logger.warning("loadImage(URL file, boolean flipped): file is null, defaultTexture used.");
            return TextureState.getDefaultTextureImage();
        }

        final String fileName = file.getFile();
        if (fileName == null) {
            logger.warning("loadImage(URL file, boolean flipped): fileName is null, defaultTexture used.");
            return TextureState.getDefaultTextureImage();
        }

        final int dot = fileName.lastIndexOf('.');
        final String fileExt = dot >= 0 ? fileName.substring(dot) : "";
        InputStream is = null;
        try {
            is = file.openStream();
            logger.finer("loadImage(URL file, boolean flipped) opened URL: " + file);
            return loadImage(fileExt, is, flipped);
        } catch (final IOException e) {
            logger.log(Level.WARNING, "loadImage(URL file, boolean flipped): defaultTexture used", e);
            return TextureState.getDefaultTextureImage();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException ioe) {
                } // ignore
            }
        }
    }

    public static Image loadImage(final String fileExt, final InputStream stream, final boolean flipped) {

        Image imageData = null;
        try {
            final ImageLoader loader = loaders.get(fileExt.toLowerCase());
            if (loader != null) {
                imageData = loader.load(stream, flipped);
            } else {
                logger.warning("Unable to read image of type: " + fileExt.toLowerCase());
            }
            if (imageData == null) {
                logger
                        .warning("loadImage(String fileExt, InputStream stream, boolean flipped): no imageData found.  defaultTexture used.");
                imageData = TextureState.getDefaultTextureImage();
            }
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Could not load Image.", e);
            imageData = TextureState.getDefaultTextureImage();
        }
        return imageData;
    }

    /**
     * Register an ImageLoader to handle all files with a specific extention. An ImageLoader can be registered to handle
     * several formats without problems.
     * 
     * @param format
     *            The file extention for the format this ImageLoader will handle. Make sure to include the dot (eg.
     *            ".BMP"). This value is case insensitive (".Bmp" will register for ".BMP", ".bmp", etc.)
     * @param handler
     */
    public static void registerHandler(final String format, final ImageLoader handler) {
        loaders.put(format.toLowerCase(), handler);
    }

    public static void unregisterHandler(final String format) {
        loaders.remove(format.toLowerCase());
    }
}
