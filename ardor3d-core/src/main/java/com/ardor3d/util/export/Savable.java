/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export;

import java.io.IOException;

public interface Savable {
    void write(Ardor3DExporter ex) throws IOException;

    void read(Ardor3DImporter im) throws IOException;

    Class<?> getClassTag();
}
