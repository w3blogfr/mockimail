package fr.w3blog.mockimail.service;

import javax.annotation.PostConstruct;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dumbster.smtp.SimpleSmtpServer;

@Controller
@RequestMapping("/mail")
public class MockimailService {

	private static final Logger logger = LoggerFactory
			.getLogger(MockimailService.class);

	private Node node;
	private Client client;

	@PostConstruct
	public void init() {
		logger.info("ElasticSearch starting...");
		try {
			ImmutableSettings.Builder settings = ImmutableSettings
					.settingsBuilder();
			settings.put("node.name", "mockimail-terreur");
			String workingDir = System.getProperty("user.dir");
			logger.info("Stockage ES index : " + workingDir);
			settings.put("path.data", workingDir);
			// settings.put("http.enabled", false);
			node = NodeBuilder.nodeBuilder().settings(settings)
					.clusterName("mockimail-es").data(true).local(true).node();
			client = node.client();
			SimpleSmtpServer simpleSmtpServer = SimpleSmtpServer.start(5000);
			simpleSmtpServer.setClientES(client);
		} catch (ElasticSearchException elasticSearchException) {
			logger.error("ElasticSearch could not be started",
					elasticSearchException);
		}
		logger.info("ElasticSearch started");

	}

	@RequestMapping(value = "/test", method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public void list() {
		logger.debug("mon message : {}", "cocuou");
		System.out.println("COucou");
	}
}
