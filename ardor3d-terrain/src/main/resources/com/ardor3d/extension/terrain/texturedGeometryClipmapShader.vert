/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
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

//varying vec4 diffuse,ambient;
//varying vec3 lightDir;
//varying vec3 normal;

void main(void){	
	gl_TexCoord[0] = gl_MultiTexCoord0;

	vVertex = (gl_Vertex.xz - eyePosition.xz) * vec2(scale/32.0);
    
    vec4 position = gl_Vertex;

//////////// terrain blending
    float scaledClipSideSize = clipSideSize * vertexDistance * 0.5;
    vec2 viewDistance = abs(position.xz - eyePosition.xz);
    float maxDistance = max(viewDistance.x, viewDistance.y)/scaledClipSideSize;
    float blend = clamp((maxDistance - 0.51) * 2.2, 0.0, 1.0);

    position.y = mix(position.y, position.w, blend);
    position.w = 1.0;
//////////////
    
    gl_Position = gl_ModelViewProjectionMatrix * position;

//    vec3 n = normalize(gl_NormalMatrix * vec3(0,1,0));
//	vec3 t = normalize(gl_NormalMatrix * vec3(0,0,1));
//	vec3 b = cross(n, t);
//	
//	lightDir = gl_LightSource[0].position.xyz;
//    	
//	diffuse = gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse;
//	ambient = gl_FrontMaterial.ambient * gl_LightSource[0].ambient;
//	ambient += gl_LightModel.ambient * gl_FrontMaterial.ambient;
//	
//	normal = gl_NormalMatrix * gl_Normal;

    eyeSpacePosition = (gl_ModelViewMatrix * position).xyz;
}
