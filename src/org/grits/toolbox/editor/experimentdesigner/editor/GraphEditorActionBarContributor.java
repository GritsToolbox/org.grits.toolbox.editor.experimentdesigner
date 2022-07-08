/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
?* http://www.eclipse.org/legal/epl-v10.html
?*
?* Contributors:
?*????Elias Volanakis - initial API and implementation
?*******************************************************************************/
package org.grits.toolbox.editor.experimentdesigner.editor;

import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;

/**
 * Contributes actions to a toolbar. This class is tied to the editor in the
 * definition of editor-extension (see plugin.xml).
 * 
 * @author Elias Volanakis @modified_by Sena Arpinar
 */
public class GraphEditorActionBarContributor extends ActionBarContributor {

	/**
	 * Create actions managed by this contributor.
	 * 
	 * @see org.eclipse.gef.ui.actions.ActionBarContributor#buildActions()
	 */
	protected void buildActions() {
		addRetargetAction(new DeleteRetargetAction());
		addRetargetAction(new UndoRetargetAction());
		addRetargetAction(new RedoRetargetAction());
		RetargetAction action = new RetargetAction(GraphEditor.SAVE_AS_TEMPLATE, null);
		action.setToolTipText("Save the experiment design as a template");
		action.setImageDescriptor(ImageRegistry.getImageDescriptor(ExperimentDesignerImage.SAVEASTEMPLATEBIG));
		action.setDisabledImageDescriptor(ImageRegistry.getImageDescriptor(ExperimentDesignerImage.SAVEASEMPLATEBIGDISABLED));
		addRetargetAction(action);
	}

	/**
	 * Add actions to the given toolbar.
	 * 
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(getAction(ActionFactory.UNDO.getId()));
		toolBarManager.add(getAction(ActionFactory.REDO.getId()));
		toolBarManager.add(getAction(GraphEditor.SAVE_AS_TEMPLATE));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.ui.actions.ActionBarContributor#declareGlobalActionKeys()
	 */
	protected void declareGlobalActionKeys() {
		// currently none
	}

}