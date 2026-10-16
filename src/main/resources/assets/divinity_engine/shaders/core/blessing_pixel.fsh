#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec2 dimensions = vec2(textureSize(Sampler0, 0));
    vec2 pixel = clamp(floor(texCoord0 * dimensions), vec2(0.0), dimensions - vec2(1.0));
    vec4 color = texture(Sampler0, (pixel + vec2(0.5)) / dimensions) * vertexColor * ColorModulator;
    if (color.a < 0.02) discard;
    fragColor = color;
}
