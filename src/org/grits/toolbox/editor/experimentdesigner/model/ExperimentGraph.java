package org.grits.toolbox.editor.experimentdesigner.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.ui.IEditorPart;
import org.grits.toolbox.core.datamodel.Entry;

@XmlRootElement
@XmlType(propOrder={"description", "nodes", "connections"})
public class ExperimentGraph extends ModelElement {
	
	public static String CURRENT_VERSION = "1.0";

	/** Property ID to use when a child is added to this graph. */
	public static final String CHILD_ADDED_PROP = "Graph.ChildAdded";
	/** Property ID to use when a child is removed from this graph. */
	public static final String CHILD_REMOVED_PROP = "Graph.ChildRemoved";
	private static final long serialVersionUID = 1;
	
	private List<GraphNode> nodes = new ArrayList<GraphNode>();
	
	private List<Connection> connections = new ArrayList<Connection>();
	
	private String name;
	private String description;
	private String createdBy;
	private Date dateCreated;
	private String uri;
	private Entry projectEntry;
	
	
	/**
	 * Add a node to this graph.
	 * 
	 * @param n
	 *            a non-null node instance
	 * @return true, if the node was added, false otherwise
	 */
	public boolean addChild(GraphNode n) {
		if (n != null && nodes.add(n)) {
			n.setId(getNextAvailableNodeId());
			firePropertyChange(CHILD_ADDED_PROP, null, n);
			if (this.connections == null) {
				this.connections = new ArrayList<Connection>();
			}
			this.connections.addAll(n.getSourceConnections());
			this.connections.addAll(n.getTargetConnections());
			return true;
		}
		return false;
	}
	
	/**
	 * checks the existing nodes' ids to figure out the maximum id
     * @return the next available node id
     */
    private Integer getNextAvailableNodeId()
    {
        Integer lastMaxId = 0;
        for(GraphNode node : this.nodes)
        {
            if(node.getId() != null 
                    && node.getId() >lastMaxId)
            {
                lastMaxId = node.getId();
            }
        }
        return lastMaxId+1;
    }

	/**
	 * Return a List of Nodes in this diagram. The returned List should not be
	 * modified.
	 */
	@SuppressWarnings("rawtypes")
	public List getChildren() {
		return nodes;
	}

	/**
	 * Remove a node from this graph.
	 * 
	 * @param n
	 *            a non-null node instance;
	 * @return true, if the node was removed, false otherwise
	 */
	public boolean removeChild(GraphNode n) {
		if (n != null && nodes.remove(n)) {
			firePropertyChange(CHILD_REMOVED_PROP, null, n);
			if (this.connections != null) {
				this.connections.removeAll(n.getSourceConnections());
				this.connections.removeAll(n.getTargetConnections());
			}
			return true;
		}
		return false;
	}

	@XmlElements({ 
	    @XmlElement(name="input", type=InputNode.class),
	    @XmlElement(name="output", type=OutputNode.class),
	    @XmlElement(name="protocol", type=ProtocolNode.class)
	})
	public List<GraphNode> getNodes() {
		return nodes;
	}

	public void setNodes(List<GraphNode> nodes) {
		this.nodes = nodes;
	}

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public String getDescription() {
		return description;
	}

	
	public void setDescription(String description) {
		this.description = description;
	}

	@XmlAttribute
	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	@XmlAttribute
	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	@XmlAttribute
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	
	public List<Connection> getConnections() {
		if (this.connections != null) {
			// add all the connections added to the nodes to the connections list before returning it
			for (Iterator<GraphNode> iterator = nodes.iterator(); iterator.hasNext();) {
				GraphNode node = (GraphNode) iterator.next();
				List<Connection> sourceConnections = node.getSourceConnections();
				for (Iterator<Connection> iterator2 = sourceConnections.iterator(); iterator2
						.hasNext();) {
					Connection connection = (Connection) iterator2.next();
					if (!connections.contains(connection)) 
						connections.add(connection);
				}
				List<Connection> targetConnections = node.getTargetConnections();
				for (Iterator<Connection> iterator2 = targetConnections.iterator(); iterator2
						.hasNext();) {
					Connection connection = (Connection) iterator2.next();
					if (!connections.contains(connection)) 
						connections.add(connection);
				}
			}
		}
		return this.connections;
	}
	
	@XmlElementWrapper(name="connections")
	public void setConnections (List<Connection> connections) {	
		this.connections = connections;
	}
	
	/**
	 * this needs to be called every time the graph is unmarshalled 
	 * to set the connections to the appropriate nodes
	 */
	@SuppressWarnings("rawtypes")
	public void setConnectionsToNodes () {
		if (this.connections == null)
			return;
		for (Iterator<Connection> iterator = this.connections.iterator(); iterator.hasNext();) {
			Connection connection = (Connection) iterator.next();
			GraphNode source = connection.getSource();
			for (Iterator iterator2 = nodes.iterator(); iterator2
					.hasNext();) {
				GraphNode node = (GraphNode) iterator2.next();
				if (node.equals(source)) {
					connection.setSource(node);
					node.addConnection(connection);
					break;
				}
			}
			
			GraphNode target = connection.getTarget();
			for (Iterator iterator2 = nodes.iterator(); iterator2
					.hasNext();) {
				GraphNode node = (GraphNode) iterator2.next();
				if (node.equals(target)) {
					connection.setTarget(node);
					node.addConnection(connection);
					break;
				}
			}
		}
	}

	public void setProjectEntry(Entry parent) {
		this.projectEntry = parent;
	}

	@XmlTransient
	public Entry getProjectEntry() {
		return projectEntry;
	}
	
	public Dimension calculateSize() {
		Dimension size = new Dimension();
		float offsetX=0;
		float rightX=0;
		float bottomY=0;
		int i=0;
		for (GraphNode node : nodes) {
			if (i==0)
				offsetX = node.getLocation().x;
			bottomY = Math.max(node.getLocation().y+ node.getSize().height, bottomY);
			rightX = Math.max(node.getLocation().x + node.getSize().width, rightX);
			i++;
		}
		
		size.setSize((int)Math.ceil(rightX+offsetX), (int)Math.ceil(bottomY));
		return size;
	}

	public void removeConnection(Connection connection) {
		if (connection != null)
			this.connections.remove(connection);
		
	}

	public void addConnection(Connection connection) {
		if (connection != null && !this.connections.contains(connection))
			this.connections.add(connection);
		
	}
}
