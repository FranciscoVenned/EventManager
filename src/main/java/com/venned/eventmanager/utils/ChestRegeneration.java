package com.venned.eventmanager.utils;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.venned.eventmanager.Main;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class ChestRegeneration implements Listener {
    private final Map<Inventory, ItemStack[]> items = new HashMap<>();
    private final Map<Inventory, Integer> regenTimers = new HashMap<>();
    private final Map<Inventory, Hologram> holograms = new HashMap<>();


    @EventHandler
    public void onOpenChest(InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof Chest) {
            if (!items.containsKey(event.getInventory())) {
                Player player = (Player) event.getPlayer();
                ItemStack[] contents = event.getInventory().getContents();

                ItemStack[] clonedContents = new ItemStack[contents.length];
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null) {
                        clonedContents[i] = contents[i].clone();
                    }
                }
                for (ItemStack stack : contents) {
                    if (stack != null) {
                        System.out.println("Items " + stack.getType().toString());
                    }
                }
                items.put(event.getInventory(), clonedContents);
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChestClosed(InventoryCloseEvent event) {
        if (items.containsKey(event.getInventory())) {
            ItemStack[] item = items.get(event.getInventory());
            int regenerationTimeInSeconds = 60;
            regenTimers.put(event.getInventory(), regenerationTimeInSeconds);
            Player player = (Player) event.getPlayer();

            new BukkitRunnable() {
                @Override
                public void run() {
                    int remainingTime = regenTimers.getOrDefault(event.getInventory(), 0);
                    remainingTime--;
                    regenTimers.put(event.getInventory(), remainingTime);

                    if (remainingTime <= 0) {
                        for (ItemStack ITEMS : item) {
                            if (ITEMS != null) {
                                event.getInventory().setContents(item);
                                items.remove(event.getInventory());
                                regenTimers.remove(event.getInventory());
                                showRegenerationHologram(event.getInventory(), player);
                                break;
                            }
                        }
                        cancel();
                    } else {
                        showTimeRemainingHologram(event.getInventory(), player, remainingTime);
                    }
                }
            }.runTaskTimer(Main.getInstance(), 20L, 20L); // Repetir cada segundo (20 ticks)
        }


    }

    private void showRegenerationHologram(Inventory inventory, Player player) {
        Hologram hologram = holograms.get(inventory);
        if (hologram != null) {
            hologram.delete();
        }
        Location location = inventory.getLocation();
        double Y = location.getY();
        double Z = location.getZ();
        Location locationclone = location.clone();
        locationclone.setY(Y + 2);
        locationclone.setZ(Z + 1);
        hologram = HologramsAPI.createHologram(Main.getInstance(), locationclone);
        hologram.appendTextLine("Regenerated chest!");
        hologram.appendTextLine("¡You can open it again!");
        hologram.getVisibilityManager().setVisibleByDefault(true);
        hologram.getVisibilityManager().showTo(player);
        holograms.put(inventory, hologram); // Almacenar el nuevo holograma en el mapa
    }
    private void showTimeRemainingHologram(Inventory inventory, Player player, int remainingTime) {
        Hologram hologram = holograms.get(inventory);
        if (hologram != null) {
            hologram.delete();
        }
        Location location = inventory.getLocation();
        double Y = location.getY();
        double Z = location.getZ();
        Location locationclone = location.clone();
        locationclone.setY(Y + 2);
        locationclone.setZ(Z + 1);
        hologram = HologramsAPI.createHologram(Main.getInstance(), locationclone);
        hologram.appendTextLine("Time left:");
        hologram.appendTextLine(formatTime(remainingTime));
        hologram.getVisibilityManager().setVisibleByDefault(true);
        hologram.getVisibilityManager().showTo(player);
        holograms.put(inventory, hologram);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
