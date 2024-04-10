    package com.venned.eventmanager;

    import com.venned.eventmanager.commands.InviteParty;
    import com.venned.eventmanager.commands.StartGame;
    import com.venned.eventmanager.listeners.ClearTeams;
    import com.venned.eventmanager.listeners.PlayersEvent;
    import com.venned.eventmanager.placeholder.EventPlaceHolder;
    import com.venned.eventmanager.task.BorderInitialTask;
    import com.venned.eventmanager.task.PhaseTask;
    import com.venned.eventmanager.task.ReconectectTask;
    import com.venned.eventmanager.task.WorldBorderShrinkTask;
    import com.venned.eventmanager.utils.*;
    import org.bukkit.*;
    import org.bukkit.block.BlockFace;
    import org.bukkit.command.Command;
    import org.bukkit.command.CommandExecutor;
    import org.bukkit.command.CommandSender;
    import org.bukkit.configuration.ConfigurationSection;
    import org.bukkit.configuration.file.FileConfiguration;
    import org.bukkit.entity.EntityType;
    import org.bukkit.entity.Firework;
    import org.bukkit.entity.Player;
    import org.bukkit.event.EventHandler;
    import org.bukkit.event.EventPriority;
    import org.bukkit.event.Listener;
    import org.bukkit.event.player.PlayerLoginEvent;
    import org.bukkit.event.player.PlayerMoveEvent;
    import org.bukkit.inventory.ItemStack;
    import org.bukkit.inventory.meta.FireworkMeta;
    import org.bukkit.inventory.meta.MapMeta;
    import org.bukkit.map.MapView;
    import org.bukkit.plugin.java.JavaPlugin;
    import org.bukkit.scheduler.BukkitRunnable;
    import org.bukkit.scheduler.BukkitTask;
    import org.bukkit.scoreboard.Scoreboard;
    import org.bukkit.scoreboard.Team;

    import javax.imageio.ImageIO;
    import java.awt.image.BufferedImage;
    import java.io.IOException;
    import java.net.URL;
    import java.util.*;

    import static com.venned.eventmanager.utils.Scoreboards.boards;

    public final class Main extends JavaPlugin implements Listener, CommandExecutor {
        public Map<Team, Player> playerTeams = new HashMap<>();
        public Map<Player, Player> pendingInvites = new HashMap<>();
        public Map<Player, TeamMember> teamMembers = new HashMap<>();
        private HashMap<String, Long> startTimes = new HashMap<>();
        public Scoreboard scoreboard;
        private Map<Player, Location> lastValidLocations = new HashMap<>();
        private World gameWorld;
        public Location gameCenter;
        private int gameRadius = getConfig().getInt("gameRadius", 600);
        private BukkitTask barrierTask;
        private WorldBorderShrinkTask worldBorderShrinkTask;
        private PhaseTask phaseTask;
        private boolean allowConnections = true;

        private ReconectectTask reconectectTask;
        private GameManager gameManager;
        private HashMap<String, PhaseArea> spawnerAreas = new HashMap<>();
        private static Main instance;

        public static Main getInstance() {
            return instance;
        }



        @Override
        public void onEnable() {
            // Plugin startup logic
            instance = this;

            saveDefaultConfig();

            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new EventPlaceHolder().register();
            }

            Bukkit.getScheduler().runTaskLater(this, () -> allowConnections = true, 100L);

            getServer().getPluginManager().registerEvents(this, this);
            this.getCommand("accept").setExecutor(this);
            this.getCommand("invite").setExecutor(new InviteParty());
            this.getCommand("startgame").setExecutor(new StartGame());

            String worldName = getConfig().getString("worldName", "world");
            gameWorld = Bukkit.getWorld(worldName);
            double centerX = getConfig().getDouble("centerX", 1024);
            double centerY = getConfig().getDouble("centerY", 100);
            double centerZ = getConfig().getDouble("centerZ", 1024);
            gameCenter = new Location(gameWorld, centerX, centerY, centerZ);

            gameManager = new GameManager();
            scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

           // getServer().getPluginManager().registerEvents(new ChestRegeneration(), this);
            getServer().getPluginManager().registerEvents(new PlayersEvent(), this);
            getServer().getPluginManager().registerEvents(new ClearTeams(), this);

            startBarrierTask();
            gameWorld.getWorldBorder().setCenter(gameCenter);

            int size = getConfig().getInt("Size", 1500);
            gameWorld.getWorldBorder().setSize(size);

            int initialsize = getConfig().getInt("initialSize", 1000);
            int shrinkAmount = getConfig().getInt("shrinkAmount", 50);
            int interval = getConfig().getInt("interval-seconds", 60);

            worldBorderShrinkTask = new WorldBorderShrinkTask(initialsize, shrinkAmount, interval);

            World world_nether = Bukkit.getWorld("world_nether");
            Location gameNether = new Location(world_nether, 8, 84,  15);
            world_nether.getWorldBorder().setCenter(gameNether);
            world_nether.getWorldBorder().setSize(500);

            int intervalPhase = getConfig().getInt("phase-interval-seconds", 60);
            phaseTask = new PhaseTask(intervalPhase);

            reconectectTask = new ReconectectTask();

            cargarAreasDesdeConfig();

        }

        @Override
        public void onDisable() {

            startTimes.clear();
            teamMembers.clear();
            playerTeams.clear();

            if (barrierTask != null) {
                barrierTask.cancel();
            }

            Scoreboards.clear();
            Scoreboards.deleteBoards("Game3");
            for (Scoreboards board : boards.values()) {

                board.clear();
            }
            boards.clear();
            for (Team team : scoreboard.getTeams()) {
                team.unregister();
            }
            if (worldBorderShrinkTask.hasStarted()) {
                worldBorderShrinkTask.cancel();
            }
            // TASKS CANCEL
            BukkitTask eventPlayerTask = BorderInitialTask.getTask("EventPlayerTask");
            BukkitTask StarGameTask = BorderInitialTask.getTask("StarGameTask");
            if(eventPlayerTask != null  && !eventPlayerTask.isCancelled()){
                eventPlayerTask.cancel();
            }

            if(StarGameTask != null  && !StarGameTask.isCancelled()){
                StarGameTask.cancel();
            }
        }

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (cmd.getName().equalsIgnoreCase("accept")) {
                    if (args.length == 1) {
                        Player inviter = Bukkit.getPlayer(args[0]);
                        if (inviter != null && pendingInvites.containsKey(inviter)) {
                            Team team = scoreboard.getPlayerTeam(inviter);
                            Team playerTeam = scoreboard.getPlayerTeam(player);
                            TeamMember leavingMember = teamMembers.get(player);
                            if (team != null) {

                                if (playerTeam != null) {
                                    playerTeams.remove(playerTeam, player);
                                    playerTeam.removePlayer(player);
                                    teamMembers.remove(player, leavingMember);
                                    if (playerTeam.getSize() == 0) {
                                        playerTeam.unregister();

                                        playerTeams.remove(playerTeam);
                                    }
                                }
                                team.addEntry(player.getName());
                                playerTeams.put(team, player);
                                teamMembers.put(player, new TeamMember(player, team, false));

                                pendingInvites.remove(inviter);

                                player.sendMessage(ChatColor.GREEN + "¡Join Team " + inviter.getName() + "!");
                                inviter.sendMessage(ChatColor.GREEN + player.getName() + " Join your Team.");

                                return true;
                            } else {
                                player.sendMessage(ChatColor.RED + "Team " + inviter.getName() + " not exist.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Nothing invites for " + args[0] + ".");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Usage /accept <player>");
                    }
                    return true;
                }
            }
            return false;
        }


        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            Location to = event.getTo();
            Location locationPlayer = player.getLocation();

            for (Map.Entry<String, PhaseArea> entry : spawnerAreas.entrySet()) {
                PhaseArea spawnerArea = entry.getValue();
                if (spawnerArea.isInArea(locationPlayer)) {

                    int areaPhase = spawnerArea.getPhase();
                    int currentPhase = gameManager.getCurrentPhase();

                    List<Location> locations = spawnerArea.getLocations();

                    if(currentPhase >= areaPhase) {

                    } else {
                        Location previousLocation = player.getLocation().clone();
                        previousLocation.subtract(player.getLocation().getDirection().multiply(2)); // Ajusta 5 según tus necesidades
                        player.teleport(previousLocation);
                        player.sendMessage(ChatColor.RED + "¡You cannot enter this area in the current phase of the game!  Phase Requeried " + areaPhase);
                        return;
                    }
                }
            }
        }

        private Location getTeleportLocationOutsideArea(Location playerLocation, Location pos1, Location pos2) {
            World world = playerLocation.getWorld();
            double x1 = pos1.getX();
            double y1 = pos1.getY();
            double z1 = pos1.getZ();

            double x2 = pos2.getX();
            double y2 = pos2.getY();
            double z2 = pos2.getZ();

            double centerX = (x1 + x2) / 2;
            double centerY = (y1 + y2) / 2;
            double centerZ = (z1 + z2) / 2;

            double directionX = centerX - playerLocation.getX();
            double directionZ = centerZ - playerLocation.getZ();

            double newX = playerLocation.getX() - directionX * 2;
            double newZ = playerLocation.getZ() - directionZ * 2;

            Location newLocation = new Location(world, newX, playerLocation.getY(), newZ);

            return newLocation;
        }


        public void cargarAreas(String nombreArea, Location pos1, Location pos2, int fase) {
            // Verificar si el nombre del área ya está en uso
            if (spawnerAreas.containsKey(nombreArea)) {
               getLogger().info("Error: The area name is already in use.");
                return;
            }
            List<Location> locations = new ArrayList<>();
            locations.add(pos1);
            locations.add(pos2);

            PhaseArea spawnerArea = new PhaseArea(locations, fase);

            spawnerAreas.put(nombreArea, spawnerArea);
        }

        private void startBarrierTask() {
            barrierTask = new BukkitRunnable() {
                @Override
                public void run() {
                   // generateBarrierParticles();
                    checkWinningTeam();
                    checkFinalBattle();
                }
            }.runTaskTimer(this, 0, 60);
        }


        private void generateBarrierParticles() {
            for (int x = -gameRadius; x <= gameRadius; x += 5) {
                for (int z = -gameRadius; z <= gameRadius; z += 5) {
                    Location particleLocation = gameCenter.clone().add(x, 1, z);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1); // Crea una opción de partícula con color rojo
                    gameWorld.spawnParticle(Particle.REDSTONE, particleLocation, 1, dustOptions);
                }
            }
        }

        public GameManager getGameManager() {
            return gameManager;
        }

        private void checkWinningTeam() {
            if (gameManager.isRunning()) {
                int aliveTeams = countAliveTeams();
                if (aliveTeams == 1) {

                    Team winningTeam = null;
                    for (Team team : scoreboard.getTeams()) {
                        if (team.getSize() > 0) {
                            winningTeam = team;
                            break;
                        }
                    }
                    if (winningTeam != null) {

                        for (String playerName : winningTeam.getEntries()) {
                            Player player = Bukkit.getPlayer(playerName);
                            if (player != null) {

                                Location playerLocation = player.getLocation();
                                Bukkit.getWorld(gameWorld.getName()).playSound(playerLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.5f);
                                World world = player.getWorld();
                                Firework firework = (Firework) world.spawnEntity(playerLocation, EntityType.FIREWORK);
                                FireworkMeta fireworkMeta = firework.getFireworkMeta();
                                fireworkMeta.addEffect(FireworkEffect.builder()
                                        .withColor(Color.RED)
                                        .with(FireworkEffect.Type.BALL)
                                        .flicker(true)
                                        .build());
                                fireworkMeta.setPower(1);
                                firework.setFireworkMeta(fireworkMeta);
                            }
                        }
                        endGame(winningTeam);
                    }
                }

            } else {

            }
        }

        private boolean teamsTeleported = false;
        private void checkFinalBattle() {
            if (gameManager.isRunning()) {
                int aliveTeams = countAliveTeams();
                if (aliveTeams == 2 && !teamsTeleported) {
                    List<Location> teamLocations = new ArrayList<>();

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        Location gameCenter = this.gameCenter;
                        Location randomLocation = getRandomLocation(gameCenter, 30);
                        player.teleport(randomLocation);
                    }

                    for(Player player : Bukkit.getOnlinePlayers()){
                        teamLocations.add(player.getLocation());
                    }

                    for (Location location : teamLocations) {
                        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        location.getWorld().spawnParticle(Particle.PORTAL, location, 50);
                        location.getWorld().spawnParticle(Particle.END_ROD, location, 50);
                        location.getWorld().strikeLightningEffect(location);
                        location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.5f);
                        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 1.5f, 1.5f);
                    }
                    Bukkit.broadcastMessage(ChatColor.RED + "¡Final battle between the remaining teams in the center of the game.!");

                    teamsTeleported = true;
                }
            }
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



        private int countAliveTeams() {
            int count = 0;
            for (Team team : scoreboard.getTeams()) {
                if (team.getSize() > 0) {
                    count++;
                }
            }
            return count;
        }

        private void endGame(Team winningTeam) {
            // Cancelar la tarea de la barrera
            if (barrierTask != null) {
                barrierTask.cancel();
            }

            if (phaseTask != null){
                phaseTask.cancel();
            }
            if (worldBorderShrinkTask.hasStarted()) {
                worldBorderShrinkTask.cancel();
            }

            BukkitTask eventPlayerTask = BorderInitialTask.getTask("EventPlayerTask");
            BukkitTask StarGameTask = BorderInitialTask.getTask("StarGameTask");
            if(eventPlayerTask != null  && !eventPlayerTask.isCancelled()){
                eventPlayerTask.cancel();
            }

            if(StarGameTask != null  && !StarGameTask.isCancelled()){
                StarGameTask.cancel();
            }

            for (String playerName : winningTeam.getEntries()) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null) {
                    ItemStack mapItem = createMapFromUrl("https://png.pngtree.com/png-vector/20220729/ourmid/pngtree-congratulations-text-with-confetti-png-image_234317.png");
                    if (mapItem != null) {
                        player.getInventory().addItem(mapItem);
                        player.sendMessage(ChatColor.GREEN + "¡Congratulations for winning!");
                    } else {
                        player.sendMessage(ChatColor.RED + "An error occurred while creating the map. Please contact the administrator.");
                    }
                }
            }
            gameManager.endGame();

            Bukkit.broadcastMessage(ChatColor.GREEN + "¡Team " + winningTeam.getName() + " a Won!");

            restartPlugin();
        }

        private ItemStack createMapFromUrl(String imageUrl) {
            try {
                BufferedImage image = ImageIO.read(new URL(imageUrl));
                if (image == null) {
                    getLogger().warning("La imagen no pudo ser descargada desde la URL.");
                    return null;
                }
                ItemStack map = new ItemStack(Material.FILLED_MAP);
                MapMeta mapMeta = (MapMeta) map.getItemMeta();
                if (mapMeta == null) {
                    getLogger().warning("No se pudo obtener el meta del mapa.");
                    return null;
                }

                MapView mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
                mapView.getRenderers().forEach(mapView::removeRenderer);
                mapView.addRenderer(new CustomMapRenderer(image));
                mapMeta.setMapView(mapView);

                map.setItemMeta(mapMeta);
                return map;
            } catch (IOException e) {
                getLogger().warning("Error al descargar la imagen desde la URL: " + e.getMessage());
                return null;
            }
        }

        public void restartPlugin() {

            allowConnections = false;

            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.broadcastMessage(ChatColor.GREEN + " The Game will restart shortly");
            }, 80L);


            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.kickPlayer("The game is restarting");
                }
            }, 300L);


            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.getPluginManager().disablePlugin(this);
                Bukkit.getPluginManager().enablePlugin(this);
            }, 300L);
        }

        public long getTimeToNextShrink() {
            if (worldBorderShrinkTask != null) {
                // Calcular el tiempo restante hasta la próxima reducción del borde del mundo
                long timeSinceLastShrink = (System.currentTimeMillis() - worldBorderShrinkTask.getLastShrinkTime()) / 1000;
                return worldBorderShrinkTask.getInterval() - timeSinceLastShrink;
            }
            return -1;
        }

        public long getTimeToPhase(){
            if(phaseTask != null){
                long timeSinceLastShrink = (System.currentTimeMillis() - phaseTask.getLastPhaseTime()) / 1000;
                return phaseTask.getInterval() - timeSinceLastShrink;
            }
            return -1;
        }

        public Map<Player, TeamMember> getTeamMembers() {
            return teamMembers;
        }

        public Location getGameAreaPos1() {
            double x1 = gameCenter.getX() - gameRadius;
            double y1 = gameCenter.getY();
            double z1 = gameCenter.getZ() - gameRadius;
            return new Location(gameCenter.getWorld(), x1, y1, z1);
        }

        public Location getGameAreaPos2() {
            double x2 = gameCenter.getX() + gameRadius;
            double y2 = gameCenter.getY();
            double z2 = gameCenter.getZ() + gameRadius;
            return new Location(gameCenter.getWorld(), x2, y2, z2);
        }

        public void cargarAreasDesdeConfig() {
            FileConfiguration config = getConfig();
            ConfigurationSection areasSection = config.getConfigurationSection("areas");

            if (areasSection == null) {
                getLogger().warning("No areas found in configuration.");
                return;
            }

            for (String areaName : areasSection.getKeys(false)) {
                ConfigurationSection areaConfig = areasSection.getConfigurationSection(areaName);
                if (areaConfig == null) {
                    getLogger().warning("Invalid area configuration for '" + areaName + "'.");
                    continue;
                }

                int phase = areaConfig.getInt("phase");
                String[] pos1Str = areaConfig.getString("pos1").split(",");
                String[] pos2Str = areaConfig.getString("pos2").split(",");
                if (pos1Str.length != 3 || pos2Str.length != 3) {
                    getLogger().warning("Poorly formatted area location for '" + areaName + "'.");
                    continue;
                }

                double x1 = Double.parseDouble(pos1Str[0].trim());
                double y1 = Double.parseDouble(pos1Str[1].trim());
                double z1 = Double.parseDouble(pos1Str[2].trim());

                double x2 = Double.parseDouble(pos2Str[0].trim());
                double y2 = Double.parseDouble(pos2Str[1].trim());
                double z2 = Double.parseDouble(pos2Str[2].trim());

                Location pos1 = new Location(gameWorld, x1, y1, z1);
                Location pos2 = new Location(gameWorld, x2, y2, z2);

                cargarAreas(areaName, pos1, pos2, phase);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerLogin(PlayerLoginEvent event) {

            if (!allowConnections) {

                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "¡No connections allowed during server restart!");
            }
        }

        public WorldBorderShrinkTask getWorldBorderShrinkTask() {
            return worldBorderShrinkTask;
        }

        public PhaseTask getPhaseTask() {
            return phaseTask;
        }

        public ReconectectTask getReconectectTask() {
            return reconectectTask;
        }

        public Location getGameCenter() {
            return gameCenter;
        }



        public HashMap<String, Long> getStartTimes() {
            return startTimes;
        }
    }
