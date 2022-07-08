/**
 * 
 */
package org.grits.toolbox.editor.experimentdesigner.namespace;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * 
 *
 */
public class ShortNamespacesHandler
{
	private static Logger logger = Logger.getLogger(ShortNamespacesHandler.class);

	private static final String VALUE_SEPARATOR = "||||";

	public static Map<String, Set<String>> NAMESPACE_VALUES_MAP = null;

	public static void loadShortNamespaceFiles()
	{
		NAMESPACE_VALUES_MAP = new HashMap<String, Set<String>>();
		try
		{
			URL resourceFileUrl = FileLocator.toFileURL(ExperimentConfig.NAMESPACE_RESOURCE_URL);
			for(String fileName : new ExperimentDesignOntologyAPI().getAllShortNamespaceFileNames())
			{
				File namespaceFile = new File(resourceFileUrl.getPath() + fileName);
				if(namespaceFile.exists())
				{
					loadShortNamespaceFile(namespaceFile);
				}
				else
				{
					logger.error("Short namespace file is missing : " + fileName);
				}
			}
		} catch (IOException e)
		{
			logger.error("Error loading namespaces\n" + e.getMessage(), e);
		} catch (Exception e)
		{
			logger.fatal("Error loading namespaces\n" + e.getMessage(), e);
		}
	}

	private static void loadShortNamespaceFile(File namespaceFile)
	{
		try
		{
			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(namespaceFile);
			Element namespaces = document == null ? null : document.getRootElement();
			if (namespaces != null 
					&& "namespaces".equals(namespaces.getName()))
			{
				List<?> children = namespaces.getChildren("namespace");
				String namespaceUri = null;
				String value = null;
				Set<String> values = null;
				for(Object child : children)
				{
					namespaceUri = ((Element) child).getAttributeValue("uri");
					if(namespaceUri != null && !namespaceUri.isEmpty())
					{
						value = ((Element) child).getChild("value").getValue();
						String[] splitValue = value.split(Pattern.quote(VALUE_SEPARATOR));
						Collections.addAll(values = new HashSet<String>(), splitValue);
						if(!NAMESPACE_VALUES_MAP.containsKey(namespaceUri))
						{
							NAMESPACE_VALUES_MAP.put(namespaceUri, values);
						}
						else
						{
							logger.error("This uri : \"" + namespaceUri + "\" appears more than once in this "
									+ "namespace file : " + namespaceFile.getAbsolutePath());
						}
					}
				}
			}
		} catch (JDOMException e)
		{
			logger.error(e.getMessage(), e);
		} catch (IOException ex)
		{
			logger.error(ex.getMessage(), ex);
		}
	}

}
