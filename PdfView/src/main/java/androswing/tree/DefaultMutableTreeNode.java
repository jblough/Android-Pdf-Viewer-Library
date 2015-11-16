package androswing.tree;

import java.util.ArrayList;

public class DefaultMutableTreeNode {
	private DefaultMutableTreeNode parent;
	private Object userObject; 
	private ArrayList<DefaultMutableTreeNode> children;
	protected DefaultMutableTreeNode(){
		parent = null;
		userObject = null;
		children = new ArrayList<DefaultMutableTreeNode>();
	}
	protected Object getUserObject() {
		return userObject;
	}
    protected void setUserObject(Object userObject) {
    	this.userObject = userObject;
	}

	public void add(DefaultMutableTreeNode newChild) {
		newChild.parent = this;
		children.add(newChild);
	}
	public DefaultMutableTreeNode getParent() {
		return parent;
	}


}
