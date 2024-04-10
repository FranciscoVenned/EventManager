package com.venned.eventmanager.commands;

import com.venned.eventmanager.Main;
import com.venned.eventmanager.task.BorderInitialTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StartGame implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] strings) {
        if (sender instanceof Player) {
            if(cmd.getName().equalsIgnoreCase("startgame")){
                if(Main.getInstance().getGameManager().isWaiting()){
                    startCountdown();
                } else {
                    sender.sendMessage(ChatColor.RED + "The game is already started, you cannot start it again.");
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by one player.");
            return true;
        }
        return false;
    }

    private void startCountdown() {

        AtomicInteger countdown = new AtomicInteger(10);

        final BukkitTask[] task = new BukkitTask[1];

        task[0] = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            int currentCount = countdown.getAndDecrement();
            if (currentCount > 0) {

                Bukkit.broadcastMessage(ChatColor.YELLOW + "¡The game will start at " + currentCount + " seconds!");
            } else {
                Main.getInstance().getGameManager().startGame();


                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location gameCenter = Main.getInstance().gameCenter;
                    Location randomLocation = getRandomLocation(gameCenter, 350);
                    player.teleport(randomLocation);
                }


                int initialDelayHours = Main.getInstance().getConfig().getInt("Initial-Delay-Border", 1);

                long currentTimeMillis = System.currentTimeMillis();
                long startTimeMillis = currentTimeMillis + TimeUnit.HOURS.toMillis(initialDelayHours);
                Main.getInstance().getStartTimes().put("world", startTimeMillis);

                int initialDelayTicks = initialDelayHours * 60 * 60 * 20;


                BukkitTask borderTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Main.getInstance().getWorldBorderShrinkTask().start();// Iniciar la tarea después del retraso inicial
                }, initialDelayTicks);
                BorderInitialTask.addTask("StarGameTask", borderTask);

                Main.getInstance().getPhaseTask().start();
                Bukkit.broadcastMessage(ChatColor.GREEN + "¡The game has started!");
                if (task[0] != null) {
                    task[0].cancel();
                }
            }
        }, 20L, 20L);
    }

    private Location getRandomLocation(Location center, int radius) {
        World world = center.getWorld();
        Random random = new Random();

        double randomX = center.getX() + random.nextInt(radius * 2) - radius;
        double randomZ = center.getZ() + random.nextInt(radius * 2) - radius;

        int maxY = world.getHighestBlockYAt((int) randomX, (int) randomZ);

        Location randomLocation = new Location(world, randomX, maxY, randomZ);

        // Ajustar la altura para que el jugador no se teletransporte sobre bloques aéreos
        while (randomLocation.getBlock().getType().isSolid() || randomLocation.getBlock().getRelative(BlockFace.UP).getType().isSolid()) {
            maxY++;
            randomLocation.setY(maxY);
        }

        return randomLocation;
    }


}
