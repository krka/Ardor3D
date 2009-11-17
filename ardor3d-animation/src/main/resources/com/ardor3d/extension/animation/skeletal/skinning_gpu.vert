/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

attribute vec3 Weights;
attribute vec4 JointIDs;

uniform mat4 JointPallete[50];

varying vec3 N;

void main(void) {
    mat4 mat = mat4(0.0);
    
    for ( int i = 0; i < 4; i++) {
        mat += JointPallete[int(JointIDs[i])] * Weights[i];
    }
    
    gl_Position = gl_ModelViewProjectionMatrix * (mat * gl_Vertex);
    
    N = gl_NormalMatrix * (mat3(mat[0].xyz,mat[1].xyz,mat[2].xyz) * gl_Normal);
    
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
