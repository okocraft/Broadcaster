package net.okocraft.broadcaster.task;

import net.okocraft.broadcaster.BroadcasterPlugin;
import net.okocraft.broadcaster.message.MessageSet;
import org.jetbrains.annotations.NotNull;

public class BroadcastTask implements Runnable {

    private final BroadcasterPlugin plugin;

    public BroadcastTask(@NotNull BroadcasterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (var player : plugin.getProxy().getPlayers()) {
            var audience = plugin.getAudiences().player(player);

            var message =
                    plugin.getMessageSets()
                            .stream()
                            .filter(m -> m.getLocale().equals(player.getLocale()))
                            .findFirst()
                            .orElse(plugin.getDefaultMessageSet())
                            .getCurrentMessage();

            audience.sendMessage(message);
        }

        plugin.getDefaultMessageSet().next();
        plugin.getMessageSets().forEach(MessageSet::next);
    }
}
