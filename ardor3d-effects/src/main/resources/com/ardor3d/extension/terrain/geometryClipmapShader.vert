/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
 
uniform float texelSize; 
uniform vec3 eyePosition;

uniform float vertexDistance;      // Clip-Gridspacing
uniform float clipSideSize;     // Clip-Size

varying vec2 texCoord;
varying vec4 diffuse,ambient;
varying vec3 lightDir;
varying vec3 eyeSpacePosition;

void main(void){	    
    vec4 position = gl_Vertex;

	texCoord = position.xz * vec2(texelSize);
    	
//////////// terrain blending
    float fac = clipSideSize * vertexDistance * 0.5;
    vec2 term = abs(position.xz - eyePosition.xz);
    term = (term - vec2(fac*0.6)) / vec2(fac*0.3);
    float result = max(term.x, term.y);
    float blend = clamp(result, 0.0, 1.0);

    position.y = mix(position.y, position.w, blend);
    position.w = 1.0;
//////////////
    
    vec3 n = normalize(gl_NormalMatrix * vec3(0,1,0));
	vec3 t = normalize(gl_NormalMatrix * vec3(0,0,1));
	vec3 b = cross(n, t);
	
	vec3 tmpVec = gl_LightSource[0].position.xyz;
	lightDir.x = dot(tmpVec, t);
	lightDir.y = dot(tmpVec, b);
	lightDir.z = dot(tmpVec, n);
    	
	diffuse = gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse;
	ambient = gl_FrontMaterial.ambient * gl_LightSource[0].ambient;
	ambient += gl_LightModel.ambient * gl_FrontMaterial.ambient;
    
    gl_Position = gl_ModelViewProjectionMatrix * position;
    
    eyeSpacePosition = (gl_ModelViewMatrix * position).xyz;
}
