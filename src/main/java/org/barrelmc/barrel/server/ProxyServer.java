/*
 * Copyright (c) 2021 BarrelMC Team
 * This project is licensed under the MIT License
 */

package org.barrelmc.barrel.server;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.SessionService;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.codec.MinecraftCodec;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.ServerClosedEvent;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.tcp.TcpServer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.ini4j.Ini;
import org.barrelmc.barrel.auth.AuthManager;
import org.barrelmc.barrel.auth.server.AuthServer;
import org.barrelmc.barrel.config.Config;
import org.barrelmc.barrel.network.JavaPacketHandler;
import org.barrelmc.barrel.player.Player;
import org.barrelmc.barrel.utils.FileManager;
import org.barrelmc.barrel.utils.NbtBlockDefinitionRegistry;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662;
import org.yaml.snakeyaml.Yaml;
import org.barrelmc.barrel.utils.Logger;
import org.barrelmc.barrel.utils.nukkit.TextFormat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.net.URL;

public class ProxyServer {

    @Getter
    private static ProxyServer instance = null;
    @Getter
    private final Map<String, Player> bedrockPlayers = new ConcurrentHashMap<>();
    @Getter
    private final BedrockCodec bedrockPacketCodec = Bedrock_v662.CODEC;

    @Getter
    private final Path dataPath;

    @Getter
    private Config config;

    @Getter
    private String defaultSkinData;
    @Getter
    private String defaultSkinGeometry;

    @Getter
    private CompoundTag registryCodec;

    @Getter
    private NbtBlockDefinitionRegistry blockDefinitions;
    
    private Logger logger;

    public ProxyServer(String dataPath) {
        instance = this;
        this.logger = new Logger(TextFormat.GOLD.getAnsiCode()+"BarrelMC");
        this.dataPath = Paths.get(dataPath);
        if (!initConfig()) {
            this.getLogger().emergency("Config file not found! Terminating...");
            System.exit(1);
        }
        loadRegistryCodec();
        loadBlockDefinitions();
        loadDefaultSkin();
        startServer();
    }
    public Logger getLogger(){
        return this.logger;
    }

    private boolean initConfig() {
        File configFile = new File(dataPath.toFile(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (inputStream == null) {
                    return false;
                }
                Files.createDirectories(configFile.getParentFile().toPath());
                Files.copy(inputStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                return false;
            }
        }
        try {
            this.config = new Yaml().loadAs(Files.newBufferedReader(configFile.toPath()), Config.class);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    private void loadRegistryCodec() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("registry-codec.nbt");
             DataInputStream dataInputStream = new DataInputStream(new GZIPInputStream(Objects.requireNonNull(inputStream)))) {
            registryCodec = (CompoundTag) NBTIO.readTag((InputStream) dataInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load registry codec", e);
        }
    }

    private void loadBlockDefinitions() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("block_palette.nbt")) {
            assert inputStream != null;
            try (NBTInputStream nbtInputStream = NbtUtils.createGZIPReader(inputStream)) {
                Object object = nbtInputStream.readTag();
                if (object instanceof NbtMap) {
                    NbtMap blocksTag = (NbtMap) object;
                    blockDefinitions = new NbtBlockDefinitionRegistry(blocksTag.getList("blocks", NbtType.COMPOUND));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load block definitions", e);
        }
    }

    private void loadDefaultSkin() {
        try {
            defaultSkinData = FileManager.getFileContents(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("skin/skin_data.txt")));
            defaultSkinGeometry = FileManager.getFileContents(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("skin/skin_geometry.json")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        SessionService sessionService = new SessionService();
        Logger consol = this.getLogger();
        Server server = new TcpServer(this.config.getBindAddress(), this.config.getPort(), MinecraftProtocol::new);
        server.setGlobalFlag(MinecraftConstants.SESSION_SERVICE_KEY, sessionService);
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, this.config.getPremiumPlayerJava());
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> new ServerStatusInfo(new VersionInfo(MinecraftCodec.CODEC.getMinecraftVersion(), MinecraftCodec.CODEC.getProtocolVersion()), new PlayerInfo(this.config.getMaxplayers(), 0, new ArrayList<>()), Component.text(this.config.getMotd()), this.getIcon(), false));
        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session -> {
            GameProfile profile = session.getFlag(MinecraftConstants.PROFILE_KEY);
            consol.info(profile.getName() + " logged in");
            if (!AuthManager.getInstance().getLoginPlayers().containsKey(profile.getName()) && this.getPlayerByName(profile.getName()) == null) {
                session.addListener(new AuthServer(session, profile.getName()));
            }
        });
        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 100);
        String StopMSG = this.config.getShutdownMessage();
        server.addListener(new ServerAdapter() {
            @Override
            public void serverClosed(ServerClosedEvent event) {
                for (var entry : ProxyServer.getInstance().getBedrockPlayers().entrySet()) {
                    Player player = entry.getValue();

                    player.disconnect(StopMSG);
                }
                consol.info("Server closed.");
            }

            @Override
            public void sessionAdded(SessionAddedEvent event) {
                event.getSession().addListener(new JavaPacketHandler());
            }

            @Override
            public void sessionRemoved(SessionRemovedEvent event) {
                GameProfile profile = event.getSession().getFlag(MinecraftConstants.PROFILE_KEY);
                if (profile == null) {
                    return;
                }
                String username = profile.getName();
                if (isBedrockPlayer(username)) {
                    getPlayerByName(username).disconnect("logged out");
                }
                if (AuthManager.getInstance().getLoginPlayers().get(username) != null) {
                    AuthManager.getInstance().getLoginPlayers().remove(username);
                }
                if (AuthManager.getInstance().getTimers().get(username) != null) {
                    AuthManager.getInstance().getTimers().get(username).cancel();
                    AuthManager.getInstance().getTimers().remove(username);
                }
                consol.info(username + " logged out");
            }
        });

        consol.info("Binding to " + this.config.getBindAddress() + " on port " + this.config.getPort());
        server.bind();
        consol.info("BarrelProxy "+TextFormat.GREEN.getAnsiCode()+"CREA "+TextFormat.AQUA.getAnsiCode()+"Edition"+TextFormat.RESET.getAnsiCode()+" is running on [" + this.config.getBindAddress() + "::" + this.config.getPort() + "]");
        consol.info("Done!");
    }

    public Player getPlayerByName(String username) {
        return this.bedrockPlayers.get(username);
    }

    public void addBedrockPlayer(Player player) {
        this.bedrockPlayers.put(player.getJavaUsername(), player);
    }

    public void removeBedrockPlayer(String javaUsername) {
        this.bedrockPlayers.remove(javaUsername);
    }

    public boolean isBedrockPlayer(String username) {
        return this.bedrockPlayers.containsKey(username);
    }
    private byte[] getIconFileToBytes(String pathPng){
        try{
        Path IconF = Path.of(this.dataPath.toFile().getAbsolutePath()+"/"+pathPng);
        return Files.readAllBytes(IconF);
        }catch(Exception e){
            e.printStackTrace();
            this.getLogger().warn("No load Icon! Use default Icon...");
            return null;
        }
    }
    private byte[] getIconUrlToBytes(String url){
        try{
        URL urlIcon = new URL(url);
        try (InputStream inputStream = urlIcon.openStream();
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }
        }catch(Exception e){
            e.printStackTrace();
            this.getLogger().warn("No load Icon! Use default Icon...");
            return null;
        }
    }
    public byte[] getIcon(){
        byte[] Icon;
        if(this.config.getIcon() == null || this.config.getIcon().trim() == ""){
            return null;
        }
        if(this.config.getIcon().startsWith("https://") || this.config.getIcon().startsWith("http://")){
            Icon = this.getIconUrlToBytes(this.config.getIcon());
        }else{
            Icon = this.getIconFileToBytes(this.config.getIcon());
        }
        return Icon;
    }
}
