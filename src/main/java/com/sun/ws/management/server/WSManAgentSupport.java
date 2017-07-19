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
 **$Log: not supported by cvs2svn $
 **Revision 1.2  2007/10/31 11:59:35  jfdenise
 **Faulty maxEnvelopSize computation linked to recent putback
 **
 **Revision 1.1  2007/10/30 09:28:22  jfdenise
 **WiseMan to take benefit of Sun JAX-WS RI Message API and WS-A offered support.
 **Commit a new JAX-WS Endpoint and a set of Message abstractions to implement WS-Management Request and Response processing on the server side.
 **
 **Revision 1.17  2007/06/15 12:13:20  jfdenise
 **Cosmetic change. Make OPERATION_TIMEOUT_DEFAULT public and added a trace.
 **
 **Revision 1.16  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: WSManAgentSupport.java,v 1.3 2007-11-30 14:32:38 denis_rachal Exp $
 */

package com.sun.ws.management.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.xml.sax.SAXException;

import com.sun.ws.management.AccessDeniedFault;
import com.sun.ws.management.EncodingLimitFault;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.TimedOutFault;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.server.message.WSManagementRequest;
import com.sun.ws.management.server.message.WSManagementResponse;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.xml.XmlBinding;
/**
 * WS-MAN agent decoupled from transport. Can be used in Servlet / JAX-WS / ...
 * context.
 *
 */

public abstract class WSManAgentSupport {
    
    private static final Logger LOG = Logger.getLogger(WSManAgentSupport.class.getName());
    protected static final long DEFAULT_TIMEOUT = 30000;
    protected static final long MIN_ENVELOPE_SIZE = 8192;
    
    public static final String OPERATION_TIMEOUT =
        WSManAgent.class.getPackage().getName() + ".operation.timeout";
    private static final String WSMAN_PROPERTY_FILE_NAME = "/wsman.properties";
    public static final String WSMAN_EXTENSIONS_PROPERTY_FILE_NAME = "/wsman-exts.properties";
    public static final String WISEMAN_PROPERTY_FILE_NAME = "/wiseman.properties";
    public static final String OPERATION_TIMEOUT_DEFAULT = "OperationTimeoutDefault";
    private static final String SCHEMA_PATH =
            "/com/sun/ws/management/resources/schemas/";
    
    private static ExecutorService pool;
    
    private static Map<String, String> properties = null;
    
    private Map<String, String> localproperties = null;
    
    protected final long defaultOperationTimeout;
    
    // XXX REVISIT, SHOULD BE STATIC BUT CURRENTLY CAN'T Due to openness of JAXBContext
    private XmlBinding binding;
    private Schema schema;
    private Map<String,String> bindingConf;
    
    static {
        // load subsystem properties and save them in a type-safe, unmodifiable Map
        final Map<String, String> propertySet = new HashMap<String, String>();
        getProperties(WSMAN_PROPERTY_FILE_NAME, propertySet);
        properties = Collections.unmodifiableMap(propertySet);
    }
    
    public static void getProperties(final String filename, final Map<String, String> propertySet) {
        if(LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, "Getting properties [" + filename
                    + "]");
        final InputStream ism = WSManAgent.class.getResourceAsStream(filename);
        if (ism != null) {
            final Properties props = new Properties();
            try {
                props.load(ism);
            } catch (IOException iex) {
                LOG.log(Level.WARNING, "Error reading properties from " +
                        filename, iex);
                throw new RuntimeException("Error reading properties from " +
                        filename +  " " + iex);
            }
            final Iterator<Entry<Object, Object>> ei = props.entrySet().iterator();
            while (ei.hasNext()) {
                final Entry<Object, Object> entry = ei.next();
                final Object key = entry.getKey();
                final Object value = entry.getValue();
                if (key instanceof String && value instanceof String) {
                    if(LOG.isLoggable(Level.FINE))
                        LOG.log(Level.FINE, "Found property " + key + "=" + value);
                    propertySet.put((String) key, (String) value);
                }
            }
        }
    }
    
    public static Schema createSchema(Source[] customSchemas) throws SAXException {
        final SchemaFactory schemaFactory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source[] stdSchemas = createStdSources();
        Source[] finalSchemas = stdSchemas;
        if(customSchemas != null) {
            if(LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "Custom schemas to load " +
                        customSchemas.length);
            finalSchemas = new Source[customSchemas.length +
                    stdSchemas.length];
            System.arraycopy(stdSchemas,0,finalSchemas,0,stdSchemas.length);
            System.arraycopy(customSchemas,0,finalSchemas,stdSchemas.length,
                    customSchemas.length);
        }
        Schema schema = null;
        try {
            schema = schemaFactory.newSchema(finalSchemas);
        } catch (SAXException ex) {
            LOG.log(Level.SEVERE, "Error setting schemas", ex);
            throw ex;
        }
        return schema;
    }
    
    public static Source[] newSources(String schemaNames, String schemaPath) {
        
        if(schemaNames == null) return null;
        
        Source[] stdSchemas = null;
        StringTokenizer t = new StringTokenizer(schemaNames, ",");
        if(LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, t.countTokens() + " schemas to load.");
        stdSchemas = new Source[t.countTokens()];
        int i = 0;
        while(t.hasMoreTokens()) {
            String name = t.nextToken();
            if(LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "Schema to load from " + schemaPath + name);
            final InputStream xsd =
                    Management.class.getResourceAsStream(schemaPath + name);
            
            if(LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "Loaded schema " + xsd);
            stdSchemas[i] = new StreamSource(xsd);
            i++;
        }
        return stdSchemas;
    }
    
    private static Source[] createStdSources() {
        // The returned list of schemas is already sorted
        String schemaNames = properties.get("schemas");
        
        //check if extensison being added then add to the schemas list.
        final Map<String, String> propertySet = new HashMap<String, String>();
        getProperties(WSMAN_EXTENSIONS_PROPERTY_FILE_NAME, propertySet);
        if(!propertySet.isEmpty()&&
                (propertySet.containsKey("extensions.schemas"))){
            String schemas = propertySet.get("extensions.schemas");
            StringTokenizer t = new StringTokenizer(schemas, ",");
            while(t.hasMoreTokens()){
                schemaNames+=","+t.nextToken();
            }
        }
        //business as usual
        return newSources(schemaNames, SCHEMA_PATH);
    }
    
    protected WSManAgentSupport() throws SAXException {
        this(null, null, null);
    }
    
    protected WSManAgentSupport(Map<String,String> wisemanConf, Source[] customSchemas,
            Map<String,String> bindingConf)
            throws SAXException {
        final Map<String, String> propertySet = new HashMap<String, String>();
        getProperties(WISEMAN_PROPERTY_FILE_NAME, propertySet);
        // Put all passed properties
        if(wisemanConf != null)
            propertySet.putAll(wisemanConf);
        localproperties = Collections.unmodifiableMap(propertySet);
        
        String opTimeout = System.getProperty(OPERATION_TIMEOUT);
        if ((opTimeout == null) || (opTimeout.length() != 0))
            opTimeout = localproperties.get(OPERATION_TIMEOUT_DEFAULT);
        
        if ((opTimeout == null) || (opTimeout.length() != 0))
            this.defaultOperationTimeout = DEFAULT_TIMEOUT;
        else {
        	long defTimeout = Long.parseLong(opTimeout);
        	if (defTimeout < 0)
        		defTimeout = Long.MAX_VALUE;
            this.defaultOperationTimeout = defTimeout;
        }
        
        schema = createSchema(customSchemas);
        this.bindingConf = bindingConf;
    }
    
    /**
     * Allow subclass to provide a ThreadPool according to their own
     * caching/threadingstrategy
     */
    protected synchronized ExecutorService getExecutorService() {
        if(pool == null)
            pool = Executors.newCachedThreadPool();
        return pool;
    }
    
    /** This method is called when processing Identify requests.
     * Modify this method to adjust the Identify
     *  processing functionality, but you may be able to simply override
     *  the getAdditionalIdentifyElements() method to add your own custom
     *  elements.
     *
     * @return Identify  This is the Identify instance to be returned to identify Request
     * @throws SecurityException
     * @throws JAXBException
     * @throws SOAPException
     */
    protected boolean processForIdentify(WSManagementRequest request, WSManagementResponse response)
    throws Exception {
        
        final boolean isIdentify = request.isIdentify();
        if(!isIdentify)
            return false;
        
        if(LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, "Serving Identity");
        
        Map<QName, String> additionals = getAdditionalIdentifyElements();
        if(LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, "Additionals QNames " + additionals);
        
        if(additionals == null)
            additionals = new HashMap<QName, String>();
        
        additionals.put(Identify.BUILD_ID, getProperties().get("build.version"));
        additionals.put(Identify.SPEC_VERSION, getProperties().get("spec.version"));
        
        response.setIdentifyResponse(
                getProperties().get("impl.vendor") + " - " +
                getProperties().get("impl.url"),
                getProperties().get("impl.version"),
                Management.NS_URI,
                additionals);
        
        return true;
    }
    
    abstract protected WSManRequestDispatcher createDispatcher(WSManagementRequest request,
            WSManagementResponse response,
            HandlerContext context) throws Exception;
    
    /** Override this method to define additional Identify elements
     *  to be returned.  This method is usually called in processForIdentify()
     *  method to add additional nodes.
     *
     * @return Map containing information to simple xml nodes.
     */
    protected Map<QName, String> getAdditionalIdentifyElements(){
        return null;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    protected static void isValidEnvelopSize(long maxEnvelopeSize, WSManagementResponse response)
    throws Exception {
        if (maxEnvelopeSize < MIN_ENVELOPE_SIZE) {
            EncodingLimitFault fault =
                    new EncodingLimitFault("MaxEnvelopeSize is set too " +
                    "small to encode faults " +
                    "(needs to be atleast " + MIN_ENVELOPE_SIZE + ")",
                    EncodingLimitFault.Detail.MAX_ENVELOPE_SIZE);
            response.setFault(fault);
        }
    }
    
    protected static long getEnvelopeSize(WSManagementRequest request) throws JAXBException, SOAPException {
        long maxEnvelopeSize = Long.MAX_VALUE;
        final MaxEnvelopeSizeType maxSize = request.getMaxEnvelopeSize();
        if (maxSize != null) {
            maxEnvelopeSize = maxSize.getValue().longValue();
        }
        return maxEnvelopeSize;
    }
    
    private long getTimeout(WSManagementRequest request) throws Exception {
        long timeout = defaultOperationTimeout;
        final Duration timeoutDuration = request.getTimeout();
        if (timeoutDuration != null) {
            timeout = timeoutDuration.getTimeInMillis(new GregorianCalendar());
        }
        return timeout;
    }
    
    public WSManagementResponse handleRequest(final WSManagementRequest request,
    		                                  final WSManagementResponse response,
                                              final HandlerContext context) {
        
        try {
            isValidEnvelopSize(getEnvelopeSize(request), response);
            
            if(!response.containsFault()) {
                final WSManRequestDispatcher dispatcher = createDispatcher(request, response,
                        context);
                
                dispatcher.authenticate();
                //Test for identify responses
                boolean identifyResponse = processForIdentify(request, response);
                if(identifyResponse)
                    return response;
                
                dispatcher.validateRequest();
                return dispatch(dispatcher);
                
                // XXX REVISIT
                // We have no more way to compute response size. JAX-WS Headers are no more at this level
            }
        } catch (Throwable ex) {
            try {
                if(ex instanceof SecurityException)
                    response.setFault(new AccessDeniedFault());
                else
                    if(ex instanceof FaultException)
                        response.setFault((FaultException)ex);
                    else
                        response.setFault(new InternalErrorFault(ex));
            }catch(Exception ex2) {
                throw new RuntimeException(ex2.toString());
            }
        }
        
        return response;
    }
    
    private WSManagementResponse dispatch(final WSManRequestDispatcher dispatcher)
            throws Throwable {

		final WSManagementRequest request = dispatcher.getRequest();
        final long timeout = getTimeout(request);
        
		FutureTask<WSManagementResponse> task = null;

		if (LOG.isLoggable(Level.FINE))
			LOG.log(Level.FINE, "Dispatching operation with OperationTimeout=" + timeout);
		try {
			final GregorianCalendar start = new GregorianCalendar();
			final long end = start.getTimeInMillis() + timeout;

			task = new FutureTask<WSManagementResponse>(dispatcher);
			// The Future returned by pool.submit does not propagate
			// ExecutionException, perform the get on FutureTask itself
			getExecutorService().submit(task);
			long timeLeft = end - new GregorianCalendar().getTimeInMillis();
			while (timeLeft > 0) {
				try {
					return task.get(timeLeft, TimeUnit.MILLISECONDS);
				} catch (ExecutionException ex) {
					throw ex.getCause();
				} catch (InterruptedException ix) {
					timeLeft = end - new GregorianCalendar().getTimeInMillis();
					continue;
				} catch (TimeoutException tx) {
					try {
						request.cancel();
					} catch (IllegalStateException e) {
						// Request is committed. Wait for the response. It's on its way.
						return task.get();
					}
					throw new TimedOutFault();
				}	
			}
			try {
				request.cancel();
			} catch (IllegalStateException e) {
				// Request is committed. Wait for the response. It's on its way.
				return task.get();
			}
			throw new TimedOutFault();
		} finally {
			if (task != null)
				task.cancel(true);
		}
	}
    
    public JAXBContext getJAXBContext() {
        return getXmlBinding().getJAXBContext();
    }
    
    public XmlBinding getXmlBinding() {
        if(binding == null) {
            try {
                //
                binding = new XmlBinding(schema, bindingConf);
            } catch (JAXBException e) {
                throw new InternalErrorFault(e);
            }
        }
        return binding;
    }
    
    private static Map<String, String> propertySet =null;
    private static String extensionsFile = null;
    /**Locates the ./wsman-ext.properties file and looks for the extensions
     * property.  Parses the contents and returns as a Map<String,String>
     * where the key is the prefix and the value is the NS_URI.
     *
     * @return extension namespaces
     */
    public static Map<String,String> locateExtensionNamespaces(){
        //Hasmap for return
        Map<String,String> extensionNamespaces = new HashMap<String, String>();
        //Temporary map
        if(propertySet==null){
            propertySet = new HashMap<String, String>();
            try{
                final InputStream ism =
                        WSManAgent.class.getResourceAsStream(WSMAN_EXTENSIONS_PROPERTY_FILE_NAME);
                if(ism!=null){
                    extensionsFile = "";
                }
            }catch(Exception ex){
                //do nothing
            }
        }
        if((extensionsFile!=null)&&(propertySet.size()==0)){
            WSManAgent.getProperties(WSMAN_EXTENSIONS_PROPERTY_FILE_NAME, propertySet);
        }
        if((!propertySet.isEmpty()&&
                (propertySet.containsKey("extensions.envelope.defs")))){
            String extensions = propertySet.get("extensions.envelope.defs");
            //Strip out each extension
            StringTokenizer t = new StringTokenizer(extensions, ",");
            while(t.hasMoreTokens()){
                String map = t.nextToken();
                StringTokenizer m = new StringTokenizer(map,"#");
                if(m.countTokens()==2){
                    extensionNamespaces.put(m.nextToken(), m.nextToken());
                }
            }
        }
        return extensionNamespaces;
    }
    
}
