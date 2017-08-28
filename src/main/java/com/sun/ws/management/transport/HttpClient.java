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
 **$Log: HttpClient.java,v $
 **Revision 1.20  2007/06/04 06:25:12  denis_rachal
 **The following fixes have been made:
 **
 **   * Moved test source to se/test/src
 **   * Moved test handlers to /src/test/src
 **   * Updated logging calls in HttpClient & Servlet
 **   * Fxed compiler warning in AnnotationProcessor
 **   * Added logging files for client junit tests
 **   * Added changes to support Maven builds
 **   * Added JAX-WS libraries to CVS ignore
 **
 **Revision 1.19  2007/05/30 20:31:07  nbeers
 **Add HP copyright header
 **
 **
 * $Id: HttpClient.java,v 1.20 2007/06/04 06:25:12 denis_rachal Exp $
 */

package com.sun.ws.management.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.sun.ws.management.Message;
import com.sun.ws.management.addressing.Addressing;

public final class HttpClient {

    private static final Logger LOG = Logger.getLogger(HttpClient.class.getName());
//    private static boolean useApacheCommonsHttpClient = true;
    private static boolean useApacheCommonsHttpClient = false;

    private HttpClient() {}

    public static void setAuthenticator(final Authenticator auth) {
        Authenticator.setDefault(auth);
    }

    public static void setTrustManager(final X509TrustManager trustManager)
    throws NoSuchAlgorithmException, KeyManagementException {

        final TrustManager[] tm = { trustManager };
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, tm, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }

    public static void setHostnameVerifier(final HostnameVerifier hv) {
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

    private static HttpURLConnection initConnection(final String to, final ContentType ct) throws IOException {
        if (to == null) {
            throw new IllegalArgumentException("Required Element is missing: " +
                    Addressing.TO);
        }

        final URL dest = new URL(to);
        final URLConnection conn = dest.openConnection();

        conn.setAllowUserInteraction(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type",
                ct == null ? ContentType.DEFAULT_CONTENT_TYPE.toString() : ct.toString());
        // TODO: get this from the properties
        conn.setRequestProperty("User-Agent", "https://wiseman.dev.java.net");

        final HttpURLConnection http = (HttpURLConnection) conn;
        http.setRequestMethod("POST");

        return http;
    }

    // type of data can be Message or byte[], others will throw IllegalArgumentException
    private static void transfer(final URLConnection conn, final Object data)
    throws IOException, SOAPException, JAXBException {
        OutputStream os = null;
        try {
            os = conn.getOutputStream();
            if (data instanceof Message) {
                ((Message) data).writeTo(os);
            } else if (data instanceof SOAPMessage) {
                ((SOAPMessage) data).writeTo(os);
            } else if (data instanceof byte[]) {
                os.write((byte[]) data);
            } else {
                throw new IllegalArgumentException("Type of data not handled: " +
                        data.getClass().getName());
            }
        } finally {
            if (os != null) { os.close(); }
        }
    }

    public static Addressing sendRequest(final SOAPMessage msg, final String destination)
    throws IOException, SOAPException, JAXBException {

//    	if(useApacheCommonsHttpClient){
//        	Addressing message = new Addressing(msg);
//        	message.setTo(destination);
//       	 return sendHttpRequest(message);
//    	}
////        log(msg);

		if (LOG.isLoggable(Level.FINE))
			LOG.fine("<request>\n" + msg + "</request>\n");
        final HttpURLConnection http = initRequest(destination,
                ContentType.createFromEncoding((String) msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING)));
        transfer(http, msg);
        final Addressing response = readResponse(http);
		if (LOG.isLoggable(Level.FINE)) {
			if (response.getBody().hasFault())
				LOG.fine("<fault>\n" + response + "</fault>\n");
			else
			    LOG.fine("<response>\n" + response + "</response>\n");
		}
        return response;
    }

    public static Addressing sendRequest(final SOAPMessage msg, final String destination,Entry<String, String>... headers)
    throws IOException, SOAPException, JAXBException {

//    	if(useApacheCommonsHttpClient){
//    		return sendHttpRequest(msg,destination);
//    	}
////        log(msg);

		if (LOG.isLoggable(Level.FINE))
			LOG.fine("<request>\n" + msg + "</request>\n");
        final HttpURLConnection http = initRequest(destination,
                ContentType.createFromEncoding((String) msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING)));
        if (headers != null) {
            for (Entry<String,String> entry: headers) {
                http.setRequestProperty(entry.getKey(),entry.getValue());
            }
        }

        transfer(http, msg);
        final Addressing response = readResponse(http);
		if (LOG.isLoggable(Level.FINE)) {
			if (response.getBody().hasFault())
				LOG.fine("<fault>\n" + response + "</fault>\n");
			else
			    LOG.fine("<response>\n" + response + "</response>\n");
		}
        return response;
    }


    public static Addressing sendRequest(final Addressing msg, final
    		Entry<String, String>... headers)
    throws IOException, JAXBException, SOAPException {

//    	if(useApacheCommonsHttpClient){
//    		return sendHttpRequest(msg, headers);
//    	}
////        log(msg);

		if (LOG.isLoggable(Level.FINE))
			LOG.fine("<request>\n" + msg + "</request>\n");
        final HttpURLConnection http = initRequest(msg.getTo(), msg.getContentType());

        if (headers != null) {
            for (Entry<String,String> entry: headers) {
                http.setRequestProperty(entry.getKey(),entry.getValue());
            }
        }

        transfer(http, msg);
        final Addressing response = readResponse(http);
		if (LOG.isLoggable(Level.FINE)) {
			if (response.getBody().hasFault())
				LOG.fine("<fault>\n" + response + "</fault>\n");
			else
				LOG.fine("<response>\n" + response + "</response>\n");
		}
        response.setXmlBinding(msg.getXmlBinding());
        return response;
//    	return sendHttpRequest(msg, headers);
    }

//    public static Addressing sendHttpRequest(final Addressing msg,
//    		Entry<String,String>... headers) throws SOAPException,
//    		JAXBException, IOException{
//    	Addressing response = buildCommonsHttpRequest(msg,msg.getTo());
//    	return response;
//    }
//
//    public static Addressing sendHttpRequest(final SOAPMessage msg,
//    		String destination) throws SOAPException,
//    		JAXBException, IOException{
//    	Addressing message = new Addressing(msg);
//    	Addressing response = buildCommonsHttpRequest(message,destination);
////    	message.
////        final HttpURLConnection http = initRequest(destination,
////                ContentType.createFromEncoding((String) msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING)));
////        transfer(http, msg);
////        return readResponse(http);
//
//    	return response;
//    }

    public static HttpURLConnection createHttpConnection(
    				String destination,Object data) throws SOAPException,
    				JAXBException, IOException{
        final HttpURLConnection http = initRequest(destination, null);
//        transfer(http, data);

//    	Addressing message = new Addressing(msg);
//    	Addressing response = buildCommonsHttpRequest(message,destination);
//    	message.
//        final HttpURLConnection http = initRequest(destination,
//                ContentType.createFromEncoding((String) msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING)));
//        transfer(http, msg);
//        return readResponse(http);

        return http;
    }

//    private static org.apache.commons.httpclient.HttpClient client = null;
//	/**
//	 * @param msg
//	 * @return
//	 * @throws IOException
//	 * @throws SOAPException
//	 * @throws JAXBException
//	 * @throws HttpException
//	 */
//	private static Addressing buildCommonsHttpRequest(final Addressing msg,String destination) throws IOException, SOAPException, JAXBException, HttpException {
////		log(msg);
////    	org.apache.commons.httpclient.HttpClient client = null;
//        PostMethod method = null;
//        Addressing response = null;
//	      try{
//	//        final Addressing response = HttpClient.sendRequest(identify.getMessage(), DESTINATION);
//	       if(client==null){
//	          client = new org.apache.commons.httpclient.HttpClient();
//	       }
////	        method = new PostMethod(msg.getTo());
//	        method = new PostMethod(destination);
//	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	        SOAPMessage message = msg.getMessage();
//	        message.writeTo(baos);
//	        method.setRequestEntity(new InputStreamRequestEntity(
//	        		new ByteArrayInputStream(baos.toByteArray())));
//	        if((msg.getContentType()==null)||
//	        		(msg.getContentType().toString().trim().length()==0)){
//	            msg.setContentType(ContentType.DEFAULT_CONTENT_TYPE);
//	        }
////	        if(msg.getContentType()){
////
////	        }
//	        method.setRequestHeader("Content-Type",
////	        		ContentType.DEFAULT_CONTENT_TYPE.toString());
//	        		msg.getContentType().toString());
//	        method.setRequestHeader("User-Agent", "https://wiseman.dev.java.net");
//	        method.setFollowRedirects(false);
//	        method.setRequestHeader("Accept", ContentType.ACCEPTABLE_CONTENT_TYPES);
//
//	        final String user = System.getProperty("wsman.user", "");
//	        final String password = System.getProperty("wsman.password", "");
//
//	        client.getState().setCredentials(
//	//        		new AuthScope("www.verisign.com", 443, "realm"),
//	        		AuthScope.ANY,
//	//                new UsernamePasswordCredentials("username", "password")
////	        		new UsernamePasswordCredentials("wsman", "secret")
//	                new UsernamePasswordCredentials(user, password)
//	        );
//
//	        method.setDoAuthentication(true);
//	        int result = client.executeMethod(method);
//	        response = new Addressing(method.getResponseBodyAsStream());
//	        response.setXmlBinding(msg.getXmlBinding());
//	        response.setContentType(msg.getContentType());
//	      }finally{
//	    	  method.releaseConnection();
//	      }
//		return response;
//    }

    private static HttpURLConnection initRequest(final String destination, final ContentType contentType)
    throws IOException {

        final HttpURLConnection http = initConnection(destination, contentType);
        http.setRequestProperty("Accept", ContentType.ACCEPTABLE_CONTENT_TYPES);
        http.setInstanceFollowRedirects(false);
        return http;
    }

    private static Addressing readResponse(final HttpURLConnection http)
    throws IOException, SOAPException {

        final InputStream is;
        final int response = http.getResponseCode();
        if (response == HttpURLConnection.HTTP_OK) {
            is = http.getInputStream();
        } else if (response == HttpURLConnection.HTTP_BAD_REQUEST ||
                response == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            // read the fault from the error stream
            is = http.getErrorStream();
        } else {
            final String detail = http.getResponseMessage();
            throw new IOException(detail == null ? Integer.toString(response) : detail);
        }

        final String responseType = http.getContentType();
        final ContentType contentType = ContentType.createFromHttpContentType(responseType);
        if (contentType==null||!contentType.isAcceptable()) {
            // dump the first 4k bytes of the response for help in debugging
            if (LOG.isLoggable(Level.INFO)) {
                final byte[] buffer = new byte[4096];
                final int nread = is.read(buffer);
                if (nread > 0) {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream(buffer.length);
                    bos.write(buffer, 0, nread);
                    LOG.info("Response discarded: " + new String(bos.toByteArray()));
                }
            }
            throw new IOException("Content-Type of response is not acceptable: " + responseType);
        }

        final Addressing addr;
        try {
            addr = new Addressing(is);
        } finally {
            if (is != null) { is.close(); }
        }

        addr.setContentType(contentType);
//        log(addr);

        return addr;
    }

    public static int sendResponse(final String to, final byte[] bits, final ContentType contentType)
    throws IOException, SOAPException, JAXBException {
//        log(bits);
        final HttpURLConnection http = initConnection(to, contentType);
        transfer(http, bits);
        return http.getResponseCode();
    }

    public static int sendResponse(final Addressing msg) throws IOException, SOAPException, JAXBException {
//        log(msg);
        final HttpURLConnection http = initConnection(msg.getTo(), msg.getContentType());
        transfer(http, msg);
        return http.getResponseCode();
    }

//    private static void log(final Addressing msg) throws IOException, SOAPException {
//        // expensive serialization ahead, so check first
//        if (LOG.isLoggable(Level.FINE)) {
//            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            msg.writeTo(baos);
//            final byte[] content = baos.toByteArray();
//            final ContentType type = msg.getContentType();
//            LOG.fine(type == null ? new String(content) : new String(content, type.getEncoding()));
//        }
//    }
//
//    private static void log(final SOAPMessage msg) throws IOException, SOAPException {
//        // expensive serialization ahead, so check first
//        if (LOG.isLoggable(Level.FINE)) {
//            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            msg.writeTo(baos);
//            final byte[] content = baos.toByteArray();
//            final String encoding = (String) msg.getProperty(SOAPMessage.CHARACTER_SET_ENCODING);
//            LOG.fine(encoding == null ? new String(content) : new String(content, encoding));
//        }
//    }
//
//    private static void log(final byte[] bits) {
//        if (LOG.isLoggable(Level.FINE)) {
//            LOG.fine(new String(bits));
//        }
//    }
	public static boolean isUseApacheCommonsHttpClient() {
		return useApacheCommonsHttpClient;
	}

	public static void setUseApacheCommonsHttpClient(
			boolean useApacheCommonsHttpClient) {
		HttpClient.useApacheCommonsHttpClient = useApacheCommonsHttpClient;
	}
}