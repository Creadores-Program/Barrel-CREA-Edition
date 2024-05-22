package org.barrelmc.barrel.network.translator.java;

import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientInformationPacket;
import org.barrelmc.barrel.network.translator.interfaces.JavaPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.RequestChunkRadiusPacket;

public class ClientInformationPacket implements JavaPacketTranslator {

    @Override
    public void translate(MinecraftPacket pk, Player player) {
        ServerboundClientInformationPacket settingsPacket = (ServerboundClientInformationPacket) pk;
        RequestChunkRadiusPacket chunkRadiusPacket = new RequestChunkRadiusPacket();
        player.setLanguage(settingsPacket.getLocale());
        chunkRadiusPacket.setRadius(settingsPacket.getRenderDistance());
        player.getBedrockClientSession().sendPacketImmediately(chunkRadiusPacket);
    }
}
