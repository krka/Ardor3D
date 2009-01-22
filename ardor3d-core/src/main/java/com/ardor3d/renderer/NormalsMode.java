/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

public enum NormalsMode {
    /**
     * When used in scenegraphs, indicates to do whatever our parent does.
     * 
     * @see com.ardor3d.scene.Spatial#getNormalsMode()
     */
    Inherit,
    /**
     * Send through the normals currently set as-is.
     */
    UseProvided,
    /**
     * Tell the card to normalize any normals data we might give it.
     */
    AlwaysNormalize,
    /**
     * If a scale other than 1,1,1 is being used then tell the card to normalize any normals data we might give it.
     */
    NormalizeIfScaled,
    /**
     * Do not send normal data to the card, even if we have some.
     */
    Off;
}
