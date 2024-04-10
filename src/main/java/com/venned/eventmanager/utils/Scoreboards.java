package com.venned.eventmanager.utils;

import com.venned.eventmanager.Main;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Scoreboards {

    public static HashMap<String, Scoreboards> boards = new HashMap<>();
    private static ScoreboardManager manager = Bukkit.getScoreboardManager();

    private Objective objective;


    private static final Scoreboard board = Main.getInstance().scoreboard;
    private static int teamCount;

    private Scoreboards() {

    }

    private Scoreboards(String name, String title) {
        objective = board.getObjective(name);
        if (objective != null) {
            objective.setDisplayName(title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        } else {
            manager.getNewScoreboard();
            Objective objective = board.registerNewObjective(name, Criteria.DUMMY, title);

            objective.setDisplayName(title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        teamCount = 0;
        new BukkitRunnable() {
            @Override
            public void run() {
                updateScores();
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }

    public static Scoreboards createBoard(String name, String title) {
        if (!boards.containsKey(name)) {
            Scoreboards board = new Scoreboards(name, title);

            boards.put(name, board);
            return board;
        } else {

            return boards.get(name);
        }
    }

    public static Scoreboards getBoard(String name) {
        if (!boards.containsKey(name)) {
            Bukkit.broadcastMessage("§4No Scoreboard named §6" + name + " §4found");
        }
        return boards.get(name);
    }

    public static void addToPlayers(String name, List<Player> players) {
        for (Player player : players) {
            Scoreboards board = getBoard(name);
            if (board != null) {
                player.setScoreboard(board.board);
            }
        }
    }

    public static void addToPlayers(String name, Player... players) {
        addToPlayers(name, Arrays.asList(players));
    }

    @SuppressWarnings("unchecked")
    public static void addToAll(String name) {
        addToPlayers(name, (List<Player>) Bukkit.getOnlinePlayers());
    }

    public static void removeFromPlayers(String name, List<Player> players) {
        for (Player player : players) {
            Scoreboards board = getBoard(name);
            if (board != null && player.getScoreboard().equals(board.board)) {
                player.setScoreboard(manager.getMainScoreboard());
            }
        }
    }

    public static void removeFromPlayers(String name, Player... players) {
        removeFromPlayers(name, Arrays.asList(players));
    }

    @SuppressWarnings("unchecked")
    public static void removeFromAll(String name) {
        removeFromPlayers(name, (List<Player>) Bukkit.getOnlinePlayers());
    }

    public static void removeFromPlayers(List<Player> players) {
        for (Player player : players) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    public static void removeFromPlayers(Player... players) {
        removeFromPlayers(Arrays.asList(players));
    }

    public static void addTeamP(String teamName, Player player) {
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard(); // Obtener el marcador principal

        // Obtener o crear el equipo para el jugador en el marcador principal
        Team playerTeam = mainScoreboard.getTeam(teamName);

        playerTeam.addEntry(player.getName());

        // Asignar el marcador principal al jugador
        player.setScoreboard(mainScoreboard);

        for (Team team : mainScoreboard.getTeams()){

        }
    }

    private void updateScores() {
        // Obtener el marcador principal
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Crear un nuevo objetivo si no existe
        Objective objective = mainScoreboard.getObjective("sidebar");
        if (objective == null) {
            objective = mainScoreboard.registerNewObjective("sidebar", "dummy", ChatColor.BOLD + "Game Info");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(ChatColor.BOLD + getRandomColor() + "Game Info");
        }

        // Iterar sobre todos los jugadores en línea para actualizar sus puntajes en el marcador
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Obtener el equipo del jugador


            // Limpiar el marcador para volver a agregar los puntajes actualizados
            for (String entry : mainScoreboard.getEntries()) {
                mainScoreboard.resetScores(entry);
            }


            FileConfiguration config = Main.getInstance().getConfig();
            List<String> scoreboardLines = config.getStringList("scoreboard");

            String title = ChatColor.translateAlternateColorCodes('&', config.getString("score-title"));
            objective.setDisplayName(ChatColor.BOLD + getRandomColor() + ChatColor.BOLD + title);


            String borderTime = getTimeToNextBorderShrink();
            long timeUntilStart = getTimeUntilStartInSeconds();

            int score = 10; // Empezar con una puntuación alta
            for (String line : scoreboardLines) {
                line = ChatColor.translateAlternateColorCodes('&', line);


                // Reemplazar los marcadores de posición con los valores correspondientes
                if (line.contains("%teams_size%")) {
                    line = line.replace("%teams_size%", String.valueOf(Main.getInstance().playerTeams.size()));
                }
                if (line.contains("%phase_current%")) {
                    line = line.replace("%phase_current%", String.valueOf(Main.getInstance().getGameManager().getCurrentPhase()));
                }
                if (line.contains("%phase_nextTime%")) {
                    line = line.replace("%phase_nextTime%", String.valueOf(getTimeToNextPhase()));
                }
                if (timeUntilStart > 0) {
                    if (line.contains("%border_time%")) {
                        line = line.replace("%border_time%", String.valueOf(getTimeUntilStart()));
                    }
                } else {
                    if (line.contains("%border_time%")) {
                        line = line.replace("%border_time%", String.valueOf(borderTime));
                    }
                }

                // Agregar la línea al scoreboard con la puntuación actual y reducir la puntuación para la siguiente línea
                objective.getScore(line).setScore(score);
                score--; // Reducir la puntuación

                if (!line.equals("---------------------------")) {
                    score--; // Reducir la puntuación
                }
            }

        }
    }



    public long getTimeUntilStartInSeconds() {
        long startTimeMillis = Main.getInstance().getStartTimes().getOrDefault("world", 0L);
        long currentTimeMillis = System.currentTimeMillis();
        long timeUntilStartMillis = startTimeMillis - currentTimeMillis;

        // Asegurarse de que el tiempo restante sea mayor o igual a 0
        timeUntilStartMillis = Math.max(timeUntilStartMillis, 0);

        long timeUntilStartSeconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilStartMillis);

        return timeUntilStartSeconds;
    }
    public String getTimeUntilStart() {
        // Obtener el tiempo inicial del HashMap usando el nombre del mundo
        long startTimeMillis = Main.getInstance().getStartTimes().getOrDefault("world", 0L);

        // Obtener el tiempo actual en milisegundos
        long currentTimeMillis = System.currentTimeMillis();

        // Calcular el tiempo restante en milisegundos
        long timeUntilStartMillis = startTimeMillis - currentTimeMillis;

        // Asegurarse de que el tiempo restante sea mayor o igual a 0
        timeUntilStartMillis = Math.max(timeUntilStartMillis, 0);

        // Convertir el tiempo restante a horas, minutos y segundos
        long hours = TimeUnit.MILLISECONDS.toHours(timeUntilStartMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilStartMillis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilStartMillis) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes);

        // Formatear la salida como una cadena
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        return timeString;
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

    private String getTimeToNextPhase() {
        // Obtener el tiempo restante hasta la próxima reducción del borde del mundo
        long timeToNextPhase = Main.getInstance().getTimeToPhase();
        // Convertir el tiempo a minutos y segundos
        long minutes = timeToNextPhase / 60;
        long seconds = timeToNextPhase % 60;
        // Formatear la salida como MM:SS
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getRandomColor() {
        ChatColor[] colors = ChatColor.values();
        // Excluir colores que no son apropiados para el título
        List<ChatColor> validColors = Arrays.stream(colors)
                .filter(color -> !color.isFormat() && color != ChatColor.RESET)
                .collect(Collectors.toList());
        // Obtener un color aleatorio de la lista
        ChatColor randomColor = validColors.get(new Random().nextInt(validColors.size()));
        return randomColor.toString();
    }

    private String getTeamName(Player player) {
        TeamMember teamMember = Main.getInstance().getTeamMembers().get(player);
        if (teamMember != null) {
            return teamMember.getTeam().getName();
        }
        return "No Team";
    }
    @SuppressWarnings("unchecked")
    public static void removeFromAll() {
        removeFromPlayers((List<Player>) Bukkit.getOnlinePlayers());
    }

    public static void deleteBoards(String... names) {
        for (String name : names) {
            removeFromAll(name);
            boards.remove(name);
        }
    }

    public static void clear() {
        System.out.println("Delete objetive");
        Objective principal = board.getObjective("Game2");
        if (principal != null){
            principal.unregister();
        }

        Objective objective = board.getObjective(DisplaySlot.SIDEBAR);
        if (objective != null) {
            objective.unregister(); // Desvincular el objetivo del tablero
        }
        // Puedes agregar más lógica aquí para limpiar cualquier otro tipo de objetivo, si es necesario
    }
    public static void clearBoards() {
        for (String boardName : boards.keySet()) {
            removeFromAll(boardName);
        }
        boards.clear();
    }

    public static void updateTeam(String boardName, String teamName, String text) {
        Scoreboards board = getBoard(boardName);
        if (board != null) {
            Team team = board.board.getTeam(teamName);
            if (team != null) {
                team.setPrefix(text);
            }
        }
    }

    // ------------------
    // Scoreboard Builder
    // ------------------

    public Scoreboards addScore(String playerName, String text, int slot) {
        String updatedText = PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(playerName), text); // Actualizar el texto con los placeholders
        board.getObjective(DisplaySlot.SIDEBAR).getScore(updatedText).setScore(slot);
        return this;
    }

    public Scoreboards addScore(String text, int slot) {
        board.getObjective(DisplaySlot.SIDEBAR).getScore(text).setScore(slot);
        return this;
    }



}
