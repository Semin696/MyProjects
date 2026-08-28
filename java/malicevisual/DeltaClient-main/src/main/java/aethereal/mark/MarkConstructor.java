package aethereal.mark;

import net.minecraft.util.math.Vec3d;

public class MarkConstructor {
    private String name;
    private double x;
    private double y;
    private double z;
    private String dimension;

    public MarkConstructor() {
        this.name = "";
        this.dimension = "";
    }

    public MarkConstructor(String name, double x, double y, double z, String dimension) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension == null ? "" : dimension;
    }

    public String name() {
        return this.name;
    }

    public void name(String name) {
        this.name = name;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    public void pos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String dimension() {
        return this.dimension == null ? "" : this.dimension;
    }

    public void dimension(String dimension) {
        this.dimension = dimension == null ? "" : dimension;
    }

    public Vec3d pos() {
        return new Vec3d(this.x, this.y, this.z);
    }

    public String coords() {
        return ((int) Math.floor(this.x)) + "  " + ((int) Math.floor(this.y)) + "  " + ((int) Math.floor(this.z));
    }
}
