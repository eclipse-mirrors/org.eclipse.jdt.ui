/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.compare.EditionSelectionDialog;
import org.eclipse.compare.HistoryItem;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.compare.JavaHistoryAction.JavaTextBufferNode;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;


/**
 * Provides "Replace from local history" for Java elements.
 */
public class JavaReplaceWithEditionAction extends JavaHistoryAction {
				
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.ReplaceWithEditionAction"; //$NON-NLS-1$
	
	private boolean fPrevious= false;

	
	public JavaReplaceWithEditionAction() {
	}
	
	public JavaReplaceWithEditionAction(boolean previous) {
		fPrevious= previous;
	}	

	// CompareMessages.getString("ReplaceFromHistory.action.label")
	
	protected ITypedElement[] buildEditions(ITypedElement target, IFile file, IFileState[] states) {
		ITypedElement[] editions= new ITypedElement[states.length+1];
		editions[0]= new ResourceNode(file);
		for (int i= 0; i < states.length; i++)
			editions[i+1]= new HistoryItem(target, states[i]);
		return editions;
	}

	/*
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		
		String errorTitle= CompareMessages.getString("ReplaceFromHistory.title"); //$NON-NLS-1$
		String errorMessage= CompareMessages.getString("ReplaceFromHistory.internalErrorMessage"); //$NON-NLS-1$
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		// shell can be null; as a result error dialogs won't be parented
		
		ISelection selection= getSelection();
		IMember input= getEditionElement(selection);
		if (input == null) {
			// shouldn't happen because Action should not be enabled in the first place
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
		
		IFile file= getFile(input);
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
										
		boolean inEditor= beingEdited(file);
		if (inEditor)
			input= (IMember) getWorkingCopy(input);

		// get a TextBuffer where to insert the text
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(file);

			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			
			ITypedElement target= new JavaTextBufferNode(buffer, inEditor);

			ITypedElement[] editions= buildEditions(target, file);

			ITypedElement ti= null;
			if (fPrevious)
				ti= d.selectPreviousEdition(target, editions, input);
			else
				ti= d.selectEdition(target, editions, input);
						
			if (ti instanceof IStreamContentAccessor) {
														
				// from the edition get the lines (text) to insert
				String[] lines= null;
				try {
					lines= JavaCompareUtilities.readLines(((IStreamContentAccessor) ti).getContents());								
				} catch (CoreException ex) {
					JavaPlugin.log(ex);
				}
				if (lines == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;
				}
				
				MemberEdit edit= new MemberEdit(input, MemberEdit.REPLACE, lines,
										CodeFormatterPreferencePage.getTabSize());
				edit.setAddLineSeparators(false);
										
				IProgressMonitor nullProgressMonitor= new NullProgressMonitor();
				
				TextBufferEditor editor= new TextBufferEditor(buffer);
				editor.add(edit);
				editor.performEdits(nullProgressMonitor);
				
				final TextBuffer bb= buffer;
				IRunnableWithProgress r= new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) throws InvocationTargetException {
						try {
							TextBuffer.commitChanges(bb, false, pm);
						} catch (CoreException ex) {
							throw new InvocationTargetException(ex);
						}
					}
				};
				
				if (inEditor) {
					JavaEditor je= getEditor(file);
					if (je != null)
						je.setFocus();
					// we don't show progress
					r.run(nullProgressMonitor);
				} else {
					ProgressMonitorDialog pd= new ProgressMonitorDialog(shell);
					pd.run(true, false, r);
				}
				
			}
	 	} catch(InvocationTargetException ex) {
			JavaPlugin.log(ex);
			MessageDialog.openError(shell, errorTitle, errorMessage);
			
		} catch(InterruptedException ex) {
			// shouldn't be called because is not cancable
			
		} catch(CoreException ex) {
			JavaPlugin.log(ex);
			MessageDialog.openError(shell, errorTitle, errorMessage);
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}
	
	private JavaEditor getEditor(IFile file) {
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getDirtyEditors();
				for (int z= 0; z < editors.length; z++) {
					IEditorPart ep= editors[z];
					if (ep instanceof JavaEditor)
						return (JavaEditor) ep;
				}
			}
		}
		return null;
	}
}
