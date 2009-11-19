/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
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

import com.ardor3d.extension.model.collada.jdom.data.GlobalData;
import com.ardor3d.extension.model.collada.jdom.data.SamplerTypes;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapAxis;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;

public class ColladaMaterialUtils {
    private static final Logger logger = Logger.getLogger(ColladaMaterialUtils.class.getName());

    /**
     * Find and apply the given material to the given Mesh.
     * 
     * @param materialName
     *            our material name
     * @param mesh
     *            the mesh to apply material to.
     */
    public static void applyMaterial(final String materialName, final Mesh mesh) {
        if (materialName == null) {
            ColladaMaterialUtils.logger.warning("materialName is null");
            return;
        }

        final Element mat = GlobalData.getInstance().getBoundMaterial(materialName);
        if (mat == null) {
            ColladaMaterialUtils.logger.warning("material not found: " + materialName);
            return;
        }

        final Element effectNode = ColladaDOMUtil.findTargetWithId(mat.getChild("instance_effect").getAttributeValue(
                "url"));
        if (effectNode == null) {
            ColladaMaterialUtils.logger.warning("material effect not found: "
                    + mat.getChild("instance_material").getAttributeValue("url"));
        }

        if ("effect".equals(effectNode.getName())) {
            final Element effect = effectNode;
            // XXX: For now, just grab the common technique:
            final Element technique = effect.getChild("profile_COMMON").getChild("technique");
            if (technique.getChild("blinn") != null || technique.getChild("phong") != null
                    || technique.getChild("lambert") != null) {
                final Element blinnPhong = technique.getChild("blinn") != null ? technique.getChild("blinn")
                        : technique.getChild("phong") != null ? technique.getChild("phong") : technique
                                .getChild("lambert");
                final MaterialState mState = new MaterialState();
                // XXX: It seems this is generally wrong?
                // if (blinnPhong.getAmbientColor() != null) {
                // mState.setAmbient(blinnPhong.getAmbientColor().asColorRGBA());
                // }

                Texture diffuseTexture = null;

                ColorRGBA transparent = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
                float transparency = 1.0f;
                boolean useTransparency = false;

                for (final Element property : (List<Element>) blinnPhong.getChildren()) {
                    if ("diffuse".equals(property.getName())) {
                        final Element propertyValue = (Element) property.getChildren().get(0);
                        if ("color".equals(propertyValue.getName())) {
                            final ColorRGBA color = ColladaDOMUtil.getColor(propertyValue.getText());
                            mState.setDiffuse(color);
                            // XXX: hack... see above
                            mState.setAmbient(color);
                        } else if ("texture".equals(propertyValue.getName())) {
                            TextureState tState = (TextureState) mesh.getLocalRenderState(StateType.Texture);
                            if (tState == null) {
                                tState = new TextureState();
                                mesh.setRenderState(tState);
                            }
                            diffuseTexture = ColladaMaterialUtils.populateTextureState(tState, propertyValue, effect);
                        }
                    } else if ("transparent".equals(property.getName())) {
                        final Element propertyValue = (Element) property.getChildren().get(0);
                        if ("color".equals(propertyValue.getName())) {
                            transparent = ColladaDOMUtil.getColor(propertyValue.getText());
                            // TODO: use this

                            useTransparency = true;
                        }
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
                            mState.setEmissive(ColladaDOMUtil.getColor(propertyValue.getText()));
                        }
                    } else if ("specular".equals(property.getName())) {
                        final Element propertyValue = (Element) property.getChildren().get(0);
                        if ("color".equals(propertyValue.getName())) {
                            mState.setSpecular(ColladaDOMUtil.getColor(propertyValue.getText()));
                        }
                    } else if ("shininess".equals(property.getName())) {
                        final Element propertyValue = (Element) property.getChildren().get(0);
                        if ("float".equals(propertyValue.getName())) {
                            float shininess = Float.parseFloat(propertyValue.getText().replace(",", "."));
                            if (shininess >= 0.0f && shininess <= 1.0f) {
                                final float oldShininess = shininess;
                                shininess *= 128;
                                ColladaMaterialUtils.logger.finest("Shininess - " + oldShininess
                                        + " - was in the [0,1] range. Scaling to [0, 128] - " + shininess);
                            } else if (shininess < 0 || shininess > 128) {
                                final float oldShininess = shininess;
                                shininess = (float) MathUtils.clamp(shininess, 0, 128);
                                ColladaMaterialUtils.logger.warning("Shininess must be between 0 and 128. Shininess "
                                        + oldShininess + " was clamped to " + shininess);
                            }
                            mState.setShininess(shininess);
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
                    blend.setSourceFunction(SourceFunction.SourceAlpha);
                    blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
                    mesh.setRenderState(blend);

                    mesh.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
                }

                mesh.setRenderState(mState);
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
    private static Texture populateTextureState(final TextureState state, final Element daeTexture, final Element effect) {
        // TODO: Use vert data to determine which texcoords and set to use.
        // final String uvName = daeTexture.getAttributeValue("texcoord");
        final int unit = 0;

        // Use texture attrib to find correct sampler
        final String textureReference = daeTexture.getAttributeValue("texture");
        Element node = ColladaDOMUtil.findTargetWithSid(textureReference);
        if (node == null) {
            // Not sure if this is quite right, but spec seems to indicate looking for global id
            node = ColladaDOMUtil.findTargetWithId("#" + textureReference);
        }

        if ("newparam".equals(node.getName())) {
            node = (Element) node.getChildren().get(0);
        }

        Element sampler = null;
        Element surface = null;
        Element image = null;

        MinificationFilter min = MinificationFilter.BilinearNoMipMaps;
        if ("sampler2D".equals(node.getName())) {
            sampler = node;
            if (sampler.getChild("minfilter") != null) {
                final String minfilter = sampler.getChild("minfilter").getText();
                min = Enum.valueOf(SamplerTypes.MinFilterType.class, minfilter).getArdor3dFilter();
            }
            // Use sampler to get correct surface
            node = ColladaDOMUtil.findTargetWithSid(sampler.getChild("source").getText());
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
            texture = ColladaMaterialUtils.loadTexture2D(image, min);
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
                    ColladaMaterialUtils.logger.warning("surface given with no usable init_from: " + surface);
                    return null;
                }

                image = ColladaDOMUtil.findTargetWithId("#" + lowest.getText());
                // image = (DaeImage) root.resolveUrl("#" + lowest.getValue());
                if (image != null) {
                    texture = ColladaMaterialUtils.loadTexture2D(image, min);
                }

                // TODO: add support for mip map levels other than 0.
            }
            // TODO: add support for the other texture types.
        } else {
            // No surface OR image... warn.
            ColladaMaterialUtils.logger.warning("texture given with no matching <sampler*> or <image> found.");
            return null;
        }

        if (texture != null) {
            if (sampler != null) {
                // Apply params from our sampler.
                ColladaMaterialUtils.applySampler(sampler, texture);
            }
            // Add to texture state.
            state.setTexture(texture, unit);
        } else {
            ColladaMaterialUtils.logger.warning("unable to load texture: " + daeTexture);
        }

        return texture;
    }

    private static Texture loadTexture2D(final Element image, final MinificationFilter minFilter) {
        return GlobalData.getInstance().loadTexture2D(image.getChild("init_from").getText(), minFilter);
    }

    private static void applySampler(final Element sampler, final Texture texture) {
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
            texture.setWrap(WrapAxis.S, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapS).getArdor3dWrapMode());
        }
        if (sampler.getChild("wrap_t") != null) {
            final String wrapT = sampler.getChild("wrap_t").getText();
            texture.setWrap(WrapAxis.T, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapT).getArdor3dWrapMode());
        }
        if (sampler.getChild("border_color") != null) {
            texture.setBorderColor(ColladaDOMUtil.getColor(sampler.getChild("border_color").getText()));
        }
    }

    @SuppressWarnings("unchecked")
    public static void bindMaterials(final Element bindMaterial) {
        if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
            return;
        }

        for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren(
                "instance_material")) {
            final Element matNode = ColladaDOMUtil.findTargetWithId(instance.getAttributeValue("target"));
            if (matNode != null && "material".equals(matNode.getName())) {
                GlobalData.getInstance().bindMaterial(instance.getAttributeValue("symbol"), matNode);
            } else {
                ColladaMaterialUtils.logger.warning("instance material target not found: "
                        + instance.getAttributeValue("target"));
            }

            // TODO: need to store bound vert data as local data. (also unstore on unbind.)
        }
    }

    @SuppressWarnings("unchecked")
    public static void unbindMaterials(final Element bindMaterial) {
        if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
            return;
        }
        for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren(
                "instance_material")) {
            GlobalData.getInstance().unbindMaterial(instance.getAttributeValue("symbol"));
        }
    }
}
