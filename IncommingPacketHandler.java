
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import akka.actor.ActorRef;

public class IncommingPacketHandler extends SimpleChannelInboundHandler<DatagramPacket> {
	protected BlockingQueue<String> queue = null;

	IncommingPacketHandler(ActorRef parserServer, BlockingQueue<String> queue) {
		this.queue = queue;
	}

	@Override
	protected void messageReceived(ChannelHandlerContext channelHandlerContext, DatagramPacket packet)
			throws Exception {
		String req = packet.content().toString(CharsetUtil.UTF_8);

		final InetAddress srcAddr = packet.sender().getAddress();
		final ByteBuf buf = packet.content();
		final int rcvPktLength = buf.readableBytes();
		final byte[] rcvPktBuf = new byte[rcvPktLength];
		buf.readBytes(rcvPktBuf);
		System.out.println("\n[INFO3]: " + req);
		try {
			queue.put(req);
			// System.out.println("Packet added to queue"+queue.take());
			System.out.println("Packet added to queue");
		} catch (Exception e) {
			System.out.println("Error occured while inserting the data into Database." + e);
		}
	}
}
