/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jface.util.Assert;

/* package */ class PseudoJavaChangeElement extends ChangeElement {

	private IJavaElement fJavaElement;
	private List fChildren;

	public PseudoJavaChangeElement(ChangeElement parent, IJavaElement element) {
		super(parent);
		fJavaElement= element;
		Assert.isNotNull(fJavaElement);
	}
	
	/**
	 * Returns the Java element.
	 * 
	 * @return the Java element managed by this node
	 */
	public IJavaElement getJavaElement() {
		return fJavaElement;
	}

	/* non Java-doc
	 * @see ChangeElement#setActive
	 */
	public void setActive(boolean active) {
		for (Iterator iter= fChildren.iterator(); iter.hasNext();) {
			ChangeElement element= (ChangeElement)iter.next();
			element.setActive(active);
		}
	}
	
	/* non Java-doc
	 * @see ChangeElement.getActive
	 */
	public int getActive() {
		Assert.isTrue(fChildren.size() > 0);
		int result= ((ChangeElement)fChildren.get(0)).getActive();
		for (int i= 1; i < fChildren.size(); i++) {
			ChangeElement element= (ChangeElement)fChildren.get(i);
			result= ACTIVATION_TABLE[element.getActive()][result];
			if (result == PARTLY_ACTIVE)
				break;
		}
		return result;
	}
	
	/* non Java-doc
	 * @see ChangeElement.getChildren
	 */
	public ChangeElement[] getChildren() {
		if (fChildren == null)
			return EMPTY_CHILDREN;
		return (ChangeElement[]) fChildren.toArray(new ChangeElement[fChildren.size()]);
	}
	
	/**
	 * Adds the given <code>TextEditChangeElement<code> as a child to this 
	 * <code>PseudoJavaChangeElement</code>
	 * 
	 * @param child the child to be added
	 */
	public void addChild(TextEditChangeElement child) {
		doAddChild(child);
	}
	
	/**
	 * Adds the given <code>PseudoJavaChangeElement<code> as a child to this 
	 * <code>PseudoJavaChangeElement</code>
	 * 
	 * @param child the child to be added
	 */
	public void addChild(PseudoJavaChangeElement child) {
		doAddChild(child);
	}
	
	private void doAddChild(ChangeElement child) {
		if (fChildren == null)
			fChildren= new ArrayList(2);
		fChildren.add(child);
	}
}

