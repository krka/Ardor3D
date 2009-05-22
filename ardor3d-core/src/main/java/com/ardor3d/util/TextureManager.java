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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryImporter;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.google.common.collect.Maps;

/**
 * <code>TextureManager</code> provides static methods for building a <code>Texture</code> object. Typically, the
 * information supplied is the filename and the texture properties.
 */
final public class TextureManager {
    private static final Logger logger = Logger.getLogger(TextureManager.class.getName());

    private static WeakHashMap<TextureKey, Texture> m_tCache = new WeakHashMap<TextureKey, Texture>();

    private TextureManager() {}

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter string. Filter parameters are used to
     * define the filtering of the texture. If there is an error loading the file, null is returned.
     * 
     * @param file
     *            the filename of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     * @param imageType
     *            the type to use for image data
     * @param flipped
     *            If true, the images Y values are flipped.
     * @return the loaded texture. If there is a problem loading the texture, null is returned.
     */
    public static Texture load(final String file, final Texture.MinificationFilter minFilter,
            final Image.Format imageType, final boolean flipped) {
        return load(getTextureURL(file), minFilter, imageType, flipped);
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter url. Filter parameters are used to define
     * the filtering of the texture. If there is an error loading the file, null is returned.
     * 
     * @param file
     *            the url of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     * @param imageType
     *            the image type to use. if Image.Format.Guess, the type is determined by ardor3d. If S3TC/DXT is
     *            available we use that. if Image.Format.GuessNoCompression, the type is determined by ardor3d without
     *            using S3TC, even if available. See com.ardor3d.image.Image.Format for possible types.
     * @param flipped
     *            If true, the images Y values are flipped.
     * @return the loaded texture. If there is a problem loading the texture, null is returned.
     * @see Image.Format
     */
    public static Texture load(final URL file, final Texture.MinificationFilter minFilter,
            final Image.Format imageType, final boolean flipped) {

        if (null == file) {
            logger.warning("Could not load image...  URL was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }

        final String fileName = file.getFile();
        if (fileName == null) {
            logger.warning("Could not load image...  fileName was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }

        final TextureKey tkey = TextureKey.getKey(file, flipped, imageType, minFilter);

        return loadFromKey(tkey, null, null);
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter string. Filter parameters are used to
     * define the filtering of the texture. If there is an error loading the file, null is returned.
     * 
     * @param file
     *            the filename of the texture image.
     * @param minFilter
     *            the filter for the near values.
     * @param magFilter
     *            the filter for the far values.
     * @param imageType
     *            the type to use for image data
     * @param wasFlipped
     *            If true, the images Y values were flipped - for cache check purposes only.
     * @return the loaded texture. If there is a problem loading the texture, null is returned.
     */
    public static Texture loadFromImage(final Image image, final Texture.MinificationFilter minFilter,
            final Image.Format imageType, final boolean wasFlipped) {
        final TextureKey key = TextureKey.getKey(null, wasFlipped, imageType, "img_" + image.hashCode(), minFilter);
        return loadFromKey(key, image, null);
    }

    public static Texture loadFromKey(final TextureKey tkey, final Image imageData, final Texture store) {
        if (tkey == null) {
            logger.warning("TextureKey is null, cannot load");
            return TextureState.getDefaultTexture();
        }

        Texture result = store;

        // First look for the texture using the supplied key
        final Texture cache = findCachedTexture(tkey);

        if (cache != null) {
            // look into cache.
            if (result == null) {
                final Texture tClone = cache.createSimpleClone();
                if (tClone.getTextureKey() == null) {
                    tClone.setTextureKey(tkey);
                }
                return tClone;
            }
            cache.createSimpleClone(result);
            return result;
        }

        Image img = imageData;
        if (img == null) {
            img = loadImage(tkey);
        }

        if (null == img) {
            logger.warning("(image null) Could not load: "
                    + (tkey.getLocation() != null ? tkey.getLocation().getFile() : tkey.getFileType()));
            return TextureState.getDefaultTexture();
        }

        // Default to Texture2D
        if (result == null) {
            if (img.getData().size() == 6) {
                result = new TextureCubeMap();
            } else {
                result = new Texture2D();
            }
        }

        result.setTextureKey(tkey);
        result.setMinificationFilter(tkey.getMinificationFilter());
        result.setImage(img);
        if (tkey._location != null) {
            result.setImageLocation(tkey._location.toString());
        }

        // Cache the no-context version
        addToCache(result);
        return result;
    }

    private static Image loadImage(final TextureKey key) {
        if (key == null) {
            return null;
        }

        if ("savable".equalsIgnoreCase(key._fileType)) {
            Savable s;
            try {
                s = BinaryImporter.getInstance().load(key._location);
            } catch (final IOException e) {
                logger.log(Level.WARNING, "Could not load Savable.", e);
                return null;
            }
            if (s instanceof Image) {
                return (Image) s;
            }
            logger.warning("Savable not of type Image.");
            return TextureState.getDefaultTextureImage();
        }
        return ImageLoaderUtil.loadImage(key._location, key._flipped);
    }

    /**
     * Convert the provided String file name into a Texture URL, first attempting to use the {@link ResourceLocatorTool}
     * , then trying to load it as a direct file path.
     * 
     * @param file
     *            the file name
     * @return a URL
     */
    private static URL getTextureURL(final String file) {
        URL url = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, file);
        if (url == null) {
            try {
                url = new URL("file:" + file);
            } catch (final MalformedURLException e) {
                logger.logp(Level.SEVERE, TextureManager.class.toString(), "getTextureURL(file)", "Exception", e);
            }
        }
        return url;
    }

    public static void addToCache(final Texture t) {
        if (TextureState.getDefaultTexture() == null
                || (t != TextureState.getDefaultTexture() && t.getImage() != TextureState.getDefaultTextureImage())) {
            m_tCache.put(t.getTextureKey(), t);
        }
    }

    public static Texture removeFromCache(final TextureKey tk) {
        return m_tCache.remove(tk);
    }

    @MainThread
    public static boolean releaseTexture(final Texture texture, final Renderer deleter) {
        if (texture == null) {
            return false;
        }

        try {
            deleter.deleteTexture(texture);
        } catch (final Exception e) {
        } // ignore.
        return removeFromCache(texture.getTextureKey()) != null;
    }

    public static void registerForCleanup(final TextureKey textureKey) {
    // TODO: implement me.
    }

    @MainThread
    public static void doTextureCleanup(final Renderer deleter) {
    // TODO: implement me.
    }

    @MainThread
    public static void clearCache(final Renderer deleter) {
        final HashMap<TextureKey, Texture> map = Maps.newHashMap(m_tCache);
        for (final Texture t : map.values()) {
            releaseTexture(t, deleter);
        }
    }

    public static Texture findCachedTexture(final TextureKey textureKey) {
        return m_tCache.get(textureKey);
    }

    @MainThread
    public static void preloadCache(final Renderer r) {
        for (final Texture t : m_tCache.values()) {
            if (t.getTextureKey()._location != null) {
                r.loadTexture(t, 0);
            }
        }
    }
}
