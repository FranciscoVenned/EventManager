package com.venned.eventmanager.listeners;

import com.venned.eventmanager.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ClearTeams implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerShutdown(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals("EventManager")) {
            Scoreboard scoreboard = Main.getInstance().scoreboard;
            for (Team team : scoreboard.getTeams()) {
                team.unregister();
            }
        }
    }
}
