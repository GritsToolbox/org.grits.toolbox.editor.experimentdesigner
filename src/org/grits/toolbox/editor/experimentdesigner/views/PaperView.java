package org.grits.toolbox.editor.experimentdesigner.views;


import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.impl.DirectToolItemImpl;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.UIElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.Paper;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.parts.GraphNodeEditPart;
import org.osgi.service.event.EventHandler;

@SuppressWarnings("restriction")
public class PaperView {
	
	public static final String VIEW_ID = "org.grits.toolbox.editor.experimentdesigner.part.paperview";
	public static final String EVENT_TOPIC_PAPER = "Paper_to_be_deleted";
	private static Logger logger = Logger.getLogger(PaperView.class);

	private ProtocolNode protocol=null;
	
	private TableViewer tableViewer;
	
	@Inject ESelectionService selectionService;
	@Inject IEventBroker eventBroker;
	private DirectToolItemImpl deleteToolItem;

	public PaperView() {
	}
	
	public TableViewer getTableViewer() {
		return tableViewer;
	}
	
	public ProtocolNode getProtocol() {
		return protocol;
	}

	@PostConstruct
	public void createPartControl(final Composite parent, MPart part, EModelService modelService ) {
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		
		tableViewer = new TableViewer(scrolledComposite, SWT.BORDER | SWT.FULL_SELECTION) ;
		Table table = tableViewer.getTable();
		
		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn idColumn = tableViewerColumn.getColumn();
		idColumn.setText("Article");
		idColumn.setWidth(680);
		
		tableViewerColumn.setLabelProvider(new OwnerDrawLabelProvider() {
			
			@Override
			protected void measure(Event event, Object element) {
				Paper paper = (Paper) element;
				Point size = event.gc.textExtent(paper.toString());
				event.width = size.x;
				event.height = size.y;
				int currentWidth = tableViewer.getTable().getColumn(event.index).getWidth();
				if (size.x > currentWidth)   // increase the column size
					tableViewer.getTable().getColumn(event.index).setWidth(size.x + 1);
			}
			
			@Override
			protected void paint(Event event, Object element) {
				Paper entry = (Paper) element;
				event.gc.drawText(entry.toString(), event.x, event.y, true);
			}
			
		});
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		tableViewer.setContentProvider(new ArrayContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement != null) {
					List<Paper> papers = ((ProtocolNode)inputElement).getPapers();
					if (papers != null)
						return papers.toArray();
				}
				return super.getElements(inputElement);
			}
		});
		
		table.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Table t = (Table)e.getSource();
				TableItem[] selection = t.getSelection();
				if (selection.length == 0) { // clear previous state
					eventBroker.send(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, UIEvents.ALL_ELEMENT_ID);  // to enable delete paper action -- it does not work!!!
					deleteToolItem.setEnabled(false);
				}
		        for (int i = 0; i < selection.length; i++) {
		        	Object s= selection[i].getData();
		        	if (s instanceof Paper) {
		        		selectionService.setSelection((Paper)s);
		        		eventBroker.send(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, UIEvents.ALL_ELEMENT_ID); // to enable delete paper action -- it does not work!!!
		        		deleteToolItem.setEnabled(true);
		        		break; // only delete the first selected
		        	}
		        }
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	
		scrolledComposite.setContent(table);
		scrolledComposite.setSize(table.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		deleteToolItem = (DirectToolItemImpl) modelService.find("org.grits.toolbox.editor.experimentdesigner.directtoolitem.deletepaper", part.getToolbar());
		
		// we need to be notified when editors get closed to be able clear the view contents
		eventBroker.subscribe(UIElement.TOPIC_TOBERENDERED, closePartEventHandler);
	}
	
	private final EventHandler closePartEventHandler = new EventHandler() {
		
		@Override
		public void handleEvent(org.osgi.service.event.Event event) {
			Object part = event.getProperty(UIEvents.EventTags.ELEMENT);
			if (part == null)
				return;
			boolean toBeRendered = (Boolean) event.getProperty(UIEvents.EventTags.NEW_VALUE);
			if (!toBeRendered && part instanceof MPart) {
				if (((MPart) part).getObject() instanceof CompatibilityEditor) {
					if (((CompatibilityEditor) ((MPart) part).getObject()).getEditor() instanceof GraphEditor) {
						if (!tableViewer.getTable().isDisposed()) {
							tableViewer.setInput(null);
							protocol = null;
					  }
					}
				}
			}
		}
	};
	
	@PreDestroy
	public void preDestroy(MPart part)
	{
		eventBroker.unsubscribe(closePartEventHandler);
	}

	@Focus
	public void setFocus() {
		tableViewer.getTable().setFocus();

	}

	@Inject
	void setSelection(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object selection) {
		if (selection != null && !(selection instanceof StructuredSelection))
			return;
		if (selection == null || !(((StructuredSelection) selection).getFirstElement() instanceof GraphNodeEditPart)) {
			protocol = null;
			if (tableViewer != null && !tableViewer.getTable().isDisposed()) {
				tableViewer.setInput(null);
			}
		} else {
			if (((StructuredSelection) selection).getFirstElement() instanceof GraphNodeEditPart) {
				Object modelObject = ((GraphNodeEditPart)((StructuredSelection) selection).getFirstElement()).getModel();
				if (modelObject instanceof ProtocolNode) {
					protocol = (ProtocolNode)modelObject;
					if (tableViewer != null && !tableViewer.getTable().isDisposed()) {
						tableViewer.setInput (protocol);
					}
				}
				else {
					// no papers to list
					protocol = null;
					if (tableViewer != null && !tableViewer.getTable().isDisposed()) {
						tableViewer.setInput(null);
					}
				}
			}
		}
	}
}
