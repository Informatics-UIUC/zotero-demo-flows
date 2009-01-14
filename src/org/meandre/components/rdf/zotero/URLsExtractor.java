package org.meandre.components.rdf.zotero;

import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.htmlparser.beans.StringBean;
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
		description = "Extract the urls for each of the entry of a Zotero RDF", 
		name = "URLs extractor", tags = "zotero, authors, information extraction", 
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------

/**
*  This class extracts the list of authors per entry from a Zotero RDF
* 
* @author Xavier LLor&agrave;
*/
public class URLsExtractor implements ExecutableComponent {



	private static final String HTTP_WWW_GUTENBERG_ORG_FILES = "http://www.gutenberg.org/files/";

	private static final String HTTP_WWW_GUTENBERG_ORG_ETEXT = "http://www.gutenberg.org/etext/";

	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "A map object containing the key elements on the request and the assiciated values", 
			name = "value_map"
	)
	public final static String INPUT_VALUEMAP = "value_map";
	
	@ComponentOutput(
			description = "A list of hashtable containing title, and url for each entry. There is one vector for" +
					      "Zotero entry", 
			name = "list_entries"
	)
	public final static String OUTPUT_LIST_URLS = "list_entries";

	// -------------------------------------------------------------------------


	public void initialize(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}
	
	public void dispose(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}

	@SuppressWarnings("unchecked")
	public void execute(ComponentContext cc)
	throws ComponentExecutionException, ComponentContextException {
		
		Map<String,byte[]> map = (Map<String, byte[]>) cc.getDataComponentFromInput(INPUT_VALUEMAP);
		List<Map<String,String>> list = new LinkedList<Map<String,String>>();
		for ( String sKey:map.keySet() ) {
			ByteArrayInputStream bais = new ByteArrayInputStream(map.get(sKey));
			Model model = ModelFactory.createDefaultModel();
			model.read(bais, null);
			//model.write(System.out,"TTL",null);
			list.addAll(pullURLs(model));
		}
		
		cc.pushDataComponentToOutput(OUTPUT_LIST_URLS, list);
	}

	private List<Map<String,String>> pullURLs(Model model) {
		final String QUERY_AUTHORS =
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
            "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"+
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"+
            "PREFIX bib: <http://purl.org/net/biblio#>\n"+
            "SELECT DISTINCT ?uri ?title " +
            "WHERE { " +
            "      ?uri rdf:type bib:Document . " +
            "      ?uri dc:title ?title ." +
            "}";

       // Query the basic properties
       //QuerySolutionMap qsmBindings = new QuerySolutionMap();
       //qsmBindings.add("component", res);

       Query query = QueryFactory.create(QUERY_AUTHORS) ;
       QueryExecution exec = QueryExecutionFactory.create(query, model, null);//qsmBindings);
       ResultSet results = exec.execSelect();

       List<Map<String,String>> lstRes = new LinkedList<Map<String,String>>();
       while ( results.hasNext() ) {
    	   QuerySolution resProps = results.nextSolution();
    	   String sURI   = resProps.getResource("uri").toString();
    	   String sTitle = resProps.getLiteral("title").getString();
    	   String sContent = pullContent(sURI);
    	   Hashtable<String,String> ht = new Hashtable<String,String>();
    	   ht.put("url", sURI);
    	   ht.put("title", sTitle);
    	   ht.put("content", sContent);
    	   lstRes.add(ht);   
       }
       
       return lstRes;
	}

	private String pullContent(String sURL) {
		sURL = processURL(sURL);
		StringBean sb = new StringBean ();
	    sb.setURL (sURL);
	    String sRes = sb.getStrings();
		return (sRes==null)?"":sRes;
	}

	private String processURL(String sUrl) {
		if ( sUrl.startsWith(HTTP_WWW_GUTENBERG_ORG_ETEXT) ) {
			// Gutenberg book
			String sTmp = sUrl.substring(HTTP_WWW_GUTENBERG_ORG_ETEXT.length());
			sUrl = HTTP_WWW_GUTENBERG_ORG_FILES+sTmp+"/"+sTmp+".txt";
		}
		
		return sUrl;
	}


}
