package net.nocturne.network;

import java.net.InetSocketAddress;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import net.nocturne.stream.InputStream;

import net.nocturne.game.player.Player;
import net.nocturne.grab.Grab;
import net.nocturne.websocket.Websocket;
import net.nocturne.network.decoders.ClientPacketsDecoder;
import net.nocturne.network.decoders.Decoder;
import net.nocturne.network.decoders.GrabPacketsDecoder;
import net.nocturne.network.decoders.LoginPacketsDecoder;
import net.nocturne.network.decoders.WorldLoginPacketsDecoder;
import net.nocturne.network.decoders.WorldPacketsDecoder;
import net.nocturne.network.decoders.WebsocketPacketsDecoder;
import net.nocturne.network.encoders.Encoder;
import net.nocturne.network.encoders.GrabPacketsEncoder;
import net.nocturne.network.encoders.LoginPacketsEncoder;
import net.nocturne.network.encoders.WorldPacketsEncoder;
import net.nocturne.network.encoders.WebsocketPacketsEncoder;
import net.nocturne.stream.OutputStream;

public class Session {

	private Channel channel;
	private Decoder decoder;
	private Encoder encoder;
	private boolean websocketFlag = false;

	// hammershield mod
	protected int iplen = -1;
	protected byte[] ipdata = null;
	protected InetSocketAddress client = null;
	// ----------------

	protected byte[] buffer = new byte[0];
	protected int bufferOffset = 0;

	public Session(Channel channel) {
		this.channel = channel;
		setDecoder(0);
	}

	public final ChannelFuture write(OutputStream outStream) {
		if (outStream == null || !channel.isConnected())
			return null;
		if(websocketFlag && !(encoder instanceof WebsocketPacketsEncoder)) {
			int lgth = outStream.getOffset()>126?(outStream.getOffset()>0xffff?outStream.getOffset()+2+6:outStream.getOffset()+2+2):outStream.getOffset()+2;
			OutputStream buffer = new OutputStream(lgth);
			byte head = (byte) 0x82;
			
			buffer.writeByte(head);
			
			if(outStream.getOffset() < 126) {
				buffer.writeByte((byte) outStream.getOffset());
			} else {
				if(outStream.getLength() < 0xffff) {
					buffer.writeByte((byte) 126);
					buffer.writeShort(outStream.getOffset());
				} else {
					buffer.writeByte((byte) 127);
					buffer.writeLong(outStream.getOffset());
				}
			}
			
			buffer.writeBytes(outStream.getBuffer(), 0, outStream.getOffset());
			
			return channel.write(ChannelBuffers.copiedBuffer(buffer.getBuffer(), 0, buffer.getOffset()));
		} else {
			return channel.write(ChannelBuffers.copiedBuffer(outStream.getBuffer(),
				0, outStream.getOffset()));
		}
	}

	public final ChannelFuture write(ChannelBuffer outStream) {
		if (outStream == null || !channel.isConnected())
			return null;
				if(websocketFlag && !(encoder instanceof WebsocketPacketsEncoder)) {
						OutputStream buffer = new OutputStream(outStream.capacity()>126?(outStream.capacity()>0xffff?outStream.capacity()+2+6:outStream.capacity()+2+2):outStream.capacity()+2);
						byte head = (byte) 0x82;
						
						buffer.writeByte(head);
						
						if(outStream.capacity() < 126) {
							buffer.writeByte((byte) outStream.capacity());
						} else {
							if(outStream.capacity() < 0xffff) {
								buffer.writeByte((byte) 126);
								buffer.writeShort(outStream.capacity());
							} else {
								buffer.writeByte((byte) 127);
								buffer.writeLong(outStream.capacity());
							}
						}
						
						buffer.writeBytes(outStream.array(), 0, outStream.capacity());
						
						return channel.write(ChannelBuffers.copiedBuffer(buffer.getBuffer(), 0, buffer.getOffset()));
					} else {
						return channel.write(outStream);
					}
	}

	public final Channel getChannel() {
		return channel;
	}

	public final Decoder getDecoder() {
		return decoder;
	}

	public GrabPacketsDecoder getGrabPacketsDecoder() {
		return (GrabPacketsDecoder) decoder;
	}

	public final Encoder getEncoder() {
		return encoder;
	}

	public final void setDecoder(int stage) {
		setDecoder(stage, null);
	}

	public final void setDecoder(int stage, Object attachement) {
		switch (stage) {
		case 0:
			decoder = new ClientPacketsDecoder(this);
			break;
		case 1:
			decoder = new GrabPacketsDecoder(this, (Grab) attachement);
			break;
		case 2:
			decoder = new LoginPacketsDecoder(this);
			break;
		case 3:
			decoder = new WorldPacketsDecoder(this, (Player) attachement);
			break;
		case 4:
			decoder = new WorldLoginPacketsDecoder(this, (Player) attachement);
			break;
		case 5:
			decoder = new WebsocketPacketsDecoder(this, (Websocket) attachement);
			websocketFlag = true;
			break;
		case -1:
		default:
			decoder = null;
			break;
		}
	}

	public final void setEncoder(int stage) {
		setEncoder(stage, null);
	}

	public final void setEncoder(int stage, Object attachement) {
		switch (stage) {
		case 0:
			encoder = new GrabPacketsEncoder(this);
			break;
		case 1:
			encoder = new LoginPacketsEncoder(this);
			break;
		case 2:
			encoder = new WorldPacketsEncoder(this, (Player) attachement);
			break;
		case 3:
			encoder = new WebsocketPacketsEncoder(this, (Websocket) attachement);
			break;
		case -1:
		default:
			encoder = null;
			break;
		}
	}

	public LoginPacketsEncoder getLoginPackets() {
		return (LoginPacketsEncoder) encoder;
	}

	public GrabPacketsEncoder getGrabPackets() {
		return (GrabPacketsEncoder) encoder;
	}

	public WorldPacketsEncoder getWorldPackets() {
		return (WorldPacketsEncoder) encoder;
	}

	public WebsocketPacketsEncoder getWebsocketPackets() {
		return (WebsocketPacketsEncoder) encoder;
	}
	
	public String getIP() {
		/*
		 * if (Settings.AR_PROTECTION) { if (client == null) return null; return
		 * client.getAddress().getHostAddress(); } else {
		 */
		if (channel == null
				|| !(channel.getRemoteAddress() instanceof InetSocketAddress))
			return null;
		InetSocketAddress addr = (InetSocketAddress) channel.getRemoteAddress();
		return addr.getAddress().getHostAddress();
		// }
	}

	public String getLocalAddress() {
		return channel.getLocalAddress().toString();
	}
	
	public void setWebsocketFlag(boolean flag) {
		websocketFlag = flag;
	}
	
	public boolean isWebsocket() {
		return websocketFlag;
	}
	
	public final byte[] decodeWebsocketMsg(byte[] msg) {
		InputStream stream = new InputStream(msg);
		int desc = stream.readUnsignedByte();
		int fin = desc >> 7 & 1;
		int rsv1 = desc >> 6 & 1;
		int rsv2 = desc >> 5 & 1;
		int rsv3 = desc >> 4 & 1;
		int opcode = desc & 15;
		int mask_n_length = stream.readUnsignedByte();
		int mask = mask_n_length >> 7 & 1;
		int lgth = mask_n_length & 127;
		int lgth1 = 0;
		if(lgth == 126) {
			lgth = stream.readUnsignedShort();
		} else if(lgth == 127) {
			lgth1 = stream.readInt();
			lgth =	stream.readInt();
		}

		byte[] key = new byte[4];
		stream.readBytes(key,0,4);
		byte[] decoded = new byte[lgth]; // stream.getLength() - stream.getOffset()];
		
		for (int i = 0; i < decoded.length; i++) {
				decoded[i] = (byte)(stream.readByte() ^ key[i & 0x3]);
		}
		
		bufferOffset = stream.getOffset();
		return decoded;
	}
}
