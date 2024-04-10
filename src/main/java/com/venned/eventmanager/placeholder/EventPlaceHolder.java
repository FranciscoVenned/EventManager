package com.venned.eventmanager.placeholder;

import com.venned.eventmanager.Main;
import com.venned.eventmanager.utils.TeamMember;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class EventPlaceHolder extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return "eventplace";
    }

    @Override
    public String getAuthor() {
        return "Francisco";
    }

    @Override
    public String getVersion() {
        return Main.getInstance().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equalsIgnoreCase("timeborde")) {
            return getTimeToNextBorderShrink();
        }  else if (params.equalsIgnoreCase("team")) {
            return getTeamName(player);
        }
        return "1";
    }

    private String getTimeToNextBorderShrink() {
        // Obtener el tiempo restante hasta la próxima reducción del borde del mundo
        long timeToNextShrink = Main.getInstance().getTimeToNextShrink();
        // Convertir el tiempo a minutos y segundos
        long minutes = timeToNextShrink / 60;
        long seconds = timeToNextShrink % 60;
        // Formatear la salida como MM:SS
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getTeamName(Player player) {
        TeamMember teamMember = Main.getInstance().getTeamMembers().get(player);
        if (teamMember != null) {
            return teamMember.getTeam().getName();
        }
        return "No Team";
    }



}
