/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.binary;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import com.ardor3d.math.MathUtils;
import com.ardor3d.util.export.Ardor3DImporter;
import com.ardor3d.util.export.ByteUtils;
import com.ardor3d.util.export.ReadListener;
import com.ardor3d.util.export.Savable;

public class BinaryImporter implements Ardor3DImporter {
    private static final Logger logger = Logger.getLogger(BinaryImporter.class.getName());

    // TODO: Provide better cleanup and reuse of this class -- Good for now.

    // Key - alias, object - bco
    protected HashMap<String, BinaryClassObject> _classes;
    // Key - id, object - the savable
    protected HashMap<Integer, Savable> _contentTable;
    // Key - savable, object - capsule
    protected IdentityHashMap<Savable, BinaryInputCapsule> _capsuleTable;
    // Key - id, opject - location in the file
    protected HashMap<Integer, Integer> _locationTable;

    public static boolean _debug = false;

    protected byte[] _dataArray;
    protected int _aliasWidth;

    public BinaryImporter() {}

    public static BinaryImporter getInstance() {
        return new BinaryImporter();
    }

    public Savable load(final InputStream is) throws IOException {
        return load(is, null, null);
    }

    public Savable load(final InputStream is, final ReadListener listener) throws IOException {
        return load(is, listener, null);
    }

    public Savable load(final InputStream is, final ReadListener listener, final ByteArrayOutputStream reuseableStream)
            throws IOException {
        _contentTable = new HashMap<Integer, Savable>();
        final GZIPInputStream zis = new GZIPInputStream(is);
        BufferedInputStream bis = new BufferedInputStream(zis);
        final int numClasses = ByteUtils.readInt(bis);
        int bytes = 4;
        _aliasWidth = ((int) MathUtils.log(numClasses, 256) + 1);
        _classes = new HashMap<String, BinaryClassObject>(numClasses);
        for (int i = 0; i < numClasses; i++) {
            final String alias = readString(bis, _aliasWidth);

            final int classLength = ByteUtils.readInt(bis);
            final String className = readString(bis, classLength);
            final BinaryClassObject bco = new BinaryClassObject();
            bco._alias = alias.getBytes();
            bco._className = className;

            final int fields = ByteUtils.readInt(bis);
            bytes += (8 + _aliasWidth + classLength);

            bco._nameFields = new HashMap<String, BinaryClassField>(fields);
            bco._aliasFields = new HashMap<Byte, BinaryClassField>(fields);
            for (int x = 0; x < fields; x++) {
                final byte fieldAlias = (byte) bis.read();
                final byte fieldType = (byte) bis.read();

                final int fieldNameLength = ByteUtils.readInt(bis);
                final String fieldName = readString(bis, fieldNameLength);
                final BinaryClassField bcf = new BinaryClassField(fieldName, fieldAlias, fieldType);
                bco._nameFields.put(fieldName, bcf);
                bco._aliasFields.put(fieldAlias, bcf);
                bytes += (6 + fieldNameLength);
            }
            _classes.put(alias, bco);
        }
        if (listener != null) {
            listener.readBytes(bytes);
        }

        final int numLocs = ByteUtils.readInt(bis);
        bytes = 4;

        _capsuleTable = new IdentityHashMap<Savable, BinaryInputCapsule>(numLocs);
        _locationTable = new HashMap<Integer, Integer>(numLocs);
        for (int i = 0; i < numLocs; i++) {
            final int id = ByteUtils.readInt(bis);
            final int loc = ByteUtils.readInt(bis);
            _locationTable.put(id, loc);
            bytes += 8;
        }

        @SuppressWarnings("unused")
        final int numbIDs = ByteUtils.readInt(bis); // XXX: NOT CURRENTLY USED
        final int id = ByteUtils.readInt(bis);
        bytes += 8;
        if (listener != null) {
            listener.readBytes(bytes);
        }

        ByteArrayOutputStream baos = reuseableStream;
        if (baos == null) {
            baos = new ByteArrayOutputStream(bytes);
        } else {
            baos.reset();
        }
        int size = -1;
        final byte[] cache = new byte[4096];
        while ((size = bis.read(cache)) != -1) {
            baos.write(cache, 0, size);
            if (listener != null) {
                listener.readBytes(size);
            }
        }
        bis = null;

        _dataArray = baos.toByteArray();
        baos = null;

        final Savable rVal = readObject(id);
        if (_debug) {
            logger.info("Importer Stats: ");
            logger.info("Tags: " + numClasses);
            logger.info("Objects: " + numLocs);
            logger.info("Data Size: " + _dataArray.length);
        }
        _dataArray = null;
        return rVal;
    }

    public Savable load(final URL f) throws IOException {
        return load(f, null);
    }

    public Savable load(final URL f, final ReadListener listener) throws IOException {
        final InputStream is = f.openStream();
        final Savable rVal = load(is, listener);
        is.close();
        return rVal;
    }

    public Savable load(final File f) throws IOException {
        return load(f, null);
    }

    public Savable load(final File f, final ReadListener listener) throws IOException {
        final FileInputStream fis = new FileInputStream(f);
        final Savable rVal = load(fis, listener);
        fis.close();
        return rVal;
    }

    public Savable load(final byte[] data) throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(data);
        final Savable rVal = load(bais);
        bais.close();
        return rVal;
    }

    public BinaryInputCapsule getCapsule(final Savable id) {
        return _capsuleTable.get(id);
    }

    protected String readString(final InputStream f, final int length) throws IOException {
        final byte[] data = new byte[length];
        for (int j = 0; j < length; j++) {
            data[j] = (byte) f.read();
        }

        return new String(data);
    }

    protected String readString(final int length, final int offset) throws IOException {
        final byte[] data = new byte[length];
        for (int j = 0; j < length; j++) {
            data[j] = _dataArray[j + offset];
        }

        return new String(data);
    }

    public Savable readObject(final int id) {

        if (_contentTable.get(id) != null) {
            return _contentTable.get(id);
        }

        try {
            int loc = _locationTable.get(id);

            final String alias = readString(_aliasWidth, loc);
            loc += _aliasWidth;

            final BinaryClassObject bco = _classes.get(alias);

            if (bco == null) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "NULL class object: "
                        + alias);
                return null;
            }

            final int dataLength = ByteUtils.convertIntFromBytes(_dataArray, loc);
            loc += 4;

            final BinaryInputCapsule cap = new BinaryInputCapsule(this, bco);
            cap.setContent(_dataArray, loc, loc + dataLength);

            final Savable out = BinaryClassLoader.fromName(bco._className, cap);

            _capsuleTable.put(out, cap);
            _contentTable.put(id, out);

            out.read(this);

            _capsuleTable.remove(out);

            return out;

        } catch (final IOException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        } catch (final ClassNotFoundException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        } catch (final InstantiationException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        } catch (final IllegalAccessException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "readObject(int id)", "Exception", e);
            return null;
        }
    }
}
