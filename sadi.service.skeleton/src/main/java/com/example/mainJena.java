package com.example;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.VCARD;
import org.sadiframework.service.ServiceCall;

public class mainJena {


	public Dataset dataset;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
/*
		// some definitions
		String personURI    = "http://somewhere/JohnSmith";
		String givenName    = "John";
		String familyName   = "Smith";
		String fullName     = givenName + " " + familyName;

		// create an empty Model
		Model model = ModelFactory.createDefaultModel();

		// create the resource
		//   and add the properties cascading style
		Resource johnSmith  = model.createResource(personURI).addProperty(VCARD.FN, fullName);
		model.write(System.out);
		
		Resource restrictionNode = model.createResource();
		restrictionNode.addProperty(VCARD.Given, givenName);
		model.add( johnSmith,VCARD.N, restrictionNode);
		
		
		// list the statements in the Model
		StmtIterator iter = model.listStatements();

		// print out the predicate, subject and object of each statement
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object

		    System.out.print(subject.toString());
		    System.out.print(" " + predicate.toString() + " ");
		    if (object instanceof Resource) {
		       System.out.print(object.toString());
		    } else {
		        // object is a literal
		        System.out.print(" \"" + object.toString() + "\"");
		    }

		    System.out.println(" .");
		} 
		
		// now write the model in XML form to a file
		model.write(System.out);
		model.write(System.out,"RDF/XML");
		model.write(System.out, "N-TRIPLE");*/
		
		// Create a dataset and read into it from file 
		// "data.trig" assumed to be TriG.
		Dataset dataset = RDFDataMgr.loadDataset("serviceNanopub.nq") ;

		//RDFDataMgr.write(System.out, dataset, Lang.TRIG) ;

	//	ServiceCall call = new ServiceCall();
		
	//	Model model = ModelFactory.createDefaultModel();
		//model.write(System.out,"RDF/XML");
		
		//Dataset dataset = DatasetFactory.create();		
		//call.setDataSet(dataset);
		RDFDataMgr.write(System.out, dataset, Lang.TRIG) ;
		
		//Dataset dataset1 = DatasetFactory.createGeneral();	
		//RDFDataMgr.write(System.out, dataset1, Lang.TRIG) ;
		//call.setDataSet(dataset1);
		
	//	System.out.print("BEFORE GETDATASET");
	//	call.getDataSet().addNamedModel("http://biordf.net/cardioSHARE/patients.rdf#patient3", model);
		//RDFDataMgr.write(System.out, dataset1, Lang.TRIG) ;
	//	System.out.print("FINAL");
	//	model.write(System.out,"RDF/XML");		
		
	}
	


}
