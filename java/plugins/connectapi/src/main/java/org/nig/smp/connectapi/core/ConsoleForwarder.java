package org.nig.smp.connectapi.core;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class ConsoleForwarder extends AbstractAppender {

    private final SessionManager sessions;

    private ConsoleForwarder(SessionManager sessions) {
        super("ConnectApiConsole", null, null, false, Property.EMPTY_ARRAY);
        this.sessions = sessions;
    }

    public static ConsoleForwarder start(SessionManager sessions) {
        ConsoleForwarder appender = new ConsoleForwarder(sessions);
        Logger root = (Logger) LogManager.getRootLogger();
        appender.start();
        root.addAppender(appender);
        return appender;
    }

    @Override
    public void append(LogEvent event) {
        if (event == null || event.getMessage() == null) return;
        String line = event.getMessage().getFormattedMessage();
        if (line == null || line.isEmpty()) return;

        JsonObject obj = new JsonObject();
        obj.addProperty("op", "log");
        obj.addProperty("level", event.getLevel() != null ? event.getLevel().name() : "INFO");
        obj.addProperty("line", line);
        obj.addProperty("ts", event.getTimeMillis());
        sessions.broadcast(obj);
    }

    public void shutdown() {
        try {
            Logger root = (Logger) LogManager.getRootLogger();
            root.removeAppender(this);
            this.stop();
        } catch (Throwable ignored) {
        }
    }
}
