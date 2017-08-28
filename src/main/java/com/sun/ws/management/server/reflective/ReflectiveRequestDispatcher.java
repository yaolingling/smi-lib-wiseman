/*
 * Copyright 2005 Sun Microsystems, Inc.
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
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: ReflectiveRequestDispatcher.java,v $
 **Revision 1.2  2007/05/31 19:47:46  nbeers
 **Add HP copyright header
 **
 **
 * $Id: ReflectiveRequestDispatcher.java,v 1.2 2007/05/31 19:47:46 nbeers Exp $
 */

package com.sun.ws.management.server.reflective;

import com.sun.ws.management.AccessDeniedFault;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.addressing.DestinationUnreachableFault;
import com.sun.ws.management.server.Handler;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.RequestDispatcher;
import com.sun.ws.management.server.WSManAgent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;

public final class ReflectiveRequestDispatcher extends RequestDispatcher {
    
    private static final Logger LOG = Logger
            .getLogger(ReflectiveRequestDispatcher.class.getName());
    
    private static final Class<Handler> HANDLER_INTERFACE = Handler.class;
    
    private static final Class[] HANDLER_PARAMS = { String.class, String.class,
    HandlerContext.class, Management.class, Management.class };
    
    private static final String HANDLER_PREFIX = RequestDispatcher.class
            .getPackage().getName()
            + ".handler";
    
    static final class HandlerEntry {
        
        private final Object instance;
        
        private final Method method;
        
        HandlerEntry(final Object instance, final Method method) {
            this.instance = instance;
            this.method = method;
        }
        
        Object getInstance() {
            return instance;
        }
        
        Method getMethod() {
            return method;
        }
    }
    
    private static WSManAgent dispatchingAgent = null;
    
    private static final Map<String, HandlerEntry> cache = new WeakHashMap<String, HandlerEntry>();
    
    private final RequestDispatcherConfig config;
    
    public ReflectiveRequestDispatcher(final Management req,
            final HandlerContext context) throws JAXBException, SOAPException {
        super(req, context);
        config = new RequestDispatcherConfig(context);
    }
    
    public Management call() throws Exception {
        final String resource = request.getResourceURI();
        if (resource == null) {
            throw new DestinationUnreachableFault("Missing the "
                    + Management.RESOURCE_URI.getLocalPart(),
                    DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
        }
        
        HandlerEntry he = getHandlerEntry(resource);
        
        final String action = request.getAction();
        try {
            he.getMethod().invoke(he.getInstance(), action, resource, context,
                    request, response);
        } catch (InvocationTargetException itex) {
            // the cause might be FaultException if a Fault is being indicated
            // by the handler
            final Throwable cause = itex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception(cause);
            }
        }
        return response;
    }
    
    private HandlerEntry getHandlerEntry(String resource) {
        HandlerEntry he = null;
        
        synchronized (cache) {
            he = cache.get(resource);
            
            if (he == null) {
                // Handler not yet cached. Check config.
                String handlerClassName = config.getHandlerName(resource);
                
                if ((handlerClassName == null)
                || (handlerClassName.length() == 0)) {
                    // Handler not configured. Create default classname
                    handlerClassName = createHandlerClassName(resource);
                }
                // Load the handler.
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, resource + " -> " + handlerClassName);
                }
                
                final Class handlerClass;
                try {
                    handlerClass = Class.forName(handlerClassName, true, Thread
                            .currentThread().getContextClassLoader());
                } catch (ClassNotFoundException cnfex) {
                    throw new DestinationUnreachableFault(
                            "Handler not found for resource " + resource,
                            DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
                } catch (Throwable e) {
                    throw new InternalErrorFault(e.getMessage());
                }
                
                // verify that handlerClass implements the Handler interface
                if (!HANDLER_INTERFACE.isAssignableFrom(handlerClass)) {
                    throw new DestinationUnreachableFault(
                            "Handler "
                            + handlerClassName
                            + " does not implement the Handler interface for resource "
                            + resource,
                            DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
                }
                
                final Method method;
                try {
                    method = handlerClass.getMethod("handle", HANDLER_PARAMS);
                } catch (NoSuchMethodException nsmex) {
                    throw new DestinationUnreachableFault(
                            "handle method not found in Handler "
                            + handlerClassName + " for resource "
                            + resource,
                            DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
                }
                
                final Object handler;
                try {
                    handler = handlerClass.newInstance();
                } catch (InstantiationException iex) {
                    throw new DestinationUnreachableFault(
                            "Could not instantiate handler " + handlerClassName
                            + " for resource " + resource,
                            DestinationUnreachableFault.Detail.INVALID_RESOURCE_URI);
                } catch (IllegalAccessException iaex) {
                    throw new AccessDeniedFault();
                }
                
                he = new HandlerEntry(handler, method);
                
                cache.put(resource, he);
            }
        }
        return he;
    }
    
    private String createHandlerClassName(final String resource) {
        Class<?> converter = null;
        try {
            converter =
                    Class.forName("com.sun.xml.internal.bind.api.impl." +
                    "NameConverter");
        }catch(ClassNotFoundException cnfe) {
            // XXX OK
        }
        if(converter == null) {
            try {
                converter =
                        Class.forName("com.sun.xml.bind.api.impl." +
                        "NameConverter");
            }catch(ClassNotFoundException cnfe) {
                throw new IllegalStateException(cnfe);
            }
        }
        
        final String pkg;
        try {
            Field f = converter.getField("standard");
            Object obj = f.get(null);
            Method m = converter.getMethod("toPackageName", String.class);
            pkg = (String) m.invoke(obj, resource);
        }catch(Exception ex) {
            throw new IllegalStateException(ex);
        }
        // final String pkg = //com.sun.xml.internal.bind.api.impl.
        //      com.sun.xml.internal.bind.api.impl.*;NameConverter.standard.toPackageName(resource);
        
        final StringBuilder sb = new StringBuilder();
        if (HANDLER_PREFIX != null) {
            sb.append(HANDLER_PREFIX);
            sb.append(".");
        }
        sb.append(pkg);
        sb.append("_Handler");
        
        return sb.toString();
    }
}
