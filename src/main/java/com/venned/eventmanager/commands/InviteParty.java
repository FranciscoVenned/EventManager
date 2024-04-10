package com.venned.eventmanager.commands;

import com.venned.eventmanager.Main;
import com.venned.eventmanager.utils.TeamMember;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class InviteParty implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (cmd.getName().equalsIgnoreCase("invite")) {
                if (args.length == 1) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        TeamMember playerMember = Main.getInstance().teamMembers.get(player);
                        TeamMember targetMember = Main.getInstance().teamMembers.get(target);
                        if (playerMember != null && targetMember != null) {
                            Team playerTeam = playerMember.getTeam();
                            Team targetTeam = targetMember.getTeam();
                            if (playerMember.isLeader()) {

                                Main.getInstance().pendingInvites.put(target, player);
                                Main.getInstance().pendingInvites.put(player, target);
                                player.sendMessage(ChatColor.GREEN + "¡Your invite player " + target.getName() + " in your team!");
                                target.sendMessage(ChatColor.GREEN + "You have been invited " + player.getName() + "! Use /accept " + player.getName() + " to join.");
                                return true;
                            } else {
                                player.sendMessage(ChatColor.RED + "Only Leader invite.");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You must be on a team to invite other players.");
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Player not found in server.");
                        return true;
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Usage, /invite <player>");
                    return true;
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }
        return false;
    }


}
