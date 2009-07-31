/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

public class DisplaySettings {
    private final int _width;
    private final int _height;
    private final int _colorDepth;
    private final int _frequency;
    private final int _alphaBits;
    private final int _depthBits;
    private final int _stencilBits;
    private final int _samples;
    private final boolean _fullScreen;
    private final boolean _stereo;
    private final CanvasRenderer _shareContext;

    public DisplaySettings(final int width, final int height, final int depthBits, final int samples) {
        _width = width;
        _height = height;
        _colorDepth = 0;
        _frequency = 0;
        _alphaBits = 0;
        _depthBits = depthBits;
        _stencilBits = 0;
        _samples = samples;
        _fullScreen = false;
        _stereo = false;
        _shareContext = null;
    }

    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final boolean fullScreen) {
        _width = width;
        _height = height;
        _colorDepth = colorDepth;
        _frequency = frequency;
        _alphaBits = 0;
        _depthBits = 8;
        _stencilBits = 0;
        _samples = 0;
        _fullScreen = fullScreen;
        _stereo = false;
        _shareContext = null;
    }

    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final int alphaBits, final int depthBits, final int stencilBits, final int samples,
            final boolean fullScreen, final boolean stereo) {
        _width = width;
        _height = height;
        _colorDepth = colorDepth;
        _frequency = frequency;
        _alphaBits = alphaBits;
        _depthBits = depthBits;
        _stencilBits = stencilBits;
        _samples = samples;
        _fullScreen = fullScreen;
        _stereo = stereo;
        _shareContext = null;
    }

    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final int alphaBits, final int depthBits, final int stencilBits, final int samples,
            final boolean fullScreen, final boolean stereo, final CanvasRenderer shareContext) {
        _width = width;
        _height = height;
        _colorDepth = colorDepth;
        _frequency = frequency;
        _alphaBits = alphaBits;
        _depthBits = depthBits;
        _stencilBits = stencilBits;
        _samples = samples;
        _fullScreen = fullScreen;
        _stereo = stereo;
        _shareContext = shareContext;
    }

    public CanvasRenderer getShareContext() {
        return _shareContext;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public int getColorDepth() {
        return _colorDepth;
    }

    public int getFrequency() {
        return _frequency;
    }

    public int getAlphaBits() {
        return _alphaBits;
    }

    public int getDepthBits() {
        return _depthBits;
    }

    public int getStencilBits() {
        return _stencilBits;
    }

    public int getSamples() {
        return _samples;
    }

    public boolean isFullScreen() {
        return _fullScreen;
    }

    public boolean isStereo() {
        return _stereo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DisplaySettings that = (DisplaySettings) o;

        if (_colorDepth != that._colorDepth) {
            return false;
        }
        if (_frequency != that._frequency) {
            return false;
        }
        if (_fullScreen != that._fullScreen) {
            return false;
        }
        if (_height != that._height) {
            return false;
        }
        if (_width != that._width) {
            return false;
        }
        if (_alphaBits != that._alphaBits) {
            return false;
        }
        if (_depthBits != that._depthBits) {
            return false;
        }
        if (_stencilBits != that._stencilBits) {
            return false;
        }
        if (_samples != that._samples) {
            return false;
        }
        if (_stereo != that._stereo) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = 17;
        result = 31 * result + _height;
        result = 31 * result + _width;
        result = 31 * result + _colorDepth;
        result = 31 * result + _frequency;
        result = 31 * result + _alphaBits;
        result = 31 * result + _depthBits;
        result = 31 * result + _stencilBits;
        result = 31 * result + _samples;
        result = 31 * result + (_fullScreen ? 1 : 0);
        result = 31 * result + (_stereo ? 1 : 0);
        return result;
    }
}
