package org.barrelmc.barrel.network.translator.bedrock;
import net.kyori.adventure.text.Component;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import net.kyori.adventure.text.Component;
import com.github.steveice10.mc.protocol.data.game.BossBarAction;
import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.data.game.BossBarDivision;
import java.util.UUID;
public class BossEventPacket implements BedrockPacketTranslator {
  @Override
    public void translate(BedrockPacket pk, Player player) {
      org.cloudburstmc.protocol.bedrock.packet.BossEventPacket packet = (org.cloudburstmc.protocol.bedrock.packet.BossEventPacket) pk;
      
      switch(packet.getAction()){
        case CREATE: {
          UUID id = UUID.randomUUID();
          player.setBossBar(id);
          player.getJavaSession().send(new ClientboundBossEventPacket(id, BossBarAction.ADD, Component.text(packet.getTitle()), packet.getHealthPercentage(), BossBarColor.PURPLE, BossBarDivision.NONE, false, false, false));
          break;
        }
      }
    }
}
