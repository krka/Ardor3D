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

public enum RenderBucketType {

    /**
     * Use your parent's RenderBucketType. If you do not have a parent, {@link #Opaque} will be used instead.
     */
    Inherit,

    /**
     * Used for objects that we want to guarantee will be rendered first.
     */
    PreBucket,

    /**
     * TODO: Add definition.
     */
    Shadow,

    /**
     * Used for surfaces that are fully opaque - can not be seen through. Drawn from front to back.
     */
    Opaque,

    /**
     * Used for surfaces that are partially transparent or translucent - can be seen through. Drawn from back to front.
     * See also the flag {@link com.ardor3d.renderer.queue.TransparentRenderBucket#setTwoPassTransparency(boolean)
     * TransparentRenderBucket.setTwoPassTransparency(boolean)} allowing you to enable two pass transparency for more
     * accurate results.
     */
    Transparent,

    /**
     * Draw in orthographic mode where the x and y coordinates are in screen space with the origin in the lower left
     * corner. Uses {@link com.ardor3d.scenegraph.hint.SceneHints#getOrthoOrder() SceneHints.getOrthoOrder()} to
     * determine draw order.
     */
    Ortho,

    /**
     * Used for objects that we want to guarantee will be rendered last.
     */
    PostBucket,

    /**
     * Do not use bucket system. Instead, draw the spatial immediately to the back buffer.
     */
    Skip
}
