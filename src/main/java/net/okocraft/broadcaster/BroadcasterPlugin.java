package net.okocraft.broadcaster;

import net.md_5.bungee.api.plugin.Plugin;
import net.okocraft.broadcaster.message.MessageFileLoader;
import net.okocraft.broadcaster.message.MessageSet;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public final class BroadcasterPlugin extends Plugin {

    private final Set<MessageSet> messageSets = new HashSet<>();

    @Override
    public void onEnable() {
        getLogger().info("Loading messages...");

        messageSets.clear();

        var directory = getDataFolder().toPath().resolve("messages");

        try {
            messageSets.addAll(MessageFileLoader.loadMessageFiles(directory));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "An exception occurred while loading message files.", e);
        }
    }
}
