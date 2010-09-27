/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text.font;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public interface FontProvider {

    UIFont getClosestMatchingFont(Map<String, Object> currentStyles, AtomicReference<Double> scale);

}
