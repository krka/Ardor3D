/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.xpath.XPath;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * A thread local singleton containing caches for various items such as XPath patterns, data read from a Collada file,
 * vertex mappings and so forth. In general this is to decrease parsing time.
 */
public final class GlobalData {

    private final Map<String, Element> _boundMaterials;
    private final Map<String, Texture> _textures;
    private final Map<String, Element> _idCache;
    private final Map<String, Element> _sidCache;
    private final Map<String, XPath> _xPathExpressions;
    private final Pattern _pattern;
    private final List<String> _transformTypes;

    private final Map<Element, float[]> _floatArrays;
    private final Map<Element, double[]> _doubleArrays;
    private final Map<Element, boolean[]> _booleanArrays;
    private final Map<Element, int[]> _intArrays;
    private final Map<Element, String[]> _stringArrays;

    private final Multimap<Element, MeshVertPairs> _vertMappings;

    private final ColladaStorage _storage;
    private ColladaOptions _options;

    private static class ThreadLocalGlobalData extends ThreadLocal<GlobalData> {
        @Override
        protected GlobalData initialValue() {
            return new GlobalData();
        }
    }

    private static ThreadLocalGlobalData threadLocalGlobalData = new ThreadLocalGlobalData();

    public static GlobalData getInstance() {
        return GlobalData.threadLocalGlobalData.get();
    }

    public static void disposeInstance() {
        GlobalData.threadLocalGlobalData.remove();
    }

    private GlobalData() {
        _boundMaterials = Maps.newHashMap();
        _textures = Maps.newHashMap();
        _idCache = Maps.newHashMap();
        _sidCache = Maps.newHashMap();
        _xPathExpressions = Maps.newHashMap();
        _pattern = Pattern.compile("\\s");

        _transformTypes = Collections.unmodifiableList(Lists.newArrayList("lookat", "matrix", "rotate", "scale",
                "scew", "translate"));

        _floatArrays = Maps.newHashMap();
        _doubleArrays = Maps.newHashMap();
        _booleanArrays = Maps.newHashMap();
        _intArrays = Maps.newHashMap();
        _stringArrays = Maps.newHashMap();
        _vertMappings = ArrayListMultimap.create();

        _storage = new ColladaStorage();
    }

    public void bindMaterial(final String ref, final Element material) {
        if (!_boundMaterials.containsKey(ref)) {
            _boundMaterials.put(ref, material);
        }
    }

    public void unbindMaterial(final String ref) {
        _boundMaterials.remove(ref);
    }

    public Element getBoundMaterial(final String ref) {
        return _boundMaterials.get(ref);
    }

    public Texture loadTexture2D(final String path, final MinificationFilter minFilter) {
        if (_textures.containsKey(path)) {
            return _textures.get(path);
        }

        final Texture texture;
        if (!getOptions().hasTextureLocator()) {
            texture = TextureManager.load(path, minFilter, Format.Guess, true);
        } else {
            final ResourceSource source = getOptions().getTextureLocator().locateResource(path);
            texture = TextureManager.load(source, minFilter, Format.Guess, true);
        }
        _textures.put(path, texture);

        return texture;
    }

    public Map<String, Element> getIdCache() {
        return _idCache;
    }

    public Map<String, Element> getSidCache() {
        return _sidCache;
    }

    public Map<String, XPath> getxPathExpressions() {
        return _xPathExpressions;
    }

    public Pattern getPattern() {
        return _pattern;
    }

    public List<String> getTransformTypes() {
        return _transformTypes;
    }

    public Map<Element, float[]> getFloatArrays() {
        return _floatArrays;
    }

    public Map<Element, double[]> getDoubleArrays() {
        return _doubleArrays;
    }

    public Map<Element, boolean[]> getBooleanArrays() {
        return _booleanArrays;
    }

    public Map<Element, int[]> getIntArrays() {
        return _intArrays;
    }

    public Map<Element, String[]> getStringArrays() {
        return _stringArrays;
    }

    public Multimap<Element, MeshVertPairs> getVertMappings() {
        return _vertMappings;
    }

    public ColladaStorage getColladaStorage() {
        return _storage;
    }

    public void setOptions(final ColladaOptions options) {
        _options = options;
    }

    public ColladaOptions getOptions() {
        return _options;
    }
}
