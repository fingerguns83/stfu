package net.fg83.stfu;

import com.google.inject.Inject;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Plugin(
        id = "stfu",
        name = "STFU",
        version = "1.0",
        url = "https://fg83.net",
        description = "Prevent join/leave spam when moving between servers.",
        authors = "fingerguns83"
)
public class Stfu {

    public Stfu plugin = this;
    private final Logger logger;
    private final ProxyServer velocity;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    private final List<Player> trackedPlayers = new ArrayList<>();

    @Inject
    public Stfu(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory){
        this.velocity = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        //logger.info("Construct");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        //logger.info("Initialized");
        try {
            // Define the resource path
            String resourcePath = "/sthu-1.0.jar";

            // Ensure the data directory exists
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            // Define the target file
            Path targetFile = dataDirectory.resolve("sthu-1.0.jar");

            // Copy the file from the resources to the target location
            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    logger.error("Resource not found: " + resourcePath);
                    return;
                }
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Resource copied successfully to " + targetFile.toString());
            }
        } catch (Exception e) {
            logger.error("Failed to copy file", e);
        }

        int pluginId = 20172; // <-- Replace with the id of your plugin!
        Metrics metrics = metricsFactory.make(this, pluginId);
    }

    @Subscribe
    public void onPlayerSuccessfulJoin(ServerConnectedEvent event){
        Player player = event.getPlayer();

        //logger.info(player.getUsername() + " attempting to join a server...");

        if (event.getPreviousServer().isEmpty()) {
            //logger.info(player.getUsername() + " is a fresh connection.");
            if (!trackedPlayers.contains(player)){

                //logger.info("Adding " + player.getUsername() + " to tracked list...");

                trackedPlayers.add(player);

                final Component joinMessage = Component.text(player.getUsername() + " joined the game.", NamedTextColor.YELLOW);

                player.sendMessage(joinMessage);

                velocity.getAllServers().forEach(registeredServer -> {
                    registeredServer.getPlayersConnected().forEach(connectedPlayer -> {
                        connectedPlayer.sendMessage(joinMessage);

                    });
                });
            }
            else {
                //logger.info("Player was already in tracked list!");
            }
        }
        else {
            //logger.info(player.getUsername() + " was already connected.");
        }
    }

    @Subscribe
    public void onPlayerLeaveEvent(DisconnectEvent event){
        Player player = event.getPlayer();
        //logger.info(player.getUsername() + " disconnected.");

        if (event.getLoginStatus().equals(DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) && trackedPlayers.contains(player)){
            //logger.info(player.getUsername() + " had a valid connection.");
            trackedPlayers.remove(player);

            final Component leaveMessage = Component.text(player.getUsername() + " left the game.", NamedTextColor.YELLOW);

            velocity.getAllServers().forEach(registeredServer -> {

                registeredServer.getPlayersConnected().forEach(connectedPlayer -> {
                    connectedPlayer.sendMessage(leaveMessage);
                });
            });
        }
        else {
            //logger.info(player.getUsername() + " was not fully connected.");
        }
    }
}
