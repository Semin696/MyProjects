package me.plreloader.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public static Component colorize(String text) {
        return LEGACY.deserialize(text);
    }

    public static String format(String message, String placeholder, String value) {
        return message.replace("%" + placeholder + "%", value);
    }
}
