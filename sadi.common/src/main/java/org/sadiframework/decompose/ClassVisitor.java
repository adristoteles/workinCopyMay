package org.sadiframework.decompose;

import org.apache.jena.ontology.OntClass;

/**
 * @author Luke McCarthy
 */
public interface ClassVisitor
{
	/**
	 * Return true if we should ignore the specified class.
	 * If a class is ignored, it will not be visited and it will not be
	 * recursively decomposed.
	 * @param c the class
	 */
	boolean ignore(OntClass c);
	
	/**
	 * Visit the specified class.
	 * @param c the class
	 */
	void visit(OntClass c);
	
	void visitPreDecompose(OntClass c);
	
	/**
	 * Visit the specified class (called after the class is decomposed).
	 * @param c the class
	 */
}
