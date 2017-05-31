package ca.wilkinsonlab.sadi.share;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import ca.wilkinsonlab.sadi.client.QueryClient;
import org.sadiframework.utils.RdfUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

public class SHAREQueryClient extends QueryClient
{
	private static final Logger log = Logger.getLogger(SHAREQueryClient.class);
		
	/** allow/disallow use of ARQ extensions to the SPARQL query language (e.g. GROUP BY, HAVING, arithmetic expressions) */
	protected final static String ALLOW_ARQ_SYNTAX_CONFIG_KEY = "share.sparql.allowARQSyntax";
	protected Syntax querySyntax;
	protected SHAREKnowledgeBase kb;
	
	public SHAREQueryClient()
	{
		this(new SHAREKnowledgeBase(Config.getConfiguration().getBoolean(ALLOW_ARQ_SYNTAX_CONFIG_KEY, true)));
	}
	
	public SHAREQueryClient(SHAREKnowledgeBase kb)
	{
		this.kb = kb;
	}
	
	public SHAREKnowledgeBase getKB()
	{
		return kb;
	}
	
	public Model getDataModel()
	{
		return kb.getDataModel();
	}
	
	public Dataset getDatasetModel()
	{
		return kb.getDatasetModel();
	}
	
	@Override
	protected QueryRunner getQueryRunner(String query, QueryClientCallback callback)
	{
		return new SHAREQueryRunner(query, callback);
	}
	
	public class SHAREQueryRunner extends QueryRunner
	{
		public SHAREQueryRunner(String query, QueryClientCallback callback)
		{
			super(query, callback);
		}
		
		protected QueryExecution getQueryExecution(String query, Model model)
		{
			return QueryExecutionFactory.create(query, kb.getQuerySyntax(), model);
		}
		
		public void run()
		{
			StopWatch stopWatch = new StopWatch();
			
			/* execute the query in the dynamic knowledge base, collecting
			 * the data that will be used by the actual reasoner...
			 */

			log.debug("populating SHARE knowledge base");

			stopWatch.start();
			kb.executeQuery(query);
			stopWatch.stop();
			log.debug(String.format("populated SHARE knowledge base in %dms", stopWatch.getTime()));
			
			kb.getReasoningModel().rebind();
			System.out.println(results);
			log.debug("using populated SHARE knowledge base to solve query");
			stopWatch.reset();
			stopWatch.start();			
			execQuery(results, query, kb.getReasoningModel());
			System.out.println(results);
			stopWatch.stop();
			log.debug(String.format("solved query against populated SHARE knowledge base in %dms", stopWatch.getTime()));
		}
		
		private void execQuery(List<Map<String, String>> results, String query, Model model)
		{
			QueryExecution qe = getQueryExecution(query, kb.getReasoningModel());
			Query q = QueryFactory.create(query, kb.getQuerySyntax());
			if (q.isSelectType()) {
				ResultSet resultSet = qe.execSelect();
				while (resultSet.hasNext()) {
					QuerySolution binding = resultSet.nextSolution();
					Map<String, String> bindingAsMap = new HashMap<String, String>();
					for (String var: resultSet.getResultVars()) {
						
						System.out.println(String.format(var));
						
						RDFNode varValue = binding.get(var);
						bindingAsMap.put(var, varValue != null ? RdfUtils.getPlainString(varValue.asNode()) : null);
					}
					results.add(bindingAsMap);
				}
			} else if (q.isAskType()) {
				boolean result = qe.execAsk();
				Map<String, String> binding = new HashMap<String, String>();
				binding.put("result", String.valueOf(result));
				results.add(binding);
			}
			qe.close();		
		}
	}
}
