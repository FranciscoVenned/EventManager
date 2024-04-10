package com.venned.eventmanager.task;

import com.venned.eventmanager.Main;
import org.bukkit.scheduler.BukkitRunnable;

public class ReconectectTask extends BukkitRunnable {
    @Override
    public void run() {
        System.out.println("Helloda");
    }

    public void start() {

        this.runTaskTimer(Main.getInstance(), 0, 60 * 20);
    }
}
