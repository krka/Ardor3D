/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.util;

import java.util.logging.Logger;

import com.ardor3d.extension.model.collada.binding.DaeTreeNode;
import com.ardor3d.extension.model.collada.binding.core.Collada;
import com.ardor3d.extension.model.collada.binding.fx.DaeBindMaterial;
import com.ardor3d.extension.model.collada.binding.fx.DaeBlinnPhong;
import com.ardor3d.extension.model.collada.binding.fx.DaeEffect;
import com.ardor3d.extension.model.collada.binding.fx.DaeImage;
import com.ardor3d.extension.model.collada.binding.fx.DaeInitFrom;
import com.ardor3d.extension.model.collada.binding.fx.DaeInstanceMaterial;
import com.ardor3d.extension.model.collada.binding.fx.DaeMaterial;
import com.ardor3d.extension.model.collada.binding.fx.DaeNewparam;
import com.ardor3d.extension.model.collada.binding.fx.DaeSampler;
import com.ardor3d.extension.model.collada.binding.fx.DaeSampler2D;
import com.ardor3d.extension.model.collada.binding.fx.DaeSurface;
import com.ardor3d.extension.model.collada.binding.fx.DaeTechnique;
import com.ardor3d.extension.model.collada.binding.fx.DaeTexture;
import com.ardor3d.extension.model.collada.binding.fx.DaeSurface.DaeSurfaceType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.WrapAxis;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.TextureManager;

public class ColladaMaterialUtils {
    private static final Logger logger = Logger.getLogger(ColladaMaterialUtils.class.getName());

    /**
     * 
     * @param materialName
     * @param rootNode
     * @param mesh
     */
    public static void applyMaterial(final String materialName, final Collada rootNode, final Mesh mesh) {
        if (materialName != null) {
            final DaeMaterial mat = rootNode.getBoundMaterial(materialName);
            if (mat != null) {
                final DaeTreeNode effectNode = rootNode.resolveUrl(mat.getInstanceEffect().getUrl());
                if (effectNode instanceof DaeEffect) {
                    final DaeEffect effect = (DaeEffect) effectNode;
                    // XXX: For now, just grab the common technique:
                    final DaeTechnique technique = effect.getProfileCommon().getTechnique();
                    if (technique.getBlinn() != null || technique.getPhong() != null || technique.getLambert() != null) {
                        final DaeBlinnPhong blinnPhong = technique.getBlinn() != null ? technique.getBlinn()
                                : technique.getPhong() != null ? technique.getPhong() : technique.getLambert();
                        final MaterialState mState = new MaterialState();
                        // XXX: It seems this is generally wrong?
                        // if (blinnPhong.getAmbientColor() != null) {
                        // mState.setAmbient(blinnPhong.getAmbientColor().asColorRGBA());
                        // }

                        if (blinnPhong.getDiffuseColor() != null) {
                            mState.setDiffuse(blinnPhong.getDiffuseColor().asColorRGBA());
                            // XXX: hack... see above
                            mState.setAmbient(blinnPhong.getDiffuseColor().asColorRGBA());
                        }
                        if (blinnPhong.getDiffuseTexture() != null) {
                            TextureState tState = (TextureState) mesh.getLocalRenderState(StateType.Texture);
                            if (tState == null) {
                                tState = new TextureState();
                                mesh.setRenderState(tState);
                            }
                            populateTextureState(tState, blinnPhong.getDiffuseTexture(), rootNode, effect);
                        }

                        if (blinnPhong.getEmissionColor() != null) {
                            mState.setEmissive(blinnPhong.getEmissionColor().asColorRGBA());
                        }

                        if (blinnPhong.getSpecularColor() != null) {
                            mState.setSpecular(blinnPhong.getSpecularColor().asColorRGBA());
                        }

                        // XXX: There are some issues with clarity on how to use alpha blending in OpenGL FFP.
                        // The best interpretation I have seen is that if transparent has a texture == diffuse,
                        // Turn on alpha blending and use diffuse alpha.
                        if (blinnPhong.getTransparentTexture() != null && blinnPhong.getDiffuseTexture() != null) {
                            if (blinnPhong.getTransparentTexture().getTexture().equals(
                                    blinnPhong.getDiffuseTexture().getTexture())) {
                                final BlendState blend = new BlendState();
                                blend.setBlendEnabled(true);
                                blend.setSourceFunction(SourceFunction.SourceAlpha);
                                blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
                                mesh.setRenderState(blend);
                            }
                        }

                        if (blinnPhong.getShininess() != null) {
                            mState.setShininess(blinnPhong.getShininess().getValue());
                        }
                        mesh.setRenderState(mState);
                    }
                } else {
                    logger.warning("material effect not found: " + mat.getInstanceEffect().getUrl());
                }
            } else {
                logger.warning("material not found: " + materialName);
            }
        }
    }

    private static void populateTextureState(final TextureState state, final DaeTexture daeTexture, final Collada root,
            final DaeEffect effect) {
        // TODO: Use vert data to determine which texcoords and set to use.
        // final String uvName = daeTexture.getTexcoord();
        final int unit = 0;

        // Use texture attrib to find correct sampler
        DaeTreeNode node = resolveSid(effect, daeTexture.getTexture());
        if (node == null) {
            // Not sure if this is quite right, but spec seems to indicate looking for global id
            node = root.resolveUrl("#" + daeTexture.getTexture());
        }
        DaeSampler sampler = null;
        DaeSurface surface = null;
        MinificationFilter min = MinificationFilter.BilinearNoMipMaps;
        DaeImage image = null;
        if (node instanceof DaeSampler) {
            sampler = (DaeSampler) node;
            if (sampler.getMinfilter() != null) {
                min = sampler.getMinfilter().getArdor3dFilter();
            }
            // Use sampler to get correct surface
            node = resolveSid(effect, sampler.getSource());
        }
        if (node instanceof DaeSurface) {
            surface = (DaeSurface) node;
            // image(s) will come from surface.
        }
        if (node instanceof DaeImage) {
            image = (DaeImage) node;
        }

        // Ok, a few possibilities here...
        Texture texture = null;
        if (surface == null && image != null) {
            // Only an image found (no sampler). Assume 2d texture. Load.
            texture = loadTexture2D(image, min);
        } else if (surface != null) {
            // We have a surface, pull images from that.
            if (surface.getType() == DaeSurfaceType.TWO_D) {
                // look for an init_from with lowest mip and use that. (usually 0)
                DaeInitFrom lowest = null;
                for (final DaeInitFrom i : surface.getInitFroms()) {
                    if (lowest == null || lowest.getMip() > i.getMip()) {
                        lowest = i;
                    }
                }

                if (lowest == null) {
                    logger.warning("surface given with no usable init_from: " + surface);
                    return;
                }

                image = (DaeImage) root.resolveUrl("#" + lowest.getValue());
                if (image != null) {
                    texture = loadTexture2D(image, min);
                }

                // TODO: add support for mip map levels other than 0.
            }
            // TODO: add support for the other texture types.
        } else {
            // No surface OR image... warn.
            logger.warning("texture given with no matching <sampler*> or <image> found.");
            return;
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
    }

    private static DaeTreeNode resolveSid(final DaeEffect effect, final String sid) {
        DaeTreeNode node = effect.findSubNode(sid);
        if (node instanceof DaeNewparam) {
            final DaeNewparam newParam = (DaeNewparam) node;
            if (newParam.getSampler() != null) {
                node = newParam.getSampler();
            } else if (newParam.getSurface() != null) {
                node = newParam.getSurface();
            }
        }
        return node;
    }

    private static Texture loadTexture2D(final DaeImage image, final MinificationFilter minFilter) {
        return TextureManager.load(image.getInitFrom(), minFilter, Format.Guess, true);
    }

    private static void applySampler(final DaeSampler sampler, final Texture texture) {
        if (sampler.getMinfilter() != null) {
            texture.setMinificationFilter(sampler.getMinfilter().getArdor3dFilter());
        }
        if (sampler.getMagfilter() != null) {
            texture.setMagnificationFilter(sampler.getMagfilter().getArdor3dFilter());
        }
        if (sampler.getWrapS() != null) {
            texture.setWrap(WrapAxis.S, sampler.getWrapS().getArdor3dWrapMode());
        }
        if (sampler.getWrapT() != null) {
            texture.setWrap(WrapAxis.T, sampler.getWrapT().getArdor3dWrapMode());
        }

        if (sampler instanceof DaeSampler2D) {
            final DaeSampler2D sampler2d = (DaeSampler2D) sampler;
            if (texture instanceof Texture2D) {
                if (sampler2d.getBorderColor() != null) {
                    texture.setBorderColor(sampler2d.getBorderColor().asColorRGBA());
                }
                if (sampler2d.getWrapT() != null) {
                    texture.setWrap(WrapAxis.T, sampler2d.getWrapT().getArdor3dWrapMode());
                }
            } else {
                logger.warning("<sampler2d> applied to a non Texture2D: " + texture.getClass().getCanonicalName());
            }
        }
    }

    public static void bindMaterials(final DaeBindMaterial bindMaterial, final Collada root) {
        if (bindMaterial == null || bindMaterial.getCommonInstanceMaterials() == null) {
            return;
        }
        for (final DaeInstanceMaterial instance : bindMaterial.getCommonInstanceMaterials()) {
            final DaeTreeNode matNode = root.resolveUrl(instance.getTarget());
            if (matNode instanceof DaeMaterial) {
                root.bindMaterial(instance.getSymbol(), (DaeMaterial) matNode);
            } else {
                logger.warning("instance material target not found: " + instance.getTarget());
            }

            // TODO: need to store bound vert data as local data. (also unstore on unbind.)
        }
    }

    public static void unbindMaterials(final DaeBindMaterial bindMaterial, final Collada root) {
        if (bindMaterial == null || bindMaterial.getCommonInstanceMaterials() == null) {
            return;
        }
        for (final DaeInstanceMaterial instance : bindMaterial.getCommonInstanceMaterials()) {
            root.unbindMaterial(instance.getSymbol());
        }
    }
}
