package org.grits.toolbox.editor.experimentdesigner.dialog;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.core.datamodel.Entry;
import org.grits.toolbox.core.datamodel.dialog.ModalDialog;
import org.grits.toolbox.core.datamodel.dialog.ProjectExplorerDialog;
import org.grits.toolbox.editor.experimentdesigner.Activator;
import org.grits.toolbox.editor.experimentdesigner.handler.NewExperimentHandler;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentDesignOntologyAPI;
import org.grits.toolbox.editor.experimentdesigner.ontology.ExperimentTemplateEntry;
import org.grits.toolbox.editor.experimentdesigner.property.ExperimentProperty;
import org.grits.toolbox.entry.sample.property.SampleProperty;

public class SelectExperimentDialog extends ModalDialog {
	private static final Logger logger = Logger.getLogger(SelectExperimentDialog.class);
	
	private Button fromTemplateRadio;
	private ComboViewer selectedTemplateCombo;
	
	private ExperimentTemplateEntry templateFile;
	private Entry experimentSelected;

	private Button copyFromButton;

	protected Button OKbutton;

	private Text existingDesignText;

	public SelectExperimentDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Select Experiment Design");
		setMessage("Select an existing experiment design");
	}
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		
		this.parent = parent;
		
		//has to be gridLayout, since it extends TitleAreaDialog
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.verticalSpacing = 10;
		parent.setLayout(gridLayout);
		
		Label label1 = new Label(parent, SWT.NONE);
		label1.setText ("Select Experiment Design: ");
		GridData gd = new GridData();
        gd.horizontalSpan = 3;
		label1.setLayoutData(gd); 
		
        
		fromTemplateRadio = new Button(parent, SWT.RADIO);
		fromTemplateRadio.setSelection(false);
		fromTemplateRadio.setText("From Template");
		fromTemplateRadio.setEnabled(false);
			
		fromTemplateRadio.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedTemplateCombo != null) {
					ISelection selection = selectedTemplateCombo.getSelection();
					if (selection instanceof IStructuredSelection) {
						Object s = ((IStructuredSelection) selection).getFirstElement();
						if (s instanceof ExperimentTemplateEntry) {
							setTemplateFile((ExperimentTemplateEntry) s);
							experimentSelected = null;	
						}
					}
				}
			}
		});
		
		selectedTemplateCombo = new ComboViewer(parent, SWT.READ_ONLY);
		selectedTemplateCombo.setContentProvider(new IStructuredContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List)
					return ((List) inputElement).toArray();
				return null;
			}
		});
		selectedTemplateCombo.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((ExperimentTemplateEntry)element).getName();
			}
		});
		selectedTemplateCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		try {
			List<ExperimentTemplateEntry> templates = new ExperimentDesignOntologyAPI().getAllExperimentTemplateEntries();
			if (templates == null || templates.isEmpty()) {
				selectedTemplateCombo.getCombo().setEnabled(false);
				fromTemplateRadio.setEnabled(false);
			}
			else {
				selectedTemplateCombo.setInput(templates);
				selectedTemplateCombo.getCombo().setEnabled(true);
				selectedTemplateCombo.getCombo().select(0);
				fromTemplateRadio.setEnabled(true);
			}
		
		} catch (Exception e) {
			logger.warn(Activator.PLUGIN_ID + " Cannot load the existing templates! " + e);
			MessageDialog.openInformation(parent.getShell(), "Warning", "Cannot retrieve experiment design templates: " + e.getMessage());
			selectedTemplateCombo.getCombo().setEnabled(false);
		}
		
		selectedTemplateCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					Object s = ((IStructuredSelection) selection).getFirstElement();
					if (s instanceof ExperimentTemplateEntry) {
						setTemplateFile((ExperimentTemplateEntry) s);
						if (fromTemplateRadio.getSelection()) {
							experimentSelected = null;
							//setReturnCode(NewExperiment.TEMPLATE);
						}
					}
				}
			}
		});
		
		Label designLabel = new Label(parent, SWT.FILL);
		designLabel.setText("");
		gd = new GridData();
        gd.horizontalSpan = 1;
		designLabel.setLayoutData(gd);
		
		copyFromButton = new Button(parent, SWT.RADIO);
		copyFromButton.setSelection(false);
		copyFromButton.setText("Copy existing design");
		copyFromButton.setToolTipText("Copy from an existing experiment design");

		copyFromButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				Button button = (Button) event.getSource();
				if (!button.getSelection())  // not selected, do nothing
					return;
				OKbutton.setEnabled(false);   // disabled until a selection is made
				Shell newShell = new Shell(parent.getShell(),SWT.PRIMARY_MODAL | SWT.SHEET);
				
				// in order to change the filtering mechanism, overriding ProjectExplorerDialog
				ProjectExplorerDialog dlg = new ProjectExplorerDialog(newShell) {
					
					Entry selectedEntry = null;
					
					@Override
					protected void okPressed() {
						experimentSelected = selectedEntry;
						super.okPressed();
					}
					
					@Override
					public Control createDialogArea(Composite parent) {
						super.createDialogArea(parent);
						getViewer().addSelectionChangedListener(
							new ISelectionChangedListener()
							{
								@Override
								public void selectionChanged(SelectionChangedEvent event) {
									ISelection selection = event.getSelection();
									
									if(selection.isEmpty()) return;
									
									//convert it to TreeSelection object type
									TreeSelection to = (TreeSelection)selection;
									
									//check if only one is selected!
									if (to.size() == 1)
									{
										// allow only sample entries with an existing experiment design
										Entry node = (Entry)to.getFirstElement();
										boolean match = false;
										
										if(node.getProperty().getType().equals(SampleProperty.TYPE))
										{
											List<Entry> children = node.getChildren();
											for (Iterator iterator = children
													.iterator(); iterator
													.hasNext();) {
												Entry entry = (Entry) iterator
														.next();
												if (entry.getProperty().getType().equals(ExperimentProperty.TYPE)) {
													match = true;
													selectedEntry = entry;
													break;
												}
											}
										}
									
										if(match)
										{
											//then enables OK button
											setEnabledOKbutton(true);
										}
										else
										{
											setEnabledOKbutton(false);
										}
									}
									else
									{
										setEnabledOKbutton(false);
									}
								}	
								
							});
						return parent;
					}
					
					public void setEnabledOKbutton (boolean enabled) {
						getButton(Dialog.OK).setEnabled(enabled);
					}
					
				};
				// Change the title bar text
				dlg.setTitle("Experiment Design Selection");
				// Customizable message displayed in the dialog
				dlg.setMessage("Choose an experiment design");
				// Calling open() will open and run the dialog.
				dlg.open();
				if (experimentSelected != null) {
					setReturnCode(NewExperimentHandler.COPY);
					existingDesignText.setText(experimentSelected.getDisplayName());
					OKbutton.setEnabled(true);
				}
			}
		});
		
				
		existingDesignText = new Text(parent, SWT.SEARCH | SWT.FILL);
		existingDesignText.setEditable(false);
		existingDesignText.setText("");
		gd = new GridData();
        gd.horizontalSpan = 1;
        gd.widthHint=100;
        existingDesignText.setLayoutData(gd);
        
        Label designLabel2 = new Label(parent, SWT.FILL);
		designLabel2.setText("");
		gd = new GridData();
        gd.horizontalSpan = 1;
		designLabel2.setLayoutData(gd);

		new Label(parent, SWT.NONE);
		createCANCELButton(parent);
		createOKButton(parent);
		
		return parent;
	}
	
	protected Button createCANCELButton(final Composite parent2) {
		//create a gridData for CANCEL button
		GridData cancelData = new GridData();
		cancelData.horizontalAlignment = SWT.END;
		cancelData.grabExcessHorizontalSpace=true;
		Button CancelButton = new Button(parent2, SWT.PUSH);
		CancelButton.setText("Cancel");
		CancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				setReturnCode(CANCEL);
				close();
			}
		});
		CancelButton.setLayoutData(cancelData);
		return CancelButton;
	}
	
	protected Button createOKButton(final Composite parent2) {
		//create a gridData for OKButton
		GridData okData = new GridData();
	//	okData.grabExcessHorizontalSpace = true;
		okData.horizontalAlignment = SWT.END;
	//	okData.horizontalSpan = 2;
		OKbutton = new Button(parent2, SWT.PUSH);
		OKbutton.setText("   OK   ");
		OKbutton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent event)
			{
				if (isValidInput()) {
					okPressed();
					close();
				}
			}
		});
		OKbutton.setLayoutData(okData);
		return OKbutton;
	}

	@Override
	protected boolean isValidInput() {
		return true;
	}

	@Override
	protected Entry createEntry() {
		return null;
	}

	public ExperimentTemplateEntry getTemplateFile() {
		return templateFile;
	}

	public void setTemplateFile(ExperimentTemplateEntry templateFile) {
		this.templateFile = templateFile;
	}

	public Entry getExperimentSelected() {
		return experimentSelected;
	}

	public void setExperimentSelected(Entry experimentSelected) {
		this.experimentSelected = experimentSelected;
	}

}
