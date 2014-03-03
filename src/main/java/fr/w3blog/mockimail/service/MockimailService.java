package fr.w3blog.mockimail.service;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
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

import com.dumbster.smtp.SimpleSmtpServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.w3blog.mockimail.config.MockimailConfig;
import fr.w3blog.mockimail.model.PartMessage;
import fr.w3blog.mockimail.model.SmtpMessage;

public class MockimailService extends AbstractHandler {

	private static final Logger logger = LoggerFactory.getLogger(MockimailService.class);

	private Node node;
	private Client client;
	private SimpleSmtpServer simpleSmtpServer;

	private MockimailConfig mockimailConfig;

	public MockimailService(MockimailConfig mockimailConfig) {
		this.mockimailConfig = mockimailConfig;
		logger.info("ElasticSearch starting...");
		try {

			// Starting elastic search
			ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();
			settings.put("node.name", "mockimail-terreur");
			String workingDir = System.getProperty("user.dir");

			// Just looking if elastic directory exist
			boolean firstLaunch = !new File(workingDir, "mockimail-es").exists();

			logger.info("Stockage ES index : " + workingDir);
			settings.put("path.data", workingDir);
			// settings.put("http.enabled", false);
			node = NodeBuilder.nodeBuilder().settings(settings).clusterName("mockimail-es").data(true).local(true).node();
			client = node.client();

			if (firstLaunch) {
				// First launch
				// Indexing Welcome message ( create index and type by the same
				// way)
				ObjectMapper mapper = new ObjectMapper();
				IndexRequest indexRequest = Requests.indexRequest(mockimailConfig.getIndexES()).type(mockimailConfig.getTypeES());
				SmtpMessage smtpMessage = new SmtpMessage();
				smtpMessage.setSubject("Welcome to Mockimail");
				smtpMessage
						.addPart(new PartMessage(
								"text/html",
								"Welcome,<br/><br/>this tools will catch all your emails sended by your application.<br/><br/>During development, instead of replace all emails in your database, prefere to juste change your Server Mail.<br/><br/>You just need to coonfigure your mail sender to use port "
										+ mockimailConfig.getSmtpPort()));
				indexRequest.source(mapper.writeValueAsString(smtpMessage));
				client.index(indexRequest).actionGet();
			}

			// Starting Mail Catcher
			simpleSmtpServer = SimpleSmtpServer.start(mockimailConfig.getSmtpPort());
			simpleSmtpServer.setMockimailConfig(mockimailConfig);
			simpleSmtpServer.setClientES(client);
		} catch (ElasticSearchException elasticSearchException) {
			logger.error("ElasticSearch could not be started", elasticSearchException);
		} catch (JsonProcessingException jsonProcessingException) {
			logger.error("Could not convert SmtpMessage to json", jsonProcessingException);
		}
		logger.info("ElasticSearch started");
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		switch (target) {
		case "/search":
			try {
				String query = null;
				if (request.getParameter("query") != null) {
					query = request.getParameter("query");
				}
				Long timeDiff = null;
				if (request.getParameter("timeDiff") != null) {
					timeDiff = Long.valueOf(request.getParameter("timeDiff"));
				}
				/**
				 * SearchResponse searchResponse = client
				 * .prepareSearch(mockimailConfig.getIndexES())
				 * .setTypes(mockimailConfig.getTypeES())
				 * .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				 * .setQuery(QueryBuilders.termQuery("multi", "test")) // Query
				 * .setFilter(FilterBuilders.rangeFilter("age").from(12).to(18))
				 * // Filter .setFrom(0).setSize(60).setExplain(true).execute().
				 * actionGet();
				 **/
				SearchRequestBuilder searchReq = client.prepareSearch(mockimailConfig.getIndexES()).setTypes(mockimailConfig.getTypeES()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
						.addSort("date", SortOrder.DESC);
				if (query != null && !query.isEmpty()) {
					searchReq.setQuery(QueryBuilders.queryString(query));
					// searchReq.setFilter(FilterBuilders.queryFilter(QueryBuilders
					// .queryString("test")));
				}
				if (timeDiff != null) {
					searchReq.setFilter(FilterBuilders.rangeFilter("date").gt(System.currentTimeMillis() - (timeDiff.longValue() * 1000)));
				}

				SearchResponse searchResponse = searchReq.execute().actionGet();
				// On retourne le json sans changement
				XContentBuilder builder = XContentFactory.jsonBuilder();
				builder.startObject();
				searchResponse.toXContent(builder, ToXContent.EMPTY_PARAMS);
				builder.endObject();
				response.getWriter().print(builder.string());
				response.getWriter().close();
			} catch (Exception exception) {
				logger.error("search error", exception);
			}
		case "/call":
			//
			break;
		case "/reset":
			//
			break;
		default:
			// logger.warning(target);
		}
		baseRequest.setHandled(true);
		baseRequest.getResponse().sendError(204);
	}

	public static void main(String... args) throws Exception {

		Integer port = 8080;
		MockimailConfig mockimailConfig = new MockimailConfig();

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.addHandler(createWebapp());

		ContextHandler dynamicContext = new ContextHandler();
		dynamicContext.setContextPath("/api");
		dynamicContext.setHandler(new MockimailService(mockimailConfig));
		contexts.addHandler(dynamicContext);

		Server server = new Server(port);
		server.setHandler(contexts);
		server.start();
		server.join();
	}

	/**
	 * Used to created handler to deserve static page (html and javascript)
	 * 
	 * @return
	 */
	private static WebAppContext createWebapp() {
		String webAppDir = "src/main/webapp/";
		WebAppContext webApp = new WebAppContext();
		webApp.setContextPath("/");
		webApp.setResourceBase(webAppDir);
		webApp.setParentLoaderPriority(true);
		return webApp;
	}
}
