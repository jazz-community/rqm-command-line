package com.cli.qm.connection;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class NamespaceContextMap implements NamespaceContext {
	HashMap<String, String> prefixMap;

	public NamespaceContextMap() {
		super();
		this.prefixMap = new HashMap<String, String>();
	}
	public NamespaceContextMap(String[] args) {
		this();
		if (args != null) setMap(args);		
	}
	public void setMap(String[] args) {
		int len = args.length;
		int i = 0;
		while (i+1 < len) {
			prefixMap.put(args[i++], args[i++]);			
		}
	}
	public String getNamespaceURI(String prefix) {
		if (prefix == null)
			throw new NullPointerException("Null prefix");
		String uri = prefixMap.get(prefix);
		return (uri != null)?uri:XMLConstants.NULL_NS_URI;
	}

	public String getPrefix(String namespaceURI) {
		// Unused
		return null;
	}

	@SuppressWarnings("unchecked")
	public Iterator getPrefixes(String namespaceURI) {
		// Unused
		return null;
	}

}
