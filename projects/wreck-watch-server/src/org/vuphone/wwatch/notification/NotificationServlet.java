package org.vuphone.wwatch.notification;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.vuphone.wwatch.inforeq.InfoHandledNotification;
import org.vuphone.wwatch.routing.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/******************************************************************************
 * Copyright (c) 2007 Jules White. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Jules White - initial API and implementation
 ****************************************************************************/
public class NotificationServlet extends HttpServlet {

	private static final Logger logger_ = Logger
	.getLogger(NotificationServlet.class.getName());

	private Map<String, NotificationHandler> handlers_;
	private NotificationParser parser_;



	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {

		try {
			Notification note = parser_.parse(req);

			if (note != null) {
				NotificationHandler handler = handlers_.get(note.getType());
				if (handler != null) {
					Notification rnote = handler.handle(note);
					if (rnote.getType().equalsIgnoreCase("infohandled")){
						
						InfoHandledNotification info = (InfoHandledNotification)rnote;
						//There's probably a better way to do this, Jules,
						//any fancy XML ideas?
						
						//Build the xml response
						Document d = null;
						try {
							d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
						} catch (ParserConfigurationException e) {
							
							logger_
							.log(
									Level.SEVERE,
									"Parser configuration exception creating document for xml response",
									e);
						}
						
						Node root = d.createElement("Points");
						for (Waypoint w:info.getAccidents()){
							Node pointR = d.createElement("Point");
							Node lat = d.createElement("Latitude");
							
							lat.appendChild(d.createTextNode(Double.toString(w.getLatitude())));
							
							pointR.appendChild(lat);
							
							Node lon = d.createElement("Longitude");
							lon.appendChild(d.createTextNode(Double.toString(w.getLongitude())));
							
							pointR.appendChild(lon);
							
							root.appendChild(pointR);
							
						}
						
					}else{
						resp.getWriter().write(note.toString());
					}

					if (rnote != null) {

					}
				} else {
					//This will just be temporary to make it do something
					resp.getWriter().write(note.toString());
				}
			}
			else {
				// to do...
			}
		} catch (NotificationFormatException e) {
			logger_
			.log(
					Level.SEVERE,
					"Error in notification parameters provided with the request",
					e);
		}
	}

	public Map<String, NotificationHandler> getHandlers() {
		return handlers_;
	}

	public void setHandlers(Map<String, NotificationHandler> handlers) {
		handlers_ = handlers;
	}

	public NotificationParser getParser() {
		return parser_;
	}

	public void setParser(NotificationParser parser) {
		parser_ = parser;
	}

}
