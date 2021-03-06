package net.okocraft.broadcaster.message;

import com.github.siroshun09.configapi.bungee.BungeeYaml;
import com.github.siroshun09.configapi.bungee.BungeeYamlFactory;
import com.github.siroshun09.configapi.common.configurable.Configurable;
import com.github.siroshun09.configapi.common.configurable.StringList;
import com.github.siroshun09.configapi.common.configurable.StringValue;
import com.github.siroshun09.configapi.common.yaml.Yaml;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.Translator;
import net.md_5.bungee.config.Configuration;
import net.okocraft.broadcaster.BroadcasterPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MessageFileLoader {

    private static final LegacyComponentSerializer DESERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private static final StringValue PREFIX_KEY = Configurable.create("prefix", "");
    private static final StringValue SUFFIX_KEY = Configurable.create("suffix", "");
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

    public static @NotNull MessageSet loadDefaultMessageFile(@NotNull BroadcasterPlugin plugin) {
        var yaml = BungeeYamlFactory.loadUnsafe(plugin, "messages/default.yml");

        var messageSet = loadMessageFile(yaml);

        return messageSet != null ? messageSet : MessageSet.EMPTY;
    }

    public static @NotNull @Unmodifiable Set<MessageSet> loadMessageFiles(@NotNull Path directory) throws IOException {
        if (Files.exists(directory)) {
            return Files.list(directory)
                    .filter(p -> p.toString().endsWith(".yml"))
                    .map(BungeeYamlFactory::loadUnsafe)
                    .map(MessageFileLoader::loadMessageFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            Files.createDirectories(directory);
            return Collections.emptySet();
        }
    }

    private static @Nullable MessageSet loadMessageFile(@NotNull Yaml yaml) {
        var fileNameWithoutExtension = yaml.getPath().getFileName().toString().replace(".yml", "");
        var locale = Translator.parseLocale(fileNameWithoutExtension);

        if (locale == null) {
            return null;
        }

        var prefixRaw = yaml.get(PREFIX_KEY);
        var prefix = prefixRaw.isEmpty() ? Component.empty() : DESERIALIZER.deserialize(prefixRaw);

        var suffixRaw = yaml.get(SUFFIX_KEY);
        var suffix = suffixRaw.isEmpty() ? Component.empty() : DESERIALIZER.deserialize(suffixRaw);

        var messages = loadMessages(yaml, prefix, suffix);
        var components = loadPlaceholders((BungeeYaml) yaml);

        return MessageSet.create(locale, messages, components);
    }

    private static @NotNull List<Component> loadMessages(@NotNull Yaml yaml, @NotNull Component prefix,
                                                         @NotNull Component suffix) {
        List<Component> result = new ArrayList<>();

        for (var message : yaml.get(MESSAGE_KEY)) {
            var component = prefix.append(DESERIALIZER.deserialize(message)).append(suffix);
            result.add(component);
        }

        return result;
    }

    private static @NotNull Set<TextReplacementConfig> loadPlaceholders(@NotNull BungeeYaml yaml) {
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
