package org.meandre.components.socialnetworking;

import java.util.Hashtable;
import java.util.List;
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

import edu.uci.ics.jung.algorithms.importance.AbstractRanker;
import edu.uci.ics.jung.algorithms.importance.DegreeDistributionRanker;
import edu.uci.ics.jung.algorithms.importance.NodeRanking;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.utils.UserData;


//------------------------------------------------------------------------- 
@Component(
		baseURL = "meandre://seasr.org/components/zotero/", 
		creator = "Xavier Llor&agrave", 
		description = "Given a collection of authors, grouped by publication, this component "+
		"generates a report based on the social network analysis. This analysis uses the JUNG "+
		"network importance algorithms to rank the authors. This component uses AuthorDegreeDistributionAnalysis, which " +
		"ranks each author based on the degree of the node.", 
		name = "Author Degree Distribution Analysis", tags = "zotero, authors, social network analysis", 
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------

/**
*  This class extracts the list of authors per entry from a Zotero RDF
* 
* @author Xavier Llor&agrave;
*/
public class AuthorDegreeDistributionAnalysis implements ExecutableComponent {

	public static final String AUTHOR = "Author";

	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "A list of vectors containing the names of the authors. There is one vector for each entry.", 
			name = "list_authors"
	)
	public final static String INPUT_LISTAUTHORS = "list_authors";
	
	@ComponentOutput(
			description = "A report of the social network analysis.", 
			name = "report"
	)
	public final static String OUTPUT_REPORT = "report";

	@ComponentOutput(
			description = "The graph generated.", 
			name = "graph"
	)
	public final static String OUTPUT_GRAPH = "graph";
	
	// -------------------------------------------------------------------------


	public void initialize(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}
	
	public void dispose(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {}

	@SuppressWarnings("unchecked")
	public void execute(ComponentContext cc)
	throws ComponentExecutionException, ComponentContextException {
		StringBuffer sbReport = new StringBuffer();
		List<Vector<String>> listAuthors = (List<Vector<String>>) cc.getDataComponentFromInput(INPUT_LISTAUTHORS);
		
		Graph g = buildGraph(listAuthors);
		AbstractRanker rank = computeRank(g);
		generateReport(sbReport,rank);
		
		cc.pushDataComponentToOutput(OUTPUT_GRAPH, g);
		cc.pushDataComponentToOutput(OUTPUT_REPORT, sbReport.toString());
	}

	private void generateReport(StringBuffer sbReport, AbstractRanker rank) {
		sbReport.append("<table>");
		sbReport.append("<tr><td>");
		sbReport.append("<strong>Author</strong>");
		sbReport.append("</td><td>");
		sbReport.append("<strong>Degree Distribution</strong>");
		sbReport.append("</td></tr>");
		for ( Object or:rank.getRankings() ) {
			NodeRanking nr = (NodeRanking) or;
			sbReport.append("<tr><td>");
			sbReport.append(nr.vertex.getUserDatum(AUTHOR));
			sbReport.append("</td><td>");
			sbReport.append(nr.rankScore);
			sbReport.append("</td></tr>");
		}
		sbReport.append("</table>");
	}

	private AbstractRanker computeRank(Graph g) {
		DegreeDistributionRanker ranker = new DegreeDistributionRanker(g);
		ranker.evaluate();
		return ranker;
	}

	private Graph buildGraph(List<Vector<String>> listAuthors) {
		UndirectedSparseGraph g = new UndirectedSparseGraph();
		Hashtable<String,UndirectedSparseVertex> htVertex = new Hashtable<String,UndirectedSparseVertex>();
		
		for ( Vector<String> vec:listAuthors ) {
			for ( String sAuthor:vec )
				if ( !htVertex.containsKey(sAuthor) ) {
					UndirectedSparseVertex v = new UndirectedSparseVertex();
					v.addUserDatum(AUTHOR, sAuthor, UserData.SHARED);
					g.addVertex(v);
					htVertex.put(sAuthor, v);
				}
		}
			
		for ( Vector<String> vec:listAuthors ) {
			Object[] oa = vec.toArray();
			for ( int i=0,iMax=oa.length ; i<iMax ; i++ )
				for ( int j=i+1,jMax=oa.length ; j<jMax ; j++ ) {
					UndirectedSparseEdge e = new UndirectedSparseEdge(htVertex.get(oa[i].toString()), htVertex.get(oa[j].toString()));
					try {
						g.addEdge(e);
					}
					catch (Exception exception) {
						// The edge was already added
					}
				}
		}
			
		return g;
	}
}
