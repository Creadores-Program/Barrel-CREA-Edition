package org.barrelmc.barrel.network.translator.java;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.barrelmc.barrel.network.translator.interfaces.JavaPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.barrelmc.barrel.server.ProxyServer;
import org.cloudburstmc.protocol.bedrock.packet.CommandRequestPacket;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOriginType;
import org.cloudburstmc.protocol.bedrock.data.command.CommandOriginData;
public class ChatCommandPacket implements JavaPacketTranslator {
  @Override
  public void translate(MinecraftPacket pk, Player player) {
    ServerboundChatCommandPacket packet = (ServerboundChatCommandPacket) pk;
    CommandRequestPacket Crp = new CommandRequestPacket();
    Crp.setVersion(ProxyServer.getInstance().getBedrockPacketCodec().getProtocolVersion());
    Crp.setCommand(packet.getCommand());
    CommandOriginData Cod = new CommandOriginData(CommandOriginType.PLAYER, java.util.UUID.fromString(player.getUUID()), null, null);
    Crp.setCommandOriginData(Cod);
    player.getBedrockClientSession().sendPacket(Crp);
  }
}
