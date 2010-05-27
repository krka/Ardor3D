/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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

    private final EnumMap<RenderBucketType, RenderBucket> renderBuckets = new EnumMap<RenderBucketType, RenderBucket>(
            RenderBucketType.class);

    public RenderQueue() {
        setupDefaultBuckets();
    }

    private void setupDefaultBuckets() {
        renderBuckets.put(RenderBucketType.PreBucket, new OpaqueRenderBucket());
        renderBuckets.put(RenderBucketType.Shadow, new OpaqueRenderBucket());
        renderBuckets.put(RenderBucketType.Opaque, new OpaqueRenderBucket());
        renderBuckets.put(RenderBucketType.Transparent, new TransparentRenderBucket());
        renderBuckets.put(RenderBucketType.Ortho, new OrthoRenderBucket());
        renderBuckets.put(RenderBucketType.PostBucket, new OpaqueRenderBucket());
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

    public void renderOnly(final Renderer renderer) {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.render(renderer);
        }
    }

    public void renderBuckets(final Renderer renderer) {
        for (final RenderBucket renderBucket : renderBuckets.values()) {
            renderBucket.sort();
            renderBucket.render(renderer);
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
