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
 * $Id: EventingModelManager.java,v 1.1 2007/04/06 10:03:11 jfdenise Exp $
 *
 */
package com.sun.ws.management.framework.eventing;

import java.io.IOException;
import java.util.Properties;


public class EventingModelManager {
	static private EventingModelManager singleton;
	private EventingModelManager() {
		super();
	}

	public static EventingModelManager getManager() {
		if(singleton==null)
			singleton=new EventingModelManager();
		return singleton;
	}

	/** 
	 * Called to register all model listeners that plan to emit
	 * events
	 */
	public void init() {
		
		
	}
	
	public static void main(String[] args) {
		EventingModelManager manager = getManager();
		//manager.getClass().get
		Properties p = new Properties();
		try {
			p.load(manager.getClass().getResourceAsStream("/WsManListeners.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		String res=p.getProperty("listener");
		System.out.println(res);
	}
	
	
}
