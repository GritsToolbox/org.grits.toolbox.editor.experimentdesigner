package org.grits.toolbox.editor.experimentdesigner.views;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.impl.DirectToolItemImpl;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.internal.events.EventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.EventTags;
import org.eclipse.e4.ui.workbench.UIEvents.UIElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.grits.toolbox.editor.experimentdesigner.editing.TextValueEditingSupport;
import org.grits.toolbox.editor.experimentdesigner.editing.UnitEditingSupport;
import org.grits.toolbox.editor.experimentdesigner.editor.GraphEditor;
import org.grits.toolbox.editor.experimentdesigner.model.EntityWithPosition;
import org.grits.toolbox.editor.experimentdesigner.model.MeasurementUnit;
import org.grits.toolbox.editor.experimentdesigner.model.Parameter;
import org.grits.toolbox.editor.experimentdesigner.model.ParameterGroup;
import org.grits.toolbox.editor.experimentdesigner.model.ProtocolNode;
import org.grits.toolbox.editor.experimentdesigner.parts.GraphNodeEditPart;
import org.osgi.service.event.EventHandler;


@SuppressWarnings("restriction")
public class ParameterView {
	public final static String VIEW_ID = "org.grits.toolbox.editor.experimentdesigner.part.parameterview";
	private static Logger logger = Logger.getLogger(ParameterView.class);

	ProtocolNode protocol;
	
	TreeViewer treeViewer;
	@Inject EventBroker eventBroker;
	@Inject ESelectionService selectionService;
	GraphEditor editor;
	
	private DirectToolItemImpl deleteParamToolItem;
	private TextValueEditingSupport textEditingSupport;
	private UnitEditingSupport unitEditingSupport;
	
	public ParameterView() {
	}

	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	@PostConstruct
	public void createPartControl(final Composite parent, MPart part, EModelService modelService) {
		treeViewer = new TreeViewer(parent, SWT.BORDER + SWT.FULL_SELECTION);
		final Tree tree = treeViewer.getTree();
		
		textEditingSupport = new TextValueEditingSupport(treeViewer);
		unitEditingSupport = new UnitEditingSupport(treeViewer);
		
		tree.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				// enable "delete parameter"
				Tree source = (Tree)e.getSource();
				TreeItem[] selections = source.getSelection();
				if (selections.length == 0) { // clear previous state
					deleteParamToolItem.setEnabled(false);
				}
				for (int i = 0; i < selections.length; i++) {
					TreeItem item = selections[i];
					Object element = item.getData();
					if (element instanceof Parameter) {
						// make sure it is not a child node of a parameter group
						TreeItem parent = item.getParentItem();
						if (parent != null && parent.getData() instanceof ParameterGroup) {
							// cannot remove individual parameter from the group
							deleteParamToolItem.setEnabled(false);
							break;
						}
						deleteParamToolItem.setEnabled(true);
						selectionService.setSelection((Parameter)element);
						break;
					}
					if (element instanceof ParameterGroup) {
						selectionService.setSelection((ParameterGroup)element);
						deleteParamToolItem.setEnabled(true);
						break;
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
		});
		
		TreeViewerColumn nameViewerColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
		TreeColumn nameColumn = nameViewerColumn.getColumn();
		nameColumn.setWidth(161);
		nameColumn.setText("name");
		ColumnViewerToolTipSupport.enableFor(nameViewerColumn.getViewer());
		nameViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof ParameterGroup)
            		return ((ParameterGroup)element).getLabel();
            	else if (element instanceof Parameter)
            		return ((Parameter)element).getName();
				return null;
			}
			
			@Override
			public Color getForeground(Object element) {
				if (element instanceof ParameterGroup) {
					if (((ParameterGroup)element).getRequired() != null && ((ParameterGroup)element).getRequired())
						return ColorConstants.red;
				}
            	else if (element instanceof Parameter) {
            		if (((Parameter)element).getRequired() != null && ((Parameter)element).getRequired())
            			return ColorConstants.red;
            	}
				return super.getForeground(element);
			}
			
			@Override
			public String getToolTipText(Object element) {
				if (element instanceof ParameterGroup)
            		return ((ParameterGroup)element).getDescription();
            	else if (element instanceof Parameter)
            		return ((Parameter)element).getDescription();
				return null;
			}
			
			@Override
			public Image getToolTipImage(Object object) {
				return null;
			}
		});
		
		nameColumn.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(    SelectionEvent e){
			      int dir=tree.getSortDirection();
			      dir=dir == SWT.UP ? SWT.DOWN : SWT.UP;
			      tree.setSortDirection(dir);
			      treeViewer.refresh();
		    }
		});
		
		TreeViewerColumn valueViewerColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
		TreeColumn valueColumn = valueViewerColumn.getColumn();
		valueColumn.setWidth(200);
		valueColumn.setText("value");
		valueViewerColumn.setEditingSupport(textEditingSupport);    // value is editable
		valueViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Parameter)
	                return ((Parameter)element).getValue();
				return null;
			}
		});
		
		TreeViewerColumn unitViewerColumn =  new TreeViewerColumn(treeViewer, SWT.NONE);
		TreeColumn unitColumn = unitViewerColumn.getColumn();
		unitColumn.setWidth(100);
		unitColumn.setText("unit");
		unitViewerColumn.setEditingSupport(unitEditingSupport);   // unit is editable if provided
		unitViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Parameter) {
            	   MeasurementUnit unit = ((Parameter)element).getUnit();
            	   if (unit != null)
            		   return unit.getLabel();
            	   else { // select the first unit
            		   List<MeasurementUnit> units = ((Parameter)element).getAvailableUnits();
            		   if (units != null && units.size() > 0) {
            			   // set the first unit as the unit
            			   ((Parameter)element).setUnit(units.get(0));
            			   return units.get(0).getLabel();
            		   }
            	   }
	            }
				return null;
			}
		});
		
		TreeViewerColumn guidelineViewerColumn =  new TreeViewerColumn(treeViewer, SWT.NONE);
		TreeColumn guidelineColumn = guidelineViewerColumn.getColumn();
		guidelineColumn.setWidth(200);
		guidelineColumn.setText("guidelines");
		guidelineViewerColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				List<String> guidelines = null;
				if (element instanceof Parameter) {
					guidelines = ((Parameter) element).getGuidelineURIs();
	            } else if (element instanceof ParameterGroup) {
	            	guidelines = ((ParameterGroup) element).getGuidelineURIs();
	            }
				if (guidelines != null) {
					String stringValue = "";
					for (String guideline : guidelines) {
						if (!stringValue.isEmpty()) 
							stringValue += ", ";
						stringValue += guideline;
					}
					return stringValue;
				}
				return null;
			}
		});
		
		treeViewer.setContentProvider(new ParameterContentProvider());	
		treeViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				int c = 0;
				int dir = ((TreeViewer)viewer).getTree().getSortDirection();
				if (e1 instanceof EntityWithPosition && e2 instanceof EntityWithPosition) {
					if (((EntityWithPosition)e1).getPosition() != null &&  ((EntityWithPosition)e2).getPosition() != null) 
						c = ((EntityWithPosition)e1).getPosition().compareTo(((EntityWithPosition)e2).getPosition());
					else {
						if (e1 instanceof ParameterGroup && e2 instanceof ParameterGroup) {
							c = ((ParameterGroup)e1).getLabel().compareTo(((ParameterGroup)e2).getLabel());
						}
						else if (e1 instanceof Parameter && e2 instanceof Parameter) {
							c = ((Parameter)e1).getName().compareTo(((Parameter)e2).getName());
						}
					}
				}
				
				return dir == SWT.UP ? c : -c;
			}
		});
		treeViewer.expandAll();
		
		// we need to show table borders
		tree.setLinesVisible(true);
		// we need to show the table header
		tree.setHeaderVisible(true);
		tree.setSortColumn(nameColumn);
		tree.setSortDirection(SWT.UP);		
		
		if (protocol == null) {
			treeViewer.setInput(new ArrayList<Object>());
		} else {
			List<Object> paramsAndParamGroups = new ArrayList<>(protocol.getParameterGroups().size() + protocol.getParameters().size());
			paramsAndParamGroups.addAll(protocol.getParameterGroups());
			paramsAndParamGroups.addAll(protocol.getParameters());
			treeViewer.setInput(paramsAndParamGroups);
		}
	    
		// we need to be notified when editors get closed to be able clear the view contents
		eventBroker.subscribe(UIElement.TOPIC_TOBERENDERED, closePartEventHandler);
		deleteParamToolItem = (DirectToolItemImpl) modelService.find("org.grits.toolbox.editor.experimentdesigner.directtoolitem.deleteparameter", part.getToolbar());
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
				  editor = (GraphEditor)((CompatibilityEditor) part.getObject()).getEditor();
				  textEditingSupport.setEditor(editor);
				  unitEditingSupport.setGraphEditor(editor);
				  logger.debug("Parameter View - Active Graph Editor changed to: " + editor.getPartName());
			  }
		  }
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
						if (treeViewer != null && !treeViewer.getTree().isDisposed()) {
 							treeViewer.getTree().setRedraw(true);
 							treeViewer.getTree().removeAll();
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
	
	class ParameterContentProvider implements  ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<?>)inputElement).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof List) {
				return ((List<?>) parentElement).toArray();
			} else if (parentElement instanceof ParameterGroup) {
				return ((ParameterGroup) parentElement).getParameters().toArray();
			} 
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof List) {
				return ((List<?>) element).size() > 0;
			} else if (element instanceof ParameterGroup) {
				return ((ParameterGroup) element).getParameters().size() > 0;
			} 
			return false;
		}
		
	}
	
	@Focus
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	@Inject
	void setSelection(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object selection) {
		if (selection != null && !(selection instanceof StructuredSelection))
			return;
		
		if (selection == null || !(((StructuredSelection) selection).getFirstElement() instanceof GraphNodeEditPart)) {
			protocol = null;
			if (treeViewer != null && !treeViewer.getTree().isDisposed()) {
				treeViewer.getTree().setRedraw(true);
				treeViewer.getTree().removeAll();
			}
		} else if (((StructuredSelection) selection).getFirstElement() instanceof GraphNodeEditPart) {
			Object modelObject = ((GraphNodeEditPart)((StructuredSelection) selection).getFirstElement()).getModel();
			if (modelObject instanceof ProtocolNode) {
				protocol = (ProtocolNode)modelObject;
				List<Object> paramsAndParamGroups = new ArrayList<>();
				if (protocol.getParameterGroups() != null)
					paramsAndParamGroups.addAll(protocol.getParameterGroups());
				if (protocol.getParameters() != null)
					paramsAndParamGroups.addAll(protocol.getParameters());
				if (treeViewer != null && !treeViewer.getTree().isDisposed()) {
					treeViewer.setInput(paramsAndParamGroups);
					treeViewer.expandAll();
				}
			}
			else {
				protocol = null;
				if (treeViewer != null && !treeViewer.getTree().isDisposed()) {
					treeViewer.getTree().setRedraw(true);
					treeViewer.getTree().removeAll();
					
				}
			}
		}
	}

	public ProtocolNode getProtocol() {
		return protocol;
	}
}
