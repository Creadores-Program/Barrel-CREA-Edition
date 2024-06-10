package org.barrelmc.barrel.network.translator.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import net.kyori.adventure.text.Component;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

public class TextPacket implements BedrockPacketTranslator {

    @Override
    public void translate(BedrockPacket pk, Player player) {
        org.cloudburstmc.protocol.bedrock.packet.TextPacket packet = (org.cloudburstmc.protocol.bedrock.packet.TextPacket) pk;

        switch (packet.getType()) {
            case TIP:
            case JUKEBOX_POPUP:
            case POPUP: {
                player.sendTip(packet.getMessage());
                break;
            }
            case SYSTEM: {
                player.getJavaSession().send(new ClientboundSystemChatPacket(Component.text(packet.getMessage()), false));
                break;
            }
            case TRANSLATION: {
                if(!player.getTraslateAd()){
                    player.setTraslateAd(true);
                    player.sendMessage("§cChat translation not implemented! Force language on Minecraft bedrock server or use Translator for Geyser Reverse Barrel for Nukkit");
                }
            }
            default: {
                player.sendMessage(packet.getMessage());
                break;
            }
        }
    }
}
