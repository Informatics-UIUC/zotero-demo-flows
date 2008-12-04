package org.meandre.components.rdf.zotero;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


//------------------------------------------------------------------------- 
@Component(
		baseURL = "meandre://seasr.org/components/zotero/", 
		creator = "Xavier Llor&agrave", 
		description = "Extract the authors for each entry of a Zotero RDF", 
		name = "Author extractor", tags = "zotero, authors, information extraction", 
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------

/**
*  This class extracts the list of authors per entry from a Zotero RDF
* 
* @author Xavier Lor&agrave;
*/
public class AuthorExtractor implements ExecutableComponent {



	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "A map object containing the key elements on the request and the assiciated values", 
			name = "value_map"
	)
	public final static String INPUT_VALUEMAP = "value_map";
	
	@ComponentOutput(
			description = "A list of vectors containing the names of the authors. There is one vector for" +
					      "Zotero entry", 
			name = "list_authors"
	)
	public final static String OUTPUT_LIST_AUTHORS = "list_authors";

	// -------------------------------------------------------------------------


	public void initialize(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}
	
	public void dispose(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}

	@SuppressWarnings("unchecked")
	public void execute(ComponentContext cc)
	throws ComponentExecutionException, ComponentContextException {
		
		Map<String,byte[]> map = (Map<String, byte[]>) cc.getDataComponentFromInput(INPUT_VALUEMAP);
		List<Vector<String>> list = null;
		for ( String sKey:map.keySet() ) {
			ByteArrayInputStream bais = new ByteArrayInputStream(map.get(sKey));
			Model model = ModelFactory.createDefaultModel();
			model.read(bais, null);
			//model.write(System.out,"TTL",null);
			list = pullGraph(model);
		}
		
		cc.pushDataComponentToOutput(OUTPUT_LIST_AUTHORS, list);
	}

	private List<Vector<String>> pullGraph(Model model) {
		final String QUERY_AUTHORS =
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"+
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"+
            "SELECT DISTINCT ?doc ?first ?last " +
            "WHERE { " +
            "      ?doc rdf:type rdf:Seq . " +
            "      ?doc ?pred ?author ." +
            "      ?author rdf:type foaf:Person ." +
            "      ?author foaf:givenname ?first . " +
            "      ?author foaf:surname ?last " +
            "}" +
            "ORDER BY ?doc" ;

       // Query the basic properties
       //QuerySolutionMap qsmBindings = new QuerySolutionMap();
       //qsmBindings.add("component", res);

       Query query = QueryFactory.create(QUERY_AUTHORS) ;
       QueryExecution exec = QueryExecutionFactory.create(query, model, null);//qsmBindings);
       ResultSet results = exec.execSelect();

       String sLastDocID = "";
       List<Vector<String>> vecRes = new LinkedList<Vector<String>>();
       Vector<String> vec = null;
       while ( results.hasNext() ) {
    	   QuerySolution resProps = results.nextSolution();
    	   String sDoc   = resProps.getResource("doc").toString();
    	   String sFirst = resProps.getLiteral("first").getString();
    	   String sLast  = resProps.getLiteral("last").getString();
    	   if ( sDoc.equals(sLastDocID)) {
    		   vec.add(sLast+", "+sFirst);
    	   }
    	   else {
    		   if ( vec!=null ) 
    			   vecRes.add(vec);
			   vec = new Vector<String>();
			   vec.add(sLast+", "+sFirst);
			   sLastDocID = sDoc;    		   
    	   }
       }
       if ( vec.size()>0 ) 
		   vecRes.add(vec);
		  
       return vecRes;
	}


}
