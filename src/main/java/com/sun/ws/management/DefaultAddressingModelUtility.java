package com.sun.ws.management;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;

import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;

import com.sun.ws.management.addressing.Addressing;

public class DefaultAddressingModelUtility extends Management {

	public DefaultAddressingModelUtility() throws SOAPException {
		super();
	}
    
    public DefaultAddressingModelUtility(final Addressing addr) throws SOAPException {
        super(addr);
    }
    
    public DefaultAddressingModelUtility(final InputStream is) throws 
    		SOAPException, IOException {
        super(is);
    }
    
    /** Retrieve the selectors from a Management instance.
     * 
     * @param message
     * @return a mapped set of selectors
     * @throws JAXBException
     * @throws SOAPException
     */
    public static Map<String,String> getSelectorsAsStringMap(Management message) 
    		throws JAXBException, SOAPException{
    	HashMap<String,String> selectors = new HashMap<String,String>();
    	 if(message!=null){
    		Set<SelectorType> selSet = message.getSelectors();
    		populateMap(selectors, selSet);
    	 }
		return selectors;
    }

	/** Take SelectorType and populate the Map<String,String> bag passed in.
	 * 
	 * @param selectors
	 * @param selSet
	 */
	public static void populateMap(HashMap<String, String> selectors, 
			Set<SelectorType> selSet) {
		if(selectors==null){
			selectors = new HashMap<String, String>();
		}
	    if(selSet!=null){
		  for (Iterator iter = selSet.iterator(); iter.hasNext();) {
			SelectorType element = (SelectorType) iter.next();
			if((element.getName()!=null)&&(element.getName().trim().length()>0)){
				selectors.put(element.getName(), 
						element.getContent().toString());
			}
		  }
	    }
	}
}
