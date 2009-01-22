/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat.graph;

import java.util.HashMap;
import java.util.TreeMap;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.Texture2D;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.util.stat.StatListener;
import com.ardor3d.util.stat.StatType;

/**
 * Base class for graphers.
 */
public abstract class AbstractStatGrapher implements StatListener {

    protected TextureRenderer texRenderer;
    protected Texture2D tex;
    protected int gWidth, gHeight;

    protected TreeMap<StatType, HashMap<String, Object>> config = new TreeMap<StatType, HashMap<String, Object>>();

    protected boolean enabled = true;

    /**
     * Must be constructed in the GL thread.
     * 
     * @param factory
     */
    public AbstractStatGrapher(final int width, final int height, final Renderer renderer,
            final ContextCapabilities caps) {
        gWidth = width;
        gHeight = height;
        // prepare our TextureRenderer
        final DisplaySettings settings = new DisplaySettings(width, height, 0, 0, 0, 8, 0, 0, false, false);
        texRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(settings, renderer, caps,
                TextureRenderer.Target.Texture2D);

        if (texRenderer != null) {
            texRenderer.setBackgroundColor(new ColorRGBA(ColorRGBA.BLACK));
        }
    }

    // - set a texture for offscreen rendering
    public void setTexture(final Texture2D tex) {
        texRenderer.setupTexture(tex);
        this.tex = tex;
    }

    public TextureRenderer getTexRenderer() {
        return texRenderer;
    }

    public void clearConfig() {
        config.clear();
    }

    public void clearConfig(final StatType type) {
        if (config.get(type) != null) {
            config.get(type).clear();
        }
    }

    public void clearConfig(final StatType type, final String key) {
        if (config.get(type) != null) {
            config.get(type).remove(key);
        }
    }

    public void addConfig(final StatType type, final HashMap<String, Object> configs) {
        config.put(type, configs);
    }

    public void addConfig(final StatType type, final String key, final Object value) {
        HashMap<String, Object> vals = config.get(type);
        if (vals == null) {
            vals = new HashMap<String, Object>();
            config.put(type, vals);
        }
        vals.put(key, value);
    }

    protected ColorRGBA getColorConfig(final StatType type, final String configName, final ColorRGBA defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof ColorRGBA) {
                return (ColorRGBA) val;
            }
        }
        return defaultVal;
    }

    protected String getStringConfig(final StatType type, final String configName, final String defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof String) {
                return (String) val;
            }
        }
        return defaultVal;
    }

    protected short getShortConfig(final StatType type, final String configName, final short defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).shortValue();
            }
        }
        return defaultVal;
    }

    protected int getIntConfig(final StatType type, final String configName, final int defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        }
        return defaultVal;
    }

    protected long getLongConfig(final StatType type, final String configName, final long defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
        }
        return defaultVal;
    }

    protected float getFloatConfig(final StatType type, final String configName, final float defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).floatValue();
            }
        }
        return defaultVal;
    }

    protected double getDoubleConfig(final StatType type, final String configName, final double defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            }
        }
        return defaultVal;
    }

    protected boolean getBooleanConfig(final StatType type, final String configName, final boolean defaultVal) {
        final HashMap<String, Object> vals = config.get(type);
        if (vals != null && vals.containsKey(configName)) {
            final Object val = vals.get(configName);
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
        }
        return defaultVal;
    }

    public boolean hasConfig(final StatType type) {
        return config.containsKey(type) && !config.get(type).isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Called when the graph needs to be reset back to the original display state. (iow, remove all points, lines, etc.)
     */
    public abstract void reset();
}
