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
 **$Log: WSManAgent.java,v $
 **Revision 1.17  2007/06/15 12:13:20  jfdenise
 **Cosmetic change. Make OPERATION_TIMEOUT_DEFAULT public and added a trace.
 **
 **Revision 1.16  2007/05/30 20:31:04  nbeers
 **Add HP copyright header
 **
 **
 * $Id: WSManAgent.java,v 1.17 2007/06/15 12:13:20 jfdenise Exp $
 */

package com.sun.ws.management.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.dmtf.schemas.wbem.wsman._1.wsman.MaxEnvelopeSizeType;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import com.sun.ws.management.AccessDeniedFault;
import com.sun.ws.management.EncodingLimitFault;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.Management;
import com.sun.ws.management.Message;
import com.sun.ws.management.TimedOutFault;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.transport.ContentType;
import com.sun.ws.management.transport.HttpClient;
import com.sun.ws.management.xml.XmlBinding;

/**
 * WS-MAN agent decoupled from transport. Can be used in Servlet / JAX-WS / ...
 * context.
 *
 */

public abstract class WSManAgent {

    private static final Logger LOG = Logger.getLogger(WSManAgent.class.getName());
    private static final long DEFAULT_TIMEOUT = 30000;
    private static final long MIN_ENVELOPE_SIZE = 8192;
    private static final long DISABLED_TIMEOUT = -1;

    public static final String OPERATION_TIMEOUT =
            WSManAgent.class.getPackage().getName() + ".operation.timeout";
    private static final String WSMAN_PROPERTY_FILE_NAME = "/wsman.properties";
    public static final String WSMAN_EXTENSIONS_PROPERTY_FILE_NAME = "/wsman-exts.properties";
    public static final String WISEMAN_PROPERTY_FILE_NAME = "/wiseman.properties";
    public static final String OPERATION_TIMEOUT_DEFAULT = "OperationTimeoutDefault";
    private static final String UUID_SCHEME = "uuid:";
    private static final String SCHEMA_PATH =
            "/com/sun/ws/management/resources/schemas/";

    private static ExecutorService pool;

    private static Map<String, String> properties = null;

    private Map<String, String> localproperties = null;
    private long defaultOperationTimeout = 0;

    // XXX REVISIT, SHOULD BE STATIC BUT CURRENTLY CAN'T Due to openess of JAXBContext
    private final XmlBinding binding;

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

    protected WSManAgent() throws SAXException {
        this(null, null, null);
    }

    protected WSManAgent(Map<String,String> wisemanConf, Source[] customSchemas,
           Map<String,String> bindingConf)
        throws SAXException {
        final Map<String, String> propertySet = new HashMap<String, String>();
        getProperties(WISEMAN_PROPERTY_FILE_NAME, propertySet);
        // Put all passed properties
        if(wisemanConf != null)
            propertySet.putAll(wisemanConf);
        localproperties = Collections.unmodifiableMap(propertySet);

        String opTimeout = System.getProperty(OPERATION_TIMEOUT);
        if(opTimeout == null)
            opTimeout = localproperties.get(OPERATION_TIMEOUT_DEFAULT);

        if(opTimeout == null)
             this.defaultOperationTimeout = DEFAULT_TIMEOUT;
        else
            this.defaultOperationTimeout = Long.parseLong(opTimeout);

        Schema schema = createSchema(customSchemas);
        try {
            //
            this.binding = new XmlBinding(schema, bindingConf);
        } catch (JAXBException e) {
            throw new InternalErrorFault(e);
        }

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
    private Identify processForIdentify(Management request)
        throws SecurityException, JAXBException, SOAPException {
    	//Test for identify message
        final Identify identify = new Identify(request);
        identify.setXmlBinding(request.getXmlBinding());

        final SOAPElement id = identify.getIdentify();
        if (id == null) {
            return null;//else exit
        }
        if(LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, "Serving Identity");

    	//As this is an indentify message then populate the response.
        Identify response = new Identify();
        response.setXmlBinding(request.getXmlBinding());
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

        return response;
    }
    /**
     * Hook your own dispatcher
     * @param agent
     */
    abstract protected RequestDispatcher createDispatcher(final Management request,
            final HandlerContext context) throws SOAPException, JAXBException,
            IOException;

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

    private static long getEnvelopeSize(Management request) throws JAXBException, SOAPException {
        long maxEnvelopeSize = Long.MAX_VALUE;
        final MaxEnvelopeSizeType maxSize = request.getMaxEnvelopeSize();
        if (maxSize != null) {
            maxEnvelopeSize = maxSize.getValue().longValue();
        }
        return maxEnvelopeSize;
    }

    public static long getValidEnvelopeSize(Management request) throws JAXBException, SOAPException {
        long maxEnvelopeSize = getEnvelopeSize(request);
        if(maxEnvelopeSize < MIN_ENVELOPE_SIZE)
            maxEnvelopeSize =  Long.MAX_VALUE;
        return maxEnvelopeSize;
    }
    /**
     * Agent request handling entry point. Return a Message due to Identify reply.
     */
    public Message handleRequest(final Management request, final HandlerContext context) {
        Addressing response = null;

        // try {
        // XXX WARNING, CREATING A JAXBCONTEXT FOR EACH REQUEST IS TOO EXPENSIVE.
        // JAXB team says that you should share as much as you can JAXBContext.
        // I propose to make XmlBinding back to be static and locked. I mean
        // that no custom package nor schema should be added to this JAXBContext.
        // It is private to wiseman and only used to handle the protocol.
        // Any model dependent JAXB processing must be done in separate JAXBContext(s).
        // Model layer(s) can implement their own JAXBContext strategies.
        // BTW, doing so, we make clear that any rechnology can be used to marsh/unmarsh.
        // Relaying on JAXB becomes an implementation detail.

        // schema might be null if no XSDs were found
        request.setXmlBinding(binding);
        // } catch (JAXBException jex) {
        //     LOG.log(Level.SEVERE, "Error initializing XML Binding", jex);
        // TODO throw new ServletException(jex);
        // }

        try {
            logMessage(LOG, request);

            long timeout = getTimeout(request);

            if((response = isValidEnvelopSize(request)) == null) {
                final RequestDispatcher dispatcher = createDispatcher(request,
                        context);
                try {
                    dispatcher.authenticate();
                    //Test for identify responses
                    Identify identifyResponse = processForIdentify(request);
                    if(identifyResponse!=null){
                        identifyResponse.setContentType(request.getContentType());
                        logMessage(LOG, identifyResponse);
                        return identifyResponse;
                    }
                    dispatcher.validateRequest();
                    response = dispatch(dispatcher, timeout);
                } catch (SecurityException sx) {
                    response = new Management();
                    response.setFault(new AccessDeniedFault());
                } catch (FaultException fex) {
                    response = new Management();
                    response.setFault(fex);
                } catch (Throwable th) {
                    response = new Management();
                    response.setFault(new InternalErrorFault(th));
                }
            }

            fillReturnAddress(request, response);
            if(LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "Request / Response content type " +
                        request.getContentType());
            response.setContentType(request.getContentType());

            Message resp = handleResponse(response, getValidEnvelopeSize(request));

        }catch(Exception ex) {
            try {
                response = new Management();
                response.setFault(new InternalErrorFault(ex.getMessage()));
                fillReturnAddress(request, response);
                response.setContentType(request.getContentType());
            }catch(Exception ex2) {
                // We can't handle the internal error.
                throw new RuntimeException(ex2.getMessage());
            }
        }

        return response;
    }

    private long getTimeout(Management request) throws Exception {
        long timeout = defaultOperationTimeout;
        final Duration timeoutDuration = request.getTimeout();
        if (timeoutDuration != null) {
            timeout = timeoutDuration.getTimeInMillis(new Date());
        }
        return timeout;
    }

    private static Management isValidEnvelopSize(Management request)
    throws Exception {
        Management response = null;
        long maxEnvelopeSize = getEnvelopeSize(request);
        if (maxEnvelopeSize < MIN_ENVELOPE_SIZE) {
            EncodingLimitFault fault =
                    new EncodingLimitFault("MaxEnvelopeSize is set too " +
                    "small to encode faults " +
                    "(needs to be atleast " + MIN_ENVELOPE_SIZE + ")",
                    EncodingLimitFault.Detail.MAX_ENVELOPE_SIZE);
            response = new Management();
            response.setFault(fault);
        }
        return response;
    }

    private Addressing dispatch(final Callable dispatcher, final long timeout)
    throws Throwable {
        final FutureTask<Management> task =
                new FutureTask<Management>(dispatcher);
        // the Future returned by pool.submit does not propagate
        // ExecutionException, perform the get on FutureTask itself
        getExecutorService().submit(task);
       
         if(LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, "timeout : " + defaultOperationTimeout);
        try {
            if (defaultOperationTimeout != DISABLED_TIMEOUT)
                return task.get(timeout, TimeUnit.MILLISECONDS);
            else
                return task.get();
        } catch (ExecutionException ex) {
            throw ex.getCause();
        } catch (InterruptedException ix) {
            // ignore
        } catch (TimeoutException tx) {
            throw new TimedOutFault();
        } finally {
            task.cancel(true);
        }
        return null;
    }

    private static void fillReturnAddress(Addressing request,
            Addressing response)
            throws JAXBException, SOAPException {
        response.setMessageId(UUID_SCHEME + UUID.randomUUID().toString());

        // messageId can be missing in a malformed request
        final String msgId = request.getMessageId();
        if (msgId != null) {
            response.addRelatesTo(msgId);
        }

        if (response.getBody().hasFault()) {
            final EndpointReferenceType faultTo = request.getFaultTo();
            if (faultTo != null) {
                response.setTo(faultTo.getAddress().getValue());
                response.addHeaders(faultTo.getReferenceParameters());
                return;
            }
        }

        final EndpointReferenceType replyTo = request.getReplyTo();
        if (replyTo != null) {
            response.setTo(replyTo.getAddress().getValue());
            response.addHeaders(replyTo.getReferenceParameters());
            return;
        }

        final EndpointReferenceType from = request.getFrom();
        if (from != null) {
            response.setTo(from.getAddress().getValue());
            response.addHeaders(from.getReferenceParameters());
            return;
        }

        response.setTo(Addressing.ANONYMOUS_ENDPOINT_URI);
    }

    private static Message handleResponse(final Message response,
            final long maxEnvelopeSize) throws SOAPException, JAXBException,
            IOException {

        if(response instanceof Identify) {
            return response;
        }

        if(!(response instanceof Management))
            throw new IllegalArgumentException(" Invalid internal response " +
                    "message " + response);

        Management mgtResp = (Management) response;
        return handleResponse(mgtResp, null, maxEnvelopeSize, false);
    }

    private static Message handleResponse(final Management response,
            final FaultException fex, final long maxEnvelopeSize,
            boolean responseTooBig) throws SOAPException, JAXBException,
            IOException {
        if (fex != null)
            response.setFault(fex);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeTo(baos);
        final byte[] content = baos.toByteArray();

        logMessage(LOG, response);

        if (content.length > maxEnvelopeSize) {

            // although we check earlier that the maxEnvelopeSize is > 8192, we still
            // need to use the responseTooBig flag to break possible infinite recursion if
            // the serialization of the EncodingLimitFault happens to exceed 8192 bytes
            if (responseTooBig) {
                LOG.warning("MaxEnvelopeSize set too small to send an EncodingLimitFault");
                // Let's try the underlying stack to send the reply. Best effort
            } else {
                if(LOG.isLoggable(Level.FINE))
                    LOG.log(Level.FINE, "Response actual size is bigger than maxSize.");
                handleResponse(response,
                        new EncodingLimitFault(Integer.toString(content.length),
                        EncodingLimitFault.Detail.MAX_ENVELOPE_SIZE_EXCEEDED), maxEnvelopeSize, true);
            }
        }else
            if(LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "Response actual size is smaller than maxSize.");


        final String dest = response.getTo();
        if (!Addressing.ANONYMOUS_ENDPOINT_URI.equals(dest)) {
            if(LOG.isLoggable(Level.FINE))
                LOG.log(Level.FINE, "Non anonymous reply to send to : " + dest);
            final int status = sendAsyncReply(response.getTo(), content, response.getContentType());
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Response to " + dest + " returned " + status);
            }
            return null;
        }

        if(LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, "Anonymous reply to send.");

        return response;
    }

    private static int sendAsyncReply(final String to, final byte[] bits, final ContentType contentType)
    throws IOException, SOAPException, JAXBException {
        return HttpClient.sendResponse(to, bits, contentType);
    }

    static void logMessage(Logger logger,
            final Message msg) throws IOException, SOAPException {
        // expensive serialization ahead, so check first
        if (logger.isLoggable(Level.FINE)) {
            if(msg == null) {
                logger.fine("Null message to log. Reply has perhaps been " +
                        "sent asynchronously");
                return;
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            msg.writeTo(baos);
            final byte[] content = baos.toByteArray();

            String encoding = msg.getContentType() == null ? null :
                msg.getContentType().getEncoding();

            logger.fine("Encoding [" + encoding + "]");

            if(encoding == null)
                logger.fine(new String(content));
            else
                logger.fine(new String(content, encoding));

        }
    }

    public XmlBinding getXmlBinding() {
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
//    	 crud
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
