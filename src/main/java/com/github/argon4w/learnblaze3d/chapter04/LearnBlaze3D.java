package com.github.argon4w.learnblaze3d.chapter04;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.util.OptionalInt;

// 指定MOD主类
@Mod(LearnBlaze3D.MOD_ID)
public class LearnBlaze3D {

	public static final String MOD_ID = "learnblaze3d04"; // 指南参考 MOD 的 MOD ID, 作为标识符, 将他替换为你在 gradle.properties 或 neoforge.mods.toml 中指定的 MOD ID.
	public static final Logger LOGGER = LogUtils.getLogger(); // 指南参考 MOD 的 Logger, 作为 Log 输出.

	public static GpuDevice device; // 我们所有对 GPU 的操作都需要经过 GpuDevice (GPU 设备).
	public static CommandEncoder encoder; // 向 GPU 发送命令的 CommandEncoder (命令编码器)
	public static GpuBuffer vertexBuffer; // 这是我们交给 GPU 后, 寄存在 GPU 内部的顶点.
	public static VertexFormat vertexFormat; // 我们的顶点格式, 告诉 GPU 他要如何从传入渲染管线的顶点内存中识别顶点数据.
	public static RenderPipeline pipeline; // 我们的渲染管线, 告诉 GPU 使用哪套着色器进行顶点变换和着色.

	// 在 GameRenderer 初始化时调用, 用于初始化各种渲染资源, 仅调用一次.
	public static void setup() {
		LOGGER.info("LearnBlaze3D正在初始化.");

		// 获取到我们需安然需要用到的的 GPU 设备.
		device = RenderSystem.getDevice();

		// 我们渲染一个最基础的三角形, 需要三个顶点, 每个顶点由 3 个 float 表示其 NDC 坐标 (x, y, z) (我们暂时不用管 Z 轴).
		// 每个 float 大小为 4 byte, 因此我们一个顶点需要 3 * 4 = 12 byte 的空间, 三个顶点需要 3 * 12 = 36 byte 的空间.
		// 分配一个 36 byte 的堆外内存空间.
		var address = MemoryUtil.nmemAlloc(36);

		// 将顶点写入内存
		// 右下角
		MemoryUtil.memPutFloat(address + 0L * 12L + 0L * 4L, 0.5f); // 第 0 个顶点的第 0 个 float.
		MemoryUtil.memPutFloat(address + 0L * 12L + 1L * 4L, -0.5f); // 第 0 个顶点的第 1 个 float.
		MemoryUtil.memPutFloat(address + 0L * 12L + 2L * 4L, 0.0f); // 第 0 个顶点的第 2 个 float.
		// 正上方
		MemoryUtil.memPutFloat(address + 1L * 12L + 0L * 4L, 0.0f); // 第 1 个顶点的第 0 个 float.
		MemoryUtil.memPutFloat(address + 1L * 12L + 1L * 4L, 0.5f); // 第 1 个顶点的第 1 个 float.
		MemoryUtil.memPutFloat(address + 1L * 12L + 2L * 4L, 0.0f); // 第 1 个顶点的第 2 个 float.
		// 左下角
		MemoryUtil.memPutFloat(address + 2L * 12L + 0L * 4L, -0.5f); // 第 2 个顶点的第 0 个 float.
		MemoryUtil.memPutFloat(address + 2L * 12L + 1L * 4L, -0.5f); // 第 2 个顶点的第 1 个 float.
		MemoryUtil.memPutFloat(address + 2L * 12L + 2L * 4L, 0.0f); // 第 2 个顶点的第 2 个 float.

		/*
		写入完成后内存看起来应该是这样的:
		+-----+------+------+-------+-------+-------+-------+-------+-------+
		| 0-3 | 4-7  | 8-11 | 12-15 | 16-19 | 20-23 | 24-27 | 28-31 | 32-35 |
		+-----+------+------+-------+-------+-------+-------+-------+-------+
		| 0.5 | -0.5 | 0.0  | 0.0   | 0.5   | 0.0   | -0.5  | -0.5  | 0.0   |
		+-----+------+------+-------+-------+-------+-------+-------+-------+
		| vertex 0          | vertex 1              | vertex 2              |
		+-------------------+-----------------------+-----------------------+
		*/

		// 通过 GPU 申请一块地盘 (GPU 内存) 存放我们发给 GPU 的顶点数据, 大小和我们之前分配的 36 byte 一致.
		// 第一个参数是这片 GPU 内存的标识符, 随便怎么来都行, 这里我们叫他 Triangle Buffer, 代表这是存放我们三角形数据的内存.
		// 第二个参数是这片 GPU 内存的用途, 是个 bit flags, 我们需要 GpuBuffer.USAGE_COPY_DST 和 GpuBuffer.USAGE_VERTEX, 按位或即可获得 bit flags.
		// 第三个参数是我们申请的 GPU 内存的大小, 和之前一致, 36 byte.
		// USAGE_COPY_DEST 的意思是 "他能作为复制操作的目标内存地址 (copy destination)", 保证了我们能把堆外内存复制到这片 GPU 内存里.
		// USAGE_VERTEX 的意思则是 "这片内存里的数据能作为顶点传递到渲染管线里".
		vertexBuffer = device.createBuffer(() -> "Triangle Buffer", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX, 36);

		// 现在我们需要对 GPU 发出命令, 告诉他把 address 代表的这段堆外内存传递给 GPU 并放到指定的 GPU 内存里.
		// 我们需要从 GPU 设备那里获得给 GPU 发号施令的工具, 命令编码器 (Command Encoder).
		encoder = device.createCommandEncoder();

		// 然后把我们的原始 address 包装成 Blaze3D 认识的 ByteBuffer.
		var offHeapBuffer = MemoryUtil.memByteBuffer(address, 36); // 把整个内存范围都打包进去, 一个字节都不能落下.

		// 再从我们的 GPU 内存中获取一片 "范围" 出来, 代表我们要复制到这片内存的哪片区域.
		// 第一个参数是 offset, 代表起点从这片内存开始偏移多少个字节, 这里填 0, 代表从头开始.
		// 第二个参数是 length, 代表这片区域有多大, 这里填 36, 代表从 0开始的整片 GPU 内存 (36 byte 大小).
		var range = vertexBuffer.slice(0L, 36L);

		// 然后用 commandEncoder 向 GPU 发出 "将这段堆外内存写入到指定的 GPU 内存内的指定范围" 的命令.
		encoder.writeToBuffer(range, offHeapBuffer);

		// 接下来我们要告诉 GPU 如何读取我们的数据, 毕竟这可没有一个预先约定好的规则.
		// 我们将构建 VertexFormat (顶点格式) 来告诉 GPU 如何读取数据.

		// 首先我们需要构建一个 VertexFormatElement, 这代表了顶点里的一个元素, 比如坐标, 颜色, 贴图什么的.
		// 这里我们的顶点只有 "坐标" 这个元素.

		// 我们可以直接使用 Minecraft 定义好的代表了 3 个 float 组成的三维空间坐标的元素.
		// var positionElement = VertexFormatElement.POSITION;
		// 但为了熟悉其原理, 我们还是一步步来手动构造. (实际渲染中不要随随便便搞新的 VertexFormatElement, 整个游戏里只能存在最多 32 个, Minecraft 自己也占用了 7 个)

		// 先获取下一个可用的元素唯一 ID, 以用于注册我们自己的元素.
		var vertexFormatElementId = VertexFormatElement.findNextId();
		// 用我们获取到的 ID 注册我们自己的元素.
		var positionElement = VertexFormatElement.register(
				vertexFormatElementId, // 我们之前拿到的 ID.
				0, // 这个是 "index", 暂时没用的样子.
				VertexFormatElement.Type.FLOAT, // type, 代表这个元素是由 float 组成的.
				false, // 是否归一化, 这个意思是如果你传入了非浮点数比如 byte, 且这个设置为 true, 那么他会把 byte 的 -128 到 127 这个范围
				       // 自动转换为 -1.0 到 1.0, unsigned byte 则是从 0 到 255 转换成 0.0 到 1.0, 但我们传入的是本身就是浮点数, 这个对我们没用.
				3 // 代表了这个元素由几个 float 组成, 这里是 3 个, 代表 x, y, z 三个 float 组成了这个元素.
		);

		// 有了顶点内部元素的定义, 我们现在可以开始构建我们的顶点格式定义了.
		vertexFormat = VertexFormat
				.builder() // 开始构建
				.add("aPosition", positionElement) // 将我们的坐标元素添加到我们的顶点格式定义里, 并命名为 aPosition, 这个命名可以让顶点着色器用相同的名字读取到这个元素.
				.build(); // 我们只有这一个元素, 所以不需要添加额外的元素了, 直接构建.
		// 现在我们定义了我们的每个顶点只有一个元素, 这个元素由 3 个 float 组成. 所以每三个 float 为一个顶点, 每个顶点都有一个由三个 float 组成的名为 aPosition的数据可供顶点着色器读取.

		// 接下来我们要定义我们的渲染管线了.
		pipeline = RenderPipeline
				.builder() // 开始构建
				.withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "triangle")) // 为我们的管线分配一个唯一的名字
				.withVertexFormat(vertexFormat, VertexFormat.Mode.TRIANGLES) // 告诉渲染管线用我们提供的顶点定义来读取顶点, 并且以三角形为图元进行渲染.
				.withVertexShader(Identifier.withDefaultNamespace("core/triangle")) // 设置这个管线的顶点着色器为 src/main/resources/assets/learnblaze3d04/shaders/core/triangle.vsh 这个着色器源码文件
				.withFragmentShader(Identifier.withDefaultNamespace("core/triangle")) // 设置这个管线的片段着色器为 src/main/resources/assets/learnblaze3d04/shaders/core/triangle.fsh 这个着色器源码文件
				.build(); // 构建管线.
	}

	// 在 GameRenderer 每帧渲染后调用, 用于渲染实例参考代码.
	public static void loop() {
		if (!Minecraft.getInstance().isGameLoadFinished()) {
			return;
		}

		try (var renderPass = encoder.createRenderPass(() -> "Draw a triangle", Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), OptionalInt.empty())) {
			renderPass.setPipeline(pipeline);
			renderPass.setVertexBuffer(0, vertexBuffer);
			renderPass.draw(0, 3);
		}
	}

	// 在 GameRenderer 终止 (如关闭游戏) 时调用, 用来清理各种渲染资源, 仅调用一次.
	public static void close() {
		LOGGER.info("LearnBlaze3D正在进行清理.");

		vertexBuffer.close();
	}
}
