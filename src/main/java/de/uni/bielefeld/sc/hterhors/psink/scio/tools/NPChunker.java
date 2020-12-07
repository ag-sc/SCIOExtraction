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

	private File npChunkerDir = new File("npchunker");
//	private  File npChunkerDir = new File("prediciton/data/npchunks/");

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

	public static void main(String[] args) throws IOException {

		new NPChunker().apply(new Document("test2",
				"They received both OEG and MSC. Groups receiving ECs, both alone (71.84 ± 5.20%) and in combination with MP (78.26 ± 0668%), performed the DFR task significantly better than all other lesioned animals; however, these two groups were not significantly different from each other."));

//		chunker.getNPs().forEach(System.out::println);
//		chunker.getVPs().forEach(System.out::println);

	}

	public NPChunker() throws IOException {
		this(new File("npchunker"));
	}

	public NPChunker(final File cacheDir) {

		if (cacheDir != null)
			npChunkerDir = cacheDir;
		else
			npChunkerDir = new File("npchunker");

		Properties props = new Properties();
		props.setProperty("parse.nthreads", "4");
		props.setProperty("pos.nthreads", "4");
		props.setProperty("annotators", "tokenize,ssplit,pos,parse");
		pipeline = new StanfordCoreNLP(props);

		npChunkerDir.mkdirs();
	}

	private File chunks;

	public void apply(final Document document) throws IOException {

		chunks = new File(npChunkerDir, document.documentID);

		System.out.println("Process: " + document.documentID);

		if (chunks.exists()) {
			nps = Files.readAllLines(chunks.toPath()).stream().filter(l->l.startsWith("@NP")).map(l -> l.split(SPLITTER))
					.map(l -> new TermIndexPair(l[1], Integer.parseInt(l[2])))
					.filter(t -> t.term.matches(".+(group|animals|rats|mice|rats|cats|dogs)"))
					.collect(Collectors.toList());
			vps = Files.readAllLines(chunks.toPath()).stream().filter(l->l.startsWith("@VP")).map(l -> l.split(SPLITTER))
					.map(l -> new TermIndexPair(l[2], Integer.parseInt(l[2])))
					.filter(t -> t.term.matches(".+(receiv|inject|contus|transplant|injur|train|treat).+"))
					.collect(Collectors.toList());
		} else {
			CoreDocument d = new CoreDocument(document.documentContent);
			System.out.println("Apply pipeline...");
			pipeline.annotate(d);
			System.out.println("annotated !");
			System.out.println("Extract NPS");
			nps = extractNPs(d);
			System.out.println("Extract VPS");
			vps = extractVPs(d);
		}
	}

	private List<TermIndexPair> nps = new ArrayList<>();
	private List<TermIndexPair> vps = new ArrayList<>();

	public List<TermIndexPair> getNPs() {
		return nps;
	}

	public List<TermIndexPair> getVPs() {
		return vps;
	}

	private List<TermIndexPair> extractNPs(CoreDocument d) throws IOException {
		TregexPattern NPpattern = TregexPattern.compile("@NP");
		for (CoreSentence sentence : d.sentences()) {
			Tree constituencyParse = sentence.constituencyParse();
			TregexMatcher matcher = NPpattern.matcher(constituencyParse);
			while (matcher.findNextMatchingNode()) {
				String term = SentenceUtils.listToString(matcher.getMatch().yield());
				
				if (term.length() > 80)
					continue;
			
				if (!term.matches(".+(group|animals|rats|mice|rats|cats|dogs)"))
					continue;

				nps.add(new TermIndexPair(term, matcher.getMatch().yieldWords().get(0).beginPosition()));
			}
		}
		PrintStream ps = new PrintStream(chunks);
		nps.forEach(np -> ps.println(np.term + SPLITTER + np.index));
		ps.close();
		return nps;
	}

	private List<TermIndexPair> extractVPs(CoreDocument d) throws IOException {
		TregexPattern VPpattern = TregexPattern.compile("@VP");
		for (CoreSentence sentence : d.sentences()) {
			Tree constituencyParse = sentence.constituencyParse();
			TregexMatcher matcher = VPpattern.matcher(constituencyParse);
			while (matcher.findNextMatchingNode()) {
				String term = SentenceUtils.listToString(matcher.getMatch().yield());

				if (term.length() > 80)
					continue;

				if (!term.matches(".+(receiv|inject|contus|transplant|injur|train|treat).+"))
					continue;

				vps.add(new TermIndexPair(term, matcher.getMatch().yieldWords().get(0).beginPosition()));
			}
		}
		PrintStream ps = new PrintStream(chunks);
		vps.forEach(np -> ps.println(np.term + SPLITTER + np.index));
		ps.close();
		return vps;
	}

}
