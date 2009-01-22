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

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
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
    protected Texture.MinificationFilter _minFilter = MinificationFilter.Trilinear;
    protected Image.Format _format = Image.Format.Guess;
    protected String _fileType;
    protected transient Object _glContextRep = null;

    public TextureKey() {}

    public TextureKey(final URL location, final boolean flipped, final Image.Format imageType,
            final Texture.MinificationFilter minFilter) {
        this._location = location;
        _flipped = flipped;
        _minFilter = minFilter;
        _format = imageType;
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
        if (_glContextRep == null) {
            if (that._glContextRep != null) {
                return false;
            }
        } else if (!_glContextRep.equals(that._glContextRep)) {
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
        int result = 17;

        result += 31 * result + (_location != null ? _location.hashCode() : 0);
        result += 31 * result + (_fileType != null ? _fileType.hashCode() : 0);
        result += 31 * result + _minFilter.hashCode();
        result += 31 * result + _format.hashCode();
        result += 31 * result + (_flipped ? 1 : 0);
        result += 31 * result + (_glContextRep != null ? _glContextRep.hashCode() : 0);

        return result;
    }

    public Texture.MinificationFilter getMinificationFilter() {
        return _minFilter;
    }

    public void setMinificationFilter(final Texture.MinificationFilter minFilter) {
        _minFilter = minFilter;
    }

    public Object getContextRep() {
        return _glContextRep;
    }

    public void setContextRep(final Object contextRep) {
        _glContextRep = contextRep;
    }

    public Image.Format getFormat() {
        return _format;
    }

    public void setFormat(final Image.Format format) {
        _format = format;
    }

    /**
     * @return Returns the flipped.
     */
    public boolean isFlipped() {
        return _flipped;
    }

    /**
     * @param flipped
     *            The flipped to set.
     */
    public void setFlipped(final boolean flipped) {
        _flipped = flipped;
    }

    /**
     * @return Returns the location.
     */
    public URL getLocation() {
        return _location;
    }

    /**
     * @param location
     *            The location to set.
     */
    public void setLocation(final URL location) {
        this._location = location;
    }

    public String getFileType() {
        return _fileType;
    }

    public void setFileType(final String fileType) {
        _fileType = fileType;
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