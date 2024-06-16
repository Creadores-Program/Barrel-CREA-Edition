package org.barrelmc.barrel.network.translator.bedrock;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.barrelmc.barrel.server.ProxyServer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockClientInitializer;
import org.cloudburstmc.protocol.bedrock.BedrockClientSession;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;

import java.net.InetSocketAddress;
import io.netty.channel.Channel;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.nio.NioEventLoopGroup;
public class TransferPacket implements BedrockPacketTranslator{
  @Override
  public void translate(BedrockPacket pk, Player player){
    org.cloudburstmc.protocol.bedrock.packet.TransferPacket packet = (org.cloudburstmc.protocol.bedrock.packet.TransferPacket) pk;
    InetSocketAddress newServer = new InetSocketAddress(packet.getAddress(), packet.getPort());
    if(ProxyServer.getInstance().getConfig().getAuth() == "offline"){
      try{
        player.setChannel((Channel)(new Bootstrap().channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
                       .group(new NioEventLoopGroup())
                        .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProxyServer.getInstance().getBedrockPacketCodec().getRaknetProtocolVersion())
                        .handler(new BedrockClientInitializer() {
                          @Override
                          protected void initSession(BedrockClientSession session) {
                            player.setBedrockClientSession(session);
                            session.setCodec(ProxyServer.getInstance().getBedrockPacketCodec());
                            RequestNetworkSettingsPacket requestNetworkSettingsPacket = new RequestNetworkSettingsPacket();
                            requestNetworkSettingsPacket.setProtocolVersion(ProxyServer.getInstance().getBedrockPacketCodec().getProtocolVersion());
                            session.sendPacketImmediately(requestNetworkSettingsPacket);
                          }
                        })
                        .connect(newServer)
                        .awaitUninterruptibly().channel()));
      }catch(Exception exception){
        player.getJavaSession().disconnect("Failed to transfer: " + exception);
      }
    }else{
      player.setChannel();
    }
  }
}
