 
package org.grits.toolbox.editor.experimentdesigner.handler;

import java.util.Arrays;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry;
import org.grits.toolbox.editor.experimentdesigner.config.ImageRegistry.ExperimentDesignerImage;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.pdfgeneration.ExperimentDesignRDFReport;
import org.grits.toolbox.editor.experimentdesigner.wordgeneration.ExperimentDesignerWordReport;

@SuppressWarnings("restriction")
public class ExperimentDesignReportHandler {
	
	public static Image wordIcon = ImageRegistry.getImageDescriptor(ExperimentDesignerImage.WORDICON).createImage();
	public static Image pdfIcon = ImageRegistry.getImageDescriptor(ExperimentDesignerImage.PDFICON).createImage();
	
	Options options;
	
	public class Options {
		Boolean project = true;
		Boolean sample = true;
		Boolean expDesign= true;
		
		public Boolean getProject() {
			return project;
		}
		public void setProject(Boolean project) {
			this.project = project;
		}
		public Boolean getSample() {
			return sample;
		}
		public void setSample(Boolean sample) {
			this.sample = sample;
		}
		public Boolean getExpDesign() {
			return expDesign;
		}
		public void setExpDesign(Boolean expDesign) {
			this.expDesign = expDesign;
		}
	}
	
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part, @Named (IServiceConstants.ACTIVE_SHELL) Shell shell) {
		if ((part.getObject() instanceof CompatibilityEditor) && ((CompatibilityEditor) part.getObject()).getEditor() instanceof GraphEditor) {
        	this.options = new Options();
        	ListDialog dialog = 
        			  new ListDialog(shell) {
        		@Override
        		protected Control createDialogArea(Composite container) {
        			Composite parent = (Composite) super.createDialogArea(container);
        			addPreferenceSelection (parent);
        			return parent;
        		}
        		
        		@Override
				protected void okPressed() {
					if (options != null && !options.expDesign && !options.project && !options.sample) {
						// nothing is selected, need to select at least one
						MessageDialog.openError(this.getShell(), "Selection Error", "Please select at least one part to be included in the report");
					} else {
						super.okPressed();
					}
				}
        	};
        	dialog.setLabelProvider(new ArrayLabelProvider());
        	dialog.setContentProvider(new ArrayContentProvider());
        	dialog.setInput(Arrays.asList(new String[] { "PDF", "Word"}));
        	dialog.setTitle("Select report format");
        	dialog.setInitialSelections(new String[] {"PDF"});
        	dialog.setWidthInChars(35);
        	dialog.setHeightInChars(10);
			// user pressed cancel
			if (dialog.open() != Window.OK) {
			    return;
			}
			Object[] result = dialog.getResult(); 
			if (result != null && result.length > 0) {
				String format = (String)result[0];
				if (format.equals("PDF")) {
					new ExperimentDesignRDFReport().generateDocument(options, (GraphEditor)((CompatibilityEditor) part.getObject()).getEditor());
				} else if (format.equals("Word")) {
					new ExperimentDesignerWordReport().generateReport(options, (GraphEditor)((CompatibilityEditor) part.getObject()).getEditor());
				} else {
					MessageDialog.openInformation(shell, "Not Supported", "The format" + format + " is not supported");
				}
			}
        }
        
        return;
		
	}
	
	protected void addPreferenceSelection(Composite parent) {
		GridLayout g = new GridLayout();
		g.numColumns = 2;
		Composite preferenceComposite =  new Composite (parent, SWT.NONE);
		preferenceComposite.setLayout(g);
	
		Label projectLabel = new Label (preferenceComposite, SWT.NONE);
		projectLabel.setText("Project Information");
		
		Button project = new Button(preferenceComposite, SWT.CHECK);
		project.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
		        if (button.getSelection())
		        	options.setProject(true);
		        else
		        	options.setProject(false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
		        if (button.getSelection())
		        	options.setProject(true);
		        else
		        	options.setProject(false);	
			}
		});
		project.setSelection(true);
		
		Label sampleLabel = new Label (preferenceComposite, SWT.NONE);
		sampleLabel.setText("Analyte Information");
		
		Button sample = new Button(preferenceComposite, SWT.CHECK);
		sample.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
		        if (button.getSelection())
		        	options.setSample(true);
		        else
		        	options.setSample(false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
		        if (button.getSelection())
		        	options.setSample(true);
		        else
		        	options.setSample(false);
			}
		});	
		sample.setSelection(true);
		
		Label expDesignLabel = new Label (preferenceComposite, SWT.NONE);
		expDesignLabel.setText("Experiment Design");
		
		Button expDesign = new Button(preferenceComposite, SWT.CHECK);
		expDesign.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
		        if (button.getSelection())
		        	options.setExpDesign(true);
		        else
		        	options.setExpDesign(false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				Button button = (Button) e.widget;
		        if (button.getSelection())
		        	options.setExpDesign(true);
		        else
		        	options.setExpDesign(false);
			}
		});	
		expDesign.setSelection(true);
	}

	static class ArrayLabelProvider extends LabelProvider implements ITableLabelProvider{
		public String getText(Object element) {
			return (String) element;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (((String)element).equals("Word"))
				return wordIcon;
			else if (((String)element).equals("PDF"))
				return pdfIcon;
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return (String) element;
		}
		
	}
	
	
	@CanExecute
	public boolean canExecute(@Named(IServiceConstants.ACTIVE_PART) MPart part) {	
		if ((part.getObject() instanceof CompatibilityEditor) && ((CompatibilityEditor) part.getObject()).getEditor() instanceof GraphEditor) {
			return true;
		}
		return false;
	}
		
}