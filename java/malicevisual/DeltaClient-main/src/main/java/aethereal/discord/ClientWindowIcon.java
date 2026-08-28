package aethereal.discord;

import net.minecraft.client.texture.NativeImage;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ClientWindowIcon {
    private ClientWindowIcon() {
    }

    public static void apply(long handle) {
        if (handle == 0L) {
            return;
        }
        try (InputStream stream = ClientWindowIcon.class.getResourceAsStream("/assets/skeleton/icon.png")) {
            if (stream == null) {
                return;
            }
            NativeImage source = NativeImage.read(stream);
            int[] sizes = {16, 32, 48, 64};
            GLFWImage.Buffer images = GLFWImage.malloc(sizes.length);
            ByteBuffer[] pixels = new ByteBuffer[sizes.length];
            try {
                for (int i = 0; i < sizes.length; i++) {
                    pixels[i] = rgba(source, sizes[i]);
                    images.position(i);
                    images.width(sizes[i]);
                    images.height(sizes[i]);
                    images.pixels(pixels[i]);
                }
                images.position(0);
                GLFW.glfwSetWindowIcon(handle, images);
            } finally {
                source.close();
                images.free();
                for (ByteBuffer buffer : pixels) {
                    if (buffer != null) {
                        MemoryUtil.memFree(buffer);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static ByteBuffer rgba(NativeImage source, int size) {
        ByteBuffer buffer = MemoryUtil.memAlloc(size * size * 4);
        int width = source.getWidth();
        int height = source.getHeight();
        for (int y = 0; y < size; y++) {
            int sourceY = Math.min(height - 1, (y * height) / size);
            for (int x = 0; x < size; x++) {
                int sourceX = Math.min(width - 1, (x * width) / size);
                int argb = source.getColorArgb(sourceX, sourceY);
                buffer.put((byte) ((argb >> 16) & 255));
                buffer.put((byte) ((argb >> 8) & 255));
                buffer.put((byte) (argb & 255));
                buffer.put((byte) ((argb >> 24) & 255));
            }
        }
        buffer.flip();
        return buffer;
    }
}
