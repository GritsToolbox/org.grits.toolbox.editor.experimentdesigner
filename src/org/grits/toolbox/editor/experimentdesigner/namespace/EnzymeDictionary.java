package org.grits.toolbox.editor.experimentdesigner.namespace;

import java.net.URL;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;
import org.grits.toolbox.core.typeahead.NamespaceHandler;
import org.grits.toolbox.editor.experimentdesigner.Activator;

public class EnzymeDictionary extends NamespaceHandler {
	public static Logger logger = Logger.getLogger(EnzymeDictionary.class);
	
	static String NAMESPACE = "http://www.grits-toolbox.org/experimentdesigner#enzyme";
	static final URL NAMESPACEURL = Platform.getBundle(Activator.PLUGIN_ID).getResource("namespace");
	
	public EnzymeDictionary() {
		super(NAMESPACE, null, "enzyme.txt", Activator.PLUGIN_ID);
	}

	@Override
	public URL getNamespaceURL() {
		return NAMESPACEURL;
	}

}
