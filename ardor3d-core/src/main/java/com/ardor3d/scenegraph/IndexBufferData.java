/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.nio.Buffer;

public abstract class IndexBufferData<T extends Buffer> extends AbstractBufferData<T> {

    public abstract int get();

    public abstract int get(int index);

    public abstract IndexBufferData<T> put(int value);

    public abstract IndexBufferData<T> put(int index, int value);

    public abstract void put(IndexBufferData<?> buf);

}
