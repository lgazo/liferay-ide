/**
 * 
 */

package com.liferay.ide.eclipse.portlet.core.model.internal;

import org.eclipse.sapphire.modeling.IModelElement;
import org.eclipse.sapphire.modeling.ModelProperty;
import org.eclipse.sapphire.modeling.xml.XmlAttribute;
import org.eclipse.sapphire.modeling.xml.XmlElement;
import org.eclipse.sapphire.modeling.xml.XmlNamespaceResolver;
import org.eclipse.sapphire.modeling.xml.XmlNode;
import org.eclipse.sapphire.modeling.xml.XmlPath;
import org.eclipse.sapphire.modeling.xml.XmlValueBindingImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.liferay.ide.eclipse.portlet.core.util.PortletAppModelConstants;

/**
 * @author kamesh.sampath
 */
public class QNamespaceValueBinding extends XmlValueBindingImpl {

	String[] params;
	XmlPath path;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.sapphire.modeling.BindingImpl#init(org.eclipse.sapphire.modeling.IModelElement,
	 * org.eclipse.sapphire.modeling.ModelProperty, java.lang.String[])
	 */
	@Override
	public void init( IModelElement element, ModelProperty property, String[] params ) {
		super.init( element, property, params );
		this.params = params;
		final XmlNamespaceResolver xmlNamespaceResolver = resource().getXmlNamespaceResolver();
		this.path = new XmlPath( params[0], xmlNamespaceResolver );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.sapphire.modeling.ValueBindingImpl#read()
	 */
	@Override
	public String read() {
		String value = null;
		XmlElement parent = xml( false );
		XmlElement qNameElement = null;
		// Fix for Alias QName not displayed in list
		if ( parent.getLocalName().equals( params[0] ) ) {
			qNameElement = parent;
		}
		else {
			qNameElement = parent.getChildElement( params[0], false );
		}
		if ( qNameElement != null ) {
			// System.out.println( qNameElement );
			XmlAttribute xmlAttribute =
				qNameElement.getAttribute( PortletAppModelConstants.DEFAULT_QNAME_PREFIX, false );
			if ( xmlAttribute != null ) {
				value = xmlAttribute.getText();
			}
		}

		// System.out.println( "QNamespaceValueBinding.read() - Value:" + value );

		return value != null ? value.trim() : value;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.sapphire.modeling.ValueBindingImpl#write(java.lang.String)
	 */
	@Override
	public void write( String value ) {
		String val = value;

		// System.out.println( "VALUE ___________________ " + val );

		XmlElement parent = xml( true );
		/*
		 * In some cases the parent node and the child nodes will be same, we need to ensure that we dont create them
		 * accidentally again
		 */
		// System.out.println( "QNamespaceValueBinding.write() - Parent local name:" + parent.getLocalName() );
		XmlElement qNameElement = null;
		if ( parent.getLocalName().equals( params[0] ) ) {
			qNameElement = parent;
		}
		else {
			qNameElement = parent.getChildElement( params[0], true );
		}

		if ( qNameElement != null && val != null ) {
			// System.out.println( "QNamespaceValueBinding.write() - 1" );
			val = value.trim();
			Element qnameDef = qNameElement.getDomNode();
			/*
			 * Check to ensure that the attribute is not added multiple times, check if the attribute already exist if
			 * yes remove it add add it afresh
			 */
			Attr oldQnsAttribute = qnameDef.getAttributeNode( PortletAppModelConstants.DEFAULT_QNAME_NS_DECL );
			if ( oldQnsAttribute == null ) {
				// System.out.println( "QNamespaceValueBinding.write() - Attrib does not exist" );
				qnameDef.setAttributeNS(
					PortletAppModelConstants.XMLNS_NS_URI, PortletAppModelConstants.DEFAULT_QNAME_NS_DECL, val );
			}
			else {
				// System.out.println( "QNamespaceValueBinding.write() - Attrib  exist" );
				qnameDef.removeAttributeNode( oldQnsAttribute );
				qnameDef.setAttributeNS(
					PortletAppModelConstants.XMLNS_NS_URI, PortletAppModelConstants.DEFAULT_QNAME_NS_DECL, val );
			}

		}// Remove the nod/e if it exists and current value is null
		else if ( qNameElement != null && val == null ) {
			qNameElement.remove();
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.sapphire.modeling.xml.XmlValueBindingImpl#getXmlNode()
	 */
	@Override
	public XmlNode getXmlNode() {
		final XmlElement element = xml( false );
		if ( element != null ) {
			return element.getChildNode( this.path, false );
		}

		return null;
	}

}
