package fr.w3blog.mockimail.config;

public class MockimailConfig {

	private int smtpPort = 5000;

	private String indexES = "mockimail";
	private String typeES = "mail";

	public int getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(int smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getIndexES() {
		return indexES;
	}

	public void setIndexES(String indexES) {
		this.indexES = indexES;
	}

	public String getTypeES() {
		return typeES;
	}

	public void setTypeES(String typeES) {
		this.typeES = typeES;
	}

}
