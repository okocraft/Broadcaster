package net.okocraft.broadcaster.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MessageSet {

    public static final MessageSet EMPTY =
            new MessageSet(Locale.getDefault(), Collections.emptyList()) {
                @Override
                public void next() {
                }

                @Override
                public @NotNull Component getCurrentMessage() {
                    return Component.empty();
                }
            };

    private final Locale locale;
    private final List<Component> messages;

    private int current = 0;

    private MessageSet(@NotNull Locale locale, @NotNull List<Component> messages) {
        this.locale = locale;
        this.messages = List.copyOf(messages);
    }

    public static MessageSet create(@NotNull Locale locale, @NotNull List<Component> messages,
                                    @NotNull Set<TextReplacementConfig> replacements) {
        var replaced = new ArrayList<Component>();

        for (var message : messages) {
            for (var replacement : replacements) {
                message = message.replaceText(replacement);
            }

            replaced.add(message);
        }

        return new MessageSet(locale, replaced);
    }

    public @NotNull Locale getLocale() {
        return locale;
    }

    public @NotNull @Unmodifiable List<Component> getMessages() {
        return messages;
    }

    public void next() {
        if (current < messages.size() - 1) {
            current++;
        } else {
            current = 0;
        }
    }

    public @NotNull Component getCurrentMessage() {
        return messages.get(current);
    }
}
