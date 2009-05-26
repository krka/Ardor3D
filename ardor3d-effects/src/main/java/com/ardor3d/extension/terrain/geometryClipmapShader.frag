/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
 
uniform sampler2D texture;
uniform sampler2D normalMap;

uniform vec3 eyePosition;

varying vec2 texCoord;
varying vec3 worldSpacePosition;

uniform vec3 lightDirection;

const float    AmbientColor    = 0.3;
const float    DiffuseColor    = 0.7;
//const float    SpecularColor   = 0.3;
//const float    SpecularPower   = 64.0;

void computePerPixelLights(vec3 eyeVec, vec3 normal, out vec3 Diffuse, out vec3 Specular)
{
   float fNDotL           = dot( normal, lightDirection ); 
   vec3  fvReflection     = normalize( ( ( 2.0 * normal ) * fNDotL ) - lightDirection ); 
//   float fRDotV           = max( 0.0, dot( fvReflection, eyeVec ) );   
   Diffuse   = vec3(AmbientColor + DiffuseColor *  min(max( 0.0, fNDotL ), 1.0) ); 
//   Specular  = vec3(SpecularColor * ( pow( fRDotV, SpecularPower ) ));
}

void main()
{  
    vec3 normal = texture2D(normalMap, texCoord).xyz * vec3(2.0) - vec3(1.0);
    normal += texture2D(normalMap, texCoord * vec2(45.0)).xyz * vec3(0.8) - vec3(0.4);
    normal = normalize(normal);
    vec3 eyeVec = normalize(eyePosition - worldSpacePosition.xyz);
    
    vec3 Diffuse = vec3(0.0,0.0,0.0);
    vec3 Specular = vec3(0.0,0.0,0.0);
    computePerPixelLights(eyeVec, normal, Diffuse, Specular);
 
	vec4 tex = texture2D(texture, texCoord);
	
    tex.rgb *= Diffuse;
//    tex.rgb += Specular;

    gl_FragColor = mix(gl_Fog.color, tex, gl_FogFragCoord);
}
