package org.barrelmc.barrel.network.translator.bedrock;
import org.barrelmc.barrel.network.translator.interfaces.BedrockPacketTranslator;
import org.barrelmc.barrel.player.Player;
import org.barrelmc.barrel.server.ProxyServer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;

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
      player.setChannel(new Bootstrap().channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
                       .group(new NioEventLoopGroup())
                        .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProxyServer.getInstance().getBedrockPacketCodec().ge)
                       );
    }else{
      player.setChannel();
    }
  }
}
