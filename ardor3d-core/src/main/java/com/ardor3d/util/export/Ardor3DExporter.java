/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface Ardor3DExporter {

    public boolean save(Savable object, OutputStream f) throws IOException;

    public boolean save(Savable object, File f) throws IOException;

    public OutputCapsule getCapsule(Savable object);
}
