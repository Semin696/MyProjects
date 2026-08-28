package aethereal.ui.shader;

import aethereal.core.EventManager;
import aethereal.core.EventTarget;
import aethereal.core.Interface;
import aethereal.event.ResizeEvent;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.awt.*;

public class NoiseShader extends Shader implements Interface {
    private static final Identifier c = Identifier.of("skeleton", "core/noise/noise_shader");
    private final Matrix4f d;
    private SimpleFramebuffer e;
    private Uniform f;
    private Uniform g;
    private Uniform h;

    public NoiseShader() {
        super(c, VertexFormats.POSITION_COLOR);
        this.d = new Matrix4f();
        EventManager.a(this);
    }

    @EventTarget
    public void a(ResizeEvent event) {
        this.e = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
    }

    public void e() {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (this.e == null || this.e.textureWidth != width || this.e.textureHeight != height) {
            this.e = new SimpleFramebuffer(width, height, true);
        }
        this.e.copyDepthFrom(mc.getFramebuffer());
        mc.getFramebuffer().beginWrite(false);
    }

    public void f() {
        if (this.e != null) {
            mc.getFramebuffer().copyDepthFrom(this.e);
            mc.getFramebuffer().beginWrite(false);
        }
    }

    @Override
    protected void b() {
        this.f = a("TintColor");
        this.g = a("Time");
        this.h = a("UseDepthMask");
    }

    public void a(float[] color) {
        a(color, 1.0f);
    }

    public void a(float[] color, float speed) {
        if (this.e != null) {
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(this.d, ProjectionType.PERSPECTIVE);
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix().identity();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
            RenderSystem.setShaderTexture(1, mc.getFramebuffer().getDepthAttachment());
            RenderSystem.setShaderTexture(2, this.e.getDepthAttachment());
            a();
            a(color, 1.0f, speed);
            int white = new Color(255, 255, 255, 255).getRGB();
            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            builder.vertex(-1.0f, -1.0f, 0.0f).color(white);
            builder.vertex(-1.0f, 1.0f, 0.0f).color(white);
            builder.vertex(1.0f, 1.0f, 0.0f).color(white);
            builder.vertex(1.0f, -1.0f, 0.0f).color(white);
            BufferRenderer.drawWithGlobalProgram(builder.end());
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.setShaderTexture(1, 0);
            RenderSystem.setShaderTexture(2, 0);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    public void b(float[] color) {
        b(color, 1.0f);
    }

    public void b(float[] color, float speed) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
        RenderSystem.setShaderTexture(1, mc.getFramebuffer().getDepthAttachment());
        RenderSystem.setShaderTexture(2, mc.getFramebuffer().getDepthAttachment());
        a();
        a(color, 0.0f, speed);
    }

    public void g() {
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShaderTexture(1, 0);
        RenderSystem.setShaderTexture(2, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void a(float[] color, float useDepthMask, float speed) {
        if (this.f != null) {
            this.f.set(color[0], color[1], color[2], color[3]);
        }
        if (this.g != null) {
            this.g.set(((System.currentTimeMillis() % 100000) / 1000.0f) * Math.max(0.05f, speed));
        }
        if (this.h != null) {
            this.h.set(useDepthMask);
        }
    }
}
