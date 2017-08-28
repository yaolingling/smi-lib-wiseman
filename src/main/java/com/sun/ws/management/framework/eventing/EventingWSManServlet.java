/*
 * Copyright 2006 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id: EventingWSManServlet.java,v 1.1 2007/04/06 10:03:11 jfdenise Exp $
 *
 */
package com.sun.ws.management.framework.eventing;

import javax.servlet.ServletException;

import com.sun.ws.management.server.reflective.WSManReflectiveServlet;

public class EventingWSManServlet extends WSManReflectiveServlet {

	public EventingWSManServlet() {
		super();
	}

	@Override
	public void init() throws ServletException {
		super.init();
		EventingModelManager.getManager().init();
	}

}
