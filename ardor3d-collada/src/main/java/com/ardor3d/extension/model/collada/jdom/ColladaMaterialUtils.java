/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.util.List;
import java.util.logging.Logger;

import org.jdom.Element;

import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.data.SamplerTypes;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Methods for parsing Collada data related to materials.
 */
public class ColladaMaterialUtils {
    private static final Logger logger = Logger.getLogger(ColladaMaterialUtils.class.getName());

    private final boolean _loadTextures;
    private final DataCache _dataCache;
    private final ColladaDOMUtil _colladaDOMUtil;
    private final ResourceLocator _textureLocator;
    private final boolean _compressTextures;

    public ColladaMaterialUtils(final boolean loadTextures, final DataCache dataCache,
            final ColladaDOMUtil colladaDOMUtil, final ResourceLocator textureLocator, final boolean compressTextures) {
        _loadTextures = loadTextures;
        _dataCache = dataCache;
        _colladaDOMUtil = colladaDOMUtil;
        _textureLocator = textureLocator;
        _compressTextures = compressTextures;
    }

    /**
     * Find and apply the given material to the given Mesh.
     * 
     * @param materialName
     *            our material name
     * @param mesh
     *            the mesh to apply material to.
     */
    @SuppressWarnings("unchecked")
    public void applyMaterial(final String materialName, final Mesh mesh) {
        if (materialName == null) {
            logger.warning("materialName is null");
            return;
        }

        Element mat = _dataCache.getBoundMaterial(materialName);
        if (mat == null) {
            logger.warning("material not bound: " + materialName + ", trying search with id.");
            mat = _colladaDOMUtil.findTargetWithId(materialName);
        }
        if (mat == null) {
            logger.warning("material not found: " + materialName);
            return;
        }

        final Element child = mat.getChild("instance_effect");
        final Element effectNode = _colladaDOMUtil.findTargetWithId(child.getAttributeValue("url"));
        if (effectNode == null) {
            logger.warning("material effect not found: " + mat.getChild("instance_material").getAttributeValue("url"));
        }

        if ("effect".equals(effectNode.getName())) {
            final Element effect = effectNode;
            // XXX: For now, just grab the common technique:
            if (effect.getChild("profile_COMMON") != null) {
                final Element technique = effect.getChild("profile_COMMON").getChild("technique");
                if (technique.getChild("blinn") != null || technique.getChild("phong") != null
                        || technique.getChild("lambert") != null) {
                    final Element blinnPhong = technique.getChild("blinn") != null ? technique.getChild("blinn")
                            : technique.getChild("phong") != null ? technique.getChild("phong") : technique
                                    .getChild("lambert");
                    final MaterialState mState = new MaterialState();

                    Texture diffuseTexture = null;
                    ColorRGBA transparent = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
                    float transparency = 1.0f;
                    boolean useTransparency = false;
                    String opaqueMode = "A_ONE";

                    for (final Element property : (List<Element>) blinnPhong.getChildren()) {
                        if ("diffuse".equals(property.getName())) {
                            final Element propertyValue = (Element) property.getChildren().get(0);
                            if ("color".equals(propertyValue.getName())) {
                                final ColorRGBA color = _colladaDOMUtil.getColor(propertyValue.getText());
                                mState.setDiffuse(MaterialFace.FrontAndBack, color);
                            } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                                TextureState tState = (TextureState) mesh
                                        .getLocalRenderState(RenderState.StateType.Texture);
                                if (tState == null) {
                                    tState = new TextureState();
                                    mesh.setRenderState(tState);
                                }
                                diffuseTexture = populateTextureState(tState, propertyValue, effect);
                            }
                        } else if ("ambient".equals(property.getName())) {
                            final Element propertyValue = (Element) property.getChildren().get(0);
                            if ("color".equals(propertyValue.getName())) {
                                final ColorRGBA color = _colladaDOMUtil.getColor(propertyValue.getText());
                                mState.setAmbient(MaterialFace.FrontAndBack, color);
                            }
                        } else if ("transparent".equals(property.getName())) {
                            final Element propertyValue = (Element) property.getChildren().get(0);
                            if ("color".equals(propertyValue.getName())) {
                                transparent = _colladaDOMUtil.getColor(propertyValue.getText());
                                // TODO: use this

                                useTransparency = true;
                            }
                            opaqueMode = property.getAttributeValue("opaque", "A_ONE");
                        } else if ("transparency".equals(property.getName())) {
                            final Element propertyValue = (Element) property.getChildren().get(0);
                            if ("float".equals(propertyValue.getName())) {
                                transparency = Float.parseFloat(propertyValue.getText().replace(",", "."));
                                // TODO: use this

                                useTransparency = true;
                            }
                        } else if ("emission".equals(property.getName())) {
                            final Element propertyValue = (Element) property.getChildren().get(0);
                            if ("color".equals(propertyValue.getName())) {
                                mState.setEmissive(MaterialFace.FrontAndBack, _colladaDOMUtil.getColor(propertyValue
                                        .getText()));
                            }
                        } else if ("specular".equals(property.getName())) {
                            final Element propertyValue = (Element) property.getChildren().get(0);
                            if ("color".equals(propertyValue.getName())) {
                                mState.setSpecular(MaterialFace.FrontAndBack, _colladaDOMUtil.getColor(propertyValue
                                        .getText()));
                            }
                        } else if ("shininess".equals(property.getName())) {
                            final Element propertyValue = (Element) property.getChildren().get(0);
                            if ("float".equals(propertyValue.getName())) {
                                float shininess = Float.parseFloat(propertyValue.getText().replace(",", "."));
                                if (shininess >= 0.0f && shininess <= 1.0f) {
                                    final float oldShininess = shininess;
                                    shininess *= 128;
                                    logger.finest("Shininess - " + oldShininess
                                            + " - was in the [0,1] range. Scaling to [0, 128] - " + shininess);
                                } else if (shininess < 0 || shininess > 128) {
                                    final float oldShininess = shininess;
                                    shininess = MathUtils.clamp(shininess, 0, 128);
                                    logger.warning("Shininess must be between 0 and 128. Shininess " + oldShininess
                                            + " was clamped to " + shininess);
                                }
                                mState.setShininess(MaterialFace.FrontAndBack, shininess);
                            }
                        }
                    }

                    // XXX: There are some issues with clarity on how to use alpha blending in OpenGL FFP.
                    // The best interpretation I have seen is that if transparent has a texture == diffuse,
                    // Turn on alpha blending and use diffuse alpha.

                    if (diffuseTexture != null && useTransparency) {
                        final BlendState blend = new BlendState();
                        blend.setBlendEnabled(true);
                        blend.setTestEnabled(true);
                        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
                        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
                        mesh.setRenderState(blend);

                        mesh.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
                    }

                    mesh.setRenderState(mState);
                }
            }
        } else {
            ColladaMaterialUtils.logger.warning("material effect not found: "
                    + mat.getChild("instance_material").getAttributeValue("url"));
        }
    }

    /**
     * Convert a <texture> element to an Ardor3D representation and store in the given state.
     * 
     * @param state
     *            the Ardor3D TextureState to add of Texture to.
     * @param daeTexture
     *            our <texture> element
     * @param effect
     *            our <instance_effect> element
     * @return the created Texture.
     */
    private Texture populateTextureState(final TextureState state, final Element daeTexture, final Element effect) {
        // TODO: Use vert data to determine which texcoords and set to use.
        // final String uvName = daeTexture.getAttributeValue("texcoord");
        final int unit = 0;

        // Use texture attrib to find correct sampler
        final String textureReference = daeTexture.getAttributeValue("texture");
        Element node = _colladaDOMUtil.findTargetWithSid(textureReference);
        if (node == null) {
            // Not sure if this is quite right, but spec seems to indicate looking for global id
            node = _colladaDOMUtil.findTargetWithId("#" + textureReference);
        }

        if ("newparam".equals(node.getName())) {
            node = (Element) node.getChildren().get(0);
        }

        Element sampler = null;
        Element surface = null;
        Element image = null;

        Texture.MinificationFilter min = Texture.MinificationFilter.BilinearNoMipMaps;
        if ("sampler2D".equals(node.getName())) {
            sampler = node;
            if (sampler.getChild("minfilter") != null) {
                final String minfilter = sampler.getChild("minfilter").getText();
                min = Enum.valueOf(SamplerTypes.MinFilterType.class, minfilter).getArdor3dFilter();
            }
            // Use sampler to get correct surface
            node = _colladaDOMUtil.findTargetWithSid(sampler.getChild("source").getText());
            // node = resolveSid(effect, sampler.getSource());
        }

        if ("newparam".equals(node.getName())) {
            node = (Element) node.getChildren().get(0);
        }

        if ("surface".equals(node.getName())) {
            surface = node;
            // image(s) will come from surface.
        } else if ("image".equals(node.getName())) {
            image = node;
        }

        // Ok, a few possibilities here...
        Texture texture = null;
        if (surface == null && image != null) {
            // Only an image found (no sampler). Assume 2d texture. Load.
            texture = loadTexture2D(image.getChild("init_from").getText(), min);
        } else if (surface != null) {
            // We have a surface, pull images from that.
            if ("2D".equals(surface.getAttributeValue("type"))) {
                // look for an init_from with lowest mip and use that. (usually 0)

                // TODO: mip?
                final Element lowest = (Element) surface.getChildren("init_from").get(0);
                // Element lowest = null;
                // for (final Element i : (List<Element>) surface.getChildren("init_from")) {
                // if (lowest == null || lowest.getMip() > i.getMip()) {
                // lowest = i;
                // }
                // }

                if (lowest == null) {
                    logger.warning("surface given with no usable init_from: " + surface);
                    return null;
                }

                image = _colladaDOMUtil.findTargetWithId("#" + lowest.getText());
                // image = (DaeImage) root.resolveUrl("#" + lowest.getValue());
                if (image != null) {
                    texture = loadTexture2D(image.getChild("init_from").getText(), min);
                }

                // TODO: add support for mip map levels other than 0.
            }
            // TODO: add support for the other texture types.
        } else {
            // No surface OR image... warn.
            logger.warning("texture given with no matching <sampler*> or <image> found.");
            return null;
        }

        if (texture != null) {
            if (sampler != null) {
                // Apply params from our sampler.
                applySampler(sampler, texture);
            }
            // Add to texture state.
            state.setTexture(texture, unit);
        } else {
            logger.warning("unable to load texture: " + daeTexture);
        }

        return texture;
    }

    private void applySampler(final Element sampler, final Texture texture) {
        if (sampler.getChild("minfilter") != null) {
            final String minfilter = sampler.getChild("minfilter").getText();
            texture.setMinificationFilter(Enum.valueOf(SamplerTypes.MinFilterType.class, minfilter).getArdor3dFilter());
        }
        if (sampler.getChild("magfilter") != null) {
            final String magfilter = sampler.getChild("magfilter").getText();
            texture
                    .setMagnificationFilter(Enum.valueOf(SamplerTypes.MagFilterType.class, magfilter)
                            .getArdor3dFilter());
        }
        if (sampler.getChild("wrap_s") != null) {
            final String wrapS = sampler.getChild("wrap_s").getText();
            texture.setWrap(Texture.WrapAxis.S, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapS)
                    .getArdor3dWrapMode());
        }
        if (sampler.getChild("wrap_t") != null) {
            final String wrapT = sampler.getChild("wrap_t").getText();
            texture.setWrap(Texture.WrapAxis.T, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapT)
                    .getArdor3dWrapMode());
        }
        if (sampler.getChild("border_color") != null) {
            texture.setBorderColor(_colladaDOMUtil.getColor(sampler.getChild("border_color").getText()));
        }
    }

    @SuppressWarnings("unchecked")
    public void bindMaterials(final Element bindMaterial) {
        if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
            return;
        }

        for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren(
                "instance_material")) {
            final Element matNode = _colladaDOMUtil.findTargetWithId(instance.getAttributeValue("target"));
            if (matNode != null && "material".equals(matNode.getName())) {
                _dataCache.bindMaterial(instance.getAttributeValue("symbol"), matNode);
            } else {
                logger.warning("instance material target not found: " + instance.getAttributeValue("target"));
            }

            // TODO: need to store bound vert data as local data. (also unstore on unbind.)
        }
    }

    @SuppressWarnings("unchecked")
    public void unbindMaterials(final Element bindMaterial) {
        if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
            return;
        }
        for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren(
                "instance_material")) {
            _dataCache.unbindMaterial(instance.getAttributeValue("symbol"));
        }
    }

    private Texture loadTexture2D(final String path, final Texture.MinificationFilter minFilter) {
        if (_dataCache.containsTexture(path)) {
            return _dataCache.getTexture(path);
        }

        final Texture texture;
        if (_textureLocator == null) {
            texture = TextureManager.load(path, minFilter, _compressTextures ? TextureStoreFormat.GuessCompressedFormat
                    : TextureStoreFormat.GuessNoCompressedFormat, true);
        } else {
            final ResourceSource source = _textureLocator.locateResource(path);
            texture = TextureManager.load(source, minFilter,
                    _compressTextures ? TextureStoreFormat.GuessCompressedFormat
                            : TextureStoreFormat.GuessNoCompressedFormat, true);
        }
        _dataCache.addTexture(path, texture);

        return texture;
    }

}
