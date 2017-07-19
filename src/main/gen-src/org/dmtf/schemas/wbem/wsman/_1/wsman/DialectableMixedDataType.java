//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-382 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.05.29 at 01:59:34 PM EDT 
//

package org.dmtf.schemas.wbem.wsman._1.wsman;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for dialectableMixedDataType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="dialectableMixedDataType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd}mixedDataType">
 *       &lt;attribute name="Dialect" type="{http://www.w3.org/2001/XMLSchema}anyURI" default="http://www.w3.org/TR/1999/REC-xpath-19991116" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dialectableMixedDataType")
public class DialectableMixedDataType extends MixedDataType {

    @XmlAttribute(name = "Dialect")
    @XmlSchemaType(name = "anyURI")
    protected String dialect;


    /**
     * Gets the value of the dialect property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getDialect() {
        if (dialect == null) {
            return "http://www.w3.org/TR/1999/REC-xpath-19991116";
        } else {
            return dialect;
        }
    }


    /**
     * Sets the value of the dialect property.
     * 
     * @param value allowed object is {@link String }
     * 
     */
    public void setDialect(String value) {
        this.dialect = value;
    }

}
