/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.record.MaterialStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>MaterialState</code> defines a state to define an objects material settings. Material is defined by the
 * emissive quality of the object, the ambient color, diffuse color and specular color. The material also defines the
 * shininess of the object and the alpha value of the object.
 */
public class MaterialState extends RenderState {

    public enum ColorMaterial {
        /** Mesh colors are ignored. This is default. */
        None,

        /** Mesh colors determine material ambient color. */
        Ambient,

        /** Mesh colors determine material diffuse color. */
        Diffuse,

        /** Mesh colors determine material ambient and diffuse colors. */
        AmbientAndDiffuse,

        /** Mesh colors determine material specular colors. */
        Specular,

        /** Mesh colors determine material emissive color. */
        Emissive;
    }

    public enum MaterialFace {
        /** Apply materials to front face only. This is default. */
        Front,

        /** Apply materials to back face only. */
        Back,

        /** Apply materials to front and back faces. */
        FrontAndBack;
    }

    /** Default ambient color for all material states. (.2, .2, .2, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_AMBIENT = new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f);

    /** Default diffuse color for all material states. (.8, .8, .8, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_DIFFUSE = new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f);

    /** Default specular color for all material states. (0, 0, 0, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_SPECULAR = new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f);

    /** Default emissive color for all material states. (0, 0, 0, 1) */
    public static final ReadOnlyColorRGBA DEFAULT_EMISSIVE = new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f);

    /** Default shininess for all material states. */
    public static final float DEFAULT_SHININESS = 0.0f;

    /** Default color material mode for all material states. */
    public static final ColorMaterial DEFAULT_COLOR_MATERIAL = ColorMaterial.None;

    /** Default material face for all material states. */
    public static final MaterialFace DEFAULT_MATERIAL_FACE = MaterialFace.Front;

    // attributes of the material
    protected final ColorRGBA _ambient = new ColorRGBA(DEFAULT_AMBIENT);
    protected final ColorRGBA _diffuse = new ColorRGBA(DEFAULT_DIFFUSE);
    protected final ColorRGBA _specular = new ColorRGBA(DEFAULT_SPECULAR);
    protected final ColorRGBA _emissive = new ColorRGBA(DEFAULT_EMISSIVE);

    protected float _shininess = DEFAULT_SHININESS;

    protected ColorMaterial _colorMaterial = DEFAULT_COLOR_MATERIAL;
    protected MaterialFace _materialFace = DEFAULT_MATERIAL_FACE;

    /**
     * Constructor instantiates a new <code>MaterialState</code> object.
     */
    public MaterialState() {}

    /**
     * @return the color of the ambient value for this material.
     */
    public ReadOnlyColorRGBA getAmbient() {
        return _ambient;
    }

    /**
     * @param ambient
     *            the new ambient color to copy into this material.
     */
    public void setAmbient(final ReadOnlyColorRGBA ambient) {
        _ambient.set(ambient);
        setNeedsRefresh(true);
    }

    /**
     * @return the color of the diffuse value for this material.
     */
    public ReadOnlyColorRGBA getDiffuse() {
        return _diffuse;
    }

    /**
     * @param diffuse
     *            the new diffuse color to copy into this material.
     */
    public void setDiffuse(final ReadOnlyColorRGBA diffuse) {
        _diffuse.set(diffuse);
        setNeedsRefresh(true);
    }

    /**
     * @return the color of the emissive value for this material.
     */
    public ReadOnlyColorRGBA getEmissive() {
        return _emissive;
    }

    /**
     * @param emissive
     *            the new emmisive color to copy into this material.
     */
    public void setEmissive(final ReadOnlyColorRGBA emissive) {
        _emissive.set(emissive);
        setNeedsRefresh(true);
    }

    /**
     * @return the color of the specular value for this material.
     */
    public ReadOnlyColorRGBA getSpecular() {
        return _specular;
    }

    /**
     * @param specular
     *            the new specular color to copy into this material.
     */
    public void setSpecular(final ReadOnlyColorRGBA specular) {
        _specular.set(specular);
        setNeedsRefresh(true);
    }

    /**
     * @return the shininess value of the material.
     */
    public float getShininess() {
        return _shininess;
    }

    /**
     * @param shininess
     *            the new shininess for this material. Must be between 0 and 128.
     */
    public void setShininess(final float shininess) {
        if (shininess < 0 || shininess > 128) {
            throw new IllegalArgumentException("Shininess must be between 0 and 128.");
        }
        _shininess = shininess;
        setNeedsRefresh(true);
    }

    /**
     * @return the color material mode of this material, which determines how geometry colors affect the material.
     * @see ColorMaterial
     */
    public ColorMaterial getColorMaterial() {
        return _colorMaterial;
    }

    /**
     * @param material
     *            the new color material mode for this material
     * @throws IllegalArgumentException
     *             if material is null
     */
    public void setColorMaterial(final ColorMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("material can not be null.");
        }
        _colorMaterial = material;
        setNeedsRefresh(true);
    }

    /**
     * <code>getMaterialFace</code> retrieves the face this material state affects.
     * 
     * @return the current face setting
     */
    public MaterialFace getMaterialFace() {
        return _materialFace;
    }

    /**
     * <code>setMaterialFace</code> sets the face this material state affects.
     * 
     * @param face
     *            the new face setting
     * @throws IllegalArgumentException
     *             if material is null
     */
    public void setMaterialFace(final MaterialFace face) {
        if (_materialFace == null) {
            throw new IllegalArgumentException("face can not be null.");
        }
        _materialFace = face;
        setNeedsRefresh(true);
    }

    @Override
    public StateType getType() {
        return StateType.Material;
    }

    @Override
    public void write(final Ardor3DExporter e) throws IOException {
        super.write(e);
        final OutputCapsule capsule = e.getCapsule(this);
        capsule.write(_ambient, "ambient", new ColorRGBA(DEFAULT_AMBIENT));
        capsule.write(_diffuse, "diffuse", new ColorRGBA(DEFAULT_DIFFUSE));
        capsule.write(_specular, "specular", new ColorRGBA(DEFAULT_SPECULAR));
        capsule.write(_emissive, "emissive", new ColorRGBA(DEFAULT_EMISSIVE));
        capsule.write(_shininess, "shininess", DEFAULT_SHININESS);
        capsule.write(_colorMaterial, "colorMaterial", DEFAULT_COLOR_MATERIAL);
        capsule.write(_materialFace, "materialFace", DEFAULT_MATERIAL_FACE);
    }

    @Override
    public void read(final Ardor3DImporter e) throws IOException {
        super.read(e);
        final InputCapsule capsule = e.getCapsule(this);
        _ambient.set((ColorRGBA) capsule.readSavable("ambient", new ColorRGBA(DEFAULT_AMBIENT)));
        _diffuse.set((ColorRGBA) capsule.readSavable("diffuse", new ColorRGBA(DEFAULT_DIFFUSE)));
        _specular.set((ColorRGBA) capsule.readSavable("specular", new ColorRGBA(DEFAULT_SPECULAR)));
        _emissive.set((ColorRGBA) capsule.readSavable("emissive", new ColorRGBA(DEFAULT_EMISSIVE)));
        _shininess = capsule.readFloat("shininess", DEFAULT_SHININESS);
        _colorMaterial = capsule.readEnum("colorMaterial", ColorMaterial.class, DEFAULT_COLOR_MATERIAL);
        _materialFace = capsule.readEnum("materialFace", MaterialFace.class, DEFAULT_MATERIAL_FACE);
    }

    @Override
    public StateRecord createStateRecord() {
        return new MaterialStateRecord();
    }
}
