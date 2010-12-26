/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.atlas;

import java.nio.FloatBuffer;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.TextureKey;

public class TextureParameter {
    private final Mesh mesh;
    private final Texture texture;
    private final int textureIndex;
    private final int targetTextureIndex;
    private final TextureKey textureKey;
    private int atlasIndex;

    public TextureParameter(final Mesh mesh, final int textureIndex, final int targetTextureIndex) {
        this.mesh = mesh;
        this.textureIndex = textureIndex;
        this.targetTextureIndex = targetTextureIndex;
        texture = ((TextureState) mesh.getWorldRenderState(StateType.Texture)).getTexture(textureIndex);
        textureKey = texture.getTextureKey();
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Texture getTexture() {
        return texture;
    }

    public FloatBuffer getTextureCoords() {
        return mesh.getMeshData().getTextureBuffer(textureIndex);
    }

    public int getWidth() {
        return texture.getImage().getWidth();
    }

    public int getHeight() {
        return texture.getImage().getHeight();
    }

    public void setAtlasIndex(final int atlasIndex) {
        this.atlasIndex = atlasIndex;
    }

    public int getAtlasIndex() {
        return atlasIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (textureKey == null ? 0 : textureKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TextureParameter)) {
            return false;
        }
        final TextureParameter other = (TextureParameter) obj;
        if (textureKey == null) {
            if (other.textureKey != null) {
                return false;
            }
        } else if (!textureKey.equals(other.textureKey)) {
            return false;
        }
        return true;
    }

    public int getTargetTextureIndex() {
        return targetTextureIndex;
    }
}