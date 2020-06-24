package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.variables.Document;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class NPChunker {

	public static int maxLength = 25;

	private final File npChunkerDir = new File("npchunker");

	private StanfordCoreNLP pipeline;
	private final static String SPLITTER = "\t";

	public static class TermIndexPair {

		public final String term;
		public final int index;

		public TermIndexPair(String term, int index) {
			this.term = term;
			this.index = index;
		}

		@Override
		public String toString() {
			return "TermIndexPair [term=" + term + ", index=" + index + "]";
		}

	}

	private final File chunks;

	public static void main(String[] args) throws IOException {

		NPChunker chunker = new NPChunker(new Document("test2",
				"Groups receiving ECs, both alone (71.84 ± 5.20%) and in combination with MP (78.26 ± 0668%), performed the DFR task significantly better than all other lesioned animals; however, these two groups were not significantly different from each other."));

		chunker.getNPs().forEach(System.out::println);
	}

	public NPChunker(final Document document) throws IOException {

		System.out.println("Process: " + document.documentID);

		chunks = new File(npChunkerDir, document.documentID);

		if (chunks.exists())
			nps = Files.readAllLines(chunks.toPath()).stream().map(l -> l.split(SPLITTER))
					.map(l -> new TermIndexPair(l[0], Integer.parseInt(l[1])))
					.filter(t -> t.term.matches(".+(group|animals|rats|mice|rats|cats|dogs)"))
					.collect(Collectors.toList());
		else {
			Properties props = new Properties();
			props.setProperty("parse.nthreads", "4");
			props.setProperty("annotators", "tokenize,ssplit,pos, parse");
			pipeline = new StanfordCoreNLP(props);
			nps = extractNPs(document.documentContent);
		}
	}

	final private List<TermIndexPair> nps;

	public List<TermIndexPair> getNPs() {
		return nps;
	}

	private List<TermIndexPair> extractNPs(String text) throws IOException {

		CoreDocument document = new CoreDocument(text);
		pipeline.annotate(document);
		List<TermIndexPair> nps = new ArrayList<>();

		TregexPattern NPpattern = TregexPattern.compile("@NP");
		for (CoreSentence sentence : document.sentences()) {
			Tree constituencyParse = sentence.constituencyParse();
			TregexMatcher matcher = NPpattern.matcher(constituencyParse);
			while (matcher.findNextMatchingNode()) {
				String term = SentenceUtils.listToString(matcher.getMatch().yield());
				if (term.length() > 80)
					continue;
				if (term.matches(".+(group|animals|rats|mice|rats|cats|dogs)"))
					continue;

				nps.add(new TermIndexPair(term, matcher.getMatch().yieldWords().get(0).beginPosition()));
			}
		}
		PrintStream ps = new PrintStream(chunks);
		nps.forEach(np -> ps.println(np.term + SPLITTER + np.index));
		ps.close();
		return nps;
	}

}
