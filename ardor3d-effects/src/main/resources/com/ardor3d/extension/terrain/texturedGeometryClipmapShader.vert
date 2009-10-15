/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
 
uniform float scale; 
uniform vec3 eyePosition;
uniform float vertexDistance;      // Clip-Gridspacing
uniform float clipSideSize;     // Clip-Size

varying vec2 vVertex; 
varying vec3 eyeSpacePosition;

void main(void){	
	gl_TexCoord[0] = gl_MultiTexCoord0;

	vVertex = (gl_Vertex.xz - eyePosition.xz) * vec2(scale);
    
    vec4 position = gl_Vertex;

//////////// terrain blending
    float fac = clipSideSize * vertexDistance * 0.5;
    vec2 term = abs(position.xz - eyePosition.xz);
    term = (term - vec2(fac*0.6)) / vec2(fac*0.3);
    float result = max(term.x, term.y);
    float blend = clamp(result, 0.0, 1.0);

    position.y = mix(position.y, position.w, blend);
    position.w = 1.0;
//////////////
    
    gl_Position = gl_ModelViewProjectionMatrix * position;

    eyeSpacePosition = (gl_ModelViewMatrix * position).xyz;
}
