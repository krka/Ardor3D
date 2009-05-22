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
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * <code>TextureKey</code> provides a way for the TextureManager to cache and retrieve <code>Texture</code> objects.
 */
final public class TextureKey implements Savable {

    protected URL _location = null;
    protected boolean _flipped;
    protected Image.Format _format = Image.Format.Guess;
    protected String _fileType;
    protected Texture.MinificationFilter _minFilter = MinificationFilter.Trilinear;
    protected final transient WeakHashMap<Object, Integer> _idCache = new WeakHashMap<Object, Integer>();
    protected int _code = Integer.MAX_VALUE;

    protected static final List<TextureKey> _keyCache = new ArrayList<TextureKey>();

    /** DO NOT USE. FOR SAVABLE USE ONLY */
    public TextureKey() {}

    private static AtomicInteger _uniqueTK = new AtomicInteger(Integer.MIN_VALUE);

    /**
     * Get a new unique TextureKey. This is meant for use by RTT and other situations where we know we are making a
     * unique texture.
     * 
     * @param minFilter
     *            our minification filter value.
     * @return the new TextureKey
     */
    public static synchronized TextureKey getRTTKey(final MinificationFilter minFilter) {
        int val = _uniqueTK.addAndGet(1);
        if (val == Integer.MAX_VALUE) {
            _uniqueTK.set(Integer.MIN_VALUE);
            val = Integer.MIN_VALUE;
        }
        return getKey(null, false, Format.Guess, "" + val, minFilter);
    }

    public static synchronized TextureKey getKey(final URL location, final boolean flipped,
            final Image.Format imageType, final Texture.MinificationFilter minFilter) {
        return getKey(location, flipped, imageType, null, minFilter);
    }

    public static synchronized TextureKey getKey(final URL location, final boolean flipped,
            final Image.Format imageType, final String fileType, final Texture.MinificationFilter minFilter) {
        final TextureKey key = new TextureKey();

        key._location = location;
        key._flipped = flipped;
        key._minFilter = minFilter;
        key._format = imageType;
        key._fileType = fileType;

        final int cacheLoc = _keyCache.indexOf(key);
        if (cacheLoc == -1) {
            _keyCache.add(key);
            TextureManager.registerForCleanup(key);
            return key;
        }

        return _keyCache.get(cacheLoc);
    }

    public static boolean removeKey(final TextureKey key) {
        return _keyCache.remove(key);
    }

    /**
     * @param glContext
     *            the object representing the OpenGL context a texture belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the texture id of a texture in the given context. If the texture is not found in the given context, 0 is
     *         returned.
     */
    public int getTextureIdForContext(final Object glContext) {
        if (_idCache.containsKey(glContext)) {
            return _idCache.get(glContext);
        }
        return 0;
    }

    /**
     * Sets the id for a texture in regards to the given OpenGL context.
     * 
     * @param glContext
     *            the object representing the OpenGL context a texture belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @param textureId
     *            the texture id of a texture. To be valid, this must be greater than 0.
     * @throws IllegalArgumentException
     *             if textureId is less than or equal to 0.
     */
    public void setTextureIdForContext(final Object glContext, final int textureId) {
        if (textureId <= 0) {
            throw new IllegalArgumentException("textureId must be > 0");
        }

        _idCache.put(glContext, textureId);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TextureKey)) {
            return false;
        }

        final TextureKey that = (TextureKey) other;
        if (_location == null) {
            if (that._location != null) {
                return false;
            }
        } else if (!_location.equals(that._location)) {
            return false;
        }

        if (_flipped != that._flipped) {
            return false;
        }
        if (_format != that._format) {
            return false;
        }
        if (_fileType == null && that._fileType != null) {
            return false;
        } else if (_fileType != null && !_fileType.equals(that._fileType)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (_code == Integer.MAX_VALUE) {

        }
        _code = 17;

        _code += 31 * _code + (_location != null ? _location.hashCode() : 0);
        _code += 31 * _code + (_fileType != null ? _fileType.hashCode() : 0);
        _code += 31 * _code + _minFilter.hashCode();
        _code += 31 * _code + _format.hashCode();
        _code += 31 * _code + (_flipped ? 1 : 0);

        return _code;
    }

    public Texture.MinificationFilter getMinificationFilter() {
        return _minFilter;
    }

    public Image.Format getFormat() {
        return _format;
    }

    /**
     * @return Returns the flipped.
     */
    public boolean isFlipped() {
        return _flipped;
    }

    /**
     * @return Returns the location.
     */
    public URL getLocation() {
        return _location;
    }

    public String getFileType() {
        return _fileType;
    }

    @Override
    public String toString() {
        final String x = "tkey: loc:" + _location + " flip: " + _flipped + " code: " + hashCode() + " imageType: "
                + _format + " fileType: " + _fileType;
        return x;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TextureKey> getClassTag() {
        return this.getClass();
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        if (_location != null) {
            capsule.write(_location.getProtocol(), "protocol", null);
            capsule.write(_location.getHost(), "host", null);
            capsule.write(_location.getFile(), "file", null);
        }
        capsule.write(_flipped, "flipped", false);
        capsule.write(_format, "format", Image.Format.Guess);
        capsule.write(_minFilter, "minFilter", MinificationFilter.Trilinear);
        capsule.write(_fileType, "fileType", null);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        final String protocol = capsule.readString("protocol", null);
        final String host = capsule.readString("host", null);
        final String file = capsule.readString("file", null);
        if (file != null) {
            _location = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, URLDecoder.decode(file,
                    "UTF-8"));
        }
        if (_location == null && protocol != null && host != null && file != null) {
            _location = new URL(protocol, host, file);
        }

        _flipped = capsule.readBoolean("flipped", false);
        _format = capsule.readEnum("format", Image.Format.class, Image.Format.Guess);
        _minFilter = capsule.readEnum("minFilter", MinificationFilter.class, MinificationFilter.Trilinear);
        _fileType = capsule.readString("fileType", null);
    }
}