package org.sadiframework.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

/**
 * This is the base class extended by synchronous SADI services.
 * @author Luke McCarthy
 */
public abstract class SynchronousServiceServlet extends ServiceServlet
{
	private static final long serialVersionUID = 1L;
	
	/* (non-Javadoc)
	 * @see org.sadiframework.service.ServiceServlet#processInput(org.sadiframework.service.ServiceCall)
	 */
	@Override
	protected void processInput(ServiceCall call) throws Exception
	{
		Resource parameters = call.getParameters();
		Model provenance = call.getProvenanceModel();
		boolean needsParameters = !parameters.hasProperty(RDF.type, OWL.Nothing);
		for (Resource inputNode: call.getInputNodes()) {
			Resource outputNode = call.getOutputModel().getResource(inputNode.getURI());
			
			if (needsParameters)
			{
				processInput(inputNode, outputNode, parameters);
				if (call.requestQuad) processInput(inputNode, outputNode,call.provenanceModel, parameters);
			}
			else if (call.requestQuad)
				 {
					System.out.println("#######INICIO SUNCHORNY######");
					outputNode.getModel().write(System.out,"RDF/XML");
					processInput(inputNode, outputNode, provenance);				
					call.setProvenanceModel(provenance);
					
					System.out.println("#######AFTER proccses######");	
					
					Model model = ModelFactory.createDefaultModel();
					model.createResource(outputNode);				
					
					System.out.println("outputNode = ");
					outputNode.getModel().write(System.out,"RDF/XML");
					System.out.println("model = ");
					model.write(System.out,"RDF/XML");
					
					//System.out.println(outputNode);
					call.getDataSet().addNamedModel(outputNode.getURI(), model);
					
					RDFDataMgr.write(System.out, call.dataset, Lang.NQ) ;
					
					System.out.println("#######2AFTER proccses######");
					
					//model.add(outputNode.as);
					
				//	System.out.println("### Else-InputNode = ");
				//	Property p = inputNode.);
				//	System.out.println("INPUTNODE = - "+inputNode+" PROPERTY = " + p);	
				//	System.out.println("OBJECT = - " +inputNode.getProperty(p));	
					
					//	System.out.println("### Else-OUTputNode = "+outputNode);
				
				//	System.out.println("### Else-OUTputNode = ff   - - -"+outputNode + outputNode.getModel().write(System.out,"TRIX"));
					
				}
				else { System.out.println("#######NORMAL CALL######"); }
		}
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 */
	public void processInput(Resource input, Resource output) throws Exception
	{
		System.out.println("###################################SINGLE PROCESS OUTPUT######");
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 * @param parameters the populated parameters object
	 */
	public void processInput(Resource input, Resource output, Resource parameters) throws Exception
	{
		System.out.println("###################################PARAMENTER PROCESS OUTPUT######");
		
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 * @param parameters the populated parameters object
	 */
	public void processInput(Resource input, Resource output, Model provenanceModel) throws Exception
	{
		System.out.println("###################################PROVENANCE PROCESS OUTPUT######");
		
	}
	
	/**
	 * Process a single input, reading properties from an input node and 
	 * attaching properties to the corresponding output node.
	 * @param input the input node
	 * @param output the output node
	 * @param parameters the populated parameters object
	 */
	public void processInput(Resource input, Resource output, Model provenanceModel, Resource parameters) throws Exception
	{
		System.out.println("###################################PROVENANCE & PARAMENTER PROCESS OUTPUT######");
		
	}
}
