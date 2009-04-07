/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <code>LittleEndien</code> is a class to read littleendien stored data via a InputStream. All functions work as
 * defined in DataInput, but assume they come from a LittleEndien input stream. Currently used to read .ms3d and .3ds
 * files.
 */
public class LittleEndien implements DataInput {

    private final BufferedInputStream in;
    private final BufferedReader inRead;

    /**
     * Creates a new LittleEndien reader from the given input stream. The stream is wrapped in a BufferedReader
     * automatically.
     * 
     * @param in
     *            The input stream to read from.
     */
    public LittleEndien(final InputStream in) {
        this.in = new BufferedInputStream(in);
        inRead = new BufferedReader(new InputStreamReader(in));
    }

    public final int readUnsignedShort() throws IOException {
        return (in.read() & 0xff) | ((in.read() & 0xff) << 8);
    }

    /**
     * read an unsigned int as a long
     */
    public final long readUInt() throws IOException {
        return ((in.read() & 0xff) | ((in.read() & 0xff) << 8) | ((in.read() & 0xff) << 16) | (((long) (in.read() & 0xff)) << 24));
    }

    public final boolean readBoolean() throws IOException {
        return (in.read() != 0);
    }

    public final byte readByte() throws IOException {
        return (byte) in.read();
    }

    public final int readUnsignedByte() throws IOException {
        return in.read();
    }

    public final short readShort() throws IOException {
        return (short) readUnsignedShort();
    }

    public final char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    public final int readInt() throws IOException {
        return ((in.read() & 0xff) | ((in.read() & 0xff) << 8) | ((in.read() & 0xff) << 16) | ((in.read() & 0xff) << 24));
    }

    public final long readLong() throws IOException {
        return ((in.read() & 0xff) | ((long) (in.read() & 0xff) << 8) | ((long) (in.read() & 0xff) << 16)
                | ((long) (in.read() & 0xff) << 24) | ((long) (in.read() & 0xff) << 32)
                | ((long) (in.read() & 0xff) << 40) | ((long) (in.read() & 0xff) << 48) | ((long) (in.read() & 0xff) << 56));
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final void readFully(final byte b[]) throws IOException {
        final int i = in.read(b, 0, b.length);
        if (i == -1) {
            throw new EOFException("EOF reached");
        }
    }

    public final void readFully(final byte b[], final int off, final int len) throws IOException {
        final int i = in.read(b, off, len);
        if (i == -1) {
            throw new EOFException("EOF reached");
        }
    }

    public final int skipBytes(final int n) throws IOException {
        return (int) in.skip(n);
    }

    public final String readLine() throws IOException {
        return inRead.readLine();
    }

    public final String readUTF() throws IOException {
        throw new IOException("Unsupported operation");
    }

    public final void close() throws IOException {
        in.close();
    }

    public final int available() throws IOException {
        return in.available();
    }
}