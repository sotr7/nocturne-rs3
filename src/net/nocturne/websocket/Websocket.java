package net.nocturne.websocket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.nocturne.cache.Cache;
import net.nocturne.executor.WebsocketThread;
import net.nocturne.network.Session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;

public class Websocket {

	public static final int MAX_REQUEST_SIZE = 500;// was 100 in 831

	private ConcurrentLinkedQueue<WebsockRequest> priorityArchives;
	private ConcurrentLinkedQueue<WebsockRequest> nonPriorityArchives;

	private boolean initialized = false;
	private boolean isLoggedIn;

	private Session session;
	private String key;
	private String protocolName;
	private String webSocketVersion;

	public Websocket(Session session, String req) {
		this.session = session;
		
		Pattern pattern = Pattern.compile("(Sec-WebSocket-Key:)(.*?)(\\r?\\n)");
		Matcher matcher = pattern.matcher(req);
		matcher.find();
		key = matcher.group(2).trim();
		pattern = Pattern.compile("(Sec-WebSocket-Version:)(.*?)(\\r?\\n)");
		matcher = pattern.matcher(req);
		matcher.find();
		webSocketVersion = matcher.group(2).trim();
		pattern = Pattern.compile("(Sec-WebSocket-Protocol:)(.*?)(\\r?\\n)");
		matcher = pattern.matcher(req);
		matcher.find();
		protocolName = matcher.group(2).trim();
		
		/*
		String lines[] = req.split("\\r?\\n");
		
		if(lines.length > 0) {
			for(int i=0; i < lines.length; i++) {
				
			}
		}
		*/
	}

	public void addArchive(int index, int archive, boolean priority) {
		if (index == 255 && archive != 255) {
			if (archive < 0 || archive >= Cache.STORE.getIndexes().length
					|| Cache.STORE.getIndexes()[archive] == null)
				return;
		} else if (index != 255) {
			if (index < 0 || index >= Cache.STORE.getIndexes().length
					|| Cache.STORE.getIndexes()[index] == null || archive < 0
					|| !Cache.STORE.getIndexes()[index].archiveExists(archive))
				return;
		}

		if (!initialized) {
			finish();
			return;
		}

		Queue<WebsockRequest> requestList = priority ? priorityArchives
				: nonPriorityArchives;
		if (requestList.size() >= MAX_REQUEST_SIZE) // in 876 its 500 for high
													// and low
			return;

		WebsockRequest req = new WebsockRequest(index, archive);
		if (requestList.contains(req))
			return;

		synchronized (requestList) {
			requestList.add(req);
		}
	}

	public long processNext(long limit) {
		if (!session.getChannel().isConnected()) {
			finish();
			return 0;
		}
		long total = 0;

		if (priorityArchives.isEmpty()) {
			synchronized (nonPriorityArchives) {
				while (!nonPriorityArchives.isEmpty() && total < limit) {
					WebsockRequest request = nonPriorityArchives.peek();
					int bytesSent = request.getBytesSent();
					if(request.isSpecial() && request.getSpecialId() == 0x1) {
						if (session.getWebsocketPackets().sendStartUpPacket())
							nonPriorityArchives.remove(request);
					} else  if (session.getWebsocketPackets().sendCacheArchive(request,
							false))
						nonPriorityArchives.remove(request);
					total += request.getBytesSent() - bytesSent;
				}
			}
		} else {
			synchronized (priorityArchives) {
				while (!priorityArchives.isEmpty() && total < limit) {
					WebsockRequest request = priorityArchives.peek();
					int bytesSent = request.getBytesSent();
					if(request.isSpecial() && request.getSpecialId() == 0x1) {
						if (session.getWebsocketPackets().sendStartUpPacket())
						priorityArchives.remove(request);
					} else if (session.getWebsocketPackets().sendCacheArchive(request, true)) {
						priorityArchives.remove(request);
					}
					total += request.getBytesSent() - bytesSent;
				}
			}
		}
		return total;
	}

	public void init() {
		if (initialized) {
			finish();
			return;
		}
		priorityArchives = new ConcurrentLinkedQueue<>();// Collections.synchronizedMap(new
		nonPriorityArchives = new ConcurrentLinkedQueue<>();// Collections.synchronizedMap(new

		initialized = true;
		WebsocketThread.add(this);
	}

	public void finish() {
		session.getChannel().close();
		if (!initialized)
			return;
		WebsocketThread.remove(this);
		priorityArchives = nonPriorityArchives = null;
		initialized = false;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public void setLoggedIn(boolean clientLoggedIn) {
		if (!initialized) {
			finish();
			return;
		}
		this.isLoggedIn = clientLoggedIn;
	}

	public Session getSession() {
		return session;
	}

	public String getKey() {
		// TODO Auto-generated method stub
		return key;
	}

	public String getProtocolName() {
		// TODO Auto-generated method stub
		return protocolName;
	}

	public void queueStartupPacket(int lgth) {
		if (!initialized) {
			finish();
			return;
		}

		Queue<WebsockRequest> requestList = priorityArchives;
		if (requestList.size() >= MAX_REQUEST_SIZE) // in 876 its 500 for high
													// and low
			return;

		WebsockRequest req = new WebsockRequest(0x1);
		req.setBytesSent(lgth);
		if (requestList.contains(req))
			return;

		synchronized (requestList) {
			requestList.add(req);
		}
	}

}