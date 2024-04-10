package com.venned.eventmanager.task;

import com.venned.eventmanager.Main;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

public class WorldBorderShrinkTask extends BukkitRunnable {

    private final int shrinkAmount; // Cantidad a reducir el tamaño del borde
    private final int interval; // Intervalo en segundos entre cada reducción
    private int currentSize; // Tamaño actual del borde
    private long lastShrinkTime; // Tiempo de la última reducción del borde
    private boolean hasStarted = false;


    public WorldBorderShrinkTask(int initialSize, int shrinkAmount, int interval) {
        this.currentSize = initialSize;
        this.shrinkAmount = shrinkAmount;
        this.interval = interval;
    }

    @Override
    public void run() {

            Bukkit.broadcastMessage(ChatColor.RED + "The edge of the world will be reduced, avoid it from catching you");
            for (World world : Bukkit.getWorlds()) {

                world.getWorldBorder().setSize(currentSize, TimeUnit.SECONDS, 30);


            }
             if (!hasStarted) {
             hasStarted = true;
             }

            currentSize -= shrinkAmount;

            lastShrinkTime = System.currentTimeMillis();

            if (currentSize < 1) {
                cancel();
            }

    }

    public void start() {

        this.runTaskTimer(Main.getInstance(), 0, interval * 20);
    }

    public long getLastShrinkTime() {
        return lastShrinkTime;
    }

    public int getInterval() {
        return interval;
    }

    public boolean hasStarted() {
        return hasStarted;
    }


}
