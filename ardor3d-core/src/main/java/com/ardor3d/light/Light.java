/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.light;

import java.io.IOException;
import java.io.Serializable;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Light</code> defines the attributes of a light element. This class is abstract and intended to be sub-classed
 * by specific lighting types. A light will illuminate portions of the scene by assigning its properties to the objects
 * in the scene. This will affect the objects color values, depending on the color of the ambient, diffuse and specular
 * light components.
 * 
 * Ambient light defines the general light of the scene, that is the intensity and color of lighting if no particular
 * lights are affecting it.
 * 
 * Diffuse lighting defines the reflection of light on matte surfaces.
 * 
 * Specular lighting defines the reflection of light on shiny surfaces.
 */
public abstract class Light implements Serializable, Savable {

    private static final long serialVersionUID = 1L;

    /**
     * dark grey (.4, .4, .4, 1)
     */
    public static final ReadOnlyColorRGBA DEFAULT_AMBIENT = new ColorRGBA(0.4f, 0.4f, 0.4f, 1.0f);

    /**
     * white (1, 1, 1, 1)
     */
    public static final ReadOnlyColorRGBA DEFAULT_DIFFUSE = new ColorRGBA(1, 1, 1, 1);

    /**
     * white (1, 1, 1, 1)
     */
    public static final ReadOnlyColorRGBA DEFAULT_SPECULAR = new ColorRGBA(1, 1, 1, 1);

    public enum Type {
        Directional, Point, Spot
    }

    // light attributes.
    private final ColorRGBA ambient = new ColorRGBA(DEFAULT_AMBIENT);
    private final ColorRGBA diffuse = new ColorRGBA(DEFAULT_DIFFUSE);
    private final ColorRGBA specular = new ColorRGBA(DEFAULT_SPECULAR);

    private boolean attenuate;
    private float constant = 1;
    private float linear;
    private float quadratic;

    private int lightMask = 0;
    private int backLightMask = 0;

    private boolean enabled;

    /** when true, indicates the lights in this lightState will cast shadows. */
    protected boolean shadowCaster;

    /**
     * Constructor instantiates a new <code>Light</code> object. All light color values are set to white.
     * 
     */
    public Light() {}

    /**
     * 
     * <code>getType</code> returns the type of the light that has been created.
     * 
     * @return the type of light that has been created.
     */
    public abstract Type getType();

    /**
     * <code>getConstant</code> returns the value for the constant attenuation.
     * 
     * @return the value for the constant attenuation.
     */
    public float getConstant() {
        return constant;
    }

    /**
     * <code>setConstant</code> sets the value for the constant attentuation.
     * 
     * @param constant
     *            the value for the constant attenuation.
     */
    public void setConstant(final float constant) {
        this.constant = constant;
    }

    /**
     * <code>getLinear</code> returns the value for the linear attenuation.
     * 
     * @return the value for the linear attenuation.
     */
    public float getLinear() {
        return linear;
    }

    /**
     * <code>setLinear</code> sets the value for the linear attentuation.
     * 
     * @param linear
     *            the value for the linear attenuation.
     */
    public void setLinear(final float linear) {
        this.linear = linear;
    }

    /**
     * <code>getQuadratic</code> returns the value for the quadratic attentuation.
     * 
     * @return the value for the quadratic attenuation.
     */
    public float getQuadratic() {
        return quadratic;
    }

    /**
     * <code>setQuadratic</code> sets the value for the quadratic attenuation.
     * 
     * @param quadratic
     *            the value for the quadratic attenuation.
     */
    public void setQuadratic(final float quadratic) {
        this.quadratic = quadratic;
    }

    /**
     * <code>isAttenuate</code> returns true if attenuation is to be used for this light.
     * 
     * @return true if attenuation is to be used, false otherwise.
     */
    public boolean isAttenuate() {
        return attenuate;
    }

    /**
     * <code>setAttenuate</code> sets if attenuation is to be used. True sets it on, false otherwise.
     * 
     * @param attenuate
     *            true to use attenuation, false not to.
     */
    public void setAttenuate(final boolean attenuate) {
        this.attenuate = attenuate;
    }

    /**
     * 
     * <code>isEnabled</code> returns true if the light is enabled, false otherwise.
     * 
     * @return true if the light is enabled, false if it is not.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 
     * <code>setEnabled</code> sets the light on or off. True turns it on, false turns it off.
     * 
     * @param value
     *            true to turn the light on, false to turn it off.
     */
    public void setEnabled(final boolean value) {
        enabled = value;
    }

    /**
     * <code>getSpecular</code> returns the specular color value for this light.
     * 
     * @return the specular color value of the light.
     */
    public ReadOnlyColorRGBA getSpecular() {
        return specular;
    }

    /**
     * <code>setSpecular</code> sets the specular color value for this light.
     * 
     * @param specular
     *            the specular color value of the light.
     */
    public void setSpecular(final ReadOnlyColorRGBA specular) {
        this.specular.set(specular);
    }

    /**
     * <code>getDiffuse</code> returns the diffuse color value for this light.
     * 
     * @return the diffuse color value for this light.
     */
    public ReadOnlyColorRGBA getDiffuse() {
        return diffuse;
    }

    /**
     * <code>setDiffuse</code> sets the diffuse color value for this light.
     * 
     * @param diffuse
     *            the diffuse color value for this light.
     */
    public void setDiffuse(final ReadOnlyColorRGBA diffuse) {
        this.diffuse.set(diffuse);
    }

    /**
     * <code>getAmbient</code> returns the ambient color value for this light.
     * 
     * @return the ambient color value for this light.
     */
    public ReadOnlyColorRGBA getAmbient() {
        return ambient;
    }

    /**
     * <code>setAmbient</code> sets the ambient color value for this light.
     * 
     * @param ambient
     *            the ambient color value for this light.
     */
    public void setAmbient(final ReadOnlyColorRGBA ambient) {
        this.ambient.set(ambient);
    }

    /**
     * @return Returns the lightMask - default is 0 or not masked.
     */
    public int getLightMask() {
        return lightMask;
    }

    /**
     * <code>setLightMask</code> sets what attributes of this light to apply as an int comprised of bitwise |'ed values
     * from LightState.Mask_XXXX. LightMask.MASK_GLOBALAMBIENT is ignored.
     * 
     * @param lightMask
     *            The lightMask to set.
     */
    public void setLightMask(final int lightMask) {
        this.lightMask = lightMask;
    }

    /**
     * Saves the light mask to a back store. That backstore is recalled with popLightMask. Despite the name, this is not
     * a stack and additional pushes will simply overwrite the backstored value.
     */
    public void pushLightMask() {
        backLightMask = lightMask;
    }

    /**
     * Recalls the light mask from a back store or 0 if none was pushed.
     * 
     * @see com.ardor3d.light.Light#pushLightMask()
     */
    public void popLightMask() {
        lightMask = backLightMask;
    }

    /**
     * @return Returns whether this light is able to cast shadows.
     */
    public boolean isShadowCaster() {
        return shadowCaster;
    }

    /**
     * @param mayCastShadows
     *            true if this light can be used to derive shadows (when used in conjunction with a shadow pass.)
     */
    public void setShadowCaster(final boolean mayCastShadows) {
        shadowCaster = mayCastShadows;
    }

    /**
     * Copies the light values from the given light into this Light.
     * 
     * @param light
     *            the Light to copy from.
     */
    public void copyFrom(final Light light) {
        ambient.set(light.ambient);
        attenuate = light.attenuate;
        constant = light.constant;
        diffuse.set(light.diffuse);
        enabled = light.enabled;
        linear = light.linear;
        quadratic = light.quadratic;
        shadowCaster = light.shadowCaster;
        specular.set(light.specular);
    }

    public void write(final Ardor3DExporter e) throws IOException {
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(ambient, "ambient", new ColorRGBA(DEFAULT_AMBIENT));
        capsule.write(diffuse, "diffuse", new ColorRGBA(DEFAULT_DIFFUSE));
        capsule.write(specular, "specular", new ColorRGBA(DEFAULT_SPECULAR));
        capsule.write(attenuate, "attenuate", false);
        capsule.write(constant, "constant", 1);
        capsule.write(linear, "linear", 0);
        capsule.write(quadratic, "quadratic", 0);
        capsule.write(lightMask, "lightMask", 0);
        capsule.write(backLightMask, "backLightMask", 0);
        capsule.write(enabled, "enabled", false);
        capsule.write(shadowCaster, "shadowCaster", false);
    }

    public void read(final Ardor3DImporter e) throws IOException {
        final InputCapsule capsule = e.getCapsule(this);
        ambient.set((ColorRGBA) capsule.readSavable("ambient", new ColorRGBA(DEFAULT_AMBIENT)));
        diffuse.set((ColorRGBA) capsule.readSavable("diffuse", new ColorRGBA(DEFAULT_DIFFUSE)));
        specular.set((ColorRGBA) capsule.readSavable("specular", new ColorRGBA(DEFAULT_SPECULAR)));
        attenuate = capsule.readBoolean("attenuate", false);
        constant = capsule.readFloat("constant", 1);
        linear = capsule.readFloat("linear", 0);
        quadratic = capsule.readFloat("quadratic", 0);
        lightMask = capsule.readInt("lightMask", 0);
        backLightMask = capsule.readInt("backLightMask", 0);
        enabled = capsule.readBoolean("enabled", false);
        shadowCaster = capsule.readBoolean("shadowCaster", false);
    }

    public Class<? extends Light> getClassTag() {
        return this.getClass();
    }
}
