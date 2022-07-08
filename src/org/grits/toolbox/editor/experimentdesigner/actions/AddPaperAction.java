package org.grits.toolbox.editor.experimentdesigner.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.grits.toolbox.core.part.EventPart;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.pubmed.DTOPublication;
import org.grits.toolbox.editor.experimentdesigner.pubmed.DTOPublicationAuthor;
import org.grits.toolbox.editor.experimentdesigner.pubmed.PubmedUtil;
import org.grits.toolbox.editor.experimentdesigner.views.PaperView;

public class AddPaperAction  {
	
	private static Logger logger = Logger.getLogger(PaperView.class);
	
	ControlDecoration dec;
	Text pubmedId;
	Label errorLabel;
	
	@Inject IEventBroker eventBroker;
	GraphEditor editor;
	
	@Execute
	public void run(@Named (IServiceConstants.ACTIVE_SHELL) Shell shell, MApplication application, MPart paperView) {
		if(paperView != null && paperView.getObject() != null)
		{
			ProtocolNode protocol = ((PaperView) paperView.getObject()).getProtocol();
			if (protocol == null) 
				return;
		
			editor = application.getContext().get(GraphEditor.class);
			
			TableViewer tableViewer = ((PaperView) paperView.getObject()).getTableViewer();
		
			PubMedFormDialog formDialog = new PubMedFormDialog(shell);
			
			formDialog.create();
			formDialog.getShell().setSize(315, 210);
			if (formDialog.open() == Window.OK) {
				
				Integer pmid = formDialog.getPmId();
				if (pmid != null) {
					PubmedUtil util = new PubmedUtil();
					try {
						List<Paper> papers = protocol.getPapers();
						if (papers == null) {
							papers = new ArrayList<>();
							protocol.setPapers(papers);
						}
						
						DTOPublication publication = util.createFromPubmedId(pmid);
						if (publication == null) {
							logger.info ("Paper with pubmed id " + pmid + " does not exist.");
							MessageDialog.openWarning(shell, "Does not Exist", "Paper with pubmed id " + pmid + " does not exist.");
							return;
						}
						Paper newPaper = new Paper();
						newPaper.setPubMedId(pmid);
						newPaper.setTitle(publication.getTitle());
						newPaper.setFormatedAuthor(publication.getFormattedAuthor());
						List<DTOPublicationAuthor> authors = publication.getAuthors();
						for (Iterator<DTOPublicationAuthor> iterator = authors.iterator(); iterator
								.hasNext();) {
							DTOPublicationAuthor dtoPublicationAuthor = (DTOPublicationAuthor) iterator
									.next();
							newPaper.addAuthor (dtoPublicationAuthor.getLastName() + ", " + dtoPublicationAuthor.getFirstName());
						}
						StringBuffer bibCitation = new StringBuffer();
						if (publication.getJournal() != null)
							bibCitation.append(publication.getJournal() + ".");
						if (publication.getVolume() != null)
							bibCitation.append (" " + publication.getVolume());
						if (publication.getNumber() != null)
							bibCitation.append ("(" + publication.getNumber() + ")");
						if (publication.getStartPage() != null) 
							bibCitation.append(" " + publication.getStartPage());
						if (publication.getEndPage() != null)
							bibCitation.append("-" + publication.getEndPage());
						if (publication.getYear() != null)
							bibCitation.append(" (" + publication.getYear() + ").");
						newPaper.setBibliographicCitation(bibCitation.toString());
						newPaper.setYear(publication.getYear());
						if (!papers.contains(newPaper)) {
							papers.add(newPaper);
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
						else {
							logger.info ( " This paper already exists for the protocol. Duplicate! ");
							MessageDialog.openInformation(shell, "Duplicate", "Paper with this pubmed id already exists for the protocol. Ignoring!");
						}
					} catch (Exception e) {
						logger.error ( " Error retrieving information from PubMed Server. ", e);
						MessageDialog.openError(shell, "Error connecting to Pubmed", "Error retrieving paper information from Pubmed Server. Please check your internet connection: " + e.getMessage());
					}
				}
			}
		}
	}
	
	class PubMedFormDialog extends FormDialog {
		
		Integer pmId;
		String pmIdText;
		
		public PubMedFormDialog(Shell shell) {
			super(shell);
		}
		
		public Integer getPmId() {
			return pmId;
		}

		public String getPmIdText() {
			return pmIdText;
		}

		public void setPmIdText(String pmIdText) {
			this.pmIdText = pmIdText;
		}

		@Override
		protected void createFormContent(org.eclipse.ui.forms.IManagedForm mform) {
			
			ScrolledForm scrolledForm = mform.getForm();
			TableWrapLayout tableWrapLayout = new TableWrapLayout();
			tableWrapLayout.numColumns = 2;
			scrolledForm.getBody().setLayout(tableWrapLayout);
			
			scrolledForm.setText("Pubmed Search");
			
			FormToolkit toolkit = mform.getToolkit();
			Label lblName = toolkit.createLabel(scrolledForm.getBody(), "Pubmed Id:");
			lblName.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.TOP, 1, 1));
			
			pubmedId = toolkit.createText(scrolledForm.getBody(), "", SWT.NONE);
			pubmedId.setLayoutData(new TableWrapData(TableWrapData.CENTER, TableWrapData.TOP, 1, 1));
			// Create a control decoration for the control.
			dec = new ControlDecoration(pubmedId, SWT.TOP | SWT.LEFT);
			// Specify the decoration image and description
			Image image = JFaceResources.getImage(DLG_IMG_MESSAGE_ERROR);
			dec.setImage(image);
			dec.setDescriptionText("Pubmed Id should be an integer");
			dec.hide();
			
			errorLabel = toolkit.createLabel(scrolledForm.getBody(), "", SWT.NONE);
			errorLabel.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.BOTTOM, 3, 2));
			
			toolkit.paintBordersFor(scrolledForm.getBody());
		};
		 
		class IntegerFieldValidator implements IValidator {
			 
		    private final String errorText;
		    private final ControlDecoration controlDecoration;
		 
		    public IntegerFieldValidator(String errorText,
		        ControlDecoration controlDecoration) {
		        super();
		        this.errorText = errorText;
		        this.controlDecoration = controlDecoration;
		    }
		 
		    public IStatus validate(Object value) {
		        if (value instanceof String) {
		            String text = (String) value;
		            if (text.trim().length() == 0) {
		                controlDecoration.show();
		                return ValidationStatus
		                        .error(errorText);
		            }
		            else {
		            	try {
		            		Integer.parseInt(text.trim());
		            	} catch (Exception e) {
		            		controlDecoration.show();
		            		return ValidationStatus
			                        .error(errorText);
		            	}
		            }
		        }
		        controlDecoration.hide();
		        errorLabel.setText("");
		        return Status.OK_STATUS;
		    }
		}
		
		@Override
		protected void okPressed() {
			pmIdText = pubmedId.getText();
			DataBindingContext dataBindingContext = new DataBindingContext();
			Binding binding = dataBindingContext.bindValue(
				    SWTObservables.observeText(pubmedId, SWT.Modify),
				    PojoProperties.value(PubMedFormDialog.class, "pmIdText").observe(this),
				    new UpdateValueStrategy()
				        .setAfterConvertValidator(new IntegerFieldValidator(
				             "Pubmed Id should be an integer",
				              dec)),
				    null);
			
			if (((Status)binding.getValidationStatus().getValue()).isOK()) {
				pmId = Integer.parseInt(pmIdText.trim());
				super.okPressed();
			}
			else {
				// display errorMessage
				errorLabel.setForeground(ColorConstants.red);
				errorLabel.setText("Please provide a valid pubmed id");
			}
				
		}
	};
}
