
package com.liferay.ide.eclipse.portlet.core.model.internal;

import org.eclipse.sapphire.modeling.IModelElement;
import org.eclipse.sapphire.modeling.ModelProperty;
import org.eclipse.sapphire.modeling.xml.XmlElement;
import org.eclipse.sapphire.modeling.xml.XmlNamespaceResolver;
import org.eclipse.sapphire.modeling.xml.XmlNode;
import org.eclipse.sapphire.modeling.xml.XmlPath;
import org.eclipse.sapphire.modeling.xml.XmlValueBindingImpl;

import com.liferay.ide.eclipse.portlet.core.util.PortletAppModelConstants;
import com.liferay.ide.eclipse.portlet.core.util.PortletUtil;

/**
 * @author kamesh.sampath
 */

public final class QNameLocalPartValueBinding

extends XmlValueBindingImpl

{

	private XmlPath path;
	private String[] params;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.sapphire.modeling.BindingImpl#init(org.eclipse.sapphire.modeling.IModelElement,
	 * org.eclipse.sapphire.modeling.ModelProperty, java.lang.String[])
	 */
	@Override
	public void init( final IModelElement element, final ModelProperty property, final String[] params ) {
		super.init( element, property, params );

		final XmlNamespaceResolver xmlNamespaceResolver = resource().getXmlNamespaceResolver();
		this.path = new XmlPath( params[0], xmlNamespaceResolver );
		this.params = params;

		// System.out.println( "TextNodeValueBinding.init()" + this.path );
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.sapphire.modeling.ValueBindingImpl#read()
	 */
	@Override
	public String read() {
		String value = null;

		final XmlElement parent = xml( false );

		// Fix for Alias QName not displayed in list
		XmlElement qNameElement = null;
		if ( parent.getLocalName().equals( params[0] ) ) {
			qNameElement = parent;
		}
		else {
			qNameElement = parent.getChildElement( params[0], false );
		}

		// System.out.println( "Reading VALUE for Element  ___________________ " + qNameElement );

		if ( qNameElement != null ) {

			value = qNameElement.getText();

			if ( value != null ) {
				value = value.trim();
				value = PortletUtil.stripPrefix( value );
			}

		}

		return value;
	}



	/*
	 * (non-Javadoc)
	 * @see org.eclipse.sapphire.modeling.ValueBindingImpl#write(java.lang.String)
	 */
	@Override
	public void write( final String value ) {
		String val = value;
		XmlElement parent = xml( true );

		/*
		 * In some cases the parent node and the child nodes will be same, we need to ensure that we dont create them
		 * accidentally again
		 */
		// System.out.println( "QNameLocalPartValueBinding.write() - Parent local name:" + parent.getLocalName() );
		XmlElement qNameElement = null;
		if ( parent.getLocalName().equals( params[0] ) ) {
			qNameElement = parent;
		}
		else {
			qNameElement = parent.getChildElement( params[0], true );
		}

		// System.out.println( "VALUE ___________________ " + val );

		// System.out.println( "TextNodeValueBinding.write() - Parent " + xml( true ).getParent() );
		if ( qNameElement != null && val != null ) {
			val = value.trim();
			qNameElement.setText( PortletAppModelConstants.DEFAULT_QNAME_PREFIX + ":" + val );
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
