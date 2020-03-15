package net.nocturne.network.encoders;

import org.apache.commons.codec.binary.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.nocturne.Settings;
import net.nocturne.cache.Cache;
import net.nocturne.websocket.WebsockRequest;
import net.nocturne.websocket.Websocket;
import net.nocturne.network.Session;
import net.nocturne.stream.OutputStream;
import net.nocturne.utils.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

public final class WebsocketPacketsEncoder extends Encoder {

	private static byte[] F255_255;

	private int encryptionValue;
	private Websocket websocket;

	public WebsocketPacketsEncoder(Session connection, Websocket attachement) {
		super(connection);
		websocket = attachement;
	}
	
    private byte[] sha1(String input){
        // StringBuffer sb = new StringBuffer();
    	byte[] result = null;
    	try {
	        // MessageDigest mDigest = MessageDigest.getInstance("SHA1");
	        result = MessageDigest.getInstance("SHA1").digest(input.getBytes("UTF-8"));
	        //for (int i = 0; i < result.length; i++) {
	        //    sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
	        //}
    	} catch(Exception e) {
    		
    	}
         
        return result; //sb.toString();
    }

	public final void sendOutdatedClientPacket() {
		OutputStream stream = new OutputStream(1);
		stream.writeByte(6);
		ChannelFuture future = session.write(stream);
		if (future != null)
			future.addListener(ChannelFutureListener.CLOSE);
		else
			session.getChannel().close();
	}
	
	public final void queueStartupPacket() {
		websocket.queueStartupPacket(3);
	}

	public final boolean sendStartUpPacket() {
		OutputStream stream = new OutputStream(3);// + Settings.GRAB_SERVER_KEYS.length * 4);
		// byte[] buf = {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,1,0,0};
		byte [] buf = {(byte) 0x82,(byte) 0x1,0};
		stream.writeBytes(buf);
		stream.setOffset(stream.getOffset());
		//for (int key : Settings.GRAB_SERVER_KEYS)
		//	stream.writeInt(key);
		session.write(stream);
		return true;
	}
	
	public final void sendHandshake() {
		try {
			//byte[] encodedBytes = Base64.encodeBase64(sha1(websocket.getKey()+Settings.WEBSOCKET_GUID).getBytes("UTF-8"));
			byte[] encodedBytes = Base64.encodeBase64(MessageDigest
                    .getInstance("SHA-1")
                    .digest((websocket.getKey() + Settings.WEBSOCKET_GUID)
                            .getBytes("UTF-8")));
			OutputStream stream = new OutputStream(Settings.WEBSOCKET_HEADER.length()+"Sec-WebSocket-Accept: ".length()+encodedBytes.length+"\r\n".length()+("Sec-WebSocket-Protocol: " + websocket.getProtocolName() + "\r\n\r\n").length());
			// stream.writeByte(0);
			stream.writeString(Settings.WEBSOCKET_HEADER);
			String str = new String(encodedBytes);
			stream.setOffset(stream.getOffset()-1);
			stream.writeString("Sec-WebSocket-Accept: "+str+"\r\n");
			// stream.writeBytes(encodedBytes);
			// stream.writeString("\r\n");
			stream.setOffset(stream.getOffset()-1);
			stream.writeString("Sec-WebSocket-Protocol: " + websocket.getProtocolName() + "\r\n\r\n");
			stream.setOffset(stream.getOffset()-1);
			
			session.write(stream);
		} catch (Exception e) {
			
		}
	}

	public final boolean sendCacheArchiveDesc(WebsockRequest request) {
		OutputStream stream = new OutputStream(7);
		byte [] buf = {(byte) 0x82,(byte) 0x5};
		stream.writeBytes(buf);
		stream.writeByte(request.getIndex());
		stream.writeInt(request.getArchive());
		
		session.write(stream);
		
		return true;
	}
	
	public final boolean sendCacheArchive(WebsockRequest request, boolean priority) {
		byte[] archive = getArchive(request.getIndex(), request.getArchive());
		if (archive == null) { // test
			// Logger.log(GrabPacketsEncoder.class,
			// "null js5 archive request : "+request.getIndex()+", "+request.getArchive());
			return true;
		}
		// ChannelBuffer buffer = ChannelBuffers.buffer(102400);
		int length = ((archive[1] & 0xff) << 24) + ((archive[2] & 0xff) << 16) + ((archive[3] & 0xff) << 8) + (archive[4] & 0xff) + 5;
		
		if (archive[0] != 0)
			length += 4;
		// OutputStream buffer = new OutputStream(length+2+(length>126?(length>0xffff?6:2):0));
		
		
		// boolean renew = false;
		int renewcount = length > 102400 - 5? (int)(length / (102400-5)) + 1: 1;
		for(int i=0; i < renewcount; i++) {
			sendCacheArchiveDesc(request);
			OutputStream buffer = new OutputStream((length-(102400-5)*i)>126?((length-(102400-5)*i)>0xffff?((length-(102400-5)*i)>102400-5?102400:(length-(102400-5)*i)+2+6):(length-(102400-5)*i)+2+2):(length-(102400-5)*i)+2);
			
			byte head = (byte) 0x82; // renewcount == 1 ? (byte) 0x82 : (i == 0 ? (byte) 0x02:(i== renewcount-1? (byte) 0x80 : 0));
			
			buffer.writeByte(head);
			if((length-(102400-5)*i) < 126) {
				buffer.writeByte((byte) (length-(102400-5)*i));
			} else {
				if((length-(102400-5)*i) < 0xffff) {
					buffer.writeByte((byte) 126);
					buffer.writeShort((length-(102400-5)*i));
				} else {
					buffer.writeByte((byte) 127);
					// buffer.writeShort(0);
					buffer.writeLong(length - (102400-5)*i < 102400-5 ? length-(102400-5)*i : 102400-5);// length);
				}
			}
			
			if(length-(102400-5)*i > 0) {
				buffer.writeBytes(archive, (102400-5)*i, length-(102400-5)*i > 102400-5? 102400-5 : length-(102400-5)*i);
				buffer.setOffset(buffer.getBuffer().length);
				session.write(buffer);
			}
		}
		
		return true;
		
		// buffer.writeByte(request.getIndex());
		// buffer.writeInt(request.getArchive() | (!priority ? ~0x7fffffff : 0));
/*
		for (int index = request.getBytesSent(); index < length; index++) {
			buffer.writeByte(archive[index]);
			if (buffer.writerIndex() == 102400 || index == length - 1) {
				if (encryptionValue != 0) {
					for (int i = 0; i < buffer.writerIndex(); i++)
						buffer.setByte(i, buffer.getByte(i) ^ encryptionValue);
				}
				request.setBytesSent(index + 1);
				session.write(buffer);
				return request.getBytesSent() == length;
			}
		}
		*/
		//return false;
	}

	public void setEncryptionValue(int encryptionValue) {
		this.encryptionValue = encryptionValue;
	}

	public int getEncryptionValue() {
		return encryptionValue;
	}

	private static byte[] getArchive(int index, int id) {
		return index == 255 && id == 255 ? getArchive255_255() : (index == 255 ? Cache.STORE.getIndex255() : Cache.STORE.getIndexes()[index].getMainFile()).getCachedArchiveData(id);
	}

	private static byte[] getArchive255_255() {
		if (F255_255 == null) {
			byte[] file = Cache.generateUkeysFile();
			OutputStream stream = new OutputStream();
			stream.writeByte(0);
			stream.writeInt(file.length);
			stream.writeBytes(file);
			byte[] data = new byte[stream.getOffset()];
			// System.out.println("Archive 255 Data: " + Arrays.toString(data));
			stream.setOffset(0);
			stream.getBytes(data, 0, data.length);
			F255_255 = data;
		}
		return F255_255;
	}

}