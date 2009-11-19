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

varying vec2 texCoord;
varying vec4 diffuse,ambient;
varying vec3 lightDir;
varying vec3 eyeSpacePosition;

void main()
{  
	vec4 base = texture2D(texture, texCoord);
	
	vec3 bump = normalize(texture2D(normalMap, texCoord).xyz * vec3(2.0) - vec3(1.0));

	vec4 vDiffuse = diffuse * vec4(max( dot(lightDir, bump), 0.0 ));	

	vec4 color = (ambient + vDiffuse) * base;
	
    float dist = length(eyeSpacePosition);
	float fog = clamp((gl_Fog.end - dist) * gl_Fog.scale, 0.0, 1.0);	
    gl_FragColor = mix(gl_Fog.color, color, fog);
}
