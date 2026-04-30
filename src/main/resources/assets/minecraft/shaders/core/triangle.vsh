// 指定我们着色器所使用 GLSL 语言的版本, 如果指定的版本过低将无法使用部分功能, 如果你的设备不支持更高的版本, 着色器编译器则会报错.
#version 330

in vec3 aPosition;

void main() {
    // gl_Position 是顶点着色器程序的输出, 我们将输入的顶点坐标变换为 NDC 坐标后将其写入 gl_Position, 由渲染管线进行下一步操作.

    // 但 gl_Position 是个 vec4 (四维向量), 第四维度是 w 轴, 目前我们暂时不需要理解它, 先将他统一设置为 1.0 即可.
    // 将我们传入的 aPosition 三维 NDC 空间向量拓展为 4 维向量后直接传递给 gl_Position 即可.
    gl_Position = vec4(aPosition, 1.0);
}
