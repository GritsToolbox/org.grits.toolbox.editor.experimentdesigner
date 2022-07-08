package org.grits.toolbox.editor.experimentdesigner.ontology;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.grits.toolbox.core.dataShare.PropertyHandler;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ExperimentConfig;
import org.grits.toolbox.editor.experimentdesigner.io.ExperimentTemplateFileHandler;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolEntry;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolExistsException;
import org.grits.toolbox.editor.experimentdesigner.io.ProtocolVariantFileHandler;
import org.grits.toolbox.editor.experimentdesigner.model.EntityWithPosition;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.MeasurementUnit;
import org.grits.toolbox.editor.experimentdesigner.model.MyColor;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolCategory;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolPaletteEntry;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.AlreadyExistsException;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * 
 * @author sena
 *
 */

public class ExperimentDesignOntologyAPI implements ExperimentDesignModelAPI{

	private static Logger logger = Logger.getLogger(ExperimentDesignOntologyAPI.class);
	private static OntologyManager om;

	private static String getOntologyLocation(String filename) throws Exception{
		try
		{
			if (filename.equals(ExperimentConfig.STANDARD_ONTOLOGY_FILE_NAME)) {
				// use it from the jar, no need to copy
				URL resourceFileUrl = FileLocator.toFileURL(ExperimentConfig.ONTOLOGY_RESOURCE_URL);
				if (resourceFileUrl == null) {
					throw new Exception ("Ontology is missing from the jar.");
				}
				String originalJarFilePath = resourceFileUrl.getPath() + filename;
				return originalJarFilePath;
			}
			else {
				String configFolderLocation = PropertyHandler.getVariable("configuration_location");
				File configFolder = new File(configFolderLocation);
				if(configFolder.isDirectory())
				{
					List<String> childFiles = Arrays.asList(configFolder.list());
					String experimentSubFolderName = configFolderLocation +
							File.separator + "org.grits.toolbox.editor.experimentdesigner";
					File experimentSubFolder = new File(experimentSubFolderName);
					boolean subFolderExists = experimentSubFolder.exists();
					if(!subFolderExists && !childFiles.contains(experimentSubFolderName))
					{
						subFolderExists = experimentSubFolder.mkdir();
					}
					if(subFolderExists){

						String configOntologyLocation = experimentSubFolder.getAbsolutePath() +
								File.separator + filename;
						childFiles = Arrays.asList(experimentSubFolder.list()); 
						if(!childFiles.contains(filename))  // first time, needs to be copied into the config
						{
							URL resourceFileUrl = FileLocator.toFileURL(ExperimentConfig.ONTOLOGY_RESOURCE_URL);
							String originalJarFilePath = resourceFileUrl.getPath() + filename;
							File originalJarFile = new File(originalJarFilePath);
							FileOutputStream configFile = new FileOutputStream(configOntologyLocation );
							Files.copy(originalJarFile.toPath(), configFile);
							configFile.close();
						}
						return configOntologyLocation;
					}
				}
			}
		} catch (Exception e)
		{
			throw e;
		}
		return null;
	}

	private static String copyFromJar(String filename) throws IOException {
		String configOntologyLocation = getConfigFolderLocation() + File.separator + filename;
		URL resourceFileUrl = FileLocator.toFileURL(ExperimentConfig.ONTOLOGY_RESOURCE_URL);
		String originalJarFilePath = resourceFileUrl.getPath() + filename;
		File originalJarFile = new File(originalJarFilePath);
		FileOutputStream configFile = new FileOutputStream(configOntologyLocation );
		Files.copy(originalJarFile.toPath(), configFile);
		configFile.close();

		return configOntologyLocation;
	}

	private static String getConfigFolderLocation () {
		return PropertyHandler.getVariable("configuration_location") + File.separator + "org.grits.toolbox.editor.experimentdesigner";
	}

	public static String getTemplateFolderLocation () {
		return getConfigFolderLocation() + File.separator + ExperimentConfig.EXPERIMENT_TEMPLATE_LOCATION;
	}

	public static String getProtocolVariantFolderLocation () {
		return getConfigFolderLocation() + File.separator + ExperimentConfig.PROTOCOLVARIANT_LOCATION;
	}

	private static File[] getConfigFilesByDirectory(String directory) throws Exception {
		try
		{
			String configFolderLocation = getConfigFolderLocation();
			File configFolder = new File(configFolderLocation);
			if(configFolder.isDirectory())
			{
				String dataFolderLocation = configFolderLocation + File.separator + directory;
				File dataFolder = new File (dataFolderLocation);
				if (!dataFolder.exists()) {
					// no variants yet
					return null;
				}

				return dataFolder.listFiles();

			}
		} catch (Exception e)
		{
			throw e;
		}

		return null;
	}

	public ExperimentDesignOntologyAPI() throws Exception{
		if (om == null) {
			// load the ontology
			logger.info(Activator.PLUGIN_ID + "- START : Loading Ontology. ");

			try {
				String filePath1 = ExperimentDesignOntologyAPI.getOntologyLocation(ExperimentConfig.STANDARD_ONTOLOGY_FILE_NAME);
				if (filePath1 == null) {
					logger.error(Activator.PLUGIN_ID + " Error loading ontology, file path is empty!");
					MessageDialog.openError(Display.getCurrent().getActiveShell(), 
							"Error", "Error Loading Ontology.");
					throw new Exception ("Error loading ontology, file path is empty!");
				}
				String filePath2 = ExperimentDesignOntologyAPI.getOntologyLocation(ExperimentConfig.LOCAL_ONTOLOGY_FILE_NAME);
				if (filePath2 == null) {
					logger.error(Activator.PLUGIN_ID + " Error loading local ontology, file path is empty!");
					MessageDialog.openError(Display.getCurrent().getActiveShell(), 
							"Error", "Error Loading Local Ontology.");
					throw new Exception ("Error loading local ontology, file path is empty!");
				}
				om  = new OntologyManager( 
						new FileInputStream(new File(filePath1)), new FileInputStream(new File(filePath2)));
				String version = om.standardOntologymodel.getOntology(OntologyManager.ontURI).getVersionInfo();  
				Ontology localOnt = om.localOntologymodel.getOntology(OntologyManager.ontURI);
				String localVersion = null;
				if (localOnt != null)
					localVersion = localOnt.getVersionInfo();

				// compare only the first digit since it is the digit indicating structural changes	
				boolean versionConflict = false;
				if (version != null) {
					int first = Integer.parseInt (version.substring(0, version.indexOf(".")));
					if (localVersion == null) {
						versionConflict = true;
					} else {
						int firstLocal = Integer.parseInt (localVersion.substring(0, localVersion.indexOf(".")));
						if (first != firstLocal)
							versionConflict = true;
					}
					// local ontology is not up-to-date
					// ask the user what to do
					if (versionConflict) {
						boolean keep = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), 
								"Error", "The local ontology is out-of-date. Do you want to keep it anyway?\n"
										+ "Choose NO to get the latest version but you will loose access to your changes");
						if (!keep) {
							String filePath = ExperimentDesignOntologyAPI.copyFromJar (ExperimentConfig.LOCAL_ONTOLOGY_FILE_NAME);
							om.reloadLocalOntology(new FileInputStream(new File(filePath)));
						}
					}
				}

			} catch (IOException ex) {
				logger.error("Error Loading Ontology", ex);
				MessageDialog.openError(Display.getCurrent().getActiveShell(), 
						"Error", "Error Loading Ontology.");
				throw ex;
			} catch (Exception ex) {
				logger.fatal("Error Loading Ontology", ex);
				MessageDialog.openError(Display.getCurrent().getActiveShell(), 
						"Error", "Error Loading Ontology. "
								+ "Please contact the developers for further information or help.");
				throw ex;
			}

			logger.info(Activator.PLUGIN_ID + "- END   : Loading Ontology. ");
		}
	}

	/**
	 * all top level categories are defined only in the standard ontology currently
	 */
	public List<ProtocolCategory> getTopLevelCategories () {
		LinkedList<ProtocolCategory> categories = new LinkedList<ProtocolCategory>();
		try
		{
			List<Individual> categoryIndividuals = om.getAllIndiviudalsOfClass(OntologyManager.TOPLEVELCATEGORY_CLASS_URI);
			for(Individual categoryIndiv : categoryIndividuals) {
				ProtocolCategory category = new ProtocolCategory ();
				category.setUri(categoryIndiv.getURI());
				category.setName(categoryIndiv.getLabel(null));
				Literal position = om.getLiteralValue(categoryIndiv, "has_position");
				int pos = position.getInt();
				category.setPosition(position.getInt());
				Literal icon = om.getLiteralValue(categoryIndiv, "has_icon");
				if (icon != null) {
					category.setIcon(icon.getString());
				}
				category.setColor(getColorForCategory(categoryIndiv)); 
				category.setDescription (categoryIndiv.getComment(null));
				addObjectInPosition (categories, pos, category);
			}
		} catch (Exception e)
		{
			logger.error(Activator.PLUGIN_ID + " Error Reading Ontology", e); 
			MessageDialog.openError(Display.getCurrent().getActiveShell(), 
					"Error", "Error Reading Ontology");
		}

		return categories;
	}
	
	/**
	 * get the top level category for the given protocol
	 * 
	 * @param protocol
	 * @return ProtocolCategory object for the palette category
	 */
	public ProtocolCategory getTopLevelCategoryForProtocol (ProtocolNode protocol) {
		ProtocolCategory topLevel = null;
		
		if (protocol.getCategory() == null)
			return null;
		
		List<Individual> topLevelcategoryIndividuals = om.getAllSubjects("has_subCategory", protocol.getCategory().getUri());
		for(Individual categoryIndiv : topLevelcategoryIndividuals) {
			topLevel = new ProtocolCategory();
			topLevel.setUri(categoryIndiv.getURI());
			topLevel.setName(categoryIndiv.getLabel(null));
			Literal position = om.getLiteralValue(categoryIndiv, "has_position");
			topLevel.setPosition(position.getInt());
			Literal icon = om.getLiteralValue(categoryIndiv, "has_icon");
			if (icon != null) {
				topLevel.setIcon(icon.getString());
			}
			topLevel.setColor(getColorForCategory(categoryIndiv)); 
			topLevel.setDescription (categoryIndiv.getComment(null));
			break;    // there should be only one top level category for a given protocol
		}
		return topLevel;
	}

	/**
	 * all protocol categories are defined only in the standard ontology currently
	 */
	public List<ProtocolCategory> getProtocolCategoriesByTopLevelCategory (ProtocolCategory topLevel) {
		LinkedList<ProtocolCategory> categories = new LinkedList<ProtocolCategory>();
		try
		{
			Individual topLevelIndividual = om.getIndividual(topLevel.getUri());
			if (topLevelIndividual == null) {
				return categories;
			}
			List<Individual> categoryIndividuals = om.getAllObjects(topLevelIndividual, "has_subCategory");
			for(Individual categoryIndiv : categoryIndividuals) {
				ProtocolCategory category = new ProtocolCategory ();
				category.setUri(categoryIndiv.getURI());
				category.setName(categoryIndiv.getLabel(null));
				Literal position = om.getLiteralValue(categoryIndiv, "has_position");
				int pos = position.getInt();
				category.setPosition(position.getInt());
				Literal icon = om.getLiteralValue(categoryIndiv, "has_icon");
				if (icon != null) {
					category.setIcon(icon.getString());
				}
				category.setColor(getColorForCategory(categoryIndiv)); 
				category.setDescription (categoryIndiv.getComment(null));
				addObjectInPosition (categories, pos, category);
			}
		} catch (Exception e)
		{
			logger.error(Activator.PLUGIN_ID + " Error Reading Ontology", e); 
			MessageDialog.openError(Display.getCurrent().getActiveShell(), 
					"Error", "Error Reading Ontology");
		}

		return categories;
	}

	MyColor getColorForCategory(Individual categoryIndiv) {
		MyColor color = new MyColor(new RGB(255, 255, 255));   // default color, white

		Literal icon = om.getLiteralValue(categoryIndiv, "has_icon");
		if (icon != null) {
			String iconName = icon.getString();
			iconName = iconName.substring(0, iconName.lastIndexOf("."));
			String[] colorCode = iconName.split("-");
			if (colorCode.length == 3) {
				try {
					color = new MyColor (new RGB(Integer.parseInt(colorCode[0]), Integer.parseInt(colorCode[1]), Integer.parseInt(colorCode[2])));
				} catch (NumberFormatException e) {
					logger.warn("Color code for the icon is incorrect in the ontology", e);
				}
			}
		}
		return color;

	}

	/**
	 * all protocol categories are defined only in the standard ontology currently
	 */
	public List<ProtocolCategory> getProtocolCategories () {
		LinkedList<ProtocolCategory> categories = new LinkedList<ProtocolCategory>();
		try
		{
			List<Individual> categoryIndividuals = om.getAllIndiviudalsOfClass(OntologyManager.PROTOCOLCATEGORY_CLASS_URI);
			for(Individual categoryIndiv : categoryIndividuals) {
				ProtocolCategory category = new ProtocolCategory ();
				category.setUri(categoryIndiv.getURI());
				category.setName(categoryIndiv.getLabel(null));
				Literal position = om.getLiteralValue(categoryIndiv, "has_position");
				int pos = position.getInt();
				category.setPosition(position.getInt());
				Literal icon = om.getLiteralValue(categoryIndiv, "has_icon");
				if (icon != null) {
					category.setIcon(icon.getString());
				}
				category.setColor(getColorForCategory(categoryIndiv)); 
				category.setDescription (categoryIndiv.getComment(null));
				addObjectInPosition (categories, pos, category);
			}
		} catch (Exception e)
		{
			logger.error(Activator.PLUGIN_ID + " Error Reading Ontology", e); 
			MessageDialog.openError(Display.getCurrent().getActiveShell(), 
					"Error", "Error Reading Ontology");
		}

		return categories;
	}

	private <T extends EntityWithPosition> void addObjectInPosition (LinkedList<T> llist, int position, T item) {

		if (llist.size() == 0) {
			llist.add(item);
		} else if (llist.get(0).getPosition() != null && llist.get(0).getPosition() > position) {
			llist.add(0, item);
		} else if (llist.get(0).getPosition() == null || llist.get(llist.size() - 1).getPosition() == null) {
			llist.add(item);
		} else if (llist.get(llist.size() - 1).getPosition() < position) {
			llist.add(llist.size(), item);
		} else {
			int i = 0;
			while (llist.get(i).getPosition() < position) {
				i++;
			}
			llist.add(i, item);
		}
	}

	@Override
	public ProtocolNode getProtocolByUri(String uri) {
		ProtocolNode protocol = null;
		OntModel model = om.standardOntologymodel;
		//check the standard ontology first
		Individual protocolIndiv = om.getIndividual(uri);
		if (protocolIndiv == null) {
			protocolIndiv = om.getIndividual(om.localOntologymodel, uri);
			model = om.localOntologymodel;
		}
		if (protocolIndiv != null) {
			protocol = new ProtocolNode ();
			protocol.setTemplateUri(uri);
			protocol.setLabel(protocolIndiv.getLabel(null));
			protocol.setDescription(protocolIndiv.getComment(null));
			Literal creator = om.getAnnotationValue(model, protocolIndiv, "creator");
			if (creator != null)
				protocol.setCreator(creator.getString());
			Literal url = om.getLiteralValue(model, protocolIndiv, "has_url");
			if (url != null) 
				protocol.setUrl(url.getString());
			Literal filename = om.getLiteralValue(model, protocolIndiv, "has_file");
			if (filename != null) 
				protocol.setFile(filename.getString());
			protocol.setParameters(getAllParametersForProtocol(model, uri));
			protocol.setParameterGroups(getAllParameterGroupsForProtocol(model, uri));
			
			List<Individual> categories = om.getAllObjects(model, protocolIndiv, "has_category");
			ProtocolCategory category = new ProtocolCategory();
			for (Individual individual : categories) {  // there should be only one
				category.setUri(individual.getURI());
				category.setName(individual.getLabel(null));
				Literal position = om.getLiteralValue(individual, "has_position");
				category.setPosition(position.getInt());
				Literal icon = om.getLiteralValue(individual, "has_icon");
				if (icon != null) {
					category.setIcon(icon.getString());
				}
				category.setColor(getColorForCategory(individual)); 
				category.setDescription (individual.getComment(null));
				break;
			}
			
			protocol.setCategory(category);
		}
		return protocol;
	}

	public List<ProtocolNode> getProtocolsByLabel (String label) {
		List<ProtocolNode> protocols = new ArrayList<>();
		
		List<Individual> allProtocols = om.getAllIndiviudalsOfClass(om.standardOntologymodel, OntologyManager.PROTOCOL_CLASS_URI);
		for (Individual individual : allProtocols) {
			String existing = individual.getLabel(null);
			if (existing != null && existing.equalsIgnoreCase(label)) 
				protocols.add(getProtocolByUri(individual.getURI()));
		}
		
		allProtocols = om.getAllIndiviudalsOfClass(om.localOntologymodel, OntologyManager.PROTOCOL_CLASS_URI);
		for (Individual individual : allProtocols) {
			String existing = individual.getLabel(null);
			if (existing != null && existing.equalsIgnoreCase(label)) 
				protocols.add(getProtocolByUri(individual.getURI()));
		}
		return protocols;
	}

	/**
	 * Returns all protocol instances for the given category, 
	 * loading all from the standard ontology and from the local ontology
	 */
	public List<ProtocolPaletteEntry> getProtocolsForCategory (ProtocolCategory category) {
		List<ProtocolPaletteEntry> entries = new ArrayList<>();

		List<Individual> protocols = om.getAllSubjects("has_category", category.getUri());
		Set<String> protocolIndivURIs = new HashSet<String>();
		for(Individual protocol : protocols) {
			protocolIndivURIs.add (protocol.getURI());
			ProtocolPaletteEntry entry = new ProtocolPaletteEntry();
			entry.setLabel(protocol.getLabel(null));
			entry.setCategory(category);
			entry.setUri(protocol.getURI());
			entry.setDescription (protocol.getComment(null));
			Literal creator = om.getAnnotationValue(protocol, "creator");
			if (creator != null)
				entry.setCreator(creator.getString());
			Literal url = om.getLiteralValue(protocol, "has_url");
			if (url != null) 
				entry.setUrl(url.getString());
			Literal filename = om.getLiteralValue(protocol, "has_file");
			if (filename != null) 
				entry.setFile(filename.getString());
			entry.setParameterGroups(this.getAllParameterGroupsForProtocol(om.standardOntologymodel, protocol.getURI()));
			entry.setParameters(this.getAllParametersForProtocol(om.standardOntologymodel, protocol.getURI()));
			entry.setPapers(this.getAllPapersForProtocol(om.standardOntologymodel, protocol.getURI()));
			entry.setColor(new Color(Display.getCurrent(), category.getColor().getRed(), category.getColor().getGreen(), category.getColor().getBlue()));
			entries.add(entry);
		}

		protocols = om.getAllSubjects(om.localOntologymodel, "has_category", category.getUri());
		for(Individual protocol : protocols) {
			if (protocolIndivURIs.contains(protocol.getURI())) {
				continue;
			}
			ProtocolPaletteEntry entry = new ProtocolPaletteEntry();
			entry.setLabel(protocol.getLabel(null));
			entry.setCategory(category);
			entry.setUri(protocol.getURI());
			entry.setDescription (protocol.getComment(null));
			Literal creator = om.getAnnotationValue(om.localOntologymodel, protocol, "creator");
			if (creator != null)
				entry.setCreator(creator.getString());
			Literal url = om.getLiteralValue(om.localOntologymodel, protocol, "has_url");
			if (url != null) 
				entry.setUrl(url.getString());
			Literal filename = om.getLiteralValue(om.localOntologymodel, protocol, "has_file");
			if (filename != null) 
				entry.setFile(filename.getString());
			entry.setParameterGroups(this.getAllParameterGroupsForProtocol(om.localOntologymodel, protocol.getURI()));
			entry.setParameters(this.getAllParametersForProtocol(om.localOntologymodel, protocol.getURI()));
			entry.setPapers (this.getAllPapersForProtocol(om.localOntologymodel, protocol.getURI()));
			entry.setColor(new Color(Display.getCurrent(), category.getColor().getRed(), category.getColor().getGreen(), category.getColor().getBlue()));
			entries.add(entry);
		}
		return entries;
	}

	/** 
	 * @return all parameter groups from the standard and local ontology 
	 * */
	@Override
	public List<ParameterGroup> getParameterGroups () {
		LinkedList<ParameterGroup> groups = new LinkedList<ParameterGroup>();
		try
		{
			Set<String> groupIndivURIs = new HashSet<String>();
			List<Individual> groupIndividuals = om.getAllIndiviudalsOfClass(OntologyManager.PARAMETERGROUP_CLASS_URI);
			Map<String, ParameterGroup> repeatingGroups = new HashMap<String, ParameterGroup>();
			for(Individual groupIndiv : groupIndividuals) {
				ParameterGroup group = new ParameterGroup ();
				group.setUri(groupIndiv.getURI());
				group.setLabel(groupIndiv.getLabel(null));
				if (repeatingGroups.get(group.getLabel()) != null) {
					// another occurrence
					group.setId(repeatingGroups.get(group.getLabel()).getId() + 1);
					repeatingGroups.remove(group.getLabel());
					repeatingGroups.put(group.getLabel(), group);
				}
				else {
					//first occurrence
					group.setId(1); 
					repeatingGroups.put(group.getLabel(), group);
				}
				group.setDescription(groupIndiv.getComment(null));
				group.setParameters(getParametersForParameterGroup(om.standardOntologymodel, group));
				groups.add(group);
				//addObjectInPosition (groups, pos, group);
				groupIndivURIs.add(groupIndiv.getURI());
			}

			groupIndividuals = om.getAllIndiviudalsOfClass(om.localOntologymodel, OntologyManager.PARAMETERGROUP_CLASS_URI);
			repeatingGroups = new HashMap<String, ParameterGroup>();
			for(Individual groupIndiv : groupIndividuals) {
				if (groupIndivURIs.contains(groupIndiv.getURI()))
					continue;
				ParameterGroup group = new ParameterGroup ();
				group.setUri(groupIndiv.getURI());
				group.setLabel(groupIndiv.getLabel(null));
				if (repeatingGroups.get(group.getLabel()) != null) {
					// another occurrence
					group.setId(repeatingGroups.get(group.getLabel()).getId() + 1);
					repeatingGroups.remove(group.getLabel());
					repeatingGroups.put(group.getLabel(), group);
				}
				else {
					//first occurrence
					group.setId(1); 
					repeatingGroups.put(group.getLabel(), group);
				}
				group.setDescription(groupIndiv.getComment(null));
				group.setParameters(getParametersForParameterGroup(om.localOntologymodel, group));
				groups.add(group);
				//addObjectInPosition (groups, pos, group);
			}
		} catch (Exception e)
		{
			logger.error(Activator.PLUGIN_ID + " Error Reading Ontology", e); 
			MessageDialog.openError(Display.getCurrent().getActiveShell(), 
					"Error", "Error Reading Ontology");
		}

		return groups;
	}

	/**
	 * 
	 * @return all the parameters defined in the standard and local ontology
	 */
	@Override
	public List<Parameter> getAllParameters() {
		LinkedList<Parameter> parameters = new LinkedList<Parameter>();
		try
		{
			Set<String> paramIndivURIs = new HashSet<String>();
			List<Individual> paramIndividuals = om.getAllIndiviudalsOfClass(OntologyManager.PARAMETER_CLASS_URI);
			for(Individual paramIndiv : paramIndividuals) {
				Parameter param = new Parameter ();
				param.setUri(paramIndiv.getURI());
				param.setName(paramIndiv.getLabel(null));
				param.setDescription(paramIndiv.getComment(null));
				List<Individual> units = om.getAllObjects(paramIndiv, "has_unit_of_measurement");
				List<MeasurementUnit> unitList = new ArrayList<>();
				MeasurementUnit mUnit;
				for(Individual unit : units) {
					mUnit = new MeasurementUnit();
					mUnit.setUri(unit.getURI());
					mUnit.setLabel(unit.getLabel(null));
					unitList.add(mUnit);
				}
				param.setAvailableUnits(unitList);
				// get the namespace
				setNamespace(om.standardOntologymodel, paramIndiv, param);
                parameters.add(param);
				paramIndivURIs.add(paramIndiv.getURI());
			}

			paramIndividuals = om.getAllIndiviudalsOfClass(om.localOntologymodel, OntologyManager.PARAMETER_CLASS_URI);
			for(Individual paramIndiv : paramIndividuals) {
				if (paramIndivURIs.contains(paramIndiv.getURI()))
					continue;
				Parameter param = new Parameter ();
				param.setUri(paramIndiv.getURI());
				param.setName(paramIndiv.getLabel(null));
				param.setDescription(paramIndiv.getComment(null));
				List<Individual> units = om.getAllObjects(om.localOntologymodel, paramIndiv, "has_unit_of_measurement");
				List<MeasurementUnit> unitList = new ArrayList<>();
				MeasurementUnit mUnit;
				for(Individual unit : units) {
					mUnit = new MeasurementUnit();
					mUnit.setUri(unit.getURI());
					mUnit.setLabel(unit.getLabel(null));
					unitList.add(mUnit);
				}
				// get the namespace
				setNamespace(om.localOntologymodel, paramIndiv, param);
				param.setAvailableUnits(unitList);
				parameters.add(param);
			}
		} catch (Exception e)
		{
			logger.error(Activator.PLUGIN_ID + " Error Reading Ontology",  e); 
			MessageDialog.openError(Display.getCurrent().getActiveShell(), 
					"Error", "Error getting parameters from the Ontology");
		}

		return parameters;
	}

	private List<Parameter> getAllParametersForProtocol (OntModel model, String protocolUri) {
		LinkedList<Parameter> parameters = new LinkedList<>();
		Individual protocolIndiv = om.getIndividual(model, protocolUri);
		//Set<String> paramIndivURIs = new HashSet<String>();
		List<Individual> parameterIndivs = om.getAllObjects(model, protocolIndiv, "has_parameter");
		for(Individual param : parameterIndivs) {
			// get the parameter and is_required values
			List<Individual> parametersFromContext = om.getAllObjects(model, param, "parameter");
			Literal required = om.getLiteralValue(model, param, "is_required");
			if (parametersFromContext != null && parametersFromContext.size() == 1) { // should have only one since "parameter" is a functional property 
				Individual parameterIndiv = parametersFromContext.get(0);    		
				Parameter parameter = new Parameter();
				parameter.setUri(parameterIndiv.getURI());
				parameter.setName(parameterIndiv.getLabel(null));
				parameter.setDescription(parameterIndiv.getComment(null));
				parameter.setRequired(required.getBoolean());
				List<Individual> units = om.getAllObjects(model, parameterIndiv, "has_unit_of_measurement");
				List<MeasurementUnit> unitList = new ArrayList<>();
				MeasurementUnit mUnit;
				for(Individual unit : units) {
					mUnit = new MeasurementUnit();
					mUnit.setUri(unit.getURI());
					mUnit.setLabel(unit.getLabel(null));
					unitList.add(mUnit);
				}
				setNamespace(model, parameterIndiv, parameter);
				parameter.setAvailableUnits(unitList);
				Literal position = om.getLiteralValue(model, param, "has_position");   // position is from the parameter context
				int pos = 0;
				if (position != null) {
					pos = position.getInt();
					parameter.setPosition(pos);
				}
				List<Individual> guidelinesFromContext = om.getAllObjects(model, param, "guideline_info");
				if (guidelinesFromContext != null) {
					List<String> guidelines = new ArrayList<>();
					for(Individual guideline : guidelinesFromContext) {
						guidelines.add(guideline.getLabel(null));
					}
					parameter.setGuidelineURIs(guidelines);
				}
				
				addObjectInPosition(parameters, pos, parameter);
				//paramIndivURIs.add(parameter.getUri());
			} 
		}
		return parameters;
	}
	
	private void setNamespace (OntModel model, Individual parameterIndiv, Parameter parameter) {
		// get the namespace
		try {
			List<Individual> namespaces = om.getAllObjects(model, parameterIndiv, "has_namespace");
			for (Individual namespace : namespaces) {
				parameter.setNamespace(namespace.getURI());
				Literal namespaceFile = om.getLiteralValue(model, namespace, "has_namespace_file");
				if (namespaceFile != null && namespaceFile.getString() != null) 
					parameter.setNamespaceFile(namespaceFile.getString());
				Literal isShortNamespace = om.getLiteralValue(om.localOntologymodel, namespace, "is_short_namespace");
				if (isShortNamespace != null)
					parameter.setShortNamespace(isShortNamespace.getBoolean());
				return; // there should be a single namespace
			}
		} catch (Exception e) {
			// it might be an old version with a literal namespace
			logger.info("migrating the ontology from old namespace to the new namespace structure");
			Literal namespace = om.getLiteralValue(om.localOntologymodel, parameterIndiv, "has_namespace");
            if (namespace != null && namespace.getString() != null) {
            	if (namespace.getString().equals(XSD.xdouble.getURI())) {  // modify it to the new version 
            		Individual namespaceIndiv = om.getIndividual(model, OntologyManager.baseURI + "double");
    				if (namespaceIndiv == null)   // it does not exist in the ontology, add it first
    					om.createNewIndividualwithURI(model, OntologyManager.NAMESPACE_CLASS_URI, OntologyManager.baseURI + "double", OntologyManager.baseURI + "double");
    				parameter.setNamespace(OntologyManager.baseURI + "double");
            	} else if (namespace.getString().equals(XSD.xstring.getURI())) {
            		Individual namespaceIndiv = om.getIndividual(model, OntologyManager.baseURI + "double");
    				if (namespaceIndiv == null)  // it does not exist in the ontology, add it first
    					om.createNewIndividualwithURI(model, OntologyManager.NAMESPACE_CLASS_URI, OntologyManager.baseURI + "string", OntologyManager.baseURI + "string");
    				parameter.setNamespace(OntologyManager.baseURI + "string");
            	}
            }
            else {
            	logger.error("migration did not work", e);
            	throw e;
            }
		}
	}

	private List<ParameterGroup> getAllParameterGroupsForProtocol (OntModel model, String protocolUri) {
		LinkedList<ParameterGroup> parameterGroups = new LinkedList<>();

		Individual protocolIndiv = om.getIndividual(model, protocolUri);
		List<Individual> parameterGroupIndivs = om.getAllObjects(model, protocolIndiv, "has_parameter_group");
		//	Map<String, ParameterGroup> repeatingGroups = new HashMap<String, ParameterGroup>();
		int groupId = 1;
		for(Individual paramGroup : parameterGroupIndivs) {
			// get the parameter_group and is_required values
			List<Individual> paramGroupsFromContext = om.getAllObjects(model, paramGroup, "parameter_group");
			Literal required = om.getLiteralValue(model, paramGroup, "is_required");	
			if (paramGroupsFromContext != null && paramGroupsFromContext.size() == 1) { 
				Individual paramGroupIndiv = paramGroupsFromContext.get(0);    
				ParameterGroup group = new ParameterGroup();
				group.setUri(paramGroupIndiv.getURI());
				group.setLabel(paramGroupIndiv.getLabel(null));
				group.setDescription(paramGroupIndiv.getComment(null));
				group.setRequired(required.getBoolean());
				group.setId (groupId ++);
				/*if (repeatingGroups.get(group.getLabel()) != null) {
	    			// another occurrence
	    			group.setId(repeatingGroups.get(group.getLabel()).getId() + 1);
	    			repeatingGroups.remove(group.getLabel());
	    			repeatingGroups.put(group.getLabel(), group);
	    		}
	    		else {
	    			//first occurrence
	    			group.setId(1); 
	    			repeatingGroups.put(group.getLabel(), group);
	    		}*/
				Literal position = om.getLiteralValue(model, paramGroup, "has_position");  // position is from the parameter group context
				int pos = 0;
				if (position != null) {
					pos = position.getInt();
					group.setPosition(pos);
				}
				
				List<Individual> guidelinesFromContext = om.getAllObjects(model, paramGroup, "guideline_info");
				if (guidelinesFromContext != null) {
					List<String> guidelines = new ArrayList<>();
					for(Individual guideline : guidelinesFromContext) {
						guidelines.add(guideline.getLabel(null));
					}
					group.setGuidelineURIs(guidelines);
				}
				
				addObjectInPosition(parameterGroups, pos, group);
				group.setParameters(this.getParametersForParameterGroup(model, group));
				//parameterGroups.add(group);
			} 
		}
		return parameterGroups;
	}

	private List<Paper> getAllPapersForProtocol (OntModel model, String protocolUri) {
		List<Paper> papers = new ArrayList<Paper>();
		Individual protocolIndiv = om.getIndividual(model, protocolUri);
		List<Individual> paperIndivs = om.getAllObjects(model, protocolIndiv, "has_reference");
		for (Iterator<Individual> iterator = paperIndivs.iterator(); iterator.hasNext();) {
			Individual paperIndiv = (Individual) iterator.next();
			Paper paper = new Paper();
			String pubmedId = paperIndiv.getLabel(null);
			if (pubmedId != null) {
				try {
					paper.setPubMedId(Integer.parseInt(pubmedId));
				} catch (NumberFormatException e) {
					// invalid pubmedId
					logger.error("Invalid pubmed id for protocol " + protocolUri, e);
				}
			}
			String comment = paperIndiv.getComment(null);
			paper.setFormatedAuthor(comment);
			Literal title = om.getAnnotationValue(model, paperIndiv, "title");
			if (title != null)
				paper.setTitle(title.getString());
			Literal issued = om.getAnnotationValue(model, paperIndiv, "issued");
			if (issued != null) 
				paper.setYear(Integer.parseInt(issued.getString()));
			Literal bibCitation = om.getAnnotationValue(model, paperIndiv, "bibliographicCitation");
			if (bibCitation != null)
				paper.setBibliographicCitation(bibCitation.getString());
			List<Literal> authors = om.getAnnotationValues(model, paperIndiv, "creator");
			for (Iterator<Literal> iterator2 = authors.iterator(); iterator2.hasNext();) {
				Literal literal = (Literal) iterator2.next();
				paper.addAuthor(literal.getString());
			}
			papers.add(paper);
		}
		return papers;
	}

	/**
	 * @param model 
	 * @param group 
	 * @return list of parameters that belong to the given parameter group in the given ontology
	 */
	private List<Parameter> getParametersForParameterGroup(OntModel model, ParameterGroup group) {
		LinkedList<Parameter> parameters = new LinkedList<>();
		Individual parameterGroupIndiv = om.getIndividual(model, group.getUri());
		//List<Individual> parameterIndivs = om.getAllObjects(model, parameterGroupIndiv, "contains_parameter");
		List<Individual> parameterContextIndivs = om.getAllObjects(model, parameterGroupIndiv, "contains_parameter");
		
		for (Individual context : parameterContextIndivs) {
			// get the parameter_group and is_required values
			List<Individual> parametersFromContext = om.getAllObjects(model, context, "subparameter");
			Literal required = om.getLiteralValue(model, context, "is_required");	
			Literal position = om.getLiteralValue(model, context, "has_position");
			List<Individual> guidelinesFromContext = om.getAllObjects(model, context, "guideline_info");
			
			if (parametersFromContext != null && parametersFromContext.size() == 1) { 
				Individual param = parametersFromContext.get(0);  
				Parameter parameter = new Parameter();
				parameter.setUri(param.getURI());
				parameter.setName(param.getLabel(null));
				parameter.setDescription(param.getComment(null));
				if (required != null)
					parameter.setRequired(required.getBoolean());
				parameter.setGroupId(group.getId());
				int pos = 0;
				if (position != null) {
					pos = position.getInt();
					parameter.setPosition(pos);
				}
				List<Individual> units = om.getAllObjects(model, param, "has_unit_of_measurement");
				List<MeasurementUnit> unitList = new ArrayList<>();
				MeasurementUnit mUnit;
				for(Individual unit : units) {
					mUnit = new MeasurementUnit();
					mUnit.setUri(unit.getURI());
					mUnit.setLabel(unit.getLabel(null));
					unitList.add(mUnit);
				}// get the namespace
				setNamespace(model, param, parameter);
				parameter.setAvailableUnits(unitList);
				
				if (guidelinesFromContext != null) {
					List<String> guidelines = new ArrayList<>();
					for(Individual guideline : guidelinesFromContext) {
						guidelines.add(guideline.getLabel(null));
					}
					parameter.setGuidelineURIs(guidelines);
				}
				
				addObjectInPosition(parameters, pos, parameter);
			}
		}
		
		
		/*for(Individual param : parameterIndivs) {
			Parameter parameter = new Parameter();
			parameter.setUri(param.getURI());
			parameter.setName(param.getLabel(null));
			parameter.setDescription(param.getComment(null));
			parameter.setRequired(group.getRequired());
			parameter.setGroupId(group.getId());
			List<Individual> units = om.getAllObjects(model, param, "has_unit_of_measurement");
			List<MeasurementUnit> unitList = new ArrayList<>();
			MeasurementUnit mUnit;
			for(Individual unit : units) {
				mUnit = new MeasurementUnit();
				mUnit.setUri(unit.getURI());
				mUnit.setLabel(unit.getLabel(null));
				unitList.add(mUnit);
			}// get the namespace
			setNamespace(model, param, parameter);
			parameter.setAvailableUnits(unitList);
			Literal position = om.getLiteralValue(param, "has_position");
			int pos = 0;
			if (position != null) {
				pos = position.getInt();
				parameter.setPosition(pos);
			}
			addObjectInPosition(parameters, pos, parameter);
		}*/

		return parameters;
	}

	@Override
	public List<ProtocolEntry> getAllProtocolVariantEntries () throws IOException {
		List<ProtocolEntry> list = ProtocolVariantFileHandler.getAllProtocolVariants(getProtocolVariantFolderLocation());
		// get all from the jar 
		URL url = ExperimentConfig.PROTOCOL_RESOURCE_URL;
		if (url == null) {
			throw new IOException ("No protocol variant folder found in the jar file");
		}
		URL resourceFileUrl = FileLocator.toFileURL(url);
		//		try {
		//			File dir = new File(resourceFileUrl.toURI());
		//			File dir = new File(new URI(resourceFileUrl.toString()));
		list.addAll(ProtocolVariantFileHandler.getAllProtocolVariants(new File(resourceFileUrl.getPath()), true));
		//		} catch (URISyntaxException e) {
		//			logger.error ("No protocol variant folder found in the jar file. URL: " + resourceFileUrl, e);
		//		}
		return list; 
	}

	@Override
	public List<ProtocolNode> getAllProtocolVariants() {
		List<ProtocolNode> protocols = new ArrayList<>();
		try {
			List<ProtocolEntry> entries = getAllProtocolVariantEntries();
			for (Iterator<ProtocolEntry> iterator = entries.iterator(); iterator.hasNext();) {
				ProtocolEntry protocolEntry = (ProtocolEntry) iterator.next();
				String filename = protocolEntry.getFilename();
				String fileLocation= null;
				if (protocolEntry.isFromJar()) {
					// get it from the jar
					URL url = ExperimentConfig.PROTOCOL_RESOURCE_URL;
					if (url != null) {
						URL resourceFileUrl = FileLocator.toFileURL(url);
						//	            		File dir = new File(new URI(resourceFileUrl.toString()));
						fileLocation = resourceFileUrl.getPath() + File.separator + filename;
					}
				}
				else {
					fileLocation = getProtocolVariantFolderLocation() + File.separator + filename;
				}
				if (fileLocation != null) {
					FileInputStream inputStream = new FileInputStream(fileLocation);
					JAXBContext context = JAXBContext.newInstance(ProtocolNode.class);
					Unmarshaller unmarshaller = context.createUnmarshaller();
					ProtocolNode node = (ProtocolNode) unmarshaller.unmarshal(inputStream);
					inputStream.close();

					protocols.add(node);
				}
			}	
		} catch (Exception e) {
			logger.warn(Activator.PLUGIN_ID + " Cannot retrieve protocol variants! ", e);
		}

		return protocols;
	}

	@Override
	public List<ProtocolNode> getProtocolVariantsByUri(String uri) {
		List<ProtocolNode> protocols = new ArrayList<>();
		try {
			List<ProtocolEntry> entries = getAllProtocolVariantEntries();
			for (Iterator<ProtocolEntry> iterator = entries.iterator(); iterator.hasNext();) {
				ProtocolEntry protocolEntry = (ProtocolEntry) iterator.next();
				if (protocolEntry.getUri().equalsIgnoreCase(uri)) {
					String filename = protocolEntry.getFilename();
					String fileLocation= null;
					if (protocolEntry.isFromJar()) {
						// get it from the jar
						URL url = ExperimentConfig.PROTOCOL_RESOURCE_URL;
						if (url != null) {			
							URL resourceFileUrl = FileLocator.toFileURL(url);
							File dir = new File(resourceFileUrl.toURI());
							fileLocation = dir.getAbsolutePath();
						}
					}
					else {
						fileLocation = getProtocolVariantFolderLocation() + File.separator + filename;
					}
					if (fileLocation != null) {	
						FileInputStream inputStream = new FileInputStream(fileLocation);
						JAXBContext context = JAXBContext.newInstance(ProtocolNode.class);
						Unmarshaller unmarshaller = context.createUnmarshaller();
						ProtocolNode node = (ProtocolNode) unmarshaller.unmarshal(inputStream);
						inputStream.close();
						protocols.add(node);
					}
				}
			}	
		} catch (Exception e) {
			logger.warn(Activator.PLUGIN_ID + " Cannot retrieve protocol variants with given uri! ",  e);
		}

		return protocols;
	}

	@Override
	/**
	 * @return the uri of the protocol template
	 */
	public String createProtocolTemplate(ProtocolNode protocol, String paletteCategoryURI) throws Exception {
		ProtocolCategory topLevel = new ProtocolCategory();
		topLevel.setUri(paletteCategoryURI);
		// check if there is already a protocol with given name within the same top-level category
		List<ProtocolNode> existing = getProtocolsByLabel(protocol.getLabel());
		if (!existing.isEmpty()) {
			// check if the existing belongs to the given top level category
			for (ProtocolNode protocolNode : existing) {
				ProtocolCategory paletteCat = getTopLevelCategoryForProtocol(protocolNode);
				if (paletteCat != null && paletteCat.getUri().equals(paletteCategoryURI))
					throw new ProtocolExistsException("A protocol template named " + protocol.getLabel() + " already exists in " + paletteCategoryURI, protocolNode.getTemplateUri());
			}
		}

		ProtocolCategory category = protocol.getCategory();
		Individual categoryIndiv;
		if (category != null) {
			categoryIndiv = om.getIndividual(om.localOntologymodel, category.getUri());
			if (categoryIndiv == null) {
				categoryIndiv = om.getIndividual(om.standardOntologymodel, category.getUri());
				if (categoryIndiv == null) 
					throw new Exception ("Cannot add the template. Category not found in ontology");
				// add it to the local ontology
				om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PROTOCOLCATEGORY_CLASS_URI, category.getName(), categoryIndiv.getURI());
			}

		} else {
			throw new Exception ("Cannot add the template. Category is missing!");
		}
		// create an individual for "protocol" class
		// append random string to the end to make sure it does not clash with any other resource
		String indivUri = om.createUniqueRandomIndividualURI(protocol.getLabel());  
		om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PROTOCOL_CLASS_URI, protocol.getLabel(), indivUri);
		om.addComment(om.localOntologymodel, indivUri, protocol.getDescription());
		if (protocol.getCreator() != null)
			om.addAnnotation(om.localOntologymodel, indivUri, "creator", protocol.getCreator());
		if (protocol.getUrl() != null) 
			om.addLiteral(om.localOntologymodel, indivUri, "has_url", om.localOntologymodel.createLiteral(protocol.getUrl()));
		if (protocol.getFile() != null)
			om.addLiteral(om.localOntologymodel, indivUri, "has_file", om.localOntologymodel.createLiteral(protocol.getFile()));

		// add an object property "protocol -> has_category -> ProtocolCategory"
		om.addProperty(om.localOntologymodel, indivUri, "has_category", categoryIndiv.getURI());	

		// add the papers
		List<Paper> papers = protocol.getPapers();
		if (papers != null) {
			for (Iterator<Paper> iterator = papers.iterator(); iterator.hasNext();) {
				Paper paper = (Paper) iterator.next();
				String paperUri = addPaperToTemplate (paper);
				om.addProperty(om.localOntologymodel, indivUri, "has_reference", paperUri);
			}
		}
		// add the parameters
		List<Parameter> parameters = protocol.getParameters();
		if (parameters != null) {
			for (Iterator<Parameter> iterator = parameters.iterator(); iterator.hasNext();) {
				Parameter parameter = (Parameter) iterator.next();
				String contextUri = addParameterToTemplate(parameter, protocol.getLabel(), indivUri);
				// add the context to the protocol
				om.addProperty(om.localOntologymodel, indivUri, "has_parameter", contextUri);
			}    
		}
		// add the parameter_groups
		List<ParameterGroup> parameterGroups = protocol.getParameterGroups();
		if (parameterGroups != null) {
			for (Iterator<ParameterGroup> iterator = parameterGroups.iterator(); iterator
					.hasNext();) {
				ParameterGroup parameterGroup = (ParameterGroup) iterator
						.next();
				addParameterGroupToTemplate (parameterGroup, protocol.getLabel(), indivUri);
			}   
		}

		// change the version
		Ontology localOnt = om.localOntologymodel.getOntology(OntologyManager.ontURI);
		String version = localOnt.getVersionInfo();
		if (version != null) {
			try {
				String firstDigit = version.substring(0, version.lastIndexOf("."));
				int secondDigit = Integer.parseInt(version.substring(version.lastIndexOf(".")+1));
				localOnt.setVersionInfo(firstDigit + "." + ++secondDigit);
			} catch (NumberFormatException e) {
				// cannot get the second digit from the version number, format is invalid
				logger.warn("Cannot increase the version number of the local ontology", e);
			}
		}

		FileOutputStream outStream = new FileOutputStream(new File (ExperimentDesignOntologyAPI.getOntologyLocation(ExperimentConfig.LOCAL_ONTOLOGY_FILE_NAME)));
		om.writeOntology(om.localOntologymodel, outStream );

		return indivUri;
	}

	private String addPaperToTemplate(Paper paper) throws UnsupportedEncodingException {
		String paperUri;
		if (paper.getPubMedId() == null) {
			paperUri = om.createUniqueRandomIndividualURI("paper");
			om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PAPER_CLASS_URI, paperUri, paperUri);
		}
		else
			paperUri = om.createNewIndividual(om.localOntologymodel, OntologyManager.PAPER_CLASS_URI, String.valueOf(paper.getPubMedId()));
		om.addComment(om.localOntologymodel, paperUri, paper.getFormatedAuthor());
		if (paper.getPubMedId() != null)
			om.addProperty(om.localOntologymodel, paperUri, "has_pubmed_id", String.valueOf(paper.getPubMedId()));
		if (paper.getTitle() != null)
			om.addAnnotation(om.localOntologymodel, paperUri, "title", paper.getTitle());
		if (paper.getBibliographicCitation() != null)
			om.addAnnotation(om.localOntologymodel, paperUri, "bibliographicCitation", paper.getBibliographicCitation());
		if (paper.getYear() != null)
			om.addAnnotation(om.localOntologymodel, paperUri, "issued", String.valueOf(paper.getYear()));
		List<String> authors = paper.getAuthors();
		if (authors != null) {
			for (Iterator<String> iterator = authors.iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				om.addAnnotation(om.localOntologymodel, paperUri, "creator", string);
			}
		}
		return paperUri;	
	}

	private void addParameterGroupToTemplate(ParameterGroup parameterGroup,
			String label, String uri) {
		// create a context for the parameter group
		String contextUri = om.createUniqueRandomIndividualURI ("Parameter Group Context");
		om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PARAMETER_GROUPCONTEXT_CLASS_URI, label + "_" + parameterGroup.getLabel(), contextUri);

		// add the context to the protocol
		om.addProperty(om.localOntologymodel, uri, "has_parameter_group", contextUri);

		Individual parameterGroupIndiv = om.getIndividual(om.localOntologymodel, parameterGroup.getUri());
		if (parameterGroupIndiv == null) {
			// it does not exist in local ontology, add it first
			om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PARAMETERGROUP_CLASS_URI, parameterGroup.getLabel(), parameterGroup.getUri());
			if (parameterGroup.getDescription() != null)
				om.addComment(om.localOntologymodel, parameterGroup.getUri(), parameterGroup.getDescription());
		}

		om.addProperty(om.localOntologymodel, contextUri, "parameter_group", parameterGroup.getUri());
		Boolean isRequired = parameterGroup.getRequired();
		if (isRequired != null && isRequired) {
			om.addLiteral(om.localOntologymodel, contextUri, "is_required", om.localOntologymodel.createTypedLiteral(true));
		}
		else {
			om.addLiteral(om.localOntologymodel, contextUri, "is_required", om.localOntologymodel.createTypedLiteral(false));
		}
		
		Integer position = parameterGroup.getPosition();
		if (position != null) {
			Literal positionLiteral = om.localOntologymodel.createLiteral(position+"");
			om.addLiteral(om.localOntologymodel, contextUri, "has_position", positionLiteral);
		}
		
		List<String> guidelines = parameterGroup.getGuidelineURIs();
		if (guidelines != null) {
			for (String guideline : guidelines) {
				String guidelineUri = OntologyManager.GUIDELINE_CLASS_URI + guideline.toLowerCase().replaceAll(" ", "_");
				om.createNewIndividualwithURI(om.localOntologymodel, 
						OntologyManager.GUIDELINE_CLASS_URI, guideline, guidelineUri);
				om.addProperty(om.localOntologymodel, contextUri, "guideline_info", guidelineUri);
			}
		}

		// handle the parameters of the group
		List<Parameter> parameters = parameterGroup.getParameters();
		for (Iterator<Parameter> iterator = parameters.iterator(); iterator.hasNext();) {
			Parameter parameter = (Parameter) iterator.next();
			/*String parameterContextUri = getContextForParameterInTemplate(parameter.getUri(), uri);
			if (parameterContextUri == null) {
				parameterContextUri = addParameterToTemplate (parameter, label, uri);
			}	*/
			String paramInParamGroupContextUri = addParameterToParameterGroup (parameter, parameterGroup);
			om.addProperty(om.localOntologymodel, parameterGroup.getUri(), "contains_parameter", paramInParamGroupContextUri);
		}
	}

	private String addParameterToParameterGroup(Parameter parameter, ParameterGroup parameterGroup) {
		// create a context for the parameter
		String contextUri = om.createUniqueRandomIndividualURI ("Parameter In Group Context");
		om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PARAMETERINGROUPCONTEXT_CLASS_URI, parameterGroup.getLabel() + "_" + parameter.getName(), contextUri);

		Individual parameterIndiv = om.getIndividual(om.localOntologymodel, parameter.getUri());
		if (parameterIndiv == null) {
			// it does not exist in local ontology, add it first
			om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PARAMETER_CLASS_URI, parameter.getName(), parameter.getUri());
			if (parameter.getDescription() != null)
				om.addComment(om.localOntologymodel, parameter.getUri(), parameter.getDescription());
			// add the namespace
			if (parameter.getNamespace() != null) {
				Individual namespace = om.getIndividual(om.localOntologymodel, parameter.getNamespace());
				if (namespace == null) {  // it does not exist in the local ontology, add it first
					// check if the namespace class is defined
					OntClass namespaceClass = om.localOntologymodel.getOntClass(OntologyManager.NAMESPACE_CLASS_URI);
					if (namespaceClass == null) { // old version, namespace class does not exists yet
						// need to fix the namespace classes/properties in the ontology
						handleOldVersionForNamespace ();
					}
					om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.NAMESPACE_CLASS_URI, parameter.getNamespace(), parameter.getNamespace());
					if (parameter.getNamespaceFile() != null) {
						om.addLiteral(om.localOntologymodel, parameter.getNamespace(), "has_namespace_file", om.localOntologymodel.createLiteral(parameter.getNamespaceFile()));
					}
					om.addLiteral(om.localOntologymodel, parameter.getNamespace(), "is_short_namespace", om.localOntologymodel.createTypedLiteral(parameter.getShortNamespace()));
				}
				om.addProperty(om.localOntologymodel, parameter.getUri(), "has_namespace", parameter.getNamespace());
			}
		}

		om.addProperty(om.localOntologymodel, contextUri, "subparameter", parameter.getUri());
		Boolean isRequired = parameter.getRequired();
		if (isRequired != null && isRequired) {
			om.addLiteral(om.localOntologymodel, contextUri, "is_required", om.localOntologymodel.createTypedLiteral(true));
		}
		else {
			om.addLiteral(om.localOntologymodel, contextUri, "is_required", om.localOntologymodel.createTypedLiteral(false));
		}
		
		Integer position = parameter.getPosition();
		if (position != null) {
			Literal positionLiteral = om.localOntologymodel.createLiteral(position+"");
			om.addLiteral(om.localOntologymodel, contextUri, "has_position", positionLiteral);
		}
		// handle unit of measurements
		List<MeasurementUnit> units = parameter.getAvailableUnits();

		for (Iterator<MeasurementUnit> iterator2 = units.iterator(); iterator2.hasNext();) {
			MeasurementUnit measurementUnit = (MeasurementUnit) iterator2
					.next();
			Individual unitIndiv = om.getIndividual(om.localOntologymodel, measurementUnit.getUri());
			if (unitIndiv == null) {
				// it does not exist in local ontology, add it first
				om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.UNIT_URI, measurementUnit.getLabel(), measurementUnit.getUri());
			}

			om.addProperty(om.localOntologymodel, parameter.getUri(), "has_unit_of_measurement", measurementUnit.getUri());
		}
		
		List<String> guidelines = parameter.getGuidelineURIs();
		if (guidelines != null) {
			for (String guideline : guidelines) {
				String guidelineUri = OntologyManager.GUIDELINE_CLASS_URI + guideline.toLowerCase().replaceAll(" ", "_");
				om.createNewIndividualwithURI(om.localOntologymodel, 
						OntologyManager.GUIDELINE_CLASS_URI, guideline, guidelineUri);
				om.addProperty(om.localOntologymodel, contextUri, "guideline_info", guidelineUri);
			}
		}

		return contextUri;
	}

	/**
	 * It returns parameter context uri for the parameter in 
	 * the template or null if does not exists
	 * @param parameterUri uri of the parameter
	 * @param protocolTemplateUri uri of the protocol template
	 * @return corresponding parameter context uri
	 */
	private String getContextForParameterInTemplate(String parameterUri, String protocolTemplateUri)
	{
		String parameterContextURI = null;
		RDFNode thisContextValue = null;
		Resource thisParameterResource = null;
		Individual templateIndividual = om.standardOntologymodel.getIndividual(protocolTemplateUri) == null ? 
				om.standardOntologymodel.getIndividual(protocolTemplateUri) :
					om.localOntologymodel.getIndividual(protocolTemplateUri);
				if(templateIndividual != null)
				{
					NodeIterator contextValues = templateIndividual.listPropertyValues(
							om.standardOntologymodel.getProperty(OntologyManager.baseURI + "has_parameter"));
					while(contextValues.hasNext())
					{
						thisContextValue = contextValues.next();
						if(thisContextValue.isResource())
						{
							thisParameterResource = thisContextValue.asResource()
									.getPropertyResourceValue(om.standardOntologymodel.getProperty("parameter"));
							if(thisParameterResource.getURI().equals(parameterUri))
							{
								parameterContextURI = thisContextValue.asResource().getURI();
								break;
							}
						}
					}
				}
				return parameterContextURI;
	}

	/**
	 * 
	 * @param parameter
	 * @param protocolLabel
	 * @param protocolUri
	 * @return parameterContextURI
	 */
	private String addParameterToTemplate (Parameter parameter, String protocolLabel, String protocolUri) {
		// create a context for the parameter
		String contextUri = om.createUniqueRandomIndividualURI ("Parameter Context");
		om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PARAMETERCONTEXT_CLASS_URI, protocolLabel + "_" + parameter.getName(), contextUri);

		Individual parameterIndiv = om.getIndividual(om.localOntologymodel, parameter.getUri());
		if (parameterIndiv == null) {
			// it does not exist in local ontology, add it first
			om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.PARAMETER_CLASS_URI, parameter.getName(), parameter.getUri());
			if (parameter.getDescription() != null)
				om.addComment(om.localOntologymodel, parameter.getUri(), parameter.getDescription());
			// add the namespace
			if (parameter.getNamespace() != null) {
				Individual namespace = om.getIndividual(om.localOntologymodel, parameter.getNamespace());
				if (namespace == null) {  // it does not exist in the local ontology, add it first
					// check if the namespace class is defined
					OntClass namespaceClass = om.localOntologymodel.getOntClass(OntologyManager.NAMESPACE_CLASS_URI);
					if (namespaceClass == null) { // old version, namespace class does not exists yet
						// need to fix the namespace classes/properties in the ontology
						handleOldVersionForNamespace ();
					}
					om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.NAMESPACE_CLASS_URI, parameter.getNamespace(), parameter.getNamespace());
					if (parameter.getNamespaceFile() != null) {
						om.addLiteral(om.localOntologymodel, parameter.getNamespace(), "has_namespace_file", om.localOntologymodel.createLiteral(parameter.getNamespaceFile()));
					}
					om.addLiteral(om.localOntologymodel, parameter.getNamespace(), "is_short_namespace", om.localOntologymodel.createTypedLiteral(parameter.getShortNamespace()));
				}
				om.addProperty(om.localOntologymodel, parameter.getUri(), "has_namespace", parameter.getNamespace());
			}
		}

		om.addProperty(om.localOntologymodel, contextUri, "parameter", parameter.getUri());
		Boolean isRequired = parameter.getRequired();
		if (isRequired != null && isRequired) {
			om.addLiteral(om.localOntologymodel, contextUri, "is_required", om.localOntologymodel.createTypedLiteral(true));
		}
		else {
			om.addLiteral(om.localOntologymodel, contextUri, "is_required", om.localOntologymodel.createTypedLiteral(false));
		}
		
		Integer position = parameter.getPosition();
		if (position != null) {
			Literal positionLiteral = om.localOntologymodel.createLiteral(position+"");
			om.addLiteral(om.localOntologymodel, contextUri, "has_position", positionLiteral);
		}
		// handle unit of measurements
		List<MeasurementUnit> units = parameter.getAvailableUnits();

		for (Iterator<MeasurementUnit> iterator2 = units.iterator(); iterator2.hasNext();) {
			MeasurementUnit measurementUnit = (MeasurementUnit) iterator2
					.next();
			Individual unitIndiv = om.getIndividual(om.localOntologymodel, measurementUnit.getUri());
			if (unitIndiv == null) {
				// it does not exist in local ontology, add it first
				om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.UNIT_URI, measurementUnit.getLabel(), measurementUnit.getUri());
			}

			om.addProperty(om.localOntologymodel, parameter.getUri(), "has_unit_of_measurement", measurementUnit.getUri());
		}
		
		List<String> guidelines = parameter.getGuidelineURIs();
		if (guidelines != null) {
			for (String guideline : guidelines) {
				String guidelineUri = OntologyManager.GUIDELINE_CLASS_URI + guideline.toLowerCase().replaceAll(" ", "_");
				om.createNewIndividualwithURI(om.localOntologymodel, 
						OntologyManager.GUIDELINE_CLASS_URI, guideline, guidelineUri);
				om.addProperty(om.localOntologymodel, contextUri, "guideline_info", guidelineUri);
			}
		}

		return contextUri;
	}
	
	private void handleOldVersionForNamespace () {
		om.localOntologymodel.createClass(OntologyManager.NAMESPACE_CLASS_URI);
		DatatypeProperty namespaceProperty = om.localOntologymodel.getDatatypeProperty(OntologyManager.baseURI + "has_namespace");
		if (namespaceProperty == null) { // has_namespace does not exist
			ObjectProperty prop = om.localOntologymodel.createObjectProperty("has_namespace");
			prop.setDomain(om.localOntologymodel.getOntClass(OntologyManager.PARAMETER_CLASS_URI));
			prop.setRange(om.localOntologymodel.getOntClass(OntologyManager.NAMESPACE_CLASS_URI));
		} else {
			ObjectProperty prop = om.localOntologymodel.getObjectProperty(OntologyManager.baseURI + "has_namespace");
			if (prop == null) {  // old version, datatype property
				// need to remove the old datatype property and add the new object property
				List<Individual> allParameterIndividuals = om.getAllIndiviudalsOfClass(om.localOntologymodel, OntologyManager.PARAMETER_CLASS_URI);
				Map<String, String> previousNameSpaces = new HashMap<>();
				for (Individual individual : allParameterIndividuals) {
					Literal oldNamespace = om.getLiteralValue(individual, "has_namespace");
					if (oldNamespace != null) {
						previousNameSpaces.put(individual.getURI(), oldNamespace.getString());
						individual.removeProperty(namespaceProperty, oldNamespace);
					}
					
				}
				
				namespaceProperty.remove();
				prop = om.localOntologymodel.createObjectProperty(OntologyManager.baseURI + "has_namespace");
				prop.setDomain(om.localOntologymodel.getOntClass(OntologyManager.PARAMETER_CLASS_URI));
				prop.setRange(om.localOntologymodel.getOntClass(OntologyManager.NAMESPACE_CLASS_URI));
				
				for (Iterator<String> iterator = previousNameSpaces.keySet().iterator(); iterator.hasNext();) {
					String paramUri = (String) iterator.next();
					String oldNamespace = previousNameSpaces.get(paramUri);
					if (oldNamespace.equals(XSD.xdouble.getURI())) {
						om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.NAMESPACE_CLASS_URI, OntologyManager.baseURI + "double", OntologyManager.baseURI + "double");
						om.addProperty(om.localOntologymodel, paramUri, "has_namespace", OntologyManager.baseURI + "double");
					} else if (oldNamespace.equals(XSD.xstring.getURI())) {
						om.createNewIndividualwithURI(om.localOntologymodel, OntologyManager.NAMESPACE_CLASS_URI, OntologyManager.baseURI + "string", OntologyManager.baseURI + "string");
						om.addProperty(om.localOntologymodel, paramUri, "has_namespace", OntologyManager.baseURI + "string");
					}
				}
			}
		}
	}

	@Override
	public void createProtocolVariant (ProtocolNode protocol) throws Exception {
		String configFolderLocation = getConfigFolderLocation();
		String protocolVariantLocation = getProtocolVariantFolderLocation();
		String protocolFileName;
		File[] existingFiles = getConfigFilesByDirectory(ExperimentConfig.PROTOCOLVARIANT_LOCATION);
		boolean indexFileExists = false;
		if (existingFiles != null) {
			String[] fileNames = new String[existingFiles.length];
			for (int i = 0; i < existingFiles.length; i++) {
				fileNames[i] = existingFiles[i].getName();
				if (existingFiles[i].getName().equals (ExperimentConfig.PROTOCOLVARIANT_INDEXFILE)) {
					indexFileExists = true;
				}
			}
			if (!indexFileExists) {// should not happen normally but if someone deletes the index file in the file system
				//TODO what to do with the existing files in the folder?
				ProtocolVariantFileHandler.createIndexFile(protocolVariantLocation);
			}
			protocolFileName = generateFileName(fileNames, ExperimentConfig.PROTOCOLVARIANT_FILE_NAME_PREFIX);
		} else {
			protocolFileName = generateFileName(new String[1], ExperimentConfig.PROTOCOLVARIANT_FILE_NAME_PREFIX);
		}

		File configFolder = new File(configFolderLocation);
		if(configFolder.isDirectory())
		{
			File protocolFolder = new File (protocolVariantLocation);
			if (!protocolFolder.exists()) {
				// create it
				protocolFolder.mkdirs();
				// need to create the index file under it
				ProtocolVariantFileHandler.createIndexFile(protocolVariantLocation);
			}

			ProtocolEntry entry = new ProtocolEntry();
			entry.setCategory(protocol.getCategory().getUri());
			entry.setName(protocol.getLabel());
			entry.setUri(protocol.getTemplateUri());
			entry.setFilename(protocolFileName);

			ProtocolVariantFileHandler.addProtocolVariant(entry, protocolVariantLocation);

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			JAXBContext context = JAXBContext.newInstance(ProtocolNode.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, PropertyHandler.GRITS_CHARACTER_ENCODING);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(protocol, os);

			//write the serialized data to the folder
			FileWriter fileWriter = new FileWriter(protocolFolder.getAbsolutePath() 
					+ File.separator + protocolFileName);
			fileWriter.write(os.toString((String)marshaller.getProperty(Marshaller.JAXB_ENCODING)));
			fileWriter.close();
			os.close();
		} else {
			throw new IOException ("Configuration folder does not exist");
		}
	}


	@Override
	public List<ExperimentTemplateEntry> getAllExperimentTemplateEntries () throws IOException {
		List<ExperimentTemplateEntry> entries = ExperimentTemplateFileHandler.getAllTemplates(getTemplateFolderLocation());

		// get the templates from the jar as well
		URL url = ExperimentConfig.EXPERIMET_TEMPLATE_RESOURCE_URL;
		if (url == null) {
			throw new IOException ("No experiment template folder found in the jar file");
		}
		URL resourceFileUrl = FileLocator.toFileURL(url);
		try {
			File dir = new File(resourceFileUrl.getPath());
			entries.addAll(ExperimentTemplateFileHandler.getAllTemplates(dir, true));
		} catch (Exception e) {
			throw new IOException ("No experiment template folder found in the jar file");
		}
		return entries;
	}

	@Override
	public void createTemplateForExperimentGraph(
			ExperimentGraph experimentDesign) throws Exception {

		// generate a uri for experiment design
		String nameWithoutSpaces = experimentDesign.getName().replace(" ", "");
		String uri = OntologyManager.baseURI + URLEncoder.encode(nameWithoutSpaces, PropertyHandler.GRITS_CHARACTER_ENCODING);
		experimentDesign.setUri(uri);
		String templateFileName;

		String configFolderLocation = getConfigFolderLocation();
		String templateFolderLocation = getTemplateFolderLocation();

		File[] existingFiles = getConfigFilesByDirectory(ExperimentConfig.EXPERIMENT_TEMPLATE_LOCATION);
		boolean indexFileExists = false;
		if (existingFiles != null) {
			String[] fileNames = new String[existingFiles.length];
			for (int i = 0; i < existingFiles.length; i++) {
				fileNames[i] = existingFiles[i].getName();
				if (existingFiles[i].getName().equals (ExperimentConfig.EXPERIMENTTEMPLATE_INDEXFILE)) {
					indexFileExists = true;
				}
			}
			if (!indexFileExists) {// should not happen normally but if someone deletes the index file in the file system
				//TODO what to do with the existing files in the folder?
				ExperimentTemplateFileHandler.createTemplateIndexFile(templateFolderLocation);
			}
			templateFileName = generateFileName(fileNames, ExperimentConfig.TEMPLATE_FILE_NAME_PREFIX);
		} else {
			templateFileName = generateFileName(new String[1], ExperimentConfig.TEMPLATE_FILE_NAME_PREFIX);
		}

		File configFolder = new File(configFolderLocation);
		if(configFolder.isDirectory())
		{
			File templateFolder = new File (templateFolderLocation);
			if (!templateFolder.exists()) {
				// create it
				templateFolder.mkdirs();
				// need to create the index file under it
				ExperimentTemplateFileHandler.createTemplateIndexFile(templateFolderLocation);
			}

			ExperimentTemplateEntry entry = new ExperimentTemplateEntry();
			entry.setCreator(experimentDesign.getCreatedBy());
			entry.setName(experimentDesign.getName());
			entry.setDescription(experimentDesign.getDescription());
			entry.setDateCreated(experimentDesign.getDateCreated());
			entry.setFilename(templateFileName);

			ExperimentTemplateFileHandler.addTemplate(entry, templateFolderLocation);

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			JAXBContext context = JAXBContext.newInstance(ExperimentGraph.class);
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_ENCODING, PropertyHandler.GRITS_CHARACTER_ENCODING);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(experimentDesign, os);

			//write the serialized data to the folder
			FileWriter fileWriter = new FileWriter(templateFolder.getAbsolutePath() 
					+ File.separator + templateFileName);
			fileWriter.write(os.toString((String)marshaller.getProperty(Marshaller.JAXB_ENCODING)));
			fileWriter.close();
			os.close();
		} else {
			throw new IOException ("Configuration folder does not exist");
		}
	}

	private static String generateFileName(String[] existingNames, String prefix)
	{
		String fileName = "";
		int randomLength = 0;
		do 
		{
			fileName = prefix;
			while(randomLength < ExperimentConfig.FILE_NAME_RANDOM_CHARACTERS_LENGTH) 
			{
				int randomcharacter = (int) (Math.random()*10);
				randomLength++;
				fileName = fileName + randomcharacter;
			}
			fileName = fileName + ExperimentConfig.FILE_TYPE_OF_EXPERIMENT;
		}
		while (Arrays.asList(existingNames).contains(fileName));
		return fileName;
	}

	public List<MeasurementUnit> getAllMeasurementUnits()
	{
		List<MeasurementUnit> measurementUnits = new ArrayList<MeasurementUnit>();
		MeasurementUnit measurementUnit = null;
		OntClass unitResource = om.standardOntologymodel.getOntClass(OntologyManager.UNIT_URI);
		Individual unitIndiv = null;
		ExtendedIterator<Individual> extendedIterator = om.standardOntologymodel.listIndividuals(unitResource);
		while(extendedIterator.hasNext())
		{
			unitIndiv = extendedIterator.next();
			measurementUnit = new MeasurementUnit();
			measurementUnit.setLabel(unitIndiv.getLabel(null));
			measurementUnit.setUri(unitIndiv.getURI());
			measurementUnits.add(measurementUnit);
		}
		extendedIterator = om.localOntologymodel.listIndividuals(unitResource);
		while(extendedIterator.hasNext())
		{
			unitIndiv = extendedIterator.next();
			measurementUnit = new MeasurementUnit();
			measurementUnit.setLabel(unitIndiv.getLabel(null));
			measurementUnit.setUri(unitIndiv.getURI());
			measurementUnits.add(measurementUnit);
		}
		return measurementUnits;
	}

	public void createMeasurementUnit(MeasurementUnit measurementUnit) throws AlreadyExistsException, UnsupportedEncodingException
	{
		if(om.localOntologymodel.getOntResource(measurementUnit.getUri()) != null)
			throw new AlreadyExistsException(measurementUnit.getUri());
		Individual unitIndiv = om.localOntologymodel.createIndividual(measurementUnit.getUri(), 
				om.localOntologymodel.getResource(OntologyManager.UNIT_URI));
		om.localOntologymodel.add(unitIndiv.asResource(), RDFS.label, measurementUnit.getLabel());
	}

	public boolean isExistingURI(String uri)
	{
		return om.localOntologymodel.getOntResource(uri) != null 
				|| om.standardOntologymodel.getOntResource(uri) != null;
	}

	public void createNamespace(String namespaceUri, String namespaceLabel,
			String newFileName) throws AlreadyExistsException
	{
		if(isExistingURI(namespaceUri))
			throw new AlreadyExistsException("Cannot create this namespace : "
					+ namespaceLabel + "\nThe uri already exists : " + namespaceUri);

		Individual namespaceIndiv = om.localOntologymodel.createIndividual(namespaceUri, 
				om.localOntologymodel.getResource(OntologyManager.NAMESPACE_CLASS_URI));
		om.localOntologymodel.add(namespaceIndiv.asResource(), RDFS.label, namespaceLabel);

		if(newFileName != null)
		{
			om.addLiteral(om.localOntologymodel,
					namespaceUri, "has_namespace_file", om.localOntologymodel.createLiteral(newFileName));

			om.addLiteral(om.localOntologymodel,
					namespaceUri, "is_short_namespace", om.localOntologymodel.createTypedLiteral(true));
		}
	}

	public Set<String> getAllShortNamespaceFileNames()
	{
		Set<String> shortNamespaceFileNames = new HashSet<String>();
		shortNamespaceFileNames.addAll(getShortNamespaceFileNames(om.standardOntologymodel));
		return shortNamespaceFileNames;
	}

	private Set<String> getShortNamespaceFileNames(OntModel model)
	{
		Set<String> shortNamespaceFileNames = new HashSet<String>();
		OntClass namespaceResource = model.getOntClass(OntologyManager.NAMESPACE_CLASS_URI);
		Individual namespaceIndiv = null;
		ExtendedIterator<Individual> extendedIterator = model.listIndividuals(namespaceResource);
		Literal shortLiteralValue = null;
		Literal fileNameLiteral = null;
		while(extendedIterator.hasNext())
		{
			namespaceIndiv = extendedIterator.next();
			shortLiteralValue = om.getLiteralValue(model, namespaceIndiv, "is_short_namespace");
			if(shortLiteralValue != null && shortLiteralValue.getBoolean())
			{
				fileNameLiteral = om.getLiteralValue(model, namespaceIndiv, "has_namespace_file");
				if(fileNameLiteral != null && fileNameLiteral.getString() != null)
				{
					shortNamespaceFileNames.add(fileNameLiteral.getString());
				}
			}
		}
		return shortNamespaceFileNames;
	}

	/**
	 * adds existing category to an existing protocol template.
	 * Assumes both of them exist in the local ontology
	 * @param protocolUri
	 * @param categoryUri
	 * @throws Exception 
	 */
	public void addCategoryForProtocol(String protocolUri, String categoryUri) throws Exception
	{
		Individual protcolIndiv = om.localOntologymodel.getIndividual(protocolUri);
		if(protcolIndiv == null || protcolIndiv.getOntClass() == null 
				|| !OntologyManager.PROTOCOL_CLASS_URI.equals(protcolIndiv.getOntClass().getURI()))
			throw new Exception("No Such Protocol Exists in the local ontology : " + protocolUri);

		Individual categoryIndiv = om.localOntologymodel.getIndividual(categoryUri);
		if(categoryIndiv == null || categoryIndiv.getOntClass() == null 
						|| !OntologyManager.PROTOCOLCATEGORY_CLASS_URI.equals(categoryIndiv.getOntClass().getURI()))
			throw new Exception("No Such Category Exists in the local ontology : " + categoryUri);

		om.addProperty(om.localOntologymodel, protocolUri, "has_category", categoryUri);
	}

	/**
	 * get version number of standard ontology
	 * @return ontology version
	 */
	public String getStandardOntologyVersion()
	{
		return om.standardOntologymodel.getOntology(OntologyManager.ontURI).getVersionInfo();  
	}

	/**
	 * get version number of local ontology
	 * @return local ontology version
	 */
	public String getLocalOntologyVersion()
	{
		return om.localOntologymodel.getOntology(OntologyManager.ontURI).getVersionInfo();
	}

	
	/**
	 * create a category (in the local ontology) under the given parent palette category with the given icon (color)
	 * 
	 * @param label category to be added
	 * @param parentURI URI of the PaletteCategory 
	 * @param icon icon should have the name in the format R-G-B.gif (where RGB are the color codes), 
	 * the icon image should already be in "icons/16" folder of the plugin
	 * @param description description of the category
	 * @param position position of the category within the parent category
	 * @return ProtocolCategory object for the newly created category
	 * @throws Exception if the parentURI does not refer to an existing category
	 */
	@Override
	public ProtocolCategory createCategory(String label, String parentURI, String icon, String description, Integer position) throws Exception {
		boolean exists = false;
		List<ProtocolCategory> existing = getProtocolCategories();
		for (ProtocolCategory protocolCategory : existing) {
			if (protocolCategory.equals(label)) {
				exists = true;
				break;
			}	
		}
		//if (om.getIndividual(om.localOntologymodel, uri) != null)
		if (exists)
			throw new ProtocolExistsException("A category named " + label + "already exists!");
		Individual parent = om.getIndividual(om.localOntologymodel, parentURI);
		if (parent == null) {
			// check if it exists in the standard ontology, if so create it in the local ontology as well
			parent = om.getIndividual(om.standardOntologymodel, parentURI);
			if (parent != null) {
				int pos = 1;
				// add it to local ontology
				List<ProtocolCategory> categories = getTopLevelCategories();
				if (categories != null)
					pos = categories.size() + 1;
				createPaletteCategory(parent.getLabel(null), pos);
			} else {
				throw new Exception ("Cannot add the category. PaletteCategory" + parentURI + " not found in ontology");
			}
		}
		// create an individual for "protocolCategory" class
		String indivUri = om.createNewIndividual(om.localOntologymodel, OntologyManager.PROTOCOLCATEGORY_CLASS_URI, label);
		// add the category to the parent as a sub category
		om.addProperty(om.localOntologymodel, parent.getURI(), "has_subCategory", indivUri);
		// add the icon
		Literal iconLit = om.localOntologymodel.createLiteral(icon);
		om.addLiteral(om.localOntologymodel, indivUri, "has_icon", iconLit);
		// add the position
		if (position != null) {
			Literal positionLiteral = om.localOntologymodel.createLiteral(position+"");
			om.addLiteral(om.localOntologymodel, indivUri, "has_position", positionLiteral);
		}
		// add the description
		if (description != null && !description.isEmpty())
			om.addComment(om.localOntologymodel, indivUri, description);
		
		Individual categoryIndiv = om.getIndividual(om.localOntologymodel, indivUri);
		ProtocolCategory category = new ProtocolCategory ();
		category.setUri(indivUri);
		category.setName(label);
		if (position != null)
			category.setPosition(position);
		if (iconLit != null) {
			category.setIcon(iconLit.getString());
		}
		category.setColor(getColorForCategory(categoryIndiv)); 
		category.setDescription (categoryIndiv.getComment(null));
		return category;
	}

	/**
	 * create a palette category (in the local ontology)
	 * 
	 * @param label of the palette category to be created
	 * @return the URI of the created palette category
	 * @throws Exception if it cannot be added to the ontology
	 */
	@Override
	public String createPaletteCategory(String label, Integer position) throws Exception {
		String uri = OntologyManager.baseURI + URLEncoder.encode(label.replace(' ', '_'), PropertyHandler.GRITS_CHARACTER_ENCODING);
		if (om.getIndividual(om.localOntologymodel, uri) != null)
			return uri;  // already exists, no need to create
		// create an individual for "paletteCategory" class
		String indivUri = om.createNewIndividual(om.localOntologymodel, OntologyManager.TOPLEVELCATEGORY_CLASS_URI, label);
		if (position != null) {
			Literal positionLiteral = om.localOntologymodel.createLiteral(position+"");
			om.addLiteral(om.localOntologymodel, indivUri, "has_position", positionLiteral);
		}
		return indivUri;
	}
}
