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
 */
package fr.w3blog.mockimail.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for a complete SMTP message - headers and message body.
 */
public class SmtpMessage {

	private Date date = new Date();

	private String subject;

	private String from;

	/*
	 * Destinataire principal
	 */
	private List<String> to = new ArrayList<String>();

	/**
	 * Destinataire en copie
	 */
	private List<String> cc = new ArrayList<String>();

	/**
	 * Destinataires cachés
	 */
	private List<String> bcc = new ArrayList<String>();

	private String contentType;

	/** Headers: Map of List of String hashed on header name. */
	private Map<String, List<String>> headers;

	/** Message body. */
	private StringBuffer body;

	private List<PartMessage> parts = new ArrayList<PartMessage>();

	/**
	 * Constructor. Initializes headers Map and body buffer.
	 */
	public SmtpMessage() {
		headers = new HashMap<String, List<String>>(10);
		body = new StringBuffer();
	}

	/**
	 * Get an Iterator over the header names.
	 * 
	 * @return an Iterator over the set of header names (String)
	 */
	public Iterator getHeaderNames() {
		Set nameSet = headers.keySet();
		return nameSet.iterator();
	}

	/**
	 * Get the value(s) associated with the given header name.
	 * 
	 * @param name
	 *            header name
	 * @return value(s) associated with the header name
	 */
	public String[] getHeaderValues(String name) {
		List values = (List) headers.get(name);
		if (values == null) {
			return new String[0];
		} else {
			return (String[]) values.toArray(new String[0]);
		}
	}

	/**
	 * Get the first values associated with a given header name.
	 * 
	 * @param name
	 *            header name
	 * @return first value associated with the header name
	 */
	public String getHeaderValue(String name) {
		List values = (List) headers.get(name);
		if (values == null) {
			return null;
		} else {
			Iterator iterator = values.iterator();
			return (String) iterator.next();
		}
	}

	/**
	 * Get the message body.
	 * 
	 * @return message body
	 */
	public String getBody() {
		return body.toString();
	}

	/**
	 * Adds a header to the Map.
	 * 
	 * @param name
	 *            header name
	 * @param value
	 *            header value
	 */
	public void addHeader(String name, String value) {
		List<String> valueList = headers.get(name);
		if (valueList == null) {
			// La liste n'existe pas encore
			valueList = new ArrayList<String>();
		}
		// On ajoute la valeur
		valueList.add(value);
		headers.put(name, valueList);
	}

	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}

	public void setBody(StringBuffer body) {
		this.body = body;
	}

	public void appendBody(String bodyPart) {
		this.body.append(bodyPart);
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public List<String> getTo() {
		return to;
	}

	public void setTo(List<String> to) {
		this.to = to;
	}

	public List<String> getCc() {
		return cc;
	}

	public void setCc(List<String> cc) {
		this.cc = cc;
	}

	public List<String> getBcc() {
		return bcc;
	}

	public void setBcc(List<String> bcc) {
		this.bcc = bcc;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public List<PartMessage> getParts() {
		return parts;
	}

	public void setParts(List<PartMessage> parts) {
		this.parts = parts;
	}

}
