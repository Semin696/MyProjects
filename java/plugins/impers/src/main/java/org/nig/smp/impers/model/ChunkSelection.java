package org.nig.smp.impers.model;

public class ChunkSelection {

    private int x1, z1;
    private int x2, z2;
    private boolean hasPos1;
    private boolean hasPos2;

    public void setPos1(int chunkX, int chunkZ) {
        this.x1 = chunkX;
        this.z1 = chunkZ;
        this.hasPos1 = true;
    }

    public void setPos2(int chunkX, int chunkZ) {
        this.x2 = chunkX;
        this.z2 = chunkZ;
        this.hasPos2 = true;
    }

    public boolean hasPos1() {
        return hasPos1;
    }

    public boolean hasPos2() {
        return hasPos2;
    }

    public boolean isComplete() {
        return hasPos1 && hasPos2;
    }

    public int getMinChunkX() {
        return Math.min(x1, x2);
    }

    public int getMaxChunkX() {
        return Math.max(x1, x2);
    }

    public int getMinChunkZ() {
        return Math.min(z1, z2);
    }

    public int getMaxChunkZ() {
        return Math.max(z1, z2);
    }

    public int getSizeX() {
        return getMaxChunkX() - getMinChunkX() + 1;
    }

    public int getSizeZ() {
        return getMaxChunkZ() - getMinChunkZ() + 1;
    }

    public int getPos1X() {
        return x1;
    }

    public int getPos1Z() {
        return z1;
    }

    public int getPos2X() {
        return x2;
    }

    public int getPos2Z() {
        return z2;
    }
}
