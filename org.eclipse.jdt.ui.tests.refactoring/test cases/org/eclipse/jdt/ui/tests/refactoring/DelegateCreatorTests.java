/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.IOException;

import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateCreator;
import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateFieldCreator;
import org.eclipse.jdt.internal.corext.refactoring.delegates.DelegateMethodCreator;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class DelegateCreatorTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "DelegateCreator/";

	public DelegateCreatorTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void methodHelper(String methodName, String[] args, boolean copy, String newName, String newTypeName) throws Exception {
		ITypeBinding destination= createTargetBinding(newTypeName);
		helper(methodName, args, null, copy, newName, destination);
	}

	private void fieldHelper(String fieldName, boolean copy, String newName, String newTypeName) throws Exception {
		ITypeBinding destination= createTargetBinding(newTypeName);
		helper(null,  null, fieldName, copy, newName, destination);
	}

	private ITypeBinding createTargetBinding(String newTypeName) throws Exception, JavaModelException {
		ITypeBinding destination= null;
		if (newTypeName != null) {
			ICompilationUnit cu2= createCUfromTestFile(getPackageP(), newTypeName);
			IType classNew= getType(cu2, newTypeName);
			CompilationUnit cuNode= new RefactoringASTParser(AST.getJLSLatest()).parse(cu2, true, null);
			TypeDeclaration td= ASTNodeSearchUtil.getTypeDeclarationNode(classNew, cuNode);
			destination= td.resolveBinding();
		}
		return destination;
	}

	private void helper(String methodName, String[] args, String fieldName, boolean copy, String newName, ITypeBinding destination) throws Exception, JavaModelException, CoreException, IOException {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");

		CompilationUnitRewrite rewrite= new CompilationUnitRewrite(cu);
		rewrite.setResolveBindings(false);
		BodyDeclaration d;
		DelegateCreator creator;
		if (methodName != null) {
			IMethod method= classA.getMethod(methodName, args);
			d= ASTNodeSearchUtil.getMethodDeclarationNode(method, rewrite.getRoot());
			creator= new DelegateMethodCreator();
		} else {
			IField field= classA.getField(fieldName);
			d= ASTNodeSearchUtil.getFieldDeclarationNode(field, rewrite.getRoot());
			creator= new DelegateFieldCreator();
		}

		creator.setDeclaration(d);
		creator.setSourceRewrite(rewrite);
		creator.setCopy(copy);
		if (newName != null) creator.setNewElementName(newName);
		if (destination != null) creator.setNewLocation(destination);
		creator.prepareDelegate();
		creator.createEdit();
		CompilationUnitChange createChange= rewrite.createChange(true);
		createChange.initializeValidationData(new NullProgressMonitor());
		createChange.perform(new NullProgressMonitor());
		assertEqualLines("invalid delegate created", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	@Test
	public void testm01() throws Exception {
		// just create a delegate without extras
		methodHelper("foo", new String[0], true, null, null);
	}

	@Test
	public void testm02() throws Exception {
		// copy existing javadoc
		methodHelper("foo", new String[0], true, null, null);
	}

	@Test
	public void testm03() throws Exception {
		// existing annotations
		methodHelper("foo", new String[0], true, null, null);
	}

	@Test
	public void testm04() throws Exception {
		// a new name
		methodHelper("foo", new String[0], true, "bar", null);
	}

	@Test
	public void testm05() throws Exception {
		// a new type
		methodHelper("foo", new String[0], true, null, "B");
	}

	@Test
	public void testm06() throws Exception {
		// a new name and new type
		methodHelper("foo", new String[0], true, "bar", "B");
	}

	@Test
	public void testm07() throws Exception {
		// ensure comments inside parameters et al. are copied as well.
		methodHelper("foo", new String[] { "QString;", "QString;" }, true, "bar", null);
	}

	@Test
	public void testm08() throws Exception {
		// import
		IPackageFragment e= getRoot().createPackageFragment("e", true, null);
		ICompilationUnit cu2= createCUfromTestFile(e, "E");
		IType classNew= getType(cu2, "E");
		CompilationUnit cuNode= new RefactoringASTParser(AST.getJLSLatest()).parse(cu2, true, null);
		TypeDeclaration td= ASTNodeSearchUtil.getTypeDeclarationNode(classNew, cuNode);
		ITypeBinding destination= td.resolveBinding();

		helper("foo", new String[0], null, true, null, destination);
	}

	@Test
	public void testm09() throws Exception {
		// abstract method: ensure no body is created
		methodHelper("foo", new String[0], true, null, null);
	}

	@Test
	public void testm10() throws Exception {
		// interface method: ensure no body is created
		methodHelper("foo", new String[0], true, null, null);
	}

	@Test
	public void testm11() throws Exception {
		// constructor
		methodHelper("A", new String[0], true, null, null);
	}

	// FIELDS

	@Test
	public void testf01() throws Exception {
		// just create a delegate without extras
		fieldHelper("foo", true, null, null);
	}

	@Test
	public void testf02() throws Exception {
		// copy existing javadoc
		fieldHelper("foo", true, null, null);
	}

	@Test
	public void testf03() throws Exception {
		// existing annotations
		fieldHelper("foo", true, null, null);
	}

	@Test
	public void testf04() throws Exception {
		// a new name
		fieldHelper("foo", true, "bar", null);
	}

	@Test
	public void testf05() throws Exception {
		// a new type
		fieldHelper("foo", true, null, "B");
	}

	@Test
	public void testf06() throws Exception {
		// a new name and new type
		fieldHelper("foo", true, "bar", "B");
	}

	@Test
	public void testf07() throws Exception {
		// ensure comments inside parameters et al. are copied as well.
		fieldHelper("foo", true, null, null);
	}

	@Test
	public void testf08() throws Exception {
		// import
		IPackageFragment e= getRoot().createPackageFragment("e", true, null);
		ICompilationUnit cu2= createCUfromTestFile(e, "E");
		IType classNew= getType(cu2, "E");
		CompilationUnit cuNode= new RefactoringASTParser(AST.getJLSLatest()).parse(cu2, true, null);
		TypeDeclaration td= ASTNodeSearchUtil.getTypeDeclarationNode(classNew, cuNode);
		ITypeBinding destination= td.resolveBinding();

		helper(null, null, "foo", true, null, destination);
	}

	@Test
	public void testf09() throws Exception {
		// initializer removed?
		fieldHelper("foo", true, null, null);
	}
}