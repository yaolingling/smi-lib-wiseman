package com.sun.ws.management.identify;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

/** This class is meant to provide general utility functionality for
 *  Identify instances and all of their related extensions.  SOAPElement 
 *  instances populated with data may be returned.
 * 
 * @author Simeon
 */
public class IdentifyUtility {
	private static final Logger LOG = 
		Logger.getLogger(IdentifyUtility.class.getName());

	/**Method parse message to return first SOAPElement with the
	 * QName entered.
	 * 
	 * @param id Identify response instance.
	 * @param identifier QName describing/identifying child to locate.
	 * @return SOAPElement containing located node.
	 * @throws SOAPException
	 */
	public static SOAPElement locateElement(Identify id,
			QName identifier) throws SOAPException{
		SOAPElement located =null;
		if(id == null){
			return located;
		}
        final SOAPElement idr = id.getIdentifyResponse();
        if(idr!=null){
          SOAPElement[] elements = id.getChildren(idr, identifier);
          if((elements!=null)&&(elements.length>0)){
        	 located = elements[0]; 
        	 if(elements.length>1){
        		LOG.log(Level.FINE, "More than one header with that QName existed. Returning first.");
        	 }
          }
        }
        return located;
	}
}
