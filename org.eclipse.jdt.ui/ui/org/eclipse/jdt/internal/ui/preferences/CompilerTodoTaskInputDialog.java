/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.CompilerConfigurationBlock.TodoTask;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

/**
 * Dialog to enter a na new task tag
 */
public class CompilerTodoTaskInputDialog extends StatusDialog {
	
	private class CompilerTodoTaskInputAdapter implements IDialogFieldListener {
		public void dialogFieldChanged(DialogField field) {
			doValidation();
		}			
	}
	
	private StringDialogField fNameDialogField;
	private ComboDialogField fPriorityDialogField;
	private TodoTask fEdit;
	
	private List fExistingNames;
		
	public CompilerTodoTaskInputDialog(Shell parent, TodoTask task, List existingEntries) {
		super(parent);
		
		fExistingNames= new ArrayList(existingEntries.size());
		for (int i= 0; i < existingEntries.size(); i++) {
			TodoTask curr= (TodoTask) existingEntries.get(i);
			if (!curr.equals(task)) {
				fExistingNames.add(curr.name);
			}
		}
		
		if (task == null) {
			setTitle(JavaUIMessages.getString("CompilerTodoTaskInputDialog.new.title")); //$NON-NLS-1$
		} else {
			setTitle(JavaUIMessages.getString("CompilerTodoTaskInputDialog.edit.title")); //$NON-NLS-1$
		}

		CompilerTodoTaskInputAdapter adapter= new CompilerTodoTaskInputAdapter();

		fNameDialogField= new StringDialogField();
		fNameDialogField.setLabelText(JavaUIMessages.getString("CompilerTodoTaskInputDialog.name.label")); //$NON-NLS-1$
		fNameDialogField.setDialogFieldListener(adapter);
		
		fNameDialogField.setText((task != null) ? task.name : "");
		
		String[] items= new String[] {
			JavaUIMessages.getString("CompilerTodoTaskInputDialog.priority.high"),
			JavaUIMessages.getString("CompilerTodoTaskInputDialog.priority.normal"),
			JavaUIMessages.getString("CompilerTodoTaskInputDialog.priority.low")
		};
		
		fPriorityDialogField= new ComboDialogField(SWT.READ_ONLY);
		fPriorityDialogField.setLabelText(JavaUIMessages.getString("CompilerTodoTaskInputDialog.priority.label"));
		fPriorityDialogField.setItems(items);
		if (task != null) {
			if (JavaCore.COMPILER_TASK_PRIORITY_HIGH.equals(task.priority)) {
				fPriorityDialogField.selectItem(0);
			} else if (JavaCore.COMPILER_TASK_PRIORITY_NORMAL.equals(task.priority)) {
				fPriorityDialogField.selectItem(1);
			} else {
				fPriorityDialogField.selectItem(2);
			}
		} else {
			fPriorityDialogField.selectItem(1);
		}
	}
	
	public TodoTask getResult() {
		TodoTask task= new TodoTask();
		task.name= fNameDialogField.getText();
		switch (fPriorityDialogField.getSelectionIndex()) {
			case 0 :
					task.priority= JavaCore.COMPILER_TASK_PRIORITY_HIGH;
				break;
			case 1 :
					task.priority= JavaCore.COMPILER_TASK_PRIORITY_NORMAL;
				break;
			default :
					task.priority= JavaCore.COMPILER_TASK_PRIORITY_LOW;
				break;				
		}
		return task;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);
		
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		inner.setLayout(layout);
		
		fNameDialogField.doFillIntoGrid(inner, 2);
		fPriorityDialogField.doFillIntoGrid(inner, 2);
		
		LayoutUtil.setHorizontalGrabbing(fNameDialogField.getTextControl(null));
		LayoutUtil.setWidthHint(fNameDialogField.getTextControl(null), convertWidthInCharsToPixels(30));
		
		fNameDialogField.postSetFocusOnDialogField(parent.getDisplay());
		
		return composite;
	}
		
	private void doValidation() {
		StatusInfo status= new StatusInfo();
		String newText= fNameDialogField.getText();
		if (newText.length() == 0) {
			status.setError(JavaUIMessages.getString("CompilerTodoTaskInputDialog.error.enterName")); //$NON-NLS-1$
		} else {
			if (newText.indexOf(',') != -1) {
				status.setError(JavaUIMessages.getString("CompilerTodoTaskInputDialog.error.comma")); //$NON-NLS-1$
			} else {
				if (fExistingNames.contains(newText)) {
					status.setError(JavaUIMessages.getString("CompilerTodoTaskInputDialog.error.entryExists")); //$NON-NLS-1$
				}
			}
		}
		updateStatus(status);
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.IMPORT_ORGANIZE_INPUT_DIALOG);
	}
}