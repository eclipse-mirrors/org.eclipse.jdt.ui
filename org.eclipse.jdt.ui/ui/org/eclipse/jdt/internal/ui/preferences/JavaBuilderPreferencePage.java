/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;

public class JavaBuilderPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private StringDialogField fResourceFilterField;
	private StatusInfo fResourceFilterStatus;
	
	private Hashtable fWorkingValues;

	private static final String PREF_RESOURCE_FILTER= "org.eclipse.jdt.core.builder.resourceCopyExclusionFilter";

	private static String[] getAllKeys() {
		return new String[] {
			PREF_RESOURCE_FILTER
		};	
	}
	
	public JavaBuilderPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("JavaBuilderPreferencePage.description")); //$NON-NLS-1$
	
		fWorkingValues= JavaCore.getOptions();


		IDialogFieldListener listener= new IDialogFieldListener() {
			public void dialogFieldChanged(DialogField field) {
				doValidation();
			}
		};
		
		fResourceFilterField= new StringDialogField();
		fResourceFilterField.setDialogFieldListener(listener);
		fResourceFilterField.setLabelText(JavaUIMessages.getString("JavaBuilderPreferencePage.filter.label"));
				
		updateControls();				
	}
	

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(layout);

		DialogField resourceFilterLabel= new DialogField();
		resourceFilterLabel.setLabelText(JavaUIMessages.getString("JavaBuilderPreferencePage.filter.description"));

		resourceFilterLabel.doFillIntoGrid(composite, 2);
		LayoutUtil.setWidthHint(resourceFilterLabel.getLabelControl(null), convertWidthInCharsToPixels(80));

		fResourceFilterField.doFillIntoGrid(composite, 2);
		LayoutUtil.setHorizontalGrabbing(fResourceFilterField.getTextControl(null));
		LayoutUtil.setWidthHint(fResourceFilterField.getTextControl(null), convertWidthInCharsToPixels(50));

		return composite;
	}
	
	/**
	 * Initializes the current options (read from preference store)
	 */
	public static void initDefaults(IPreferenceStore store) {
		Hashtable hashtable= JavaCore.getDefaultOptions();
		Hashtable currOptions= JavaCore.getOptions();
		String[] allKeys= getAllKeys();
		for (int i= 0; i < allKeys.length; i++) {
			String key= allKeys[i];
			String defValue= (String) hashtable.get(key);
			if (defValue != null) {
				store.setDefault(key, defValue);
			} else {
				JavaPlugin.logErrorMessage("JavaBuilderPreferencePage: value is null: " + key);
			}
			// update the JavaCore options from the pref store
			String val= store.getString(key);
			if (val != null) {
				currOptions.put(key, val);
			}			
		}
		JavaCore.setOptions(currOptions);
	}	
	

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}


	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		String[] allKeys= getAllKeys();
		// preserve other options
		// store in JCore and the preferences
		Hashtable actualOptions= JavaCore.getOptions();
		IPreferenceStore store= getPreferenceStore();
		boolean hasChanges= false;
		for (int i= 0; i < allKeys.length; i++) {
			String key= allKeys[i];
			String val=  (String) fWorkingValues.get(key);
			String oldVal= (String) actualOptions.get(key);
			hasChanges= hasChanges | !val.equals(oldVal);
			
			actualOptions.put(key, val);
			store.setValue(key, val);
		}
		JavaCore.setOptions(actualOptions);
		
		if (hasChanges) {
			String title= JavaUIMessages.getString("JavaBuilderPreferencePage.needsbuild.title");
			String message= JavaUIMessages.getString("JavaBuilderPreferencePage.needsbuild.message");
			if (MessageDialog.openQuestion(getShell(), title, message)) {
				doFullBuild();
			}
		}
		return super.performOk();
	}


	private void doValidation() {
		IStatus status= validateResourceFilters();
		updateStatus(status);
	}
	
	private String[] getFilters(String text) {
		StringTokenizer tok= new StringTokenizer(text, ",");
		int nTokens= tok.countTokens();
		String[] res= new String[nTokens];
		for (int i= 0; i < res.length; i++) {
			res[i]= tok.nextToken().trim();
		}
		return res;
	}
	
	
	private IStatus validateResourceFilters() {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
				
		String text= fResourceFilterField.getText();
		String[] filters= getFilters(text);
		for (int i= 0; i < filters.length; i++) {
			String fileName= filters[i].replace('*', 'x');
			IStatus status= workspace.validateName(fileName, IResource.FILE);
			if (status.matches(IStatus.ERROR)) {
				String message= JavaUIMessages.getFormattedString("JavaBuilderPreferencePage.filter.invalidsegment.error", status.getMessage());
				return new StatusInfo(IStatus.ERROR, message);
			}
		}
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < filters.length; i++) {
			if (i > 0) {
				buf.append(',');
			}
			buf.append(filters[i]);
		}
		fWorkingValues.put(PREF_RESOURCE_FILTER, buf.toString());
		return new StatusInfo();
	}
	

	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

	private void doFullBuild() {
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			String title= JavaUIMessages.getString("JavaBuilderPreferencePage.builderror.title");
			String message= JavaUIMessages.getString("JavaBuilderPreferencePage.builderror.message");
			ExceptionHandler.handle(e, getShell(), title, message);
		}
	}		
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fWorkingValues= JavaCore.getDefaultOptions();
		updateControls();
		doValidation();
		super.performDefaults();
	}
	
	private void updateControls() {
		// update the UI
		String[] filters= getFilters((String) fWorkingValues.get(PREF_RESOURCE_FILTER));
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < filters.length; i++) {
			if (i > 0) {
				buf.append(", ");
			}
			buf.append(filters[i]);			
		}
		fResourceFilterField.setText(buf.toString());
	}

}