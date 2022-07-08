package org.grits.toolbox.editor.experimentdesigner.ontology;

import java.io.IOException;
import java.util.List;

import org.grits.toolbox.editor.experimentdesigner.io.ProtocolEntry;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolCategory;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolPaletteEntry;

public interface ExperimentDesignModelAPI {

	List<ProtocolCategory> getProtocolCategories ();
    
    List<ProtocolPaletteEntry> getProtocolsForCategory (ProtocolCategory category);
    
    List<ProtocolNode> getAllProtocolVariants();
    
    List<Parameter> getAllParameters();

    List<ParameterGroup> getParameterGroups();   

	List<ExperimentTemplateEntry> getAllExperimentTemplateEntries() throws IOException;

	List<ProtocolNode> getProtocolVariantsByUri(String uri);

	List<ProtocolEntry> getAllProtocolVariantEntries() throws IOException;
	
	/**
	 * retrieves the protocol node from the ontology with all its fields (including the category)
	 * 
	 * @param uri protocol uri to be retrieved
	 * @return ProtocolNode for the given uri or null if it does not exist
	 */
	ProtocolNode getProtocolByUri (String uri);
	
	void createProtocolVariant(ProtocolNode protocol) throws Exception;
	
	void createTemplateForExperimentGraph(ExperimentGraph experimentDesign) throws Exception;
	
	ProtocolCategory createCategory (String label, String parentURI, String icon, String description, Integer position) throws Exception;
	
	String createPaletteCategory (String label, Integer positionl) throws Exception;

	String createProtocolTemplate(ProtocolNode protocol, String paletteCategoryURI) throws Exception;
	 
}
