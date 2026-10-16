#version 150

in vec2 panelUv;
uniform vec4 ColorModulator;
uniform vec2 UiSize;
uniform float UiTime;
uniform vec3 GodTint;
out vec4 fragColor;

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

void main() {
    // Quantize in GUI pixels so the texture follows Minecraft's GUI scale.
    vec2 pixel = floor(panelUv * UiSize);
    vec2 tile = floor(pixel / 4.0);
    vec2 uv = pixel / max(UiSize, vec2(1.0));
    float cloud = sin(tile.x * 0.11 + sin(tile.y * 0.09) * 2.0)
                + cos(tile.y * 0.14 - tile.x * 0.045);
    float band = floor(clamp(cloud * 0.22 + 0.34, 0.0, 1.0) * 4.0) / 4.0;
    // Keep the nebula in the margin, without a bright core behind body text.
    float edge = smoothstep(0.35, 1.0, uv.x) * (0.5 + uv.y * 0.5);
    vec3 color = vec3(0.028, 0.043, 0.076);
    color += mix(vec3(0.040, 0.062, 0.095), GodTint * 0.045, 0.2) * band * edge;
    color += (hash(tile) - 0.5) * 0.007;
    vec2 cell = floor(pixel / 24.0);
    vec2 local = mod(pixel, 24.0);
    vec2 center = floor(vec2(hash(cell + 4.0), hash(cell + 29.0)) * 18.0) + 3.0;
    vec2 d = abs(local - center);
    float seed = hash(cell + 71.0);
    float star = step(d.x, 0.1) * step(d.y, 0.1);
    float cross = max(step(d.x, 0.1) * step(d.y, 1.1), step(d.y, 0.1) * step(d.x, 1.1));
    star = max(star, cross * step(0.97, seed) * 0.55);
    float shine = 0.28 + floor((sin(UiTime * 0.25 + seed * 60.0) * 0.5 + 0.5) * 3.0) * 0.035;
    color += vec3(0.55, 0.69, 0.83) * star * step(0.79, seed) * shine;
    fragColor = vec4(color, 1.0) * ColorModulator;
}
