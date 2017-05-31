package org.sadiframework.decompose;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.vocabulary.OWL;

/**
 * @author Luke McCarthy
 */
public class DefaultClassVisitor implements ClassVisitor
{
	public DefaultClassVisitor()
	{	
	}
	
	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#ignore(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public boolean ignore(OntClass c)
	{
		/* bottom out explicitly at owl:Thing, or we'll have problems when
		 * we enumerate equivalent classes...
		 */
		return c.equals( OWL.Thing );
	}

	/* (non-Javadoc)
	 * @see ca.wilkinsonlab.sadi.decompose.ClassVisitor#visit(com.hp.hpl.jena.ontology.OntClass)
	 */
	@Override
	public void visit(OntClass c)
	{
	}

	@Override
	public void visitPreDecompose(OntClass c) {
		// TODO Auto-generated method stub
		
	}
}
