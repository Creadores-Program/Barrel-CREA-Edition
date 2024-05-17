package org.barrelmc.barrel.network.translator.bedrock;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetTitleTextPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundClearTitlesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetTitlesAnimationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import net.kyori.adventure.text.Component;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import net.kyori.adventure.text.Component;
public class SetTitlePacket implements BedrockPacketTranslator {
  @Override
    public void translate(BedrockPacket pk, Player player) {
      org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket packet = (org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket) pk;
      switch(packet.getType()){
        case TITLE_JSON:
        case TITLE: {
          player.getJavaSession().send(new ClientboundSetTitleTextPacket(Component.text(packet.getText()));
          break;
        }
        case CLEAR: {
          player.getJavaSession().send(new ClientboundClearTitlesPacket(false));
          break;
        }
        case RESET: {
          player.getJavaSession().send(new ClientboundClearTitlesPacket(true));
          break;
        }
        case SUBTITLE_JSON:
        case SUBTITLE: {
          player.getJavaSession().send(new ClientboundSetSubtitleTextPacket(Component.text(packet.getText())));
          break;
        }
        case ACTIONBAR_JSON:
        case ACTIONBAR: {
          player.getJavaSession().send(new ClientboundSetActionBarTextPacket(Component.text(packet.getText())));
          break;
        }
        case TIMES: {
          player.getJavaSession().send(new ClientboundSetTitlesAnimationPacket(packet.getFadeInTime(), packet.getStayTime(), packet.getFadeOutTime()));
          break;
        }
      }
    }
}
