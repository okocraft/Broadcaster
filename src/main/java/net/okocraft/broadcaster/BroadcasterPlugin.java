package net.okocraft.broadcaster;

import com.github.siroshun09.configapi.bungee.BungeeYamlFactory;
import com.github.siroshun09.configapi.common.configurable.Configurable;
import com.github.siroshun09.configapi.common.configurable.LongValue;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Plugin;
import net.okocraft.broadcaster.message.MessageFileLoader;
import net.okocraft.broadcaster.message.MessageSet;
import net.okocraft.broadcaster.task.BroadcastTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class BroadcasterPlugin extends Plugin {

    private static final LongValue BROADCAST_INTERVAL = Configurable.create("interval", 300L);

    private final Set<MessageSet> messageSets = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private MessageSet defaultMessageSet;
    private BungeeAudiences audiences;

    @Override
    public void onEnable() {
        getLogger().info("Loading messages...");

        defaultMessageSet = MessageFileLoader.loadDefaultMessageFile(this);

        messageSets.clear();

        var directory = getDataFolder().toPath().resolve("messages");

        try {
            messageSets.addAll(MessageFileLoader.loadMessageFiles(directory));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "An exception occurred while loading message files.", e);
        }

        audiences = BungeeAudiences.create(this);

        long interval = BungeeYamlFactory.loadUnsafe(this, "config.yml").get(BROADCAST_INTERVAL);
        var task = new BroadcastTask(this);

        scheduler.scheduleAtFixedRate(task, interval, interval, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        scheduler.shutdownNow();
    }

    public @NotNull MessageSet getDefaultMessageSet() {
        return defaultMessageSet;
    }

    public @NotNull Set<MessageSet> getMessageSets() {
        return messageSets;
    }

    public @NotNull BungeeAudiences getAudiences() {
        return audiences;
    }
}
