/*******************************************************************************
 * Copyright (c) 2004, 2010 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elias Volanakis - initial API and implementation
 *******************************************************************************/
package org.grits.toolbox.editor.experimentdesigner.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.PaletteStack;
import org.eclipse.gef.palette.PaletteToolbar;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.grits.toolbox.core.utilShare.ErrorUtils;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;
import org.grits.toolbox.editor.experimentdesigner.model.Connection;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolCategory;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolPaletteEntry;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;

/**
 * Utility class that can create a GEF Palette.
 * 
 * @see #createPalette()
 * @author Sena Arpinar
 */
final class GraphEditorPaletteFactory {
	
	private static Logger logger = Logger.getLogger(GraphEditorPaletteFactory.class);

	/** Create the "Nodes" drawer. */
	/*private static PaletteContainer createOutputDrawer() {
		PaletteDrawer componentsDrawer = new PaletteDrawer("Output Nodes");

		CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(
				"Output", "Create an output node ", OutputNode.class,
				new SimpleFactory(OutputNode.class),
				ImageDescriptor.createFromFile(Activator.class,
						"icons/ellipse16.gif"), ImageDescriptor.createFromFile(
						Activator.class, "icons/ellipse24.gif"));
		componentsDrawer.add(component);

		return componentsDrawer;
	}*/
	
	private static PaletteContainer createDrawer (ProtocolCategory topLevel, List<PaletteContainer> contents) {
		PaletteDrawer componentsDrawer = new PaletteDrawer(topLevel.getName());
		componentsDrawer.setDescription(topLevel.getDescription());
	
		for (PaletteContainer paletteContainer : contents) {
			componentsDrawer.add(new PaletteSeparator());
			PaletteEntry categoryLabel = new PaletteEntry(paletteContainer.getLabel(), paletteContainer.getDescription(), PaletteEntry.PALETTE_TYPE_UNKNOWN );
			componentsDrawer.add(categoryLabel);
			componentsDrawer.add(paletteContainer);
		}
		
		return componentsDrawer;
	}
	
	@SuppressWarnings("rawtypes")
	private static PaletteContainer createStack (ProtocolCategory category, List<ProtocolPaletteEntry> entries) {
		PaletteStack toolStack = new PaletteStack(category.getName(), category.getDescription(), null);
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			ProtocolPaletteEntry protocolPaletteEntry = (ProtocolPaletteEntry) iterator
					.next();
			ProtocolNodeCreationFactory factory = new ProtocolNodeCreationFactory();
			factory.setText(protocolPaletteEntry.getLabel());
			factory.setColor(protocolPaletteEntry.getColor());
			factory.setUri(protocolPaletteEntry.getUri());
			factory.setUrl(protocolPaletteEntry.getUrl());
			factory.setCategory(protocolPaletteEntry.getCategory());
			factory.setCreator(protocolPaletteEntry.getCreator());
			factory.setParameters(protocolPaletteEntry.getParameters());
			factory.setParameterGroups(protocolPaletteEntry.getParameterGroups());
			factory.setPapers (protocolPaletteEntry.getPapers());
			factory.setDescription(protocolPaletteEntry.getDescription());
			factory.setFile(protocolPaletteEntry.getFile());
			
			if (protocolPaletteEntry.getCategory() == null) {
				// problem in the ontology
				logger.error(Activator.PLUGIN_ID + " Protocol does not have a category. Fix the ontology!");
				ErrorUtils.createErrorMessageBox(Display.getCurrent().getActiveShell(), "Protocol does not have a category.\nContact the developers to fix the ontology!");
				return null;
			}
			int position = protocolPaletteEntry.getCategory().getPosition();
			CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(
					protocolPaletteEntry.getLabel(), protocolPaletteEntry.getDescription(),
					factory,
					ImageRegistry.getSmallIcon(category.getIcon()), ImageRegistry.getLargeIcon(category.getIcon()));
			toolStack.add(component);
		}
		return toolStack;
	}

	/**
	 * Creates the PaletteRoot and adds all palette elements. Use this factory
	 * method to create a new palette for your graphical editor.
	 * 
	 * @return a new PaletteRoot
	 */
	static PaletteRoot createPalette() {
		PaletteRoot palette = new PaletteRoot();
		palette.add(createToolsGroup(palette));
		
		try {
			List<ProtocolCategory> categories;
			ExperimentDesignOntologyAPI ontologyAPI = new ExperimentDesignOntologyAPI();
			categories = ontologyAPI.getTopLevelCategories();
			int i=0;
			for (ProtocolCategory protocolCategory : categories) {
				List<ProtocolCategory> subCategories = ontologyAPI.getProtocolCategoriesByTopLevelCategory(protocolCategory);
				List<PaletteContainer> stacks = new ArrayList<PaletteContainer>();
				for (final ProtocolCategory subCategory: subCategories) {
					if (subCategory.getName().equals(protocolCategory.getName()))
						continue;
					stacks.add(createStack(subCategory, ontologyAPI.getProtocolsForCategory(subCategory)));
				}
				
				PaletteDrawer drawer = (PaletteDrawer) createDrawer (protocolCategory, stacks );
				if (i == 0) { // first drawer
					drawer.setInitialState(PaletteDrawer.INITIAL_STATE_OPEN);
				} else {
					drawer.setInitialState(PaletteDrawer.INITIAL_STATE_CLOSED);
				}
				
				palette.add(drawer);
				i++;
			}
		} catch (Exception e) {
			logger.error(Activator.PLUGIN_ID + " Error Reading Ontology. Cannot create the palette! ", e); 
            MessageDialog.openError(Display.getCurrent().getActiveShell(), 
                    "Error", "Error Reading Ontology. Cannot create the palette!\nError:" + e);
		}
		
		return palette;
	}

	/** Create the "Tools" group. */
	private static PaletteContainer createToolsGroup(PaletteRoot palette) {
		PaletteToolbar toolbar = new PaletteToolbar("Tools");

		// Add a selection tool to the group
		ToolEntry tool = new PanningSelectionToolEntry();
		toolbar.add(tool);
		palette.setDefaultEntry(tool);

		// Add a marquee tool to the group
		toolbar.add(new MarqueeToolEntry());

		// Add (solid-line) connection tool
		tool = new ConnectionCreationToolEntry("Connection",
				"Create a connection", new CreationFactory() {
					public Object getNewObject() {
						return null;
					}

					// see GraphNodeEditPart#createEditPolicies()
					// this is abused to transmit the desired line style
					public Object getObjectType() {
						return Connection.SOLID_CONNECTION;
					}
				}, ImageRegistry.getImageDescriptor(ExperimentDesignerImage.CONNECTION16),
				   ImageRegistry.getImageDescriptor(ExperimentDesignerImage.CONNECTION24));
		toolbar.add(tool);
		
		CombinedTemplateCreationEntry component = new CombinedTemplateCreationEntry(
				"New", "Create a new protocol",
				new SimpleFactory(ProtocolNode.class),
				ImageRegistry.getSmallIcon("255-255-255.gif"), 
				ImageRegistry.getLargeIcon("255-255-255.gif"));
		toolbar.add(component);
		
		return toolbar;
	}

	/** Utility class. */
	private GraphEditorPaletteFactory() {
		// Utility class
	}

}