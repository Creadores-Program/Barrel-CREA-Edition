package org.barrelmc.barrel.network.translator.bedrock;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.barrelmc.barrel.server.ProxyServer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
public class TransferPacket implements BedrockPacketTranslator{
  @Override
  public void translate(BedrockPacket pk, Player player){
    try {
            player.getBedrockClientSession().disconnect();
        } catch (Throwable ignored) {
        }
        if (player.getChannel().isOpen()) {
          try{
            player.getChannel().disconnect();
            player.getChannel().parent().disconnect();
            player.getChannel().close();
            player.getChannel().parent().close();
          }catch(Throwable ignored){
            
          }
        }
    org.cloudburstmc.protocol.bedrock.packet.TransferPacket packet = (org.cloudburstmc.protocol.bedrock.packet.TransferPacket) pk;
    if(ProxyServer.getInstance().getConfig().getAuth().equals("offline")){
      try{
        player.offlineLogin(player.getHelloPacketJava(), packet.getAddress(), packet.getPort());
      }catch(Exception exception){
        player.getJavaSession().disconnect("Failed to transfer: " + exception);
      }
    }else{
      try{
        player.onlineLogin(player.getHelloPacketJava(), packet.getAddress(), packet.getPort());
      }catch(Exception exception){
        player.getJavaSession().disconnect("Failed to transfer: " + exception);
      }
    }
  }
}
