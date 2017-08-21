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
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: DelegatingHandler.java,v $
 **Revision 1.2  2007/05/31 19:47:47  nbeers
 **Add HP copyright header
 **
 **
 * $Id: DelegatingHandler.java,v 1.2 2007/05/31 19:47:47 nbeers Exp $
 *
 */
package com.sun.ws.management.framework.handlers;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.ws.management.Management;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.framework.enumeration.Enumeratable;
import com.sun.ws.management.framework.transfer.Transferable;
import com.sun.ws.management.server.HandlerContext;

/**
 * The Deligating Handler attemps for forward your action request
 * to a deligate class before claiming that it does not support
 * your action.
 * 
 * @author wire
 *
 */
public class DelegatingHandler extends DefaultHandler {
    private static Logger LOG = Logger.getLogger(DelegatingHandler.class.getName());
    protected Object delegate;
    public DelegatingHandler(Object delegate) {
        super();
        setDelegate(delegate);
    }


    @Override
    public void create(HandlerContext context, Management request, Management response) {
        if(!(delegate instanceof Transferable)){
            LOG.log(Level.SEVERE,"A call to create on the class "+delegate.getClass().getName()+" failed because it did not implement the Transferable interface.");
            super.create(context, request, response);
        }
        Transferable transferableDeligate = (Transferable)delegate;
        transferableDeligate.create(context,request,response);

    }


    @Override
    public void delete(HandlerContext context, Management request, Management response) {
        if(!(delegate instanceof Transferable)){
            LOG.log(Level.SEVERE,"A call to delete on the class "+delegate.getClass().getName()+" failed because it did not implement the Transferable interface.");
            super.delete(context, request, response);
        }
        Transferable transferableDeligate = (Transferable)delegate;
        transferableDeligate.delete(context,request,response);
    }
    @Override
    public void get(HandlerContext context, Management request, Management response) {
        if(!(delegate instanceof Transferable)){
            LOG.log(Level.SEVERE,"A call to get on the class "+delegate.getClass().getName()+" failed because it did not implement the Transferable interface.");
            super.get(context, request, response);
        }
        Transferable transferableDeligate = (Transferable)delegate;
        transferableDeligate.get(context,request,response);
    }

    @Override
    public void put(HandlerContext context, Management request, Management response) {
        if(!(delegate instanceof Transferable)){
            LOG.log(Level.SEVERE,"A call to put on the class "+delegate.getClass().getName()+" failed because it did not implement the Transferable interface.");
            super.put(context, request, response);
        }
        Transferable transferableDeligate = (Transferable)delegate;
        transferableDeligate.put(context,request, response);
    }

    @Override
    public void release(HandlerContext context, Enumeration enuRequest, Enumeration enuResponse) {
        if(!(delegate instanceof Enumeratable)){
            LOG.log(Level.SEVERE,"A call to put on the class "+delegate.getClass().getName()+" failed because it did not implement the Enumeratable interface.");
            super.release(context, enuRequest, enuResponse);
        }
        Enumeratable enumeratableDeligate = (Enumeratable)delegate;
        enumeratableDeligate.release(context,enuRequest, enuResponse);
    }

    @Override
    public void pull(HandlerContext context, Enumeration enuRequest, Enumeration enuResponse) {
        if(!(delegate instanceof Enumeratable)){
            LOG.log(Level.SEVERE,"A call to put on the class "+delegate.getClass().getName()+" failed because it did not implement the Enumeratable interface.");
            super.release(context, enuRequest, enuResponse);
        }
        Enumeratable enumeratableDeligate = (Enumeratable)delegate;
        enumeratableDeligate.pull(context,enuRequest, enuResponse);

    }

    @Override
    public void enumerate(HandlerContext context, Enumeration enuRequest, Enumeration enuResponse) {
        if(!(delegate instanceof Enumeratable)){
            LOG.log(Level.SEVERE,"A call to put on the class "+delegate.getClass().getName()+" failed because it did not implement the Enumeratable interface.");
            super.release(context, enuRequest, enuResponse);
        }
        Enumeratable enumeratableDeligate = (Enumeratable)delegate;
        enumeratableDeligate.enumerate(context,enuRequest, enuResponse);
    }

    @Override
    public void getStatus(HandlerContext context, Enumeration enuRequest, Enumeration enuResponse) {
         if(!(delegate instanceof Enumeratable)){
            LOG.log(Level.SEVERE,"A call to put on the class "+delegate.getClass().getName()+" failed because it did not implement the Enumeratable interface.");
            super.release(context, enuRequest, enuResponse);
        }
        Enumeratable enumeratableDeligate = (Enumeratable)delegate;
        enumeratableDeligate.getStatus(context,enuRequest, enuResponse);
    }

    @Override
    public void renew(HandlerContext context, Enumeration enuRequest, Enumeration enuResponse) {
        if(!(delegate instanceof Enumeratable)){
            LOG.log(Level.SEVERE,"A call to put on the class "+delegate.getClass().getName()+" failed because it did not implement the Enumeratable interface.");
            super.release(context, enuRequest, enuResponse);
        }
        Enumeratable enumeratableDeligate = (Enumeratable)delegate;
        enumeratableDeligate.renew(context,enuRequest, enuResponse);
    }

    /**
     * Attempts to call a custom action based on introspection of the deligate.
     * Assumes the last part of the action URI maps to the method name on the delegate
     * class in lower case.
     */
    @Override
    public boolean customDispatch(String action, HandlerContext context, Management request, Management response) throws Exception {
        String[] actionParts = action.split("/");
        if(actionParts.length==0)
            return false;

        String methodName=actionParts[actionParts.length-1].toLowerCase();
        Method method=null;
        try {
            method=delegate.getClass().getMethod(methodName,HandlerContext.class,Management.class,Management.class);
            method.invoke(delegate,context,request,response);
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE,"A call to a custom method \""+methodName+"\" on the class "+delegate.getClass().getName()+" failed because of this error:"+ e.getMessage());
            return false;
        }

    }

    public void setDelegate(Object delegate){
        this.delegate=delegate;
    }

}
