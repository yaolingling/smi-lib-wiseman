//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-382 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.05.29 at 01:59:34 PM EDT 
//

package org.dmtf.schemas.wbem.wsman.identity._1.wsmanidentity;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the org.dmtf.schemas.wbem.wsman.identity._1.wsmanidentity package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content. The Java representation of XML content can consist of schema
 * derived interfaces and classes representing the binding of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in this
 * class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ProductVendor_QNAME = new QName("http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", "ProductVendor");
    private final static QName _Identify_QNAME = new QName("http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", "Identify");
    private final static QName _ProductVersion_QNAME = new QName("http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", "ProductVersion");
    private final static QName _IdentifyResponse_QNAME = new QName("http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", "IdentifyResponse");
    private final static QName _ProtocolVersion_QNAME = new QName("http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", "ProtocolVersion");


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.dmtf.schemas.wbem.wsman.identity._1.wsmanidentity
     * 
     */
    public ObjectFactory() {
    }


    /**
     * Create an instance of {@link IdentifyType }
     * 
     */
    public IdentifyType createIdentifyType() {
        return new IdentifyType();
    }


    /**
     * Create an instance of {@link IdentifyResponseType }
     * 
     */
    public IdentifyResponseType createIdentifyResponseType() {
        return new IdentifyResponseType();
    }


    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", name = "ProductVendor")
    public JAXBElement<String> createProductVendor(String value) {
        return new JAXBElement<String>(_ProductVendor_QNAME, String.class, null, value);
    }


    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IdentifyType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", name = "Identify")
    public JAXBElement<IdentifyType> createIdentify(IdentifyType value) {
        return new JAXBElement<IdentifyType>(_Identify_QNAME, IdentifyType.class, null, value);
    }


    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", name = "ProductVersion")
    public JAXBElement<String> createProductVersion(String value) {
        return new JAXBElement<String>(_ProductVersion_QNAME, String.class, null, value);
    }


    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IdentifyResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", name = "IdentifyResponse")
    public JAXBElement<IdentifyResponseType> createIdentifyResponse(IdentifyResponseType value) {
        return new JAXBElement<IdentifyResponseType>(_IdentifyResponse_QNAME, IdentifyResponseType.class, null, value);
    }


    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd", name = "ProtocolVersion")
    public JAXBElement<String> createProtocolVersion(String value) {
        return new JAXBElement<String>(_ProtocolVersion_QNAME, String.class, null, value);
    }

}
