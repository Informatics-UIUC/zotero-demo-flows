package org.meandre.components.readability.measures;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.annotations.Component;
import org.meandre.annotations.ComponentInput;
import org.meandre.annotations.ComponentOutput;
import org.meandre.annotations.Component.Mode;
import org.meandre.core.ComponentContext;
import org.meandre.core.ComponentContextException;
import org.meandre.core.ComponentContextProperties;
import org.meandre.core.ComponentExecutionException;
import org.meandre.core.ExecutableComponent;

/** This class implements the Flesch Kincaid Readability measure as explained
 * at http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test. The code is
 * based on the work done by Daniel Shiffman at
 * http://www.shiffman.net/teaching/a2z/week1/
 *
 * @author Xavier Llor&agrave;
 * @author Loretta Auvil - updated
 */
//-------------------------------------------------------------------------
@Component(
		baseURL = "meandre://seasr.org/components/zotero/",
		creator = "Xavier Llor&agrave",
		description = "Computes the Flesch Kincaid readability measure as explained at http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test. The code is based on the work done by Daniel Shiffman at http://www.shiffman.net/teaching/a2z/week1/",
		name = "Flesch Kincaid readability measure", tags = "zotero, text, readability, measure",
		mode = Mode.compute, firingPolicy = Component.FiringPolicy.all
)
//-------------------------------------------------------------------------
public class FleschKincaidReadabilityMeasure
implements ExecutableComponent {

	private static final String FRES = "Flesch Reading Ease Score";

	private static final String FGL = "Flesch Grade Level";

	private static final String TOTAL_SENTENCES = "Total Sentences";

	private static final String TOTAL_WORDS = "Total Words";

	private static final String TOTAL_SYLLABLES = "Total Syllables";

	private static final String FLESCH_KINCAID_WIKIPEDIA_URL = "http://en.wikipedia.org/wiki/Flesch-Kincaid_Readability_Test";

	// -------------------------------------------------------------------------

	@ComponentInput(
			description = "Text content of the url page.", 
			name = "Text"
	)
	public final static String INPUT_CONTENT = "Text";

	@ComponentInput(
			description = "Boolean value for whether the current item passed is the last item.",
			name = "last_item"
	)
	public final static String INPUT_LAST_ITEM = "last_item";

	@ComponentInput(
			description = "Title for the current item.",
			name = "item_title"
	)
	public final static String INPUT_ITEM_TITLE = "item_title";

	@ComponentInput(
			description = "URL for the current item.",
			name = "item_url"
	)
	public final static String INPUT_ITEM_URL = "item_url";

	@ComponentOutput(
			description = "A report of the Flesch Kincaid readability measures.", 
			name = "report"
	)
	public final static String OUTPUT_REPORT = "report";

	// -------------------------------------------------------------------------

	private StringBuffer sb;

	public void initialize(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {
	}

	public void dispose(ComponentContextProperties ccp)
	throws ComponentExecutionException, ComponentContextException {
	}

	@SuppressWarnings("unchecked")
	public void execute(ComponentContext cc)
	throws ComponentExecutionException, ComponentContextException {
		// check to see if sb is null and create; otherwise it will be appended while
		// gathering data for all the submitted urls
		if (sb == null) {
			sb = new StringBuffer();
			sb.append("<h1>Readability Analysis</h1>");
		}

		String title = (String)cc.getDataComponentFromInput(INPUT_ITEM_TITLE);
		String url = (String)cc.getDataComponentFromInput(INPUT_ITEM_URL);
		String content = (String)cc.getDataComponentFromInput(INPUT_CONTENT);

		sb.append("<h2>"+title+"</h2>");
		JSONObject json = computeMeasure(content);
		sb.append(generateReport(url,json)+"<br/>");

		// null sb so that it is ready to execute the again for another run
		if ((String)cc.getDataComponentFromInput(INPUT_LAST_ITEM)=="true") {
			cc.pushDataComponentToOutput(OUTPUT_REPORT, sb.toString());
			sb = null;
		}
	}

	private String generateReport(String sURL, JSONObject json) {
		StringBuffer sbReport = new StringBuffer();

		sbReport.append("<table>");

		sbReport.append("<tr><td colspan=\"2\">");
		sbReport.append("(<a href=\""+sURL+"\">"+sURL+"</a>)");
		sbReport.append("</td></tr>");

		sbReport.append("<tr><td colspan=\"2\">");
		sbReport.append("<strong><a href='" + FLESCH_KINCAID_WIKIPEDIA_URL + "'>Flesch-Kincaid Readability Test</a></strong>");
		sbReport.append("</td></tr>");

		sbReport.append("<tr><td>");
		sbReport.append(TOTAL_SYLLABLES);
		sbReport.append("</td><td>");
		try {
			sbReport.append(json.get(TOTAL_SYLLABLES));
		} catch (JSONException e3) {
			sbReport.append("");
		}
		sbReport.append("</td><tr>");

		sbReport.append("<tr><td>");
		sbReport.append(TOTAL_WORDS);
		sbReport.append("</td><td>");
		try {
			sbReport.append(json.get(TOTAL_WORDS));
		} catch (JSONException e2) {
			sbReport.append("");
		}
		sbReport.append("</td><tr>");

		sbReport.append("<tr><td>");
		sbReport.append(TOTAL_SENTENCES);
		sbReport.append("</td><td>");
		try {
			sbReport.append(json.get(TOTAL_SENTENCES));
		} catch (JSONException e1) {
			sbReport.append("");
		}
		sbReport.append("</td><tr>");

		sbReport.append("<tr><td>");
		sbReport.append(FRES);
		sbReport.append("</td><td>");
		try {
			sbReport.append(Math.round(json.getDouble(FRES)));
		} catch (JSONException e) {
			sbReport.append("");
		}
		sbReport.append("</td><tr>");

		sbReport.append("<tr><td>");
		sbReport.append(FGL);
		sbReport.append("</td><td>");
		try {
			sbReport.append(String.format("%.1f", json.getDouble(FGL)));
		} catch (JSONException e) {
			sbReport.append("");
		}
		sbReport.append("</td><tr>");

		sbReport.append("</td></tr>");

		sbReport.append("</table>");

		return sbReport.toString();
	}

	private JSONObject computeMeasure (String content ) {

		int syllables = 0;
		int sentences = 0;
		int words     = 0;

		String delimiters = ".,':;?{}[]=-+_!@#$%^&*() ";
		StringTokenizer tokenizer = new StringTokenizer(content,delimiters);
		//go through all words
		while (tokenizer.hasMoreTokens())
		{
			String word = tokenizer.nextToken();
			syllables += countSyllables(word);
			words++;
		}
		//look for sentence delimiters
		String sentenceDelim = ".:;?!";
		StringTokenizer sentenceTokenizer = new StringTokenizer(content,sentenceDelim);
		sentences = sentenceTokenizer.countTokens();

		//calculate flesch reading ease score
		final float f1 = (float) 206.835;
		final float f2 = (float) 84.6;
		final float f3 = (float) 1.015;
		float r1 = (float) syllables / (float) words;
		float r2 = (float) words / (float) sentences;
		float fres = f1 - (f2*r1) - (f3*r2);

		//calculate the flesch grade level
		float fgl = 0.39f * ((float) words / (float)sentences) +
		11.8f * ((float)syllables / (float)words) - 15.59f;

		JSONObject json = new JSONObject();

		try {
			json.put(TOTAL_SYLLABLES,syllables);
			json.put(TOTAL_WORDS,words);
			json.put(TOTAL_SENTENCES,sentences);
			json.put(FRES,fres);
			json.put(FGL,fgl);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}

	// A method to count the number of syllables in a word
	// Pretty basic, just based off of the number of vowels
	// This could be improved
	private static int countSyllables(String word) {
		int      syl    = 0;
		boolean  vowel  = false;
		int      length = word.length();

		//check each word for vowels (don't count more than one vowel in a row)
		for(int i=0; i<length; i++) {
			if        (isVowel(word.charAt(i)) && (vowel==false)) {
				vowel = true;
				syl++;
			} else if (isVowel(word.charAt(i)) && (vowel==true)) {
				vowel = true;
			} else {
				vowel = false;
			}
		}

		char tempChar = word.charAt(word.length()-1);
		//check for 'e' at the end, as long as not a word w/ one syllable
		if (((tempChar == 'e') || (tempChar == 'E')) && (syl != 1)) {
			syl--;
		}
		return syl;
	}

	//check if a char is a vowel (count y)
	private static boolean isVowel(char c) {
		if      ((c == 'a') || (c == 'A')) { return true;  }
		else if ((c == 'e') || (c == 'E')) { return true;  }
		else if ((c == 'i') || (c == 'I')) { return true;  }
		else if ((c == 'o') || (c == 'O')) { return true;  }
		else if ((c == 'u') || (c == 'U')) { return true;  }
		else if ((c == 'y') || (c == 'Y')) { return true;  }
		else                               { return false; }
	}
}
