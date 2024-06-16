package org.barrelmc.barrel.network.translator.bedrock;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.barrelmc.barrel.server.ProxyServer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
public class TransferPacket implements BedrockPacketTranslator{
  @Override
  public void translate(BedrockPacket pk, Player player){
    org.cloudburstmc.protocol.bedrock.packet.TransferPacket packet = (org.cloudburstmc.protocol.bedrock.packet.TransferPacket) pk;
    if(ProxyServer.getInstance().getConfig().getAuth() == "offline"){
      try{
        player.offlineLogin(player.getJavaSession(), packet.getAddress(), packet.getPort());
      }catch(Exception exception){
        player.getJavaSession().disconnect("Failed to transfer: " + exception);
      }
    }else{
      try{
        player.onlineLogin(player.getJavaSession(), packet.getAddress(), packet.getPort());
      }catch(Exception exception){
        player.getJavaSession().disconnect("Failed to transfer: " + exception);
      }
    }
  }
}
