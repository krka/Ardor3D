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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface Ardor3DImporter {

    public Savable load(InputStream f) throws IOException;

    public Savable load(URL f) throws IOException;

    public Savable load(File f) throws IOException;

    public InputCapsule getCapsule(Savable id);
}
