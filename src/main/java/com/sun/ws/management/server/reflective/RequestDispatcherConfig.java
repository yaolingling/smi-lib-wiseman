/*
 * Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt 
 **
 **$Log: RequestDispatcherConfig.java,v $
 **Revision 1.2  2007/05/31 19:47:45  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: RequestDispatcherConfig.java,v 1.2 2007/05/31 19:47:45 nbeers Exp $
 */
package com.sun.ws.management.server.reflective;

import com.sun.ws.management.server.HandlerContext;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.java.dev.wiseman.server.handler.config._1.ResourceHandlersType;
import net.java.dev.wiseman.server.handler.config._1.ResourceHandlersType.ResourceHandler;

public class RequestDispatcherConfig {
    
    private static final Logger LOG = Logger
            .getLogger(RequestDispatcherConfig.class.getName());
    public static final String RESOURCE_HANDLER_CONFIG_FILE = "/resource-handler-config.xml";
    public static final String J2EE_RESOURCE_HANDLER_CONFIG = "/WEB-INF" + RESOURCE_HANDLER_CONFIG_FILE;
    public static final String J2SE_RESOURCE_HANDLER_CONFIG = RESOURCE_HANDLER_CONFIG_FILE;
    
    private static final String RESOURCE_HANDLER_PACKAGE_NAME = "net.java.dev.wiseman.server.handler.config._1";
    
    private static final ArrayList<HandlerConfig> handlers = new ArrayList<HandlerConfig>();
    
    private static boolean initDone = false;
    
    private static final class HandlerConfig {
        
        private final Pattern resourcePattern;
        
        private final String handler;
        
        HandlerConfig(final Pattern resourcePattern, final String handler) {
            this.resourcePattern = resourcePattern;
            this.handler = handler;
        }
        
        Pattern getResourcePattern() {
            return resourcePattern;
        }
        
        String getHandler() {
            return handler;
        }
    }
    
    RequestDispatcherConfig(HandlerContext context) {
        // Only load the static 'handlers' once
        synchronized (handlers) {
            if (initDone == false) {
                initDone = true;
                try {
                    // Get the ServletContext
                    Object servletCtx = context.
                            getRequestProperties().
                            get(HandlerContext.SERVLET_CONTEXT);
                    InputStream is = null;
                    if(servletCtx != null) {
                        // We are in a J2EE context
                        try {
                            Class srvletContextClass = Class.forName("javax.servlet.ServletContext");
                            Method getResource = srvletContextClass.getMethod("getResourceAsStream", 
                                java.lang.String.class);
                            is = (InputStream) getResource.invoke(servletCtx, J2EE_RESOURCE_HANDLER_CONFIG);
                        }catch(Exception ex) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.log(Level.FINE,
                                    "WARNING: Failed to access to servlet classes");
                            }
                            return;
                        }
                    } else {
                        // We are in a J2SE context
                        // XXX REVISIT, could add flexibility by relying on a classloader
                        is = RequestDispatcherConfig.class.
                                getResourceAsStream(J2SE_RESOURCE_HANDLER_CONFIG);
                    }
                    
                    // Create a JAXBContext
                    JAXBContext jbc = JAXBContext
                            .newInstance(RESOURCE_HANDLER_PACKAGE_NAME);
                    
                    // Create an Unmarshaller
                    Unmarshaller unmarshaller = jbc.createUnmarshaller();
                    
                    // Check if InputStream was successfully opened
                    if (is == null) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.log(Level.FINE,
                                    "WARNING: Failed to load configuration "
                                    + RESOURCE_HANDLER_CONFIG_FILE);
                        }
                        return;
                    }
                    
                    // Read in the config file
                    JAXBElement<?> handlersElem = (JAXBElement<?>) unmarshaller
                            .unmarshal(is);
                    
                    ResourceHandlersType rh = (ResourceHandlersType) handlersElem
                            .getValue();
                    List<ResourceHandler> list = rh.getResourceHandler();
                    
                    // Save the list in our private ArrayList
                    for (int index = 0; index < list.size(); index++) {
                        ResourceHandler handlerEntry = (ResourceHandler) list
                                .get(index);
                        Pattern pattern = Pattern.compile(handlerEntry
                                .getResourcePattern());
                        String className = handlerEntry
                                .getResourceHandlerClass();
                        HandlerConfig handler = new HandlerConfig(pattern,
                                className);
                        handlers.add(handler);
                    }
                } catch (JAXBException je) {
                    je.printStackTrace();
                }
            }
        }
    }
    
    public String getHandlerName(String resource) {
        // Search the list for a match. First match is returned.
        String handlerName = null;
        
        // No synchonization necessary here since we only write the ArrayList
        // once
        for (int index = 0; index < handlers.size(); index++) {
            HandlerConfig handler = (HandlerConfig) handlers.get(index);
            Matcher matcher = handler.getResourcePattern().matcher(resource);
            if (matcher.matches() == true) {
                handlerName = handler.getHandler();
                break;
            }
        }
        return handlerName;
    }
}
