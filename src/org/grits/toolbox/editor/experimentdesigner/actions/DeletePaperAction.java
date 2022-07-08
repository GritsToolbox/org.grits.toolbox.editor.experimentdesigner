package org.grits.toolbox.editor.experimentdesigner.actions;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.core.part.EventPart;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.views.PaperView;

public class DeletePaperAction {
	
	private static final Logger logger = Logger.getLogger(DeletePaperAction.class);

	@Inject IEventBroker eventBroker;
	GraphEditor editor;
	
	@Execute
	public void run(@Named (IServiceConstants.ACTIVE_SHELL) Shell shell, MApplication application, MPart paperView, 
			@Optional @Named (IServiceConstants.ACTIVE_SELECTION) Paper paperToBeDeleted) {
		if(paperView != null && paperView.getObject() != null)
		{
			ProtocolNode protocol = ((PaperView) paperView.getObject()).getProtocol();
			if (protocol == null) 
				return;
		
			editor = application.getContext().get(GraphEditor.class);
			TableViewer tableViewer = ((PaperView) paperView.getObject()).getTableViewer();
			
			if (paperToBeDeleted != null) {
				boolean deleted = protocol.getPapers().remove(paperToBeDeleted);
				if (deleted) {
					tableViewer.getTable().setRedraw(true);
					tableViewer.getTable().removeAll();
					tableViewer.setInput(protocol);
					eventBroker.post(EventPart.EVENT_TOPIC_VALUE_MODIFIED, protocol);
					if (editor == null) {
						logger.error("Cannot get the reference to Graph Editor");
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Update Error", "Cannot get the reference to Graph Editor");
						return;
					}
					editor.refreshProtocolNode(protocol);
				}
			}
		}
	}
	
	@CanExecute
	public boolean canExecute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Paper paperToBeDeleted) {
		return paperToBeDeleted != null;
	}
}
