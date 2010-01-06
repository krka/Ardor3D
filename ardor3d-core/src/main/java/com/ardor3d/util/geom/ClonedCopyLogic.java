/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;

public class ClonedCopyLogic implements CopyLogic {
    public Spatial copy(final Spatial source, final AtomicBoolean recurse) {
        try {
            recurse.set(false);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final BinaryExporter exporter = new BinaryExporter();
            exporter.save(source, bos);
            bos.flush();
            final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            final BinaryImporter importer = new BinaryImporter();
            final Savable sav = importer.load(bis);
            return (Spatial) sav;
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
