package fr.w3blog.mockimail.service;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dumbster.smtp.SimpleSmtpServer;

import fr.w3blog.mockimail.config.MockimailConfig;

@Controller
@RequestMapping("/mail")
public class MockimailService {

	private static final Logger logger = LoggerFactory
			.getLogger(MockimailService.class);

	@Autowired
	private MockimailConfig mockimailConfig;

	private Node node;
	private Client client;
	private SimpleSmtpServer simpleSmtpServer;

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
			simpleSmtpServer = SimpleSmtpServer.start(mockimailConfig
					.getSmtpPort());
			simpleSmtpServer.setMockimailConfig(mockimailConfig);
			simpleSmtpServer.setClientES(client);
		} catch (ElasticSearchException elasticSearchException) {
			logger.error("ElasticSearch could not be started",
					elasticSearchException);
		}
		logger.info("ElasticSearch started");

	}

	/**
	 * @param query
	 * @param timeDiff
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "/search", method = RequestMethod.GET, produces = { "application/json; charset=UTF-8" })
	@ResponseBody
	public String search(@RequestParam(required = false) String query,
			@RequestParam(required = false) Long timeDiff) throws IOException {

		/**
		 * SearchResponse searchResponse = client
		 * .prepareSearch(mockimailConfig.getIndexES())
		 * .setTypes(mockimailConfig.getTypeES())
		 * .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		 * .setQuery(QueryBuilders.termQuery("multi", "test")) // Query
		 * .setFilter(FilterBuilders.rangeFilter("age").from(12).to(18)) //
		 * Filter
		 * .setFrom(0).setSize(60).setExplain(true).execute().actionGet();
		 **/
		SearchRequestBuilder searchReq = client
				.prepareSearch(mockimailConfig.getIndexES())
				.setTypes(mockimailConfig.getTypeES())
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.addSort("date", SortOrder.DESC);
		if (query != null && !query.isEmpty()) {
			searchReq.setQuery(QueryBuilders.queryString(query));
			// searchReq.setFilter(FilterBuilders.queryFilter(QueryBuilders
			// .queryString("test")));
		}
		if (timeDiff != null) {
			searchReq
					.setFilter(FilterBuilders.rangeFilter("date").gt(
							System.currentTimeMillis()
									- (timeDiff.longValue() * 1000)));
		}

		SearchResponse searchResponse = searchReq.execute().actionGet();
		// On retourne le json sans changement
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		searchResponse.toXContent(builder, ToXContent.EMPTY_PARAMS);
		builder.endObject();
		return builder.string();

	}

	/**
	 * Permet de fermer proprement le serveur smtp et le noeud ElasticSearch
	 * 
	 */
	@PreDestroy
	public void shutdown() {
		logger.info("Shutdown...");
		if (node != null) {
			node.close();
		}
		if (client != null) {
			client.close();
		}

		if (simpleSmtpServer != null) {
			simpleSmtpServer.stop();
		}
	}

	public void destroy() throws Exception {

	}
}
