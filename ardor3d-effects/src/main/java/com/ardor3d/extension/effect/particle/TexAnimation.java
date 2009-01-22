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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class TexAnimation implements Savable {

    protected List<AnimationEntry> entries = new ArrayList<AnimationEntry>();

    public void addEntry(final AnimationEntry entry) {
        entries.add(entry);
    }

    public void addEntry(final int index, final AnimationEntry entry) {
        entries.add(index, entry);
    }

    public void clearEntries() {
        entries.clear();
    }

    public Iterator<AnimationEntry> getEntries() {
        return entries.iterator();
    }

    public void removeEntry(final AnimationEntry entry) {
        entries.remove(entry);
    }

    public void removeEntry(final int index) {
        entries.remove(index);
    }

    public int getTexIndexAtAge(double age, double maxAge, final ParticleSystem particles) {
        // find what AnimationEntry we last passed...
        double trAge = 0, lastAge = 0;
        AnimationEntry latest = null;
        maxAge /= 1000f;
        age /= 1000f;
        for (int i = 0; i < entries.size(); i++) {
            final AnimationEntry entry = entries.get(i);
            trAge += (entry.getOffset() * maxAge);
            if (trAge <= age) {
                latest = entry;
                lastAge = trAge;
            } else {
                break;
            }
        }

        if (latest == null) {
            return particles.getStartTexIndex();
        } else {
            int index = (int) ((age - lastAge) / latest.rate);
            index %= latest.frames.length;
            return latest.frames[index];
        }
    }

    public Class<? extends TexAnimation> getClassTag() {
        return getClass();
    }

    public void read(final Ardor3DImporter im) throws IOException {
        final InputCapsule capsule = im.getCapsule(this);
        entries = capsule.readSavableList("entries", null);
        if (entries == null) {
            entries = new ArrayList<AnimationEntry>();
        }
    }

    public void write(final Ardor3DExporter ex) throws IOException {
        final OutputCapsule capsule = ex.getCapsule(this);
        capsule.writeSavableList(entries, "entries", null);
    }

}
