package org.nig.smp.connectapi.websocket;

public final class WebSocketFrame {

    public static final int OP_CONTINUATION = 0x0;
    public static final int OP_TEXT = 0x1;
    public static final int OP_BINARY = 0x2;
    public static final int OP_CLOSE = 0x8;
    public static final int OP_PING = 0x9;
    public static final int OP_PONG = 0xA;

    private WebSocketFrame() {
    }

    public static byte[] serverFrame(int opcode, byte[] payload) {
        int len = payload.length;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(len + 14);
        out.write(0x80 | opcode);
        if (len < 126) {
            out.write(len);
        } else if (len <= 0xFFFF) {
            out.write(126);
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) {
                out.write((int) (len >>> (8 * i)) & 0xFF);
            }
        }
        out.write(payload, 0, payload.length);
        return out.toByteArray();
    }

    public static byte[] serverText(String text) {
        return serverFrame(OP_TEXT, text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static byte[] serverClose() {
        return serverFrame(OP_CLOSE, new byte[]{0x03, (byte) 0xE8});
    }

    public static byte[] serverPong(byte[] payload) {
        return serverFrame(OP_PONG, payload);
    }
}
