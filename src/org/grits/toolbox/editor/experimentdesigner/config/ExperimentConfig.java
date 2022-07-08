package org.grits.toolbox.editor.experimentdesigner.config;

import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.grits.toolbox.editor.experimentdesigner.Activator;


public class ExperimentConfig {

	public static final String FILE_NAME_PREFIX = "experiment";
	public static final int FILE_NAME_RANDOM_CHARACTERS_LENGTH = 5;
	public static final String FILE_TYPE_OF_EXPERIMENT = ".xml";
	public static final String EXPERIMENT_FOLDER_NAME = "experiments";
	public static final String STANDARD_ONTOLOGY_FILE_NAME = "experimentdesignontology.owl";
	public static final URL ONTOLOGY_RESOURCE_URL = Platform.getBundle(Activator.PLUGIN_ID).getResource("ontology");
	public static final URL PROTOCOL_RESOURCE_URL = Platform.getBundle(Activator.PLUGIN_ID).getResource("protocols");
	public static final URL FILE_RESOURCE_URL = Platform.getBundle(Activator.PLUGIN_ID).getResource("files");
	public static final URL EXPERIMET_TEMPLATE_RESOURCE_URL = Platform.getBundle(Activator.PLUGIN_ID).getResource("experiments");
	public static final URL NAMESPACE_RESOURCE_URL = Platform.getBundle(Activator.PLUGIN_ID).getResource("namespace");
	public static final String PROTOCOLVARIANT_LOCATION = "protocols";
	public static final String PROTOCOLVARIANT_FILE_NAME_PREFIX = "protocol";
	public static final int DESCRIPTION_LENGTH = 10000;   //TODO get it from PropertyHandler
	public static final String TEMPLATE_FILE_NAME_PREFIX = "experimentTemplate";
	public static final String EXPERIMENT_TEMPLATE_LOCATION = "experimentTemplates";
	public static final String LOCAL_ONTOLOGY_FILE_NAME = "localexperimentdesignontology.owl";
	
	public static final String PROTOCOLVARIANT_INDEXFILE = "protocol.xml";
	public static final String EXPERIMENTTEMPLATE_INDEXFILE = "template.xml";
	public static final String SHORT_NAMESPACE_FILE_PREFIX = "namespaces";
}
