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

import java.lang.ref.ReferenceQueue;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;

/**
 * <code>TextureManager</code> provides static methods for building a <code>Texture</code> object. Typically, the
 * information supplied is the filename and the texture properties.
 */
final public class TextureManager {
    private static final Logger logger = Logger.getLogger(TextureManager.class.getName());

    private static Map<TextureKey, Texture> _tCache = new MapMaker().weakKeys().weakValues().makeMap();

    private static ReferenceQueue<TextureKey> _textureRefQueue = new ReferenceQueue<TextureKey>();

    private TextureManager() {}

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter string. Filter parameters are used to
     * define the filtering of the texture. If there is an error loading the file, null is returned.
     * 
     * @param name
     *            the name of the texture image.
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
    public static Texture load(final String name, final Texture.MinificationFilter minFilter,
            final Image.Format imageType, final boolean flipped) {
        return load(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, name), minFilter, imageType,
                flipped);
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter url. Filter parameters are used to define
     * the filtering of the texture. If there is an error loading the file, null is returned.
     * 
     * @param source
     *            the source of the texture image.
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
    public static Texture load(final ResourceSource source, final Texture.MinificationFilter minFilter,
            final Image.Format imageType, final boolean flipped) {

        if (null == source) {
            logger.warning("Could not load image...  source was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }

        final TextureKey tkey = TextureKey.getKey(source, flipped, imageType, minFilter);

        return loadFromKey(tkey, null, null);
    }

    /**
     * <code>loadTexture</code> loads a new texture defined by the parameter url. Filter parameters are used to define
     * the filtering of the texture. If there is an error loading the file, null is returned.
     * 
     * @param loc
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
    public static Texture load(final URL loc, final Texture.MinificationFilter minFilter, final Image.Format imageType,
            final boolean flipped) {

        if (null == loc) {
            logger.warning("Could not load image...  URL was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }

        final String fileName = loc.getFile();
        if (fileName == null) {
            logger.warning("Could not load image...  URL's fileName was null. defaultTexture used.");
            return TextureState.getDefaultTexture();
        }

        final TextureKey tkey = TextureKey.getKey(new URLResourceSource(loc), flipped, imageType, minFilter);

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
            img = ImageLoaderUtil.loadImage(tkey.getSource(), tkey.isFlipped());
        }

        if (null == img) {
            logger.warning("(image null) Could not load: " + tkey.getSource());
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

        // Cache the no-context version
        addToCache(result);
        return result;
    }

    public static void addToCache(final Texture t) {
        if (TextureState.getDefaultTexture() == null
                || (t != TextureState.getDefaultTexture() && t.getImage() != TextureState.getDefaultTextureImage())) {
            _tCache.put(t.getTextureKey(), t);
        }
    }

    public static Texture findCachedTexture(final TextureKey textureKey) {
        return _tCache.get(textureKey);
    }

    public static Texture removeFromCache(final TextureKey tk) {
        return _tCache.remove(tk);
    }

    /**
     * Delete all textures from card. This will gather all texture ids believed to be on the card and try to delete
     * them. If a deleter is passed in, textures that are part of the currently active context (if one is active) will
     * be deleted immediately. If a deleter is not passed in, we do not have an active context, or we encounter textures
     * that are not part of the current context, then we will queue those textures to be deleted later using the
     * GameTaskQueueManager.
     * 
     * @param deleter
     *            if not null, this renderer will be used to immediately delete any textures in the currently active
     *            context. All other textures will be queued to delete in their own contexts.
     */
    public static void cleanAllTextures(final Renderer deleter) {
        cleanAllTextures(deleter, null);
    }

    /**
     * Delete all textures from card. This will gather all texture ids believed to be on the card and try to delete
     * them. If a deleter is passed in, textures that are part of the currently active context (if one is active) will
     * be deleted immediately. If a deleter is not passed in, we do not have an active context, or we encounter textures
     * that are not part of the current context, then we will queue those textures to be deleted later using the
     * GameTaskQueueManager.
     * 
     * If a non null map is passed into futureStore, it will be populated with Future objects for each queued context.
     * These objects may be used to discover when the deletion tasks have all completed.
     * 
     * @param deleter
     *            if not null, this renderer will be used to immediately delete any textures in the currently active
     *            context. All other textures will be queued to delete in their own contexts.
     * @param futureStore
     *            if not null, this map will be populated with any Future task handles created during cleanup.
     */
    public static void cleanAllTextures(final Renderer deleter, final Map<Object, Future<Void>> futureStore) {
        // gather up expired textures... these don't exist in our cache
        Multimap<Object, Integer> idMap = gatherGCdIds();

        // Walk through the cached items and gather those too.
        for (final TextureKey key : _tCache.keySet()) {
            // possibly lazy init
            if (idMap == null) {
                idMap = ArrayListMultimap.create();
            }

            if (Constants.useMultipleContexts) {
                final Set<Object> contextObjects = key.getContextObjects();
                for (final Object o : contextObjects) {
                    // Add id to map
                    idMap.put(o, key.getTextureIdForContext(o));
                }
            } else {
                idMap.put(ContextManager.getCurrentContext().getGlContextRep(), key.getTextureIdForContext(null));
            }
        }

        // delete the ids
        if (!idMap.isEmpty()) {
            handleTextureDelete(deleter, idMap, futureStore);
        }
    }

    /**
     * Delete any textures from the card that have been recently garbage collected in Java. If a deleter is passed in,
     * gc'd textures that are part of the currently active context (if one is active) will be deleted immediately. If a
     * deleter is not passed in, we do not have an active context, or we encounter gc'd textures that are not part of
     * the current context, then we will queue those textures to be deleted later using the GameTaskQueueManager.
     * 
     * @param deleter
     *            if not null, this renderer will be used to immediately delete any gc'd textures in the currently
     *            active context. All other gc'd textures will be queued to delete in their own contexts.
     */
    public static void cleanExpiredTextures(final Renderer deleter) {
        cleanExpiredTextures(deleter, null);
    }

    /**
     * Delete any textures from the card that have been recently garbage collected in Java. If a deleter is passed in,
     * gc'd textures that are part of the currently active context (if one is active) will be deleted immediately. If a
     * deleter is not passed in, we do not have an active context, or we encounter gc'd textures that are not part of
     * the current context, then we will queue those textures to be deleted later using the GameTaskQueueManager.
     * 
     * If a non null map is passed into futureStore, it will be populated with Future objects for each queued context.
     * These objects may be used to discover when the deletion tasks have all completed.
     * 
     * @param deleter
     *            if not null, this renderer will be used to immediately delete any gc'd textures in the currently
     *            active context. All other gc'd textures will be queued to delete in their own contexts.
     * @param futureStore
     *            if not null, this map will be populated with any Future task handles created during cleanup.
     */
    public static void cleanExpiredTextures(final Renderer deleter, final Map<Object, Future<Void>> futureStore) {
        // gather up expired textures...
        final Multimap<Object, Integer> idMap = gatherGCdIds();

        // send to be deleted on next render.
        if (idMap != null) {
            handleTextureDelete(deleter, idMap, futureStore);
        }
    }

    @SuppressWarnings("unchecked")
    private static Multimap<Object, Integer> gatherGCdIds() {
        Multimap<Object, Integer> idMap = null;
        // Pull all expired textures from ref queue and add to an id multimap.
        ContextIdReference<TextureKey> ref;
        while ((ref = (ContextIdReference<TextureKey>) _textureRefQueue.poll()) != null) {
            // lazy init
            if (idMap == null) {
                idMap = ArrayListMultimap.create();
            }
            if (Constants.useMultipleContexts) {
                final Set<Object> contextObjects = ref.getContextObjects();
                for (final Object o : contextObjects) {
                    // Add id to map
                    idMap.put(o, ref.get(o));
                }
            } else {
                idMap.put(ContextManager.getCurrentContext().getGlContextRep(), ref.get(null));
            }
            ref.clear();
        }
        return idMap;
    }

    private static void handleTextureDelete(final Renderer deleter, final Multimap<Object, Integer> idMap,
            final Map<Object, Future<Void>> futureStore) {
        Object currentGLRef = null;
        // Grab the current context, if any.
        if (deleter != null && ContextManager.getCurrentContext() != null) {
            currentGLRef = ContextManager.getCurrentContext().getGlContextRep();
        }
        // For each affected context...
        for (final Object glref : idMap.keySet()) {
            // If we have a deleter and the context is current, immediately delete
            if (deleter != null && glref.equals(currentGLRef)) {
                deleter.deleteTextureIds(idMap.get(glref));
            }
            // Otherwise, add a delete request to that context's render task queue.
            else {
                final Future<Void> future = GameTaskQueueManager.getManager(ContextManager.getContextForRef(glref))
                        .render(new RendererCallable<Void>() {
                            public Void call() throws Exception {
                                getRenderer().deleteTextureIds(idMap.get(glref));
                                return null;
                            }
                        });
                if (futureStore != null) {
                    futureStore.put(glref, future);
                }
            }
        }
    }

    @MainThread
    public static void preloadCache(final Renderer r) {
        for (final Texture t : _tCache.values()) {
            if (t == null) {
                continue;
            }
            if (t.getTextureKey().getSource() != null) {
                r.loadTexture(t, 0);
            }
        }
    }

    static ReferenceQueue<TextureKey> getRefQueue() {
        return _textureRefQueue;
    }
}
