package aethereal.discord;

import aethereal.lib.javassist.Frame;
import aethereal.lib.log4j.LogManager;
import aethereal.lib.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class WindowsConnection implements aethereal.lib.jsoup.Connection {

    private static final Logger a = LogManager.b(WindowsConnection.class);

    final RandomAccessFile d;
    private final ReentrantLock c = new ReentrantLock();
    private final InputStream e = new InputStream() {
        @Override
        public int read() throws IOException {
            byte[] b2 = new byte[1];
            int n = read(b2, 0, 1);
            if (n == -1) {
                return -1;
            }
            return b2[0] & 255;
        }

        @Override
        public int read(byte[] b2, int off, int len) throws IOException {
            if (b2 == null) {
                throw new NullPointerException("b is marked non-null but is null");
            }
            if (len == 0) {
                return 0;
            }
            if (WindowsConnection.this.g) {
                return -1;
            }
            try {
                int n = WindowsConnection.this.d.read(b2, off, len);
                if (n == 0) {
                    Thread.yield();
                    n = WindowsConnection.this.d.read(b2, off, len);
                }
                return n < 0 ? -1 : n;
            } catch (IOException ex) {
                if (WindowsConnection.this.g) {
                    return -1;
                }
                throw ex;
            }
        }
    };
    private final OutputStream f = new OutputStream() {
        @Override
        public void write(int b2) throws IOException {
            WindowsConnection.this.d.write(b2);
        }

        @Override
        public void write(byte[] b2, int off, int len) throws IOException {
            if (b2 == null) {
                throw new NullPointerException("b is marked non-null but is null");
            }
            WindowsConnection.this.d.write(b2, off, len);
        }
    };
    volatile boolean g;

    public WindowsConnection(String path) throws IOException {
        this.d = b(path);
        a.a("Connected to Windows pipe: {}", path);
    }

    static RandomAccessFile b(String path) throws IOException {
        try {
            return new RandomAccessFile(path, "rw");
        } catch (FileNotFoundException e) {
            String message = e.getMessage();
            if (message != null && (message.toLowerCase().contains("access") || message.contains("Отказано"))) {
                throw new IOException(d(path), e);
            }
            throw e;
        }
    }

    public static String d(String path) {
        return "Access denied opening " + path + ". Discord is running as administrator while the game is not. Fully quit Discord (including the tray icon) and start it normally, without 'Run as administrator'.";
    }

    @Override
    public boolean a() {
        return !this.g;
    }

    @Override
    public Frame b() throws IOException {
        this.c.lock();
        try {
            h();
            return FrameReader.a(this.e);
        } finally {
            this.c.unlock();
        }
    }

    @Override
    public void a(Frame frame) throws IOException {
        this.c.lock();
        try {
            h();
            FrameWriter.a(this.f, frame);
        } finally {
            this.c.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.g) {
            return;
        }
        this.g = true;
        this.d.close();
        a.a("Windows connection closed");
    }

    private void h() throws IOException {
        if (this.g) {
            throw new IOException("Connection is closed");
        }
    }
}
