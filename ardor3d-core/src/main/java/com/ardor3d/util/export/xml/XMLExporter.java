/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import com.ardor3d.util.export.Ardor3DExporter;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Part of the ardor3d XML IO system
 */
public class XMLExporter implements Ardor3DExporter {
    public static final String ELEMENT_MAPENTRY = "MapEntry";
    public static final String ELEMENT_KEY = "Key";
    public static final String ELEMENT_VALUE = "Value";
    public static final String ELEMENT_FLOATBUFFER = "FloatBuffer";
    public static final String ATTRIBUTE_SIZE = "size";

    private DOMOutputCapsule _domOut;

    public XMLExporter() {

    }

    public boolean save(final Savable object, final OutputStream f) throws IOException {
        try {
            // Initialize Document when saving so we don't retain state of previous exports
            _domOut = new DOMOutputCapsule(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument(),
                    this);
            _domOut.write(object, object.getClass().getName(), null);
            DOM_PrettyPrint.serialize(_domOut.getDoc(), f);
            f.flush();
            return true;
        } catch (final Exception ex) {
            final IOException e = new IOException();
            e.initCause(ex);
            throw e;
        }
    }

    public boolean save(final Savable object, final File f) throws IOException {
        return save(object, new FileOutputStream(f));
    }

    public OutputCapsule getCapsule(final Savable object) {
        return _domOut;
    }

    public static XMLExporter getInstance() {
        return new XMLExporter();
    }

}
