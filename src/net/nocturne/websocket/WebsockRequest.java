package net.nocturne.websocket;

public class WebsockRequest {

	private int index, archive, bytesSent, specialId;
	boolean special = false;

	public WebsockRequest(int index, int archive) {
		this.index = index;
		this.archive = archive;
	}

	public WebsockRequest(int i) {
		// TODO Auto-generated constructor stub
		special = true;
		specialId = i;
	}

	@Override
	public int hashCode() {
		return index + (archive << 9);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof WebsockRequest))
			return false;
		WebsockRequest o = (WebsockRequest) other;
		return index == o.index && archive == o.archive;
	}

	public int getIndex() {
		return index;
	}

	public int getArchive() {
		return archive;
	}

	public void setBytesSent(int i) {
		bytesSent = i;
	}

	public int getBytesSent() {
		return bytesSent;
	}
	
	public boolean isSpecial() {
		return special;
	}

	public int getSpecialId() {
		return specialId;
	}

}
