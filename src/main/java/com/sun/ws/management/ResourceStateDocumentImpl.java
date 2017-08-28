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
 **$Log: ResourceStateDocumentImpl.java,v $
 **Revision 1.2  2007/05/30 20:31:05  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: ResourceStateDocumentImpl.java,v 1.2 2007/05/30 20:31:05 nbeers Exp $
 */
package com.sun.ws.management;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class ResourceStateDocumentImpl implements ResourceStateDocument {

	private Document stateDocument;
	private XPath xpath;

	public ResourceStateDocumentImpl(Document stateDocument) {
		this.stateDocument=stateDocument;
		xpath = XPathFactory.newInstance().newXPath();
	}
	
	/**
	 * Returns the underlying DOM model for the SOAP body of this resource.
	 * @return a DOM of the SOAP body.
	 */
	public Document getDocument() {
		return stateDocument;
	}
	
	/** 
	 * Returns a list of nodes that match the provided XPath criteria.
	 * 
	 * @param xPathExpression
	 * @return A list of matching nodes.
	 * @throws XPathExpressionException
	 * @throws NoMatchFoundExceptionServer 
	 */
	public NodeList getValues(String xPathExpression) throws XPathExpressionException, NoMatchFoundExceptionServer{
		Object nodes = xpath.evaluate(xPathExpression, stateDocument, XPathConstants.NODESET);
		if(nodes==null)
			throw new NoMatchFoundExceptionServer("No Element could be found to match your XPath expression.");	
		NodeList nodelist = (NodeList)nodes;		
		return nodelist;
	}
	
	/**
	 * Returns the element text of the Element pointed to by the provided XPath.
	 * @param xPathExpression
	 * @return A string containg the element text.
	 * @throws XPathExpressionException
	 * @throws NoMatchFoundExceptionServer 
	 */
	public String getValueText(String xPathExpression) throws XPathExpressionException, NoMatchFoundExceptionServer{
		Object resultOb = xpath.evaluate(xPathExpression, stateDocument, XPathConstants.STRING);
		if(resultOb==null)
			throw new NoMatchFoundExceptionServer("No Element could be found to match your XPath expression.");	
		String results = resultOb.toString();
		return results;
	}
	
	/**
	 * Returns the QNames of the topmost wrapper elements in the SOAP body.
	 * @return the QNames of the topmost elements in the SOAP body.
	 */
	public QName[] getFieldNames(){
		return getFieldNames(null);
	}

	/**
	 * A conveneince method. Assumes your document is simple having a wrapper element
	 * and a set of child state value elements. This method returns the QNames of the
	 * child elements of the first wrapper element in the body of the SOAP document.
	 * @return
	 */
	public QName[] getWrappedFieldNames(){
		return getFieldNames(stateDocument.getFirstChild());
	}
	
	/**
	 * If your state is complex you may need to get the QNames present
	 * as the children of any element in the state document. It is assumed
	 * that you would use an XPath to locate a set of elements deep in the
	 * body of the state document. This function will return all the QNames
	 * which are the children of the conext node.
	 * @param context
	 * @return a list of QNames which are the children of context
	 */
	public QName[] getFieldNames(Node context){
		if(context==null)
			context=stateDocument;
		Vector names=new Vector();
		NodeList children = context.getChildNodes();
		for(int index=0;index<children.getLength();index++){
			Node child = children.item(index);
			names.add(new QName(child.getNamespaceURI(),child.getLocalName()));
		}
		return (QName[]) names.toArray(new QName[0]);		
	}
	
	/**
	 * Sets all the text elements of the selected nodes to the value provided.
	 * 
	 * <b>Warning:</b> Make sure your xpath results in a unique node because if you
	 * select more than one, they all will get set to value.
	 * @param xPathExpression
	 * @param value
	 * @throws XPathExpressionException
	 * @throws NoMatchFoundExceptionServer 
	 */
	public void setFieldValues(String xPathExpression,String value) throws XPathExpressionException, NoMatchFoundExceptionServer{
		NodeList nodes = getValues(xPathExpression);
		if(nodes.getLength()==0)
			throw new NoMatchFoundExceptionServer("No Element could be found to match your XPath expression.");	
		for(int index=0;index<nodes.getLength();index++){
			Node node = nodes.item(index);
			node.setTextContent(value);
		}
	}

	/**
	 * Sets the element text of the specified QName to null. Skips document node
	 * and first wrapper element as a conveniance.
	 * @param name
	 * @param value
	 * @throws NoMatchFoundExceptionServer
	 */
	public void setWrappedFieldValue(QName name,String value) throws NoMatchFoundExceptionServer{
		setFieldValue(name,value,stateDocument.getFirstChild());
	}

	/**
	 * Sets the element text of the first element that matches QName relative
	 * to the provided dom node.
	 * @param name A QName of an element which is a direct decendant of the context node.
	 * @param value Text to assign to the text element of the selected element
	 * @param context This value cannot be null.
	 * @throws NoMatchFoundExceptionServer
	 */
	public void setFieldValue(QName name,String value,Node context) throws NoMatchFoundExceptionServer{
		NodeList nodes = context.getChildNodes();
		Node matchedNode=null;
		for(int index=0;index<nodes.getLength();index++){
			Node node = nodes.item(index);
			String localPart = node.getLocalName();
			String namespaceUri = node.getNamespaceURI();
			if(namespaceUri.equals(name.getNamespaceURI()))
				if(localPart.equals(name.getLocalPart())){
					matchedNode=node;
					break;
				}
		}
		if(matchedNode==null)
			throw new NoMatchFoundExceptionServer("No Element could be found to match your QName.");
		matchedNode.setTextContent(value);
	}

	public String getWrappedValueText(QName name) throws XPathExpressionException, NoMatchFoundExceptionServer {
		return getValueText(name,stateDocument.getFirstChild());
	}
	public String getValueText(QName name,Node context) throws XPathExpressionException, NoMatchFoundExceptionServer {
		if(name==null||context==null){
			throw new NoMatchFoundExceptionServer("QName or Node was null.");			
		}
		NodeList nodes = context.getChildNodes();
		Node matchedNode=null;
		for(int index=0;index<nodes.getLength();index++){
			Node node = nodes.item(index);
			String localPart = node.getLocalName();
			String namespaceUri = node.getNamespaceURI();
			if(namespaceUri==null)
				continue;			
				
			if(namespaceUri.equals(name.getNamespaceURI()))
				if(localPart.equals(name.getLocalPart())){
					matchedNode=node;
					return matchedNode.getTextContent();
				}
		}
		throw new NoMatchFoundExceptionServer("No Element could be found to match your QName.");

	}
	
    public void prettyPrint(final OutputStream os,Document doc) throws SOAPException, ParserConfigurationException, SAXException, IOException {
    
    OutputFormat format = new OutputFormat(doc);
    format.setLineWidth(72);
    format.setIndenting(true);
    format.setIndent(2);
    XMLSerializer serializer = new XMLSerializer(os, format);
    serializer.serialize(doc);    
    os.write("\n".getBytes());
    }
    
    @Override
    public String toString() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            prettyPrint(bos,stateDocument);
        } catch (Exception ex) {
            return null;
        }
        return new String(bos.toByteArray());
    }


}
