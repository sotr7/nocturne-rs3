package net.nocturne.network.decoders;

import net.nocturne.websocket.Websocket;
import net.nocturne.network.Session;
import net.nocturne.stream.InputStream;

public final class WebsocketPacketsDecoder extends Decoder {

	private Websocket websocket;

	public WebsocketPacketsDecoder(Session connection, Websocket attachement) {
		super(connection);
		websocket = attachement;
	}

	@Override
	public final int decode(InputStream stream) {
		int desc = stream.readUnsignedByte();
		int fin = desc >> 7 & 1;
		int rsv1 = desc >> 6 & 1;
		int rsv2 = desc >> 5 & 1;
		int rsv3 = desc >> 4 & 1;
		int opcode = desc & 15;
		int mask_n_length = stream.readUnsignedByte();
		int mask = mask_n_length >> 7 & 1;
		long lgth = mask_n_length & 127;
		if(lgth >= 126) {
			lgth = stream.readShort();
			if(lgth == 127) {
				lgth *= 256*256*256*256;
				lgth += stream.readInt();
			}
		}
		if(mask == 0) {
			websocket.finish();
		}
		
		byte[] key = new byte[4];
		stream.readBytes(key,0,4);
		byte[] encoded_msg = new byte[stream.getLength()-stream.getOffset()];
		stream.readBytes(encoded_msg, 0,stream.getLength()-stream.getOffset());
		
		InputStream msg = new InputStream(decodeMaskedMsg(key,encoded_msg));
		
		switch(opcode) {
		case 0: // denotes a continuation frame
			break;
		case 1: // denotes a text frame
			break;
		case 2: // denotes a binary frame
			int cmd = msg.readByte();
			int request_count = 0;
			switch(cmd) {
			case 6: // connection status
				// msg.setOffset(msg.getLength());
				break;
			case 33: // get index description
				websocket.addArchive(msg.readUnsignedByte(), msg.readInt(), false);
				break;
			case 0:
			case 1:
				websocket.addArchive(msg.readUnsignedByte(), msg.readInt(), cmd == 1);
				request_count = (int) msg.getLength()/6;
				for(int i=0; i < request_count-1; i++) {
					cmd = msg.readByte();
					websocket.addArchive(msg.readUnsignedByte(), msg.readInt(), cmd == 1);
				}
				break;
			case 17:
				websocket.addArchive(msg.readUnsignedByte(), msg.readInt(), false);
				request_count = (int) msg.getLength()/6;
				for(int i=0; i < request_count-1; i++) {
					cmd = msg.readByte();
					websocket.addArchive(msg.readUnsignedByte(), msg.readInt(), false);
				}
				break;
			default:
				break;
			}
			break;
		case 8: // denotes a connection close
			break;
		case 9: // denotes a ping
			break;
		case 10: // denotes a pong
			break;
		default:
			break;
		}
		
		/*
		while (stream.getRemaining() >= 6) {
			int packetId = stream.readUnsignedByte();
			switch (packetId) {
			case 0:
			case 1:
				websocket.addArchive(stream.readUnsignedByte(), stream.readInt(),
						packetId == 1);
				break;
			case 2:
			case 3:
				websocket.setLoggedIn(packetId == 2);
				stream.skip(5);
				break;
			case 4:
				session.getWebsocketPackets().setEncryptionValue(
						stream.readUnsignedByte());
				stream.readInt();
				break;
			case 6:
				websocket.init();
				stream.skip(5);
				break;
			case 7:
			default:
				websocket.finish();
				stream.skip(5);
				break;
			}
		}
		*/
		return stream.getOffset();
	}
	
	private final byte[] decodeMaskedMsg(byte[] key, byte[] msg) {
		byte[] decoded = new byte[msg.length];
		
		for (int i = 0; i < msg.length; i++) {
		    decoded[i] = (byte)(msg[i] ^ key[i & 0x3]);
		}
		
		return decoded;
	}
}
