/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;

/**
 * Text spatial which uses textures generated by UIFont
 */
public class TextMesh extends Mesh {

    public TextMesh() {
        super("text");
        getMeshData().setIndexMode(IndexMode.Triangles);
        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);

        // -- never cull
        setModelBound(null);
        getSceneHints().setCullHint(CullHint.Never);

        // -- default to non-pickable
        getSceneHints().setAllPickingHints(false);
    }
}