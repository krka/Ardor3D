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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.util.UrlUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class URLResourceSource implements ResourceSource {
    private static final Logger logger = Logger.getLogger(URLResourceSource.class.getName());

    private URL _url;
    private String _type;

    /**
     * Construct a new URLResourceSource. Must set URL separately.
     */
    public URLResourceSource() {}

    /**
     * Construct a new URLResourceSource from a specific URL.
     * 
     * @param sourceUrl
     *            The url to load the resource from. Must not be null. If the URL has a valid URL filename (see
     *            {@link URL#getFile()}) and an extension (eg. http://url/myFile.png) then the extension (.png in this
     *            case) is used as the type.
     */
    public URLResourceSource(final URL sourceUrl) {
        assert (sourceUrl != null) : "sourceUrl must not be null";
        _url = sourceUrl;

        // add type, if present
        final String fileName = _url.getFile();
        if (fileName != null) {
            final int dot = fileName.lastIndexOf('.');
            if (dot >= 0) {
                _type = fileName.substring(dot);
            } else {
                _type = UNKNOWN_TYPE;
            }
        }
    }

    /**
     * Construct a new URLResourceSource from a specific URL and type.
     * 
     * @param sourceUrl
     *            The url to load the resource from. Must not be null.
     * @param type
     *            our type. Usually a file extension such as .png. Required for generic loading when multiple resource
     *            handlers could be used.
     */
    public URLResourceSource(final URL sourceUrl, final String type) {
        assert (sourceUrl != null) : "sourceUrl must not be null";
        _url = sourceUrl;

        _type = type;
    }

    public ResourceSource getRelativeSource(final String name) {
        try {
            final URL srcURL = UrlUtils.resolveRelativeURL(_url, "./" + name);
            if (srcURL != null) {
                // check if the URL can be opened
                // just force it to try to grab info
                srcURL.openStream().close();
                // Ok satisfied... return
                return new URLResourceSource(srcURL);

            }
        } catch (final MalformedURLException ex) {
        } catch (final IOException ex) {
        }
        if (logger.isLoggable(Level.WARNING)) {
            logger.logp(Level.WARNING, getClass().getName(), "getRelativeSource(String)",
                    "Unable to find relative file {0} from {1}.", new Object[] { name, _url });
        }
        return null;
    }

    public void setURL(final URL url) {
        _url = url;
    }

    public URL getURL() {
        return _url;
    }

    public String getName() {
        return _url.toString();
    }

    public String getType() {
        return _type;
    }

    public void setType(final String type) {
        _type = type;
    }

    public InputStream openStream() throws IOException {
        return _url.openStream();
    }

    /**
     * @return the string representation of this vector.
     */
    @Override
    public String toString() {
        return "URLResourceSource [url=" + _url + ", type=" + _type + "]";
    }

    /**
     * @return returns a unique code for this vector object based on its values. If two vectors are numerically equal,
     *         they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + _url.hashCode();
        result += 31 * result + _type.hashCode();

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this vector and the provided vector have the same x, y and z values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof URLResourceSource)) {
            return false;
        }
        final URLResourceSource comp = (URLResourceSource) o;
        return _type.equals(comp._type) && _url.equals(comp._url);
    }

    public Class<?> getClassTag() {
        return URLResourceSource.class;
    }

    public void read(final InputCapsule capsule) throws IOException {
        final String protocol = capsule.readString("protocol", null);
        final String host = capsule.readString("host", null);
        final String file = capsule.readString("file", null);
        if (file != null) {
            // see if we would like to divert this to a new location.
            final ResourceSource src = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, URLDecoder
                    .decode(file, "UTF-8"));
            if (src instanceof URLResourceSource) {
                _url = ((URLResourceSource) src)._url;
                _type = ((URLResourceSource) src)._type;
                return;
            }
        }

        if (_url == null && protocol != null && host != null && file != null) {
            _url = new URL(protocol, host, file);
        }

        _type = capsule.readString("type", null);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_url.getProtocol(), "protocol", null);
        capsule.write(_url.getHost(), "host", null);
        capsule.write(_url.getFile(), "file", null);

        capsule.write(_type, "type", null);
    }
}
