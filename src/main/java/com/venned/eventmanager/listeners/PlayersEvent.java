package com.venned.eventmanager.listeners;

import com.venned.eventmanager.Main;
import com.venned.eventmanager.task.BorderInitialTask;
import com.venned.eventmanager.utils.Scoreboards;
import com.venned.eventmanager.utils.TeamMember;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayersEvent implements Listener {

    private int teamCounter = 1;
    private final Map<UUID, BukkitTask> disconnectTasks = new HashMap<>();
    private final long reconnectTimeMillis = 60 * 1000;

    private static final int TEAM_COUNT_TO_START_GAME = Main.getInstance().getConfig().getInt("Min_Team_Start_Game", 2);
    private int teamCount = 0;



    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        //  sendTitle(player, "¡Bienvenido al servidor!", "Disfruta tu tiempo aquí", 10, 70, 20);

        BukkitTask disconnectTask = disconnectTasks.remove(player.getUniqueId());
        if (disconnectTask != null) {
            player.sendMessage(ChatColor.RED + "You have successfully reconnected");
            Bukkit.getWorld(player.getWorld().getName()).playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.5f);
            disconnectTask.cancel();
            return;
        }

        if (Main.getInstance().getGameManager().isRunning()) {
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }
        if (Main.getInstance().getGameManager().isEnded()) {
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }

        String teamName = "" + teamCounter++;

        Scoreboards.createBoard("Game3", "Team")
                .addScore(player.getName(), "Time Until Next Shrink: %eventplace_timeborde%", 12);
        Scoreboards.addToPlayers("Game3", player);


        player.teleport(Main.getInstance().gameCenter);

        teamCount++;

        Team team = Main.getInstance().scoreboard.registerNewTeam(teamName);


        Scoreboards.addTeamP(teamName, player);


        team.addEntry(player.getName());

        Main.getInstance().playerTeams.put(team, player);

        ChatColor randomColor;
        do {
            randomColor = ChatColor.values()[new Random().nextInt(ChatColor.values().length)];
        } while (randomColor == ChatColor.MAGIC || randomColor == ChatColor.ITALIC || randomColor == ChatColor.STRIKETHROUGH
                || randomColor == ChatColor.BOLD || randomColor == ChatColor.UNDERLINE || randomColor == ChatColor.RESET);


        team.setPrefix( randomColor + "["  + teamName  + "] ");
        if (Main.getInstance().getGameManager().isWaiting()) {
            if (teamCount >= TEAM_COUNT_TO_START_GAME) {
                startCountdown();
            }
        }

        player.setPlayerListHeader(ChatColor.GREEN + "¡Welcome to Battle Royal!");
        player.setPlayerListFooter(ChatColor.RED + "Good Game ");

        team.addEntry(player.getName());

        Main.getInstance().teamMembers.put(player, new TeamMember(player, team, true));

    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }




    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer(); // Hacer player final

        disconnectTasks.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {

                disconnectTasks.remove(player.getUniqueId());
                TeamMember leavingMember = Main.getInstance().teamMembers.get(player);
                Team teamP = Main.getInstance().scoreboard.getPlayerTeam(player);

                if (teamP != null){

                    teamP.removePlayer(player);
                    Bukkit.broadcastMessage(ChatColor.RED + " Eliminated Player a " + player.getName());
                    Main.getInstance().pendingInvites.values().remove(player);
                    Main.getInstance().teamMembers.remove(player, leavingMember);



                    if (teamP.getSize() == 0) {
                        Main.getInstance().teamMembers.remove(player, teamP);
                        Main.getInstance().teamMembers.remove(player);
                        teamP.unregister();
                        Bukkit.broadcastMessage(ChatColor.RED +  "Team eliminated due to not having more members");
                        --teamCount;
                        Main.getInstance().playerTeams.remove(teamP);

                        return;
                    }

                }

                if (leavingMember != null) {
                    Team team = leavingMember.getTeam();
                    if (team != null) {
                        if (leavingMember.isLeader()) {
                            Bukkit.broadcastMessage(ChatColor.RED + "Disconnected Leader");
                            team.removePlayer(player);
                            team.removeEntry(player.getName());
                            for (Map.Entry<Player, TeamMember> entry : Main.getInstance().teamMembers.entrySet()) {
                                Player nextLeader = entry.getKey();
                                TeamMember nextMember = entry.getValue();
                                if (!nextLeader.equals(player) && nextMember.getTeam().equals(team)) {
                                    nextMember.setLeader(true); // Establecer el próximo líder
                                    team.setPrefix(ChatColor.GREEN + "[" + ChatColor.YELLOW + team.getName() + ChatColor.GREEN + "] ");
                                    break;
                                }
                            }
                        }

                    }

                }

        }, 1000));

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
                    Location randomLocation = getRandomLocation(gameCenter, 100);
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

        while (randomLocation.getBlock().getType().isSolid() || randomLocation.getBlock().getRelative(BlockFace.UP).getType().isSolid()) {
            maxY++;
            randomLocation.setY(maxY);
        }

        return randomLocation;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (Main.getInstance().getGameManager().isRunning()) {

                Team team = Main.getInstance().scoreboard.getPlayerTeam(player);
                Bukkit.broadcastMessage(ChatColor.RED + "Player Removed " + player.getName() + " from Team " + team.getName());
                Main.getInstance().playerTeams.remove(team, player);

                // Quitar al jugador del equipo
                team.removePlayer(player);
                // Eliminar al jugador del mapa
                Main.getInstance().teamMembers.remove(player);
                Main.getInstance().pendingInvites.values().remove(player);
                teamCount--;
                if (team.getSize() == 0) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Team " + team.getName()  + " was eliminated, it has no more members.");
                    team.unregister();
                }

        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();
            TeamMember attackerM = Main.getInstance().teamMembers.get(attacker);
            Team attackerP = attackerM.getTeam();

            TeamMember defenderM = Main.getInstance().teamMembers.get(victim);
            Team defenderP = defenderM.getTeam();


            Team attacketTeam = Main.getInstance().scoreboard.getPlayerTeam(attacker);
            Team victimTeam = Main.getInstance().scoreboard.getPlayerTeam(victim);

            String TeamNameAttacker = attacketTeam.getName();
            String TeamNameVictim = victimTeam.getName();


            if (TeamNameAttacker.equalsIgnoreCase(TeamNameVictim)){
                event.setCancelled(true);
            }


        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if(Main.getInstance().getGameManager().isRunning()) {
                player.setGameMode(GameMode.SPECTATOR);
        }
    }


}
