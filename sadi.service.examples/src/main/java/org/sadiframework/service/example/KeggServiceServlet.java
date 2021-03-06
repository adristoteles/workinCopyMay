package org.sadiframework.service.example;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sadiframework.service.AsynchronousServiceServlet;
import org.sadiframework.service.ServiceCall;
import org.sadiframework.utils.KeggUtils;
import org.sadiframework.utils.LSRNUtils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;


public abstract class KeggServiceServlet extends AsynchronousServiceServlet
{
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(KeggServiceServlet.class);

	@Override
	public int getInputBatchSize()
	{
		// override input batch size to 1024, the maximum for the KEGG API...
		return 100;
	}

	protected abstract void processInput(String keggRecordId, String keggRecord, Resource output);

	protected abstract Resource getInputLSRNIdentifierType();

	protected String getInputIdPrefix()
	{
		return "";
	}

	@Override
	protected void processInputBatch(ServiceCall call)
	{
		Collection<Resource> inputNodes = call.getInputNodes();
		Model outputModel = call.getOutputModel();
		Map<String, Resource> idToOutputNode = new HashMap<String, Resource>(inputNodes.size());
		for (Resource inputNode: inputNodes) {
			String id = LSRNUtils.getID(inputNode, getInputLSRNIdentifierType());
			if(id == null) {
				log.warn(String.format("skipping input node %s, unable to determine KEGG record ID", inputNode.getURI()));
				continue;
			}
			id = String.format("%s%s", getInputIdPrefix(), id);
			idToOutputNode.put(id, outputModel.getResource(inputNode.getURI()));
		}

		Set<Entry<String,String>> entries;
		try {
			entries = KeggUtils.getKeggRecords(idToOutputNode.keySet()).entrySet();
		} catch(IOException e) {
			throw new RuntimeException("error contacting KEGG service", e);
		}

		log.debug(String.format("retrieved %d entries", entries.size()));
		for (Entry<String,String> entry: entries) {
			String keggId = StringUtils.removeStart(entry.getKey(), getInputIdPrefix());
			processInput(keggId, entry.getValue(), idToOutputNode.get(entry.getKey()));
		}
	}
}
