package org.nig.smp.connectapi.websocket;

import org.nig.smp.connectapi.core.MessageHandler;
import org.nig.smp.connectapi.core.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketConnection implements Runnable {

    private static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Pattern HEADER = Pattern.compile("^\\s*(\\S+)\\s*:\\s*(.*?)\\s*$");

    private final Socket socket;
    private final SessionManager sessions;
    private final MessageHandler handler;

    private volatile boolean open = true;
    private volatile boolean authed = false;
    private OutputStream out;

    public WebSocketConnection(Socket socket, SessionManager sessions, MessageHandler handler) {
        this.socket = socket;
        this.sessions = sessions;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (Socket s = socket) {
            s.setTcpNoDelay(true);
            InputStream in = s.getInputStream();
            this.out = s.getOutputStream();

            if (!handshake(in, out)) {
                return;
            }

            sessions.add(this);
            handler.onOpen(this);

            ByteArrayOutputStream message = new ByteArrayOutputStream();
            int fragmentOpcode = -1;

            while (open) {
                int b0 = in.read();
                if (b0 < 0) break;
                int b1 = in.read();
                if (b1 < 0) break;

                boolean fin = (b0 & 0x80) != 0;
                int opcode = b0 & 0x0F;
                boolean masked = (b1 & 0x80) != 0;
                long len = b1 & 0x7F;

                if (len == 126) {
                    len = readUInt16(in);
                } else if (len == 127) {
                    len = readUInt64(in);
                }
                if (len > 8 * 1024 * 1024) {
                    close("frame too large");
                    break;
                }

                byte[] mask = null;
                if (masked) {
                    mask = new byte[4];
                    readFully(in, mask);
                }

                byte[] payload = new byte[(int) len];
                readFully(in, payload);
                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] ^= mask[i & 3];
                    }
                }

                if (opcode == WebSocketFrame.OP_CLOSE) {
                    sendRaw(WebSocketFrame.serverClose());
                    close("client closed");
                    break;
                } else if (opcode == WebSocketFrame.OP_PING) {
                    sendRaw(WebSocketFrame.serverPong(payload));
                    continue;
                } else if (opcode == WebSocketFrame.OP_PONG) {
                    continue;
                } else if (opcode == WebSocketFrame.OP_TEXT || opcode == WebSocketFrame.OP_BINARY || opcode == WebSocketFrame.OP_CONTINUATION) {
                    if (opcode == WebSocketFrame.OP_CONTINUATION) {
                        message.write(payload);
                    } else {
                        message.reset();
                        message.write(payload);
                        fragmentOpcode = opcode;
                    }
                    if (fin) {
                        byte[] data = message.toByteArray();
                        message.reset();
                        if (fragmentOpcode == WebSocketFrame.OP_TEXT && data.length > 0) {
                            handler.onText(this, new String(data, StandardCharsets.UTF_8));
                        }
                        fragmentOpcode = -1;
                    }
                }
            }
        } catch (IOException ignored) {
            // connection dropped
        } finally {
            close("disconnected");
        }
    }

    private boolean handshake(InputStream in, OutputStream out) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int lines = 0;
        while (true) {
            int c = in.read();
            if (c < 0) return false;
            buf.write(c);
            if (c == '\n') {
                lines++;
                byte[] b = buf.toByteArray();
                int size = b.length;
                boolean blank = size >= 4 && b[size - 4] == '\r' && b[size - 3] == '\n'
                        && b[size - 2] == '\r' && b[size - 1] == '\n';
                if (!blank && size >= 2 && b[size - 2] == '\n' && b[size - 1] == '\n') {
                    blank = true;
                }
                if (blank) break;
                if (lines > 100) return false;
            }
        }

        String headers = buf.toString(StandardCharsets.ISO_8859_1.name());
        String key = null;
        boolean upgrade = false;
        for (String line : headers.split("\r\n|\n")) {
            if (line.startsWith("GET ")) {
                upgrade = line.contains("HTTP/1.1");
                continue;
            }
            Matcher m = HEADER.matcher(line);
            if (m.matches()) {
                if ("Sec-WebSocket-Key".equalsIgnoreCase(m.group(1))) {
                    key = m.group(2);
                }
                if ("Upgrade".equalsIgnoreCase(m.group(1)) && "websocket".equalsIgnoreCase(m.group(2))) {
                    upgrade = true;
                }
            }
        }
        if (key == null || !upgrade) {
            return false;
        }

        String accept;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            accept = Base64.getEncoder().encodeToString(
                    sha1.digest((key + WS_MAGIC).getBytes(StandardCharsets.ISO_8859_1)));
        } catch (NoSuchAlgorithmException e) {
            return false;
        }

        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n"
                + "\r\n";
        out.write(response.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
        return true;
    }

    private static int readUInt16(InputStream in) throws IOException {
        int b0 = in.read(), b1 = in.read();
        if (b0 < 0 || b1 < 0) throw new IOException("eof");
        return (b0 << 8) | b1;
    }

    private static long readUInt64(InputStream in) throws IOException {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            int b = in.read();
            if (b < 0) throw new IOException("eof");
            v = (v << 8) | b;
        }
        return v;
    }

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int read = 0;
        while (read < buf.length) {
            int n = in.read(buf, read, buf.length - read);
            if (n < 0) throw new IOException("eof");
            read += n;
        }
    }

    public synchronized void send(String text) {
        sendRaw(WebSocketFrame.serverText(text));
    }

    public synchronized void sendRaw(byte[] data) {
        if (!open || out == null) return;
        try {
            out.write(data);
            out.flush();
        } catch (IOException e) {
            close("write failed");
        }
    }

    public void close(String reason) {
        if (!open) return;
        open = false;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        sessions.remove(this);
        handler.onClose(this, reason);
    }

    public boolean isAuthed() {
        return authed;
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;
    }
}
