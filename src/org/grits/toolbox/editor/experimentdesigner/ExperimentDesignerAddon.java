 
package org.grits.toolbox.editor.experimentdesigner;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.EventTags;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.utils.ExperimentDesignerUtils;

@SuppressWarnings("restriction")
public class ExperimentDesignerAddon {

	@Inject
	@Optional
	public void applicationStarted(
			IEclipseContext eclipseContext) {
		eclipseContext.set(ExperimentDesignerUtils.class,
				ContextInjectionFactory.make(ExperimentDesignerUtils.class, eclipseContext));
		//TODO can put other utility classes here
		// EperimentTemplateFileHandler
		// ProtocolFileHandler
		// ExperimentDesignOntologyAPI etc.
	}
	
	@Inject
	@Optional
	public void subscribeTopicPartActivation(@UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) org.osgi.service.event.Event event, IEclipseContext eclipseContext) {
		  Object element = event.getProperty(EventTags.ELEMENT);
		  if (!(element instanceof MPart)) {
		    return;
		  }
		  
		  MPart part = (MPart) element;
		  if (part.getObject() instanceof CompatibilityEditor) {
			  if (((CompatibilityEditor) part.getObject()).getEditor() instanceof GraphEditor) {
				  GraphEditor editor = (GraphEditor)((CompatibilityEditor) part.getObject()).getEditor();
				  eclipseContext.set(GraphEditor.class, editor);
			  }
		  }
	} 

}
