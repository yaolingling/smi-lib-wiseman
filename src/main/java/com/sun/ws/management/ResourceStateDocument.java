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
 **$Log: not supported by cvs2svn $
 ** 
 *
 * $Id: ResourceStateDocument.java,v 1.3 2007-05-30 20:31:05 nbeers Exp $
 */
package com.sun.ws.management;


import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Represents any response document. Provides access via XPath to an part of a
 * returned document. Can be used to access action reponses or pull responses. A
 * wrapper for any document.
 * 
 * @author wire
 *
 */
public interface ResourceStateDocument {

	/**
	 * Returns the underlying DOM model for the SOAP body of this resource.
	 * @return a DOM of the SOAP body.
	 */
	public Document getDocument();

	/** 
	 * Returns a list of nodes that match the provided XPath criteria.
	 * 
	 * @param xPathExpression
	 * @return A list of matching nodes.
	 * @throws XPathExpressionException
	 * @throws NoMatchFoundExceptionServer 
	 */
	public NodeList getValues(String xPathExpression)
			throws XPathExpressionException, NoMatchFoundExceptionServer;

	/**
	 * Returns the element text of the Element pointed to by the provided XPath.
	 * @param xPathExpression
	 * @return A string containg the element text.
	 * @throws XPathExpressionException
	 * @throws NoMatchFoundExceptionServer 
	 */
	public String getValueText(String xPathExpression)
			throws XPathExpressionException, NoMatchFoundExceptionServer;

	public String getWrappedValueText(QName name)
		throws XPathExpressionException, NoMatchFoundExceptionServer;

	public String getValueText(QName name,Node context) throws XPathExpressionException, NoMatchFoundExceptionServer;
	
	/**
	 * Returns the QNames of the topmost wrapper elements in the SOAP body.
	 * @return the QNames of the topmost elements in the SOAP body.
	 */
	public QName[] getFieldNames();

	/**
	 * A conveneince method. Assumes your document is simple having a wrapper element
	 * and a set of child state value elements. This method returns the QNames of the
	 * child elements of the first wrapper element in the body of the SOAP document.
	 * @return the QNames of the child elements of the first wrapper element in the body.
	 */
	public QName[] getWrappedFieldNames();

	/**
	 * If your state is complex you may need to get the QNames present
	 * as the children of any element in the state document. It is assumed
	 * that you would use an XPath to locate a set of elements deep in the
	 * body of the state document. This function will return all the QNames
	 * which are the children of the conext node.
	 * @param context
	 * @return a list of QNames which are the children of context
	 */
	public QName[] getFieldNames(Node context);

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
	public void setFieldValues(String xPathExpression, String value)
			throws XPathExpressionException, NoMatchFoundExceptionServer;

	/**
	 * Sets the element text of the specified QName to null. Skips document node
	 * and first wrapper element as a conveniance.
	 * @param name
	 * @param value
	 * @throws NoMatchFoundExceptionServer
	 */
	public void setWrappedFieldValue(QName name, String value)
			throws NoMatchFoundExceptionServer;

	/**
	 * Sets the element text of the first element that matches QName relative
	 * to the provided dom node.
	 * @param name A QName of an element which is a direct decendant of the context node.
	 * @param value Text to assign to the text element of the selected element
	 * @param context This value cannot be null.
	 * @throws NoMatchFoundExceptionServer
	 */
	public void setFieldValue(QName name, String value, Node context)
			throws NoMatchFoundExceptionServer;

}