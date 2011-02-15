package com.ardor3d.util;

import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class URLInputSupplier implements InputSupplier<InputStream> {
    private final URL url;

    public URLInputSupplier(URL url) {
        this.url = url;
    }

    @Override
    public InputStream getInput() throws IOException {
        return url.openStream();
    }
}
