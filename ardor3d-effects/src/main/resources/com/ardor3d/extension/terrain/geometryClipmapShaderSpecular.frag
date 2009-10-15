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
varying vec3 lightDir,eyeVec;
varying vec3 eyeSpacePosition;

void main()
{  
	vec4 base = texture2D(texture, texCoord);

	vec3 vVec = normalize(eyeVec);
	
	vec3 bump = normalize(texture2D(normalMap, texCoord).xyz * vec3(2.0) - vec3(1.0));

	vec4 vDiffuse = diffuse * vec4(max( dot(lightDir, bump), 0.0 ));	
	vec4 vAmb = vec4(dot(bump, vec3(0,0,1))*0.4+0.6)*ambient;
//	vec4 vAmb = ambient;

	float specular = pow(clamp(dot(reflect(-lightDir, bump), vVec), 0.0, 1.0), 
	                 gl_FrontMaterial.shininess );
		
	vec4 vSpecular = gl_LightSource[0].specular * gl_FrontMaterial.specular * vec4(specular);	
	
	vec4 color = (vAmb + vDiffuse) * base + vSpecular;

    float dist = length(eyeSpacePosition);
	float fog = clamp((gl_Fog.end - dist) * gl_Fog.scale, 0.0, 1.0);	
    gl_FragColor = mix(gl_Fog.color, color, fog);
}
