package org.barrelmc.barrel.network.translator.bedrock;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
public class ToastRequestPacket implements BedrockPacketTranslator {
  @Override
  public void translate(BedrockPacket pk, Player player) {
    org.cloudburstmc.protocol.bedrock.packet.ToastRequestPacket packet = (org.cloudburstmc.protocol.bedrock.packet.ToastRequestPacket) pk;
    player.sendTip(packet.getTitle() + "\n" + packet.getContent());
  }
}
