package org.barrelmc.barrel.network.translator.java;
import org.barrelmc.barrel.network.translator.interfaces.JavaPacketTranslator;
import com.github.steveice10.mc.protocol.codec.MinecraftPacket;
import org.barrelmc.barrel.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.ModalFormResponsePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerClosePacket;
public class FormResponseCloseAdapterPacket implements JavaPacketTranslator{
  @Override
  public void translate(MinecraftPacket pk, Player player) {
    ClientboundContainerClosePacket closeformpk = (ClientboundContainerClosePacket)pk;
    if(player.isForm(closeformpk.getContainerId())){
      ModalFormResponsePacket formResPacket = new ModalFormResponsePacket();
      formResPacket.setFormId(closeformpk.getContainerId());
      formResPacket.setFormData("{ canceled: true; }");
      player.getBedrockClientSession().sendPacket(formResPacket);
    }
  }
}
