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

	private final File npChunkerDir = new File("npchunker/");

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

		NPChunker chunker = new NPChunker(new Document("test",
				"Intrathecal spinal progenitor cell transplantation for the treatment of neurotrophic pain."));

		chunker.getNPs().forEach(System.out::println);
	}

	public NPChunker(final Document document) throws IOException {

		chunks = new File(npChunkerDir, document.documentID);

		if (chunks.exists())
			nps = Files.readAllLines(chunks.toPath()).stream().map(l -> l.split(SPLITTER))
					.map(l -> new TermIndexPair(l[0], Integer.parseInt(l[1]))).collect(Collectors.toList());
		else {
			Properties props = new Properties();
			props.setProperty("parse.nthreads", "8");
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
				nps.add(new TermIndexPair(SentenceUtils.listToString(matcher.getMatch().yield()),
						matcher.getMatch().yieldWords().get(0).beginPosition()));
			}
		}
		PrintStream ps = new PrintStream(chunks);

		nps.forEach(np -> ps.println(np.term + SPLITTER + np.index));
		ps.close();
		return nps;
	}

}
