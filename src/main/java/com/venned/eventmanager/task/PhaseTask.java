package com.venned.eventmanager.task;

import com.sun.org.apache.xerces.internal.xs.StringList;
import com.venned.eventmanager.Main;
import com.venned.eventmanager.utils.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class PhaseTask extends BukkitRunnable {

    private final int interval;
    private int phase = -1;
    private long lastPhaseTime;

    private final FileConfiguration config = Main.getInstance().getConfig();

    public PhaseTask(int interval) {
        this.interval = interval;
    }

    @Override
    public void run() {
        if(Main.getInstance().getGameManager().isRunning()){
            phase++;
            Bukkit.broadcastMessage(ChatColor.RED + "Entering the Phase " + phase);
            Main.getInstance().getGameManager().setCurrentPhase(phase);
            lastPhaseTime = System.currentTimeMillis();

            ConfigurationSection section = config.getConfigurationSection("Phases_Titles");
            if (section != null){
                String phaseKey = String.valueOf(phase);
                if (section.contains(phaseKey)){
                    List<String> titles = section.getStringList(phaseKey);
                    String title1 = ChatColor.translateAlternateColorCodes('&', titles.get(0));
                    String title2 = ChatColor.translateAlternateColorCodes('&', titles.get(1));
                    if(titles.size() > 2) {
                        String title3 = ChatColor.translateAlternateColorCodes('&', titles.get(2));
                        String title4 = ChatColor.translateAlternateColorCodes('&', titles.get(3));
                        String title5 = ChatColor.translateAlternateColorCodes('&', titles.get(4));
                        String title6 = ChatColor.translateAlternateColorCodes('&', titles.get(5));
                        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.sendTitle(title1, title2, 10, 120, 20);
                            }
                        }, 60);
                        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.sendTitle(title3, title4, 10, 120, 20);
                            }
                        }, 200);

                        if (!title5.isEmpty()) {
                            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                                for (Player player : Bukkit.getOnlinePlayers()) {
                                    player.sendTitle(title5, title6, 10, 120, 20);
                                }
                            }, 300);
                        }

                    }  else {

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendTitle(title1, title2, 10, 70, 20);
                        }
                    }
                }
            }


        }

    }

    public void start() {
        this.runTaskTimer(Main.getInstance(), 0, interval * 20);
    }

    public void startFirts(){
        this.runTaskTimer(Main.getInstance(), 0, interval * 20);
    }

    public int getInterval() {
        return interval;
    }
    public long getLastPhaseTime() {
        return lastPhaseTime;
    }
}
