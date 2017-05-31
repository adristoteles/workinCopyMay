package org.sadiframework.decompose;

import org.apache.jena.ontology.Restriction;

/**
 * @author Luke McCarthy
 */
public interface RestrictionVisitor
{
	/**
	 * Visit the specified property restriction.
	 * @param r the restriction
	 */
	void visit(Restriction r);
}
