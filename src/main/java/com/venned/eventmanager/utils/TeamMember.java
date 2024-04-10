package com.venned.eventmanager.utils;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class TeamMember {
    private final Player player;
    private final Team team;
    private boolean leader;

    public TeamMember(Player player, Team team, boolean leader) {
        this.player = player;
        this.team = team;
        this.leader = leader;
    }

    public Player getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }
}
