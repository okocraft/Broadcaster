package net.okocraft.broadcaster.message;

import com.github.siroshun09.configapi.bungee.BungeeYaml;
import com.github.siroshun09.configapi.common.configurable.Configurable;
import com.github.siroshun09.configapi.common.configurable.StringList;
import com.github.siroshun09.configapi.common.yaml.Yaml;
import com.github.siroshun09.mcmessage.message.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MessageFileLoader {

    private static final LegacyComponentSerializer DESERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private static final StringList MESSAGE_KEY = Configurable.createStringList("messages", Collections.emptyList());

    private static final String COMPONENTS_KEY = "components";
    private static final String TEXT_SUFFIX = ".text";
    private static final String TYPE_SUFFIX = ".type";
    private static final String CONTENTS_SUFFIX = ".contents";
    private static final String CLICK_EVENT_SUFFIX = ".click-event";
    private static final String HOVER_EVENT_SUFFIX = ".hover-event";

    private MessageFileLoader() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull List<Message> loadMessages(@NotNull Yaml yaml) {
        List<Message> result = new ArrayList<>();

        for (var message : yaml.get(MESSAGE_KEY)) {
            result.add(Message.of(message));
        }

        return result;
    }

    public static @NotNull Set<TextReplacementConfig> loadPlaceholders(@NotNull BungeeYaml yaml) {
        var bungeeConfig = (Configuration) yaml.getConfig();
        var componentsSection = bungeeConfig.getSection(COMPONENTS_KEY);

        if (componentsSection == null) {
            return Collections.emptySet();
        }

        Set<TextReplacementConfig> result = new HashSet<>();

        for (var key : componentsSection.getKeys()) {
            result.add(TextReplacementConfig.builder()
                    .match('%' + key + '%')
                    .replacement(buildComponent(componentsSection, key))
                    .build()
            );
        }

        return result;
    }

    private static @NotNull Component buildComponent(@NotNull Configuration section, @NotNull String keyPrefix) {
        var text = section.getString(keyPrefix + TEXT_SUFFIX);

        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        var baseComponent = DESERIALIZER.deserialize(text);

        var clickEventKey = keyPrefix + CLICK_EVENT_SUFFIX;

        if (section.contains(clickEventKey)) {
            baseComponent = baseComponent.clickEvent(
                    buildClickEvent(section, clickEventKey)
            );
        }

        var hoverEventKey = keyPrefix + HOVER_EVENT_SUFFIX;

        if (section.contains(hoverEventKey)) {
            baseComponent = baseComponent.hoverEvent(
                    buildHoverEvent(section, hoverEventKey)
            );
        }

        return baseComponent;
    }

    private static @Nullable ClickEvent buildClickEvent(@NotNull Configuration section, @NotNull String keyPrefix) {
        var contents = section.getString(keyPrefix + CONTENTS_SUFFIX);

        if (contents == null || contents.isEmpty()) {
            return null;
        }

        var type = section.getString(keyPrefix + TYPE_SUFFIX);

        switch (type.toLowerCase()) {
            case "open_url":
                return ClickEvent.openUrl(contents);
            case "run_command":
                return ClickEvent.runCommand(contents);
            case "suggest_command":
                return ClickEvent.suggestCommand(contents);
            case "copy_to_clipboard":
                return ClickEvent.copyToClipboard(contents);
            default:
                return null;
        }
    }

    private static @Nullable HoverEvent<Component> buildHoverEvent(@NotNull Configuration section, @NotNull String keyPrefix) {
        var contents = section.getString(keyPrefix + CONTENTS_SUFFIX);

        if (contents != null && !contents.isEmpty()) {
            return HoverEvent.showText(DESERIALIZER.deserialize(contents));
        } else {
            return null;
        }
    }
}
