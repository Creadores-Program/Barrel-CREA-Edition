/*
 * Copyright (c) 2021 BarrelMC Team
 * This project is licensed under the MIT License
 */

package org.barrelmc.barrel.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheCenterPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundHelloPacket;
import com.github.steveice10.packetlib.Session;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.barrelmc.barrel.auth.AuthManager;
import org.barrelmc.barrel.auth.Xbox;
import org.barrelmc.barrel.config.Config;
import org.barrelmc.barrel.math.Vector3;
import org.barrelmc.barrel.network.BedrockBatchHandler;
import org.barrelmc.barrel.network.translator.PacketTranslatorManager;
import org.barrelmc.barrel.server.ProxyServer;
import org.barrelmc.barrel.utils.Utils;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockClientSession;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockClientInitializer;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import java.net.InetSocketAddress;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Player extends Vector3 {

    @Getter
    private final Session javaSession;
    @Getter
    private BedrockClientSession bedrockClientSession;
    @Getter
    private Channel channel;
    @Getter
    private final PacketTranslatorManager packetTranslatorManager;

    private String accessToken = null;
    @Getter
    private ECPublicKey publicKey;
    @Getter
    private ECPrivateKey privateKey;

    @Setter
    @Getter
    private long runtimeEntityId;
    @Getter
    private String username;
    @Getter
    private String javaUsername;
    @Getter
    private String xuid;
    @Getter
    private String UUID;

    @Setter
    @Getter
    private int scoreSortorder;

    @Setter
    @Getter
    private StartGamePacket startGamePacketCache;

    @Setter
    @Getter
    private String traslateAd = "false";

    private boolean tickPlayerInputStarted = false;
    private final ScheduledExecutorService playerInputExecutor = Executors.newScheduledThreadPool(1);

    @Setter
    @Getter
    private Vector3f oldPosition;

    @Setter
    @Getter
    private Vector3f lastServerPosition;

    @Setter
    @Getter
    private Vector2f lastServerRotation;

    @Setter
    @Getter
    private boolean isImmobile = false;

    @Setter
    @Getter
    private boolean isSneaking = false;
    @Setter
    @Getter
    private boolean isSprinting = false;
    @Setter
    @Getter
    private PlayerActionType diggingStatus;
    @Setter
    @Getter
    private Vector3i diggingPosition;
    @Setter
    @Getter
    private Direction diggingFace;

    @Setter
    @Getter
    private GameType gameMode = GameType.ADVENTURE;

    @Getter
    private final Set<PlayerAuthInputData> playerAuthInputData = EnumSet.noneOf(PlayerAuthInputData.class);
    @Getter
    private final List<PlayerBlockActionData> playerAuthInputActions = new ObjectArrayList<>();
    @Setter
    @Getter
    private ItemUseTransaction playerAuthInputItemUseTransaction = null;

    @Getter
    @Setter
    private int hotbarSlot = 0;

    @Getter
    @Setter
    private String language = "en-US";

    public Player(ServerboundHelloPacket loginPacket, Session javaSession) {
        this.packetTranslatorManager = new PacketTranslatorManager(this);
        this.javaSession = javaSession;

        if (ProxyServer.getInstance().getConfig().getAuth().equals("offline")) {
            this.offlineLogin(loginPacket);
        } else {
            this.accessToken = AuthManager.getInstance().getAccessTokens().remove(loginPacket.getUsername());
            this.onlineLogin(loginPacket);
        }
    }

    public void startSendingPlayerInput() {
        if (!tickPlayerInputStarted) {
            tickPlayerInputStarted = true;

            PlayerAuthInputThread playerAuthInputThread = new PlayerAuthInputThread();
            playerAuthInputThread.player = this;
            playerAuthInputThread.tick = getStartGamePacketCache().getCurrentTick();

            playerInputExecutor.scheduleAtFixedRate(playerAuthInputThread, 0, 50, TimeUnit.MILLISECONDS);
        }
    }

    private void onlineLogin(ServerboundHelloPacket javaLoginPacket) {
        Config config = ProxyServer.getInstance().getConfig();
        InetSocketAddress bedrockAddress = new InetSocketAddress(config.getBedrockAddress(), config.getBedrockPort());
        try {
            channel = new Bootstrap().channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
                    .group(new NioEventLoopGroup())
                    .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProxyServer.getInstance().getBedrockPacketCodec().getRaknetProtocolVersion())
                    .handler(new BedrockClientInitializer() {
                        @Override
                        protected void initSession(BedrockClientSession session) {
                            bedrockClientSession = session;
                            session.setCodec(ProxyServer.getInstance().getBedrockPacketCodec());
                            session.setPacketHandler(new BedrockBatchHandler(Player.this));

                            RequestNetworkSettingsPacket requestNetworkSettingsPacket = new RequestNetworkSettingsPacket();
                            requestNetworkSettingsPacket.setProtocolVersion(ProxyServer.getInstance().getBedrockPacketCodec().getProtocolVersion());
                            session.sendPacketImmediately(requestNetworkSettingsPacket);
                        }
                    })
                    .connect(bedrockAddress)
                    .awaitUninterruptibly().channel();
            this.javaUsername = javaLoginPacket.getUsername();
            ProxyServer.getInstance().addBedrockPlayer(this);
        } catch (Exception exception){
            javaSession.disconnect("Failed to connect: " + exception);
        }
    }

    public LoginPacket getOnlineLoginPacket() throws Exception {
        LoginPacket loginPacket = new LoginPacket();

        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC");
        keyPairGen.initialize(new ECGenParameterSpec("secp256r1"));

        KeyPair ecdsa256KeyPair = keyPairGen.generateKeyPair();
        this.publicKey = (ECPublicKey) ecdsa256KeyPair.getPublic();
        this.privateKey = (ECPrivateKey) ecdsa256KeyPair.getPrivate();

        Xbox xbox = new Xbox(this.accessToken);
        //String userToken = xbox.getUserToken(this.publicKey, this.privateKey);
        String deviceToken = xbox.getDeviceToken(this.publicKey, this.privateKey);
        //String titleToken = xbox.getTitleToken(this.publicKey, this.privateKey, deviceToken);
        String xsts = xbox.getXBLToken(this.accessToken, this.publicKey, this.privateKey, deviceToken);

        KeyPair ecdsa384KeyPair = EncryptionUtils.createKeyPair();
        this.publicKey = (ECPublicKey) ecdsa384KeyPair.getPublic();
        this.privateKey = (ECPrivateKey) ecdsa384KeyPair.getPrivate();

        String chainData = xbox.requestMinecraftChain(xsts, this.publicKey);
        JsonObject chainDataObject = JsonParser.parseString(chainData).getAsJsonObject();
        JsonArray minecraftNetChain = chainDataObject.get("chain").getAsJsonArray();
        String firstChainHeader = minecraftNetChain.get(0).getAsString();
        firstChainHeader = firstChainHeader.split("\\.")[0];
        firstChainHeader = new String(Base64.getDecoder().decode(firstChainHeader.getBytes()));
        String firstKeyx5u = JsonParser.parseString(firstChainHeader).getAsJsonObject().get("x5u").getAsString();

        JsonObject newFirstChain = new JsonObject();
        newFirstChain.addProperty("certificateAuthority", true);
        newFirstChain.addProperty("exp", Instant.now().getEpochSecond() + TimeUnit.HOURS.toSeconds(6));
        newFirstChain.addProperty("identityPublicKey", firstKeyx5u);
        newFirstChain.addProperty("nbf", Instant.now().getEpochSecond() - TimeUnit.HOURS.toSeconds(6));

        {
            String publicKeyBase64 = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
            JsonObject jwtHeader = new JsonObject();
            jwtHeader.addProperty("alg", "ES384");
            jwtHeader.addProperty("x5u", publicKeyBase64);

            String jwt = generateJwt(jwtHeader, newFirstChain);

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jwt);
            for(int i = 0; i < minecraftNetChain.size(); i++){
                jsonArray.add(minecraftNetChain.get(i));
            }
            for (JsonElement o : jsonArray) {
                loginPacket.getChain().add(o.getAsString());
            }
        }
        {
            String lastChain = minecraftNetChain.get(minecraftNetChain.size() - 1).getAsString();
            String lastChainPayload = lastChain.split("\\.")[1];
            lastChainPayload = new String(Base64.getDecoder().decode(lastChainPayload.getBytes()));

            JsonObject payloadObject = JsonParser.parseString(lastChainPayload).getAsJsonObject();
            JsonObject extraData = payloadObject.get("extraData").getAsJsonObject();

            this.username = extraData.get("displayName").getAsString();
            this.xuid = extraData.get("XUID").getAsString();
            this.UUID = extraData.get("identity").getAsString();
        }

        loginPacket.setExtra(this.getSkinData());
        loginPacket.setProtocolVersion(ProxyServer.getInstance().getBedrockPacketCodec().getProtocolVersion());
        return loginPacket;
    }

    private void offlineLogin(ServerboundHelloPacket javaLoginPacket) {
        this.xuid = "";
        this.username = this.javaUsername = javaLoginPacket.getUsername();
        if(ProxyServer.getInstance().getConfig().getUseJavaId() && javaLoginPacket.getProfileId() != null){
            this.UUID = javaLoginPacket.getProfileId().toString();
        }else{
            this.UUID = java.util.UUID.randomUUID().toString();
        }
        Config config = ProxyServer.getInstance().getConfig();
        InetSocketAddress bedrockAddress = new InetSocketAddress(config.getBedrockAddress(), config.getBedrockPort());
        try {
            channel = new Bootstrap().channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
                    .group(new NioEventLoopGroup())
                    .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProxyServer.getInstance().getBedrockPacketCodec().getRaknetProtocolVersion())
                    .handler(new BedrockClientInitializer() {
                        @Override
                        protected void initSession(BedrockClientSession session) {
                            bedrockClientSession = session;
                            session.setCodec(ProxyServer.getInstance().getBedrockPacketCodec());
                            session.setPacketHandler(new BedrockBatchHandler(Player.this));

                            RequestNetworkSettingsPacket requestNetworkSettingsPacket = new RequestNetworkSettingsPacket();
                            requestNetworkSettingsPacket.setProtocolVersion(ProxyServer.getInstance().getBedrockPacketCodec().getProtocolVersion());
                            session.sendPacketImmediately(requestNetworkSettingsPacket);
                        }
                    })
                    .connect(bedrockAddress)
                    .awaitUninterruptibly().channel();
            ProxyServer.getInstance().addBedrockPlayer(this);
        } catch (Exception exception) {
            javaSession.disconnect("Failed to connect: " + exception);
        }
    }

    public LoginPacket getLoginPacket() {
        LoginPacket loginPacket = new LoginPacket();

        KeyPair ecdsa384KeyPair = EncryptionUtils.createKeyPair();
        this.publicKey = (ECPublicKey) ecdsa384KeyPair.getPublic();
        this.privateKey = (ECPrivateKey) ecdsa384KeyPair.getPrivate();

        String publicKeyBase64 = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());

        JsonObject chain = new JsonObject();
        chain.addProperty("exp", Instant.now().getEpochSecond() + TimeUnit.HOURS.toSeconds(6));
        chain.addProperty("identityPublicKey", publicKeyBase64);
        chain.addProperty("nbf", Instant.now().getEpochSecond() - TimeUnit.HOURS.toSeconds(6));

        JsonObject extraData = new JsonObject();
        extraData.addProperty("identity", this.UUID);
        extraData.addProperty("XUID", this.xuid);
        extraData.addProperty("displayName", this.username);
        chain.addProperty("extraData", extraData);

        JsonObject jwtHeader = new JsonObject();
        jwtHeader.addProperty("alg", "ES384");
        jwtHeader.addProperty("x5u", publicKeyBase64);

        String jwt = generateJwt(jwtHeader, chain);

        JsonArray chainDataJsonArray = new JsonArray();
        chainDataJsonArray.add(jwt);

        for (JsonElement o : chainDataJsonArray) {
            loginPacket.getChain().add(o.getAsString());
        }

        loginPacket.setExtra(this.getSkinData());
        loginPacket.setProtocolVersion(ProxyServer.getInstance().getBedrockPacketCodec().getProtocolVersion());
        return loginPacket;
    }

    private String getSkinData() {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());

        JsonObject jwtHeader = new JsonObject();
        jwtHeader.addProperty("alg", "ES384");
        jwtHeader.addProperty("x5u", publicKeyBase64);

        JsonObject skinData = new JsonObject();

        skinData.addProperty("AnimatedImageData", new JsonArray());
        skinData.addProperty("ArmSize", "");
        skinData.addProperty("CapeData", "");
        skinData.addProperty("CapeId", "");
        skinData.addProperty("PlayFabId", java.util.UUID.randomUUID().toString());
        skinData.addProperty("CapeImageHeight", 0);
        skinData.addProperty("CapeImageWidth", 0);
        skinData.addProperty("CapeOnClassicSkin", false);
        skinData.addProperty("ClientRandomId", new Random().nextLong());
        skinData.addProperty("CompatibleWithClientSideChunkGen", false);
        skinData.addProperty("CurrentInputMode", 1);
        skinData.addProperty("DefaultInputMode", 1);
        skinData.addProperty("DeviceId", java.util.UUID.randomUUID().toString());
        skinData.addProperty("DeviceModel", "Barrel CREA Edition");
        skinData.addProperty("DeviceOS", 7);
        skinData.addProperty("GameVersion", ProxyServer.getInstance().getBedrockPacketCodec().getMinecraftVersion());
        skinData.addProperty("GuiScale", 0);
        skinData.addProperty("LanguageCode", this.language);
        skinData.addProperty("PersonaPieces", new JsonArray());
        skinData.addProperty("PersonaSkin", false);
        skinData.addProperty("PieceTintColors", new JsonArray());
        skinData.addProperty("PlatformOfflineId", "");
        skinData.addProperty("PlatformOnlineId", "");
        skinData.addProperty("PremiumSkin", false);
        skinData.addProperty("SelfSignedId", this.UUID);
        skinData.addProperty("ServerAddress", ProxyServer.getInstance().getConfig().getBedrockAddress() + ":" + ProxyServer.getInstance().getConfig().getBedrockPort());
        skinData.addProperty("SkinAnimationData", "");
        skinData.addProperty("SkinColor", "#0");
        skinData.addProperty("SkinData", ProxyServer.getInstance().getDefaultSkinData());
        skinData.addProperty("SkinGeometryData", Base64.getEncoder().encodeToString(ProxyServer.getInstance().getDefaultSkinGeometry().getBytes()));
        skinData.addProperty("SkinId", this.UUID + ".Custom");
        skinData.addProperty("SkinImageHeight", 64);
        skinData.addProperty("SkinImageWidth", 64);
        skinData.addProperty("SkinResourcePatch", "ewogICAiZ2VvbWV0cnkiIDogewogICAgICAiZGVmYXVsdCIgOiAiZ2VvbWV0cnkuaHVtYW5vaWQuY3VzdG9tIgogICB9Cn0K");
        skinData.addProperty("ThirdPartyName", this.username);
        skinData.addProperty("ThirdPartyNameOnly", false);
        skinData.addProperty("UIProfile", 0);
        skinData.addProperty("IsEditorMode", 0);
        skinData.addProperty("TrustedSkin", 1);
        skinData.addProperty("SkinGeometryDataEngineVersion", Base64.getEncoder().encodeToString(ProxyServer.getInstance().getBedrockPacketCodec().getMinecraftVersion().getBytes()));
        skinData.addProperty("OverrideSkin", false);

        return generateJwt(jwtHeader, skinData);
    }

    private String generateJwt(JsonObject jwtHeader, JsonObject chain) {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(jwtHeader.toJSONString().getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(chain.toJSONString().getBytes());

        byte[] dataToSign = (header + "." + payload).getBytes();
        byte[] signatureBytes = null;
        try {
            Signature signature = Signature.getInstance("SHA384withECDSA");
            signature.initSign(this.privateKey);
            signature.update(dataToSign);
            signatureBytes = Utils.DERToJOSE(signature.sign(), Utils.AlgorithmType.ECDSA384);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ignored) {
        }
        String signatureString = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        return header + "." + payload + "." + signatureString;
    }

    public void sendMessage(String message) {
        this.javaSession.send(new ClientboundSystemChatPacket(Component.text(message), false));
    }

    public void sendTip(String message) {
        this.javaSession.send(new ClientboundSystemChatPacket(Component.text(message), true));
    }

    public void disconnect(String reason) {
        playerInputExecutor.shutdown();
        try {
            this.bedrockClientSession.disconnect();
        } catch (Throwable ignored) {
        }
        if (this.channel.isOpen()) {
            this.channel.disconnect();
            this.channel.parent().disconnect();
        }
        this.javaSession.disconnect(reason);
        ProxyServer.getInstance().removeBedrockPlayer(javaUsername);
        ProxyServer.getInstance().getLogger().info(javaUsername + " disconnected: " + reason);
    }

    @Override
    public void setPosition(Vector3f vector3f) {
        if (this.getFloorX() >> 4 != vector3f.getFloorX() >> 4 || this.getFloorZ() >> 4 != vector3f.getFloorZ() >> 4) {
            this.javaSession.send(new ClientboundSetChunkCacheCenterPacket(vector3f.getFloorX() >> 4, vector3f.getFloorZ() >> 4));
        }
        super.setPosition(vector3f);
    }

    @Override
    public void setPosition(double x, double y, double z) {
        if (this.getFloorX() >> 4 != (int) x >> 4 || this.getFloorZ() >> 4 != (int) z >> 4) {
            this.javaSession.send(new ClientboundSetChunkCacheCenterPacket((int) x >> 4, (int) z >> 4));
        }
        super.setPosition(x, y, z);
    }
}

class PlayerAuthInputThread implements Runnable {
    public Player player;
    public long tick;

    public void run() {
        try {
            if (player.getBedrockClientSession().isConnected()) {
                ++tick;

                PlayerAuthInputPacket pk = new PlayerAuthInputPacket();

                pk.setPosition(player.getVector3f());
                pk.setRotation(Vector3f.from(player.getPitch(), player.getYaw(), player.getYaw()));
                pk.setMotion(Vector2f.ZERO);
                pk.setInputInteractionModel(InputInteractionModel.CROSSHAIR);
                pk.setInputMode(InputMode.MOUSE);
                pk.setPlayMode(ClientPlayMode.SCREEN);
                pk.setVrGazeDirection(null);
                pk.setTick(tick);
                pk.setDelta(Vector3f.from(player.getVector3f().getX() - player.getOldPosition().getX(), player.getVector3f().getY() - player.getOldPosition().getY(), player.getVector3f().getZ() - player.getOldPosition().getZ()));
                pk.setItemStackRequest(null);
                pk.setItemUseTransaction(player.getPlayerAuthInputItemUseTransaction());
                pk.setAnalogMoveVector(Vector2f.ZERO);

                pk.getInputData().addAll(player.getPlayerAuthInputData());
                pk.getPlayerActions().addAll(player.getPlayerAuthInputActions());

                if (player.isSneaking()) {
                    pk.getInputData().add(PlayerAuthInputData.SNEAKING);
                }
                if (player.isSprinting()) {
                    pk.getInputData().add(PlayerAuthInputData.SPRINTING);
                }
                if (player.getDiggingStatus() == PlayerActionType.START_BREAK) {
                    pk.getInputData().add(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS);

                    PlayerBlockActionData blockActionData = new PlayerBlockActionData();
                    blockActionData.setAction(PlayerActionType.CONTINUE_BREAK);
                    blockActionData.setBlockPosition(player.getDiggingPosition());
                    blockActionData.setFace(player.getDiggingFace().ordinal());
                    pk.getPlayerActions().add(blockActionData);
                }

                player.getBedrockClientSession().sendPacketImmediately(pk);

                player.getPlayerAuthInputData().removeAll(player.getPlayerAuthInputData());
                player.getPlayerAuthInputActions().removeAll(player.getPlayerAuthInputActions());
                player.setPlayerAuthInputItemUseTransaction(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
