package org.barrelmc.barrel.network.translator.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.barrelmc.barrel.network.translator.TranslatorUtils;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.barrelmc.barrel.server.ProxyServer;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;

public class StartGamePacket implements BedrockPacketTranslator {

    @Override
    public void translate(BedrockPacket pk, Player player) {
        org.cloudburstmc.protocol.bedrock.packet.StartGamePacket packet = (org.cloudburstmc.protocol.bedrock.packet.StartGamePacket) pk;

        player.setRuntimeEntityId(packet.getRuntimeEntityId());
        player.setOldPosition(packet.getPlayerPosition());
        player.setPosition(packet.getPlayerPosition());
        player.setLastServerPosition(packet.getPlayerPosition());
        player.setLastServerRotation(packet.getRotation());

        player.setStartGamePacketCache(packet);

        player.setGameMode(packet.getPlayerGameType());

        SimpleDefinitionRegistry<ItemDefinition> itemDefinitions = SimpleDefinitionRegistry.<ItemDefinition>builder()
                .addAll(packet.getItemDefinitions())
                .add(new SimpleItemDefinition("minecraft:empty", 0, false))
                .build();
        player.getBedrockClientSession().getPeer().getCodecHelper().setItemDefinitions(itemDefinitions);

        if (!packet.isBlockNetworkIdsHashed()) {
            player.getBedrockClientSession().getPeer().getCodecHelper().setBlockDefinitions(ProxyServer.getInstance().getBlockDefinitions());
        }

        ClientboundLoginPacket serverJoinGamePacket = new ClientboundLoginPacket(
                (int) packet.getRuntimeEntityId(), false,
                TranslatorUtils.translateGamemodeToJE(packet.getPlayerGameType()),
                TranslatorUtils.translateGamemodeToJE(packet.getPlayerGameType()),
                new String[]{"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"}, ProxyServer.getInstance().getRegistryCodec(),
                "minecraft:overworld", "minecraft:overworld", 100,
                ProxyServer.getInstance().getConfig().getMaxplayers(), 16, 16, false, true, false, false, null, 0
        );

        player.getJavaSession().send(serverJoinGamePacket);

        Vector3f position = packet.getPlayerPosition();
        Vector2f rotation = packet.getRotation();
        ClientboundPlayerPositionPacket serverPlayerPositionRotationPacket = new ClientboundPlayerPositionPacket(position.getX(), position.getY(), position.getZ(), rotation.getY(), rotation.getX(), 0);
        player.getJavaSession().send(serverPlayerPositionRotationPacket);
        player.getJavaSession().send(new ClientboundEntityEventPacket((int) packet.getRuntimeEntityId(), EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0));
    }

    @Override
    public boolean immediate() {
        return true;
    }
}
