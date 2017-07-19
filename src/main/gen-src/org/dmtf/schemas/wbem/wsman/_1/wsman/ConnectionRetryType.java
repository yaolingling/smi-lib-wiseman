//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-382 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.05.29 at 01:59:34 PM EDT 
//

package org.dmtf.schemas.wbem.wsman._1.wsman;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ConnectionRetryType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ConnectionRetryType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd>attributableDuration">
 *       &lt;attribute name="Total" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConnectionRetryType")
public class ConnectionRetryType extends AttributableDuration {

    @XmlAttribute(name = "Total")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger total;


    /**
     * Gets the value of the total property.
     * 
     * @return possible object is {@link BigInteger }
     * 
     */
    public BigInteger getTotal() {
        return total;
    }


    /**
     * Sets the value of the total property.
     * 
     * @param value allowed object is {@link BigInteger }
     * 
     */
    public void setTotal(BigInteger value) {
        this.total = value;
    }

}
