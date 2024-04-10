package com.venned.eventmanager.utils;

import org.bukkit.Location;

import java.util.List;

public class PhaseArea {
    private List<Location> locations;
    private int phase;

    public PhaseArea(List<Location> locations, int phase) {
        this.locations = locations;
        this.phase = phase;

    }

    public List<Location> getLocations() {
        return locations;
    }

    public boolean isInArea(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        if (locations.size() < 2) {
            System.out.println("Error: The spawner area must have at least two locations.");
            return false;
        }

        Location pos1 = locations.get(0);
        Location pos2 = locations.get(1);

        double x1 = pos1.getX();
        double y1 = pos1.getY();
        double z1 = pos1.getZ();

        double x2 = pos2.getX();
        double y2 = pos2.getY();
        double z2 = pos2.getZ();

        String worldName = location.getWorld().getName();

        boolean isInArea = x >= Math.min(x1, x2) && x <= Math.max(x1, x2)
                && y >= Math.min(y1, y2) && y <= Math.max(y1, y2)
                && z >= Math.min(z1, z2) && z <= Math.max(z1, z2)
                && worldName.equals(pos1.getWorld().getName()) && worldName.equals(pos2.getWorld().getName());


        if (isInArea) {

        } else {

        }

        return isInArea;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }
}

