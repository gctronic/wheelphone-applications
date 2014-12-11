/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 Confidential and Proprietary - Qualcomm Connected Experiences, Inc.
 ==============================================================================*/

const char* vertexShader = MAKESTRING(
attribute vec4 vertexPosition;
attribute vec4 vertexNormal;
attribute vec2 vertexTexCoord;

varying vec2 texCoord;
varying vec4 normal;

uniform mat4 modelViewProjectionMatrix;

void main()
{
    gl_Position = modelViewProjectionMatrix * vertexPosition;
    normal = vertexNormal;
    texCoord = vertexTexCoord;
}
);
