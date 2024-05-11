package org.barrelmc.barrel.network.translator.bedrock;

import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;

public class FormRequestPacket implements BedrockPacketTranslator {
  @Override
  public void translate(BedrockPacket pk, Player player) {
    org.cloudburstmc.protocol.bedrock.packet.ModalFormRequestPacket packet = (org.cloudburstmc.protocol.bedrock.packet.ModalFormRequestPacket) pk;
    player.getJavaSession().send(new ClientboundOpenScreenPacket(packet.getFormId(), ContainerType.GENERIC_9X6, packet.getFormData()));
  }
}
