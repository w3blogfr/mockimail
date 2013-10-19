/*
 * Dumbster - a dummy SMTP server
 * Copyright 2004 Jason Paul Kitchen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Updated and adapted for mockimail
 * https://github.com/w3blogfr/mockimail
 */
package com.dumbster.smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.w3blog.mockimail.config.MockimailConfig;

/**
 * Dummy SMTP server for testing purposes.
 * 
 * @todo constructor allowing user to pass preinitialized ServerSocket
 */
public class SimpleSmtpServer implements Runnable {

	private static final Logger logger = LoggerFactory
			.getLogger(SimpleSmtpServer.class);

	/**
	 * Stores all of the email received since this instance started up.
	 */
	private List<SmtpMessage> receivedMail;

	private MockimailConfig mockimailConfig;

	private Client clientES;

	/**
	 * Default SMTP port is 25.
	 */
	public static final int DEFAULT_SMTP_PORT = 25;

	/**
	 * Indicates whether this server is stopped or not.
	 */
	private volatile boolean stopped = true;

	/**
	 * Handle to the server socket this server listens to.
	 */
	private ServerSocket serverSocket;

	/**
	 * Port the server listens on - set to the default SMTP port initially.
	 */
	private int port = DEFAULT_SMTP_PORT;

	/**
	 * Timeout listening on server socket.
	 */
	private static final int TIMEOUT = 500;

	private ObjectMapper mapper;

	/**
	 * Constructor.
	 * 
	 * @param port
	 *            port number
	 */
	public SimpleSmtpServer(int port) {
		receivedMail = new ArrayList<SmtpMessage>();
		this.port = port;
		mapper = new ObjectMapper();
	}

	/**
	 * Main loop of the SMTP server.
	 */
	public void run() {
		stopped = false;
		try {
			try {
				serverSocket = new ServerSocket(port);
				serverSocket.setSoTimeout(TIMEOUT); // Block for maximum of 1.5
													// seconds
			} finally {
				synchronized (this) {
					// Notify when server socket has been created
					notifyAll();
				}
			}

			// Server: loop until stopped
			while (!isStopped()) {
				// Start server socket and listen for client connections
				Socket socket = null;
				try {
					socket = serverSocket.accept();
				} catch (Exception e) {
					if (socket != null) {
						socket.close();
					}
					continue; // Non-blocking socket timeout occurred: try
								// accept() again
				}

				// Get the input and output streams
				BufferedReader input = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream());

				synchronized (this) {
					/*
					 * We synchronize over the handle method and the list update
					 * because the client call completes inside the handle
					 * method and we have to prevent the client from reading the
					 * list until we've updated it. For higher concurrency, we
					 * could just change handle to return void and update the
					 * list inside the method to limit the duration that we hold
					 * the lock.
					 */
					List<SmtpMessage> msgs = handleTransaction(out, input);
					for (SmtpMessage smtpMessage : msgs) {
						IndexRequest indexRequest = Requests.indexRequest(
								mockimailConfig.getIndexES()).type(
								mockimailConfig.getTypeES());
						indexRequest.source(mapper
								.writeValueAsString(smtpMessage));
						clientES.index(indexRequest).actionGet();

					}
					receivedMail.addAll(msgs);
				}
				socket.close();
			}
		} catch (Exception e) {
			logger.error("Smtp Server could not be started", e);
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Check if the server has been placed in a stopped state. Allows another
	 * thread to stop the server safely.
	 * 
	 * @return true if the server has been sent a stop signal, false otherwise
	 */
	public synchronized boolean isStopped() {
		return stopped;
	}

	/**
	 * Stops the server. Server is shutdown after processing of the current
	 * request is complete.
	 */
	public synchronized void stop() {
		// Mark us closed
		stopped = true;
		try {
			// Kick the server accept loop
			serverSocket.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * Handle an SMTP transaction, i.e. all activity between initial connect and
	 * QUIT command.
	 * 
	 * @param out
	 *            output stream
	 * @param input
	 *            input stream
	 * @return List of SmtpMessage
	 * @throws IOException
	 */
	private List<SmtpMessage> handleTransaction(PrintWriter out,
			BufferedReader input) throws IOException {
		// Initialize the state machine
		SmtpState smtpState = SmtpState.CONNECT;
		SmtpRequest smtpRequest = new SmtpRequest(SmtpActionType.CONNECT, "",
				smtpState);

		// Execute the connection request
		SmtpResponse smtpResponse = smtpRequest.execute();

		// Send initial response
		sendResponse(out, smtpResponse);
		smtpState = smtpResponse.getNextState();

		List<SmtpMessage> msgList = new ArrayList<SmtpMessage>();
		SmtpMessage msg = new SmtpMessage();

		while (smtpState != SmtpState.CONNECT) {
			String line = input.readLine();

			if (line == null) {
				break;
			}

			// Create request from client input and current state
			SmtpRequest request = SmtpRequest.createRequest(line, smtpState);
			// Execute request and create response object
			SmtpResponse response = request.execute();
			// Move to next internal state
			smtpState = response.getNextState();
			// Send reponse to client
			sendResponse(out, response);

			// Store input in message
			String params = request.getParams();
			store(msg, response, params);

			// If message reception is complete save it
			if (smtpState == SmtpState.QUIT) {
				msgList.add(msg);
				msg = new SmtpMessage();
			}
		}

		return msgList;
	}

	/**
	 * Update the headers or body depending on the SmtpResponse object and line
	 * of input.
	 * 
	 * @param response
	 *            SmtpResponse object
	 * @param params
	 *            remainder of input line after SMTP command has been removed
	 */
	private void store(SmtpMessage smtpMessage, SmtpResponse response,
			String params) {
		if (params != null) {
			if (SmtpState.DATA_HDR.equals(response.getNextState())) {
				int headerNameEnd = params.indexOf(':');
				if (headerNameEnd >= 0) {
					String name = params.substring(0, headerNameEnd).trim();
					String value = params.substring(headerNameEnd + 1).trim();
					if ("Date".equals(name)) {
						try {
							SimpleDateFormat sdf = new SimpleDateFormat(
									"EEE, dd MMM yyyy HH:mm:ss Z",
									Locale.ENGLISH);
							smtpMessage.setDate(sdf.parse(value));
						} catch (ParseException e) {
							logger.warn("Format Date incorrect");
							smtpMessage.addHeader(name, value);
						}
					} else if ("Subject".equals(name)) {
						// Subject
						smtpMessage.setSubject(value);
					} else if ("To".equals(name)) {
						// Destinataire
						smtpMessage.getTo().add(value);
					} else if ("From".equals(name)) {
						// Expéditeur
						smtpMessage.setFrom(value);
					} else if ("Cc".equals(name)) {
						// Destinataire en copie
						smtpMessage.getCc().add(value);
					} else if ("Bcc".equals(name)) {
						// Destinataire
						smtpMessage.getBcc().add(value);
					} else {
						// Other headers
						smtpMessage.addHeader(name, value);
					}
				}
			} else if (SmtpState.DATA_BODY == response.getNextState()) {
				smtpMessage.appendBody(params);
			}
		}
	}

	/**
	 * Send response to client.
	 * 
	 * @param out
	 *            socket output stream
	 * @param smtpResponse
	 *            response object
	 */
	private static void sendResponse(PrintWriter out, SmtpResponse smtpResponse) {
		if (smtpResponse.getCode() > 0) {
			int code = smtpResponse.getCode();
			String message = smtpResponse.getMessage();
			out.print(code + " " + message + "\r\n");
			out.flush();
		}
	}

	/**
	 * Get email received by this instance since start up.
	 * 
	 * @return List of String
	 */
	public synchronized Iterator<SmtpMessage> getReceivedEmail() {
		return receivedMail.iterator();
	}

	/**
	 * Get the number of messages received.
	 * 
	 * @return size of received email list
	 */
	public synchronized int getReceivedEmailSize() {
		return receivedMail.size();
	}

	public Client getClientES() {
		return clientES;
	}

	public void setClientES(Client clientES) {
		this.clientES = clientES;
	}

	public MockimailConfig getMockimailConfig() {
		return mockimailConfig;
	}

	public void setMockimailConfig(MockimailConfig mockimailConfig) {
		this.mockimailConfig = mockimailConfig;
	}

	/**
	 * Creates an instance of SimpleSmtpServer and starts it. Will listen on the
	 * default port.
	 * 
	 * @return a reference to the SMTP server
	 */
	public static SimpleSmtpServer start() {
		return start(DEFAULT_SMTP_PORT);
	}

	/**
	 * Creates an instance of SimpleSmtpServer and starts it.
	 * 
	 * @param port
	 *            port number the server should listen to
	 * @return a reference to the SMTP server
	 */
	public static SimpleSmtpServer start(int port) {
		logger.info("SMTP Server starting on " + port);
		SimpleSmtpServer server = new SimpleSmtpServer(port);
		Thread t = new Thread(server);

		// Block until the server socket is created
		synchronized (server) {
			try {
				t.start();
				server.wait();
			} catch (InterruptedException e) {
				// Ignore don't care.
			}
		}
		return server;
	}

}
