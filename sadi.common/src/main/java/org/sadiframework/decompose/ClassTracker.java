package org.sadiframework.decompose;

import org.apache.jena.ontology.OntClass;

/**
 * @author Luke McCarthy
 */
public interface ClassTracker
{
	/**
	 * Returns true if the specified class has been seen before.
	 * @param c the class
	 * @return true if the specified class has been seen before
	 */
	boolean seen(OntClass c);
}
