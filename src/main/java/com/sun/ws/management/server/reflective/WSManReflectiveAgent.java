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
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com),
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt
 **
 **$Log: WSManReflectiveAgent.java,v $
 **Revision 1.3  2007/05/30 20:30:32  nbeers
 **Add HP copyright header
 **
 **
 * $Id: WSManReflectiveAgent.java,v 1.3 2007/05/30 20:30:32 nbeers Exp $
 */

package com.sun.ws.management.server.reflective;

import com.sun.ws.management.Management;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.metadata.annotations.AnnotationProcessor;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.RequestDispatcher;
import com.sun.ws.management.server.WSManAgent;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.transform.Source;
import org.xml.sax.SAXException;

/**
 * Agent that delegates to a Reflective Request Dispatcher
 */
public class WSManReflectiveAgent extends WSManAgent {
    private static final String METADATA_PROPERTY_FILE_NAME = "/metadata.properties";
    private Map<String, String> metadataProperties;

    public WSManReflectiveAgent() throws SAXException {
        this(null, null, null);
    }

    protected WSManReflectiveAgent(Map<String,String> wisemanConf, Source[] customSchemas,
            Map<String,String> bindingConf) throws SAXException {
        super(wisemanConf, customSchemas, bindingConf);
         // Handle metadata property file
        // XXX REVISIT, YOU CAN ADD A CONSTRUCTOR TO ALLOW INSTANCE CONFIGURATION
        final Map<String, String> propertySet = new HashMap<String, String>();
        getProperties(METADATA_PROPERTY_FILE_NAME, propertySet);
        metadataProperties = Collections.unmodifiableMap(propertySet);
    }

    @Override
    protected RequestDispatcher createDispatcher(Management request, HandlerContext context)
            throws SOAPException, JAXBException, IOException {
    	request = processForMissingTrailingSlash(request);
        return new ReflectiveRequestDispatcher(request, context);
    }


    private Management processForMissingTrailingSlash(Management request) {
    	String address = null;
    	try {
			if((request!=null)&&
				((address = request.getTo())!=null)&&
				(address.trim().length()>0)){
				//does not have the trailing slash
			   if(address.lastIndexOf("/")!= address.length()-1){
				 request.setTo(address+"/");
			   }
			}
		}
    	/* Silently fail as this is only a nicety for the developers/clients if they forget to
    	 * add the trailing slash to ensure servlet engine processing.  Not sure what the right
    	 * WsManagement response should be here as it's really just a Wiseman-using servlet problem.
    	 */
    	catch (JAXBException e) {
		} catch (SOAPException e) {
		}
		return request;
	}

	public static Map<QName, String> getMetadataConfiguration(Map<String, String> propertySet) {
        if(propertySet == null) {
            propertySet = new HashMap<String, String>();
            getProperties(METADATA_PROPERTY_FILE_NAME, propertySet);
        }
         //Add additional elements to the IdentifyResponse object.
        Map<QName,String> extraIdInfo = new HashMap<QName, String>();

        //If MetaData resource is exposed, then populate that information here.
        String value = null;
        //Determine whether to turn on/off metaData exposure
        String mDataEnabled="metadata.index.enabled";
        value = propertySet.get(mDataEnabled);
        boolean exposeMetaDataContent = false;
        exposeMetaDataContent = Boolean.parseBoolean(value);
        if((value!=null)&&(value.trim().length()>0)){
            extraIdInfo.put(
                    AnnotationProcessor.META_DATA_ENABLED,
                    value);
        }

        //If enabled then populate then expose default meta data resource
        if(exposeMetaDataContent){
            //MetaData Addressing TO field
            String mDataTo="metadata.index.to";
            value = propertySet.get(mDataTo);
            if((value!=null)&&(value.trim().length()>0)){
                extraIdInfo.put(
                        AnnotationProcessor.META_DATA_TO,
                        value);
            }
            //MetaData WsManagement ResourceURI field
            String mDataResourceURI="metadata.index.resourceURI";
            value = propertySet.get(mDataResourceURI);
            if((value!=null)&&(value.trim().length()>0)){
                extraIdInfo.put(
                        AnnotationProcessor.META_DATA_RESOURCE_URI,
                        value);
            }
            //MetaData descriptive information
            String mDataDesc="metadata.index.description";
            value = propertySet.get(mDataDesc);
            if((value!=null)&&(value.trim().length()>0)){
                extraIdInfo.put(
                        AnnotationProcessor.META_DATA_DESCRIPTION,
                        value);
            }
        }
        return extraIdInfo;
    }

    /* (non-Javadoc)
         * @see com.sun.ws.management.server.RequestDispatcher#getAdditionalIdentifyElements()
         */
    @Override
    protected Map<QName, String> getAdditionalIdentifyElements() {
       return getMetadataConfiguration(metadataProperties);
    }
}

