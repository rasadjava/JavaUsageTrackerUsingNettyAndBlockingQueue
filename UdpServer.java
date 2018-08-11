
import akka.actor.ActorRef;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.bootstrap.Bootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.ChannelPipeline;

import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.javausage.tracker.datasource.impl.SavePackets;

import akka.actor.AbstractActor;

/**
 * Discards any incoming data.
 */
public class UdpServer implements Runnable {

	private int port;
	ActorRef serverActor = null;
	protected BlockingQueue<String> queue = null;

	public UdpServer(int port, BlockingQueue<String> queue) {
		this.port = port;
		this.queue = queue;
	}

	public void run() {
		final NioEventLoopGroup group = new NioEventLoopGroup();
		try {
			final Bootstrap b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
					.handler(new ChannelInitializer<NioDatagramChannel>() {
						@Override
						public void initChannel(final NioDatagramChannel ch) throws Exception {

							ChannelPipeline p = ch.pipeline();
							p.addLast(new IncommingPacketHandler(serverActor, queue));
						}
					});

			// Bind and start to accept incoming connections.
			Integer pPort = port;
			InetAddress address = InetAddress.getLocalHost();
			System.out.printf("waiting for message %s %s", String.format(pPort.toString()),
					String.format(address.toString()));
			b.bind(address, port).sync().channel().closeFuture().await();
		} catch (Exception e) {

		} finally {
			System.out.print("In Server Finally");
		}
	}

	public static void main(String[] args) throws InterruptedException {
		BlockingQueue<String> queuee = new ArrayBlockingQueue<String>(1024);
		int port = 9876;
		UdpServer udpServer = new UdpServer(port, queuee);
		new Thread(udpServer).start();
		Thread.sleep(20000);
		SavePackets savePacketInterfaceImpl = new SavePackets(queuee);
		new Thread(savePacketInterfaceImpl).start();
	}
}