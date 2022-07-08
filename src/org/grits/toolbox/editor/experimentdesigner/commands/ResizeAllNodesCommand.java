package org.grits.toolbox.editor.experimentdesigner.commands;

import java.util.HashMap;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.commands.Command;
import org.grits.toolbox.editor.experimentdesigner.model.ExperimentGraph;
import org.grits.toolbox.editor.experimentdesigner.model.GraphNode;

public class ResizeAllNodesCommand extends Command {

	private final ExperimentGraph parent;
	HashMap<GraphNode, Dimension> sizeMap;
	
	public ResizeAllNodesCommand(ExperimentGraph graph) {
		this.parent = graph;
	}
	
	/**
	 * Can execute if all the necessary information has been provided.
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		return parent != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		// store all sizes
		sizeMap = new HashMap<>();
		for (GraphNode node : parent.getNodes()) {
			Dimension current = node.getSize();
			sizeMap.put(node, current);
		}
		redo();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		Dimension maxSize = new Dimension(0, 0);
		for (GraphNode node : parent.getNodes()) {
			Dimension current = node.getSize();
			if (current.getArea() > maxSize.getArea())
				maxSize = current;
		}
		
		for (GraphNode node : parent.getNodes()) {
			node.setSize(maxSize);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		if (sizeMap != null) {
			for (GraphNode node : parent.getNodes()) {
				Dimension oldSize = sizeMap.get(node);
				if (oldSize != null)
					node.setSize(oldSize);
			}
		}
	}
}
