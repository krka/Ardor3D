/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>RampEntry</code> defines an entry for a ParticleAppearanceRamp.
 * 
 * @see ParticleAppearanceRamp
 */
public class RampEntry implements Savable {

    public static final double DEFAULT_OFFSET = 0.05f; // (5% of lifetime)
    public static final double DEFAULT_SIZE = -1; // special case -> negative = no size change at this entry
    public static final double DEFAULT_SPIN = Float.MAX_VALUE; // special case -> no spin change
    public static final double DEFAULT_MASS = Float.MAX_VALUE; // special case -> no mass change
    public static final ColorRGBA DEFAULT_COLOR = null; // special case -> no color change

    protected double offset = DEFAULT_OFFSET;
    protected ColorRGBA color = DEFAULT_COLOR; // no color change at this entry
    protected double size = DEFAULT_SIZE;
    protected double spin = DEFAULT_SPIN;
    protected double mass = DEFAULT_MASS;

    public RampEntry() {}

    /**
     * Construct new addition to color ramp
     * 
     * @param offset
     *            amount of time (as a percent of total lifetime) between the last appearance and this one.
     */
    public RampEntry(final double offset) {
        setOffset(offset);
    }

    public ColorRGBA getColor() {
        return color;
    }

    public void setColor(final ColorRGBA color) {
        this.color = color;
    }

    public boolean hasColorSet() {
        return color != DEFAULT_COLOR;
    }

    public double getSize() {
        return size;
    }

    public void setSize(final double size) {
        this.size = size;
    }

    public boolean hasSizeSet() {
        return size != DEFAULT_SIZE;
    }

    public double getSpin() {
        return spin;
    }

    public void setSpin(final double spin) {
        this.spin = spin;
    }

    public boolean hasSpinSet() {
        return spin != DEFAULT_SPIN;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(final double mass) {
        this.mass = mass;
    }

    public boolean hasMassSet() {
        return mass != DEFAULT_MASS;
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(final double offset) {
        this.offset = offset;
    }

    public Class<? extends RampEntry> getClassTag() {
        return getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule capsule = im.getCapsule(this);
        offset = capsule.readDouble("offsetMS", DEFAULT_OFFSET);
        size = capsule.readDouble("size", DEFAULT_SIZE);
        spin = capsule.readDouble("spin", DEFAULT_SPIN);
        mass = capsule.readDouble("mass", DEFAULT_MASS);
        color = (ColorRGBA) capsule.readSavable("color", DEFAULT_COLOR);
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(offset, "offsetMS", DEFAULT_OFFSET);
        capsule.write(size, "size", DEFAULT_SIZE);
        capsule.write(spin, "spin", DEFAULT_SPIN);
        capsule.write(mass, "mass", DEFAULT_MASS);
        capsule.write(color, "color", DEFAULT_COLOR);
    }

    private static String convColorToHex(final ColorRGBA color) {
        if (color == null) {
            return null;
        }
        String sRed = Integer.toHexString((int) (color.getRed() * 255 + .5f));
        if (sRed.length() == 1) {
            sRed = "0" + sRed;
        }
        String sGreen = Integer.toHexString((int) (color.getGreen() * 255 + .5f));
        if (sGreen.length() == 1) {
            sGreen = "0" + sGreen;
        }
        String sBlue = Integer.toHexString((int) (color.getBlue() * 255 + .5f));
        if (sBlue.length() == 1) {
            sBlue = "0" + sBlue;
        }
        return "#" + sRed + sGreen + sBlue;
    }

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();
        if (offset > 0) {
            builder.append("prev+");
            builder.append((int) (offset * 100));
            builder.append("% age...");
        }
        if (color != DEFAULT_COLOR) {
            builder.append("  color:");
            builder.append(convColorToHex(color).toUpperCase());
            builder.append(" a: ");
            builder.append((int) (color.getAlpha() * 100));
            builder.append("%");
        }

        if (size != DEFAULT_SIZE) {
            builder.append("  size: " + size);
        }

        if (mass != DEFAULT_MASS) {
            builder.append("  mass: " + spin);
        }

        if (spin != DEFAULT_SPIN) {
            builder.append("  spin: " + spin);
        }

        return builder.toString();
    }
}
