/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.queue;

import java.util.EnumMap;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;

public class RenderQueue {
    private final Renderer _renderer;

    private final EnumMap<RenderBucketType, RenderBucket> renderBuckets = new EnumMap<RenderBucketType, RenderBucket>(
            RenderBucketType.class);

    public RenderQueue(final Renderer r) {
        _renderer = r;

        setupDefaultBuckets();
    }

    private void setupDefaultBuckets() {
        // Default these buckets to opaque queue functionality
        final RenderBucket opaqueRenderBucket = new OpaqueRenderBucket(_renderer);
        renderBuckets.put(RenderBucketType.PreBucket, opaqueRenderBucket);
        renderBuckets.put(RenderBucketType.Shadow, opaqueRenderBucket);
        renderBuckets.put(RenderBucketType.PostBucket, opaqueRenderBucket);

        renderBuckets.put(RenderBucketType.Opaque, new OpaqueRenderBucket(_renderer));
        renderBuckets.put(RenderBucketType.Transparent, new TransparentRenderBucket(_renderer));
        renderBuckets.put(RenderBucketType.Ortho, new OrthoRenderBucket(_renderer));
    }

    public void setRenderBucket(final RenderBucketType type, final RenderBucket renderBucket) {
        renderBuckets.put(type, renderBucket);
    }

    public RenderBucket getRenderBucket(final RenderBucketType type) {
        return renderBuckets.get(type);
    }

    public void addToQueue(final Spatial spatial, final RenderBucketType type) {
        if (type == RenderBucketType.Inherit || type == RenderBucketType.Skip) {
            throw new Ardor3dException("Can't add spatial to bucket of type: " + type);
        }

        if (renderBuckets.containsKey(type)) {
            final RenderBucket renderBucket = renderBuckets.get(type);
            renderBucket.add(spatial);
        } else {
            throw new Ardor3dException("No bucket exists of type: " + type);
        }
    }

    public void removeFromQueue(final Spatial spatial, final RenderBucketType type) {
        if (type == RenderBucketType.Inherit || type == RenderBucketType.Skip) {
            throw new Ardor3dException("Can't add spatial to bucket of type: " + type);
        }

        if (renderBuckets.containsKey(type)) {
            final RenderBucket renderBucket = renderBuckets.get(type);
            renderBucket.remove(spatial);
        } else {
            throw new Ardor3dException("No bucket exists of type: " + type);
        }
    }

    public void clearBuckets() {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.clear();
        }
    }

    public void sortBuckets() {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.sort();
        }
    }

    public void renderOnly() {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.render();
        }
    }

    public void renderBuckets() {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.sort();
            renderBucket.render();
            renderBucket.clear();
        }
    }

    public void pushBuckets() {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.pushBucket();
        }
    }

    public void popBuckets() {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.popBucket();
        }
    }
}
