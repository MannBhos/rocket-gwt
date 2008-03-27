/*
 * Copyright Miroslav Pokorny
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rocket.compiler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import rocket.util.client.Checker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;

/**
 * The Gwt compiler typically inserts a clint call before any static field
 * references belonging to a class with a static initializer. This class attempts
 * to spot and record field references that dont need the clint call because
 * with the same method body exists a call to a method or field which guarantees
 * that the clint would have been called.
 * 
 * @author Miroslav Pokorny
 * 
 */
public class StaticFieldClinitRemover implements JavaCompilationWorker {

	private final static boolean VISIT_CHILD_NODES = true;

	/**
	 * Kicks off the process that involves visiting all methods and attempting
	 * to locate field references to static methods.
	 * 
	 */
	public boolean work(final JProgram jprogram, final TreeLogger logger) {
		final TreeLogger branch = logger.branch(TreeLogger.INFO, this.getClass().getName(), null);

		// first we need to know which types have static initializers...
		final Set typesWithAStaticInitializer = this.findTypesWithStaticInitializers(jprogram, branch);

		// visit all methods finding any static field accesses that dont need a
		// clint.
		this.findStaticFieldReferencesNotRequiringClints( jprogram, typesWithAStaticInitializer, branch);

		branch.log(TreeLogger.DEBUG,"No changes were committed.", null);
		return false;
	}

	/**
	 * Finds all types that have static initializers and populates a set,
	 * logging each type.
	 * 
	 * @param jprogram
	 * @param logger
	 * @return
	 */
	protected Set findTypesWithStaticInitializers( final JProgram jprogram, final TreeLogger logger ){
		Checker.notNull( "parameter:jprogram", jprogram );
		Checker.notNull( "parameter:logger", logger );

		final TreeLogger branch = logger.branch( TreeLogger.DEBUG, "Finding types with static initializers", null );

		final Set types = new HashSet();
		final JVisitor visitor = new JVisitor() {

			public boolean visit( final JClassType type, final Context context ){
				if( type.hasStaticInitializer() ){
					final boolean newAdd = types.add( type );
					if( newAdd ){
						branch.log( TreeLogger.DEBUG, type.getName(), null );
					}
				}
				return ! VISIT_CHILD_NODES;
			}

			public boolean visit( final JInterfaceType type, final Context context ){
				return ! VISIT_CHILD_NODES;
			}

			public boolean visit( final JArrayType type, final Context context ){
				return ! VISIT_CHILD_NODES;
			}			
		};
		visitor.accept(jprogram);

		return types;
	}

	/**
	 * Visits each and every method within this program and attempts to find
	 * field references that dont require a clint because a method or field
	 * belonging to the same enclosing type preceeds them.
	 * 
	 * @param jprogram
	 * @param typesWithAStaticInitializer
	 * @param logger
	 */
	protected void findStaticFieldReferencesNotRequiringClints( final JProgram jprogram, final Set typesWithAStaticInitializer, final TreeLogger logger ){
		Checker.notNull( "parameter:jprogram", jprogram );
		Checker.notNull( "parameter:typesWithAStaticInitializer", typesWithAStaticInitializer );
		Checker.notNull( "parameter:logger", logger );

		final TreeLogger classTypeLogger = logger.branch( TreeLogger.DEBUG, "Finding static field references.", null );
		final LoggingJModVisitor visitor = new LoggingJModVisitor(){
			public Type getLoggingLevel( final JNode node ){
				return TreeLogger.DEBUG;
			}

			public boolean visit( final JArrayType array, final Context context ){
				return ! VISIT_CHILD_NODES; 
			}
			public boolean visit( final JInterfaceType interfacee, final Context context){
				return ! VISIT_CHILD_NODES;
			}

			public boolean visit( final JMethod method, final Context context ){
				boolean visitMethodBody = ! VISIT_CHILD_NODES;
				if( false == method.isStaticInitializer() ){
					super.visit(method, context );
					this.setCallSites( new HashSet() );
					this.setFieldReferences( new HashSet() );

					visitMethodBody = VISIT_CHILD_NODES;
				}

				return visitMethodBody;
			}

			public void endVisit( final JMethod method, final Context context ){
				this.clearCallSites();
				this.clearFieldReferences();
			}

			/**
			 * Contains all the static method call sites that are currently
			 * active in this scope.
			 */
			private Set callSites;

			Set getCallSites(){
				Checker.notNull( "field:callSites", callSites );
				return this.callSites;
			}
			void setCallSites( final Set callSites ){
				Checker.notNull( "parameter:callSites", callSites );
				this.callSites = callSites;
			}
			void clearCallSites(){
				this.callSites = null;
			}

			/**
			 * Contains all the static field references that are currently
			 * active in this scope.
			 */
			private Set fieldReferences;

			Set getFieldReferences(){
				Checker.notNull( "field:fieldReferences", fieldReferences );
				return this.fieldReferences;
			}
			void setFieldReferences( final Set fieldReferences ){
				Checker.notNull( "parameter:fieldReferences", fieldReferences );
				this.fieldReferences = fieldReferences;
			}
			void clearFieldReferences(){
				this.fieldReferences = null;
			}

			public boolean visit( final JMethodCall callSite, final Context context ){
				this.addCallSite( callSite );
				return VISIT_CHILD_NODES;
			}

			public void addCallSite( final JMethodCall methodCall ){
				Checker.notNull("parameter:methodCall", methodCall );

				while( true ){
					final JMethod target = methodCall.getTarget();

					// ignore static dispatcher methods...
					if( target.isStaticDispatcher() ){
						break;
					}

					// ignore static initializers...
					if( target.isStaticInitializer() ){
						break;
					}

					// the enclosing type for this method must have a static initializer.
					final JReferenceType enclosingType = target.getEnclosingType();
					if( false == typesWithAStaticInitializer.contains( enclosingType )){
						break;
					}

					final TreeLogger logger = this.getLogger();
					if( logger.isLoggable( TreeLogger.DEBUG )){
						logger.log( TreeLogger.DEBUG, methodCall.toSource(), null );
					}

					// record this call site...
					final int scope = this.getScope();
					this.getCallSites().add(new ClinitReference(){
						boolean callsEnclosingTypeClinit( final JFieldRef fieldReference ){
							final JReferenceType fieldReferenceType = fieldReference.getField().getEnclosingType();
							return enclosingType.equals( fieldReferenceType );
						}
						int getScope(){
							return scope;
						}
						String toSource(){
							return methodCall.toSource();
						}
					});
					break;
				}
			}

			public void endVisit( final JMethodCall callSite, final Context context ){
				this.getCallSites().remove( callSite );
			}

			public boolean visit( final JFieldRef fieldReference, final Context context ){
				if( fieldReference.getField().isStatic() ){
					this.addFieldReference( fieldReference );

					final Set fieldReferences = this.getFieldReferences();
					final Set callSites = this.getCallSites();
					final TreeLogger logger = this.getLogger();
					StaticFieldClinitRemover.this.processFieldReference(fieldReference, callSites, fieldReferences, logger );
				}
				return VISIT_CHILD_NODES;
			}

			protected void addFieldReference( final JFieldRef fieldReference ){
				Checker.notNull("parameter:fieldReference", fieldReference);

				while( true ){
					final JField field = fieldReference.getField();
					// the field must be static...
					if( false == field.isStatic() ){
						break;
					}

					// enclosing type doesnt have a static initializer...
					final JReferenceType enclosingType = field.getEnclosingType();
					if( false == typesWithAStaticInitializer.contains( enclosingType )){
						break;
					}

					final int scope = this.getScope();
					this.getFieldReferences().add(new ClinitReference(){
						boolean callsEnclosingTypeClinit( final JFieldRef otherFieldReference ){
							boolean same = false;

							if( fieldReference != otherFieldReference ){
								final JReferenceType otherFieldReferenceType = otherFieldReference.getField().getEnclosingType();
								same = enclosingType.equals( otherFieldReferenceType );
							}

							return same;
						}
						int getScope(){
							return scope;
						}
						String toSource(){
							return fieldReference.toSource();
						}
					});
					break;
				}

			}

			public void endVisit( final JFieldRef fieldReference, final Context context ){
				this.getFieldReferences().remove( fieldReference );
			}

			public boolean visit(final JBlock block, final Context context) {
				this.incrementScope();
				return VISIT_CHILD_NODES;
			} 

			public void endVisit(final JBlock block, final Context context) {
				this.decrementScope();
			}

			public boolean visit(final JDoStatement doStatement, final Context context) {
				this.incrementScope();
				return VISIT_CHILD_NODES;
			} 

			public void endVisit(final JDoStatement doStatement, final Context context) {
				this.decrementScope();
			}
			public boolean visit(final JForStatement forStatement, final Context context) {
				this.incrementScope();
				return VISIT_CHILD_NODES;
			} 

			public void endVisit(final JForStatement forStatement, final Context context) {
				this.decrementScope();
			}

			public boolean visit(final JIfStatement ifStatement, final Context context) {
				this.incrementScope();
				return VISIT_CHILD_NODES;
			} 

			public void endVisit(final JIfStatement ifStatement, final Context context) {
				this.decrementScope();
			}

			public boolean visit(final JSwitchStatement switchStatement, final Context context) {
				this.incrementScope();
				return VISIT_CHILD_NODES;
			} 

			public void endVisit(final JSwitchStatement switchStatement, final Context context) {
				this.decrementScope();
			}

			public boolean visit(final JWhileStatement whileStatement, final Context context) {
				this.incrementScope();
				return VISIT_CHILD_NODES;
			} 

			public void endVisit(final JWhileStatement whileStatement, final Context context) {
				this.decrementScope();
			}

			/**
			 * The current scope
			 */
			private int scope;

			public int getScope() {
				return this.scope;
			}

			public void setScope(final int scope) {
				this.scope = scope;
			}

			protected void incrementScope(){
				this.setScope( this.getScope() + 1 );
			}

			/**
			 * Not only decrements the scope counter but also removes any references that are out of scope.
			 */
			protected void decrementScope(){			
				final int newScope = this.getScope() - 1;

				this.removeOutOfScopeReferences( newScope , this.getCallSites() );
				this.removeOutOfScopeReferences( newScope , this.getFieldReferences() );

				this.setScope( newScope );
			}

			/**
			 * Loops thru all HasClintReference instances removing those that are out of scope.
			 * @param scope
			 * @param references
			 */
			protected void removeOutOfScopeReferences( final int scope, final Set references ){
				Checker.notNull("parameter:references", references );

				final Iterator i = references.iterator();
				while( i.hasNext() ){
					final ClinitReference reference = (ClinitReference)i.next();
					if( reference.getScope() > scope ){
						i.remove();
					}
				}
			}


		};
		visitor.accept( jprogram, classTypeLogger );
	}

	/**
	 * Represents a handle to a class via a method call or field reference
	 */
	static abstract class ClinitReference{
		abstract boolean callsEnclosingTypeClinit( JFieldRef fieldReference );

		abstract int getScope();
		/**
		 * Returns the source form of the enclosed statement/expression.
		 * @return
		 */
		abstract String toSource();
	}

	/**
	 * Checks and records whether the given field reference already has a preceeding method callsite or field reference that would have ensure the class's static initializer has been run.
	 * @param fieldReference
	 * @param callSites
	 * @param fieldReferences
	 * @param logger
	 */
	protected void processFieldReference( final JFieldRef fieldReference, final Set callSites, final Set fieldReferences, final TreeLogger logger ){
		Checker.notNull("parameter:fieldReference", fieldReference );

		// field must static.
		if( false == fieldReference.getField().isStatic() ){
			throw new AssertionError( "The parameter:fieldReference is not a static field reference, fieldReference: " + fieldReference );
		}

		Checker.notNull("parameter:callSites", callSites );
		Checker.notNull("parameter:fieldReferences", fieldReferences );

		TreeLogger branch = TreeLogger.NULL;
		if( logger.isLoggable( TreeLogger.DEBUG )){
			branch = logger.branch( TreeLogger.DEBUG, Compiler.getFullyQualifiedFieldName(fieldReference.getField()), null );
		}

		boolean requiresClint = true;

		while( true ){
			if( this.findPreviousClinit(fieldReference, callSites, branch )){
				requiresClint = false;
				break;
			}
			if( this.findPreviousClinit(fieldReference, fieldReferences, branch )){
				requiresClint = false;
				break;
			}
			break;
		}

		// requires clint record it...
		if( requiresClint ){
			if( logger.isLoggable( TreeLogger.DEBUG )){
				branch.log( TreeLogger.DEBUG, "Clinit required.", null );
			}
		} else {
			this.markFieldReferenceAsNotRequiringClinit( fieldReference, branch );
		}
	}

	/**
	 * Scans all the references attempting to find a enclosing type match.
	 * @param fieldReference
	 * @param references
	 * @param logger
	 * @return True if a reference was found that guarantees static the classes static initializer was executed, false otherwise.
	 */
	protected boolean findPreviousClinit( final JFieldRef fieldReference, final Set references, final TreeLogger logger ){
		Checker.notNull("parameter:fieldReference", fieldReference );
		Checker.notNull("parameter:references", references );
		Checker.notNull("parameter:logger", logger );

		boolean found = false;

		final Iterator i = references.iterator();
		while( i.hasNext() ){
			final ClinitReference reference = (ClinitReference)i.next();
			if( reference.callsEnclosingTypeClinit(fieldReference)){

				if( logger.isLoggable( TreeLogger.DEBUG )){
					logger.log( TreeLogger.DEBUG, "Clinit not required, must have been called previously by " + Compiler.inline( reference.toSource() ), null );
				}

				found = true;
				break;
			}
		}
		return found;
	}

	/**
	 * Marks a static field belonging to a class with a static initializer as not requiring clinit.
	 * This enables GenerateJavaScriptAST to skip the insertion of a clinit guard around this particular field reference.
	 * @param reference
	 * @param logger
	 */
	protected void markFieldReferenceAsNotRequiringClinit( final JFieldRef reference, final TreeLogger logger ){
		Checker.notNull("parameter:reference", reference );
		Checker.notNull("parameter:logger", logger );

		Compiler.addFieldReferenceNotRequiringClinit(reference );

		logger.log( TreeLogger.DEBUG, "Marking as not requiring clinit to be inserted by GenerateJavaScriptAST", null );
	}
}
