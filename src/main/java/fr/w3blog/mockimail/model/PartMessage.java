package fr.w3blog.mockimail.model;

public class PartMessage {

	private String contentType;

	private String body;

	private String fileName;

	public PartMessage() {
		super();
	}

	public PartMessage(String contentType, String body) {
		super();
		this.contentType = contentType;
		this.body = body;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
