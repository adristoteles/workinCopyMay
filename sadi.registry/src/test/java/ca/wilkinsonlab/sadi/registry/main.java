package ca.wilkinsonlab.sadi.registry;

import org.apache.jena.rdf.model.Model;

import virtuoso.jena.driver.VirtModel;

public class main {

	public static void main(String[] args) {

		System.out.println("initVirtuosoRegistryModel");
		Model m = VirtModel.openDatabaseModel("http://localhost:8890/sparql", "jdbc:virtuoso://localhost:1111", "dba", "dba");
		System.out.println("initVirtuosoRegistryModel 2");

	}

}
