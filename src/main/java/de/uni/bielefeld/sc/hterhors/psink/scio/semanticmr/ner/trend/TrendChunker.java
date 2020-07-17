package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.trend;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class TrendChunker {

	public static int maxLength = 25;

	private StanfordCoreNLP pipeline;

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

	public static void main(String[] args) {

		TrendChunker chunker = new TrendChunker();

		chunker.extractChunks(
				"Groups receiving ECs, both alone (71.84 ± 5.20%) and in combination with MP (78.26 ± 0668%), "
						+ "performed the DFR task significantly better than all other lesioned animals; however, these two"
						+ " groups were not significantly different from each other.")
				.forEach(System.out::println);
	}

	public TrendChunker() {

//
		Properties props = new Properties();
		props.setProperty("parse.nthreads", "4");
		props.setProperty("annotators", "tokenize,ssplit,pos, parse");
		pipeline = new StanfordCoreNLP(props);
	}

	public List<TermIndexPair> extractChunks(String text) {

		CoreDocument document = new CoreDocument(text);
		pipeline.annotate(document);
		List<TermIndexPair> nps = new ArrayList<>();
//		(ROOT (S (S (NP (NNS Groups)) (VP (VBG receiving) (SBAR (S (NP (NP (NNS ECs)) (, ,) (NP (NP (NP (DT both) (RB alone)) (-LRB- -LRB-) (NP (NP (QP (CD 71.84) (CD ±))) (NP (CD 5.20) (NN %))) (-RRB- -RRB-)) (CC and) (NP (PP (IN in) (NP (NN combination))) (PP (IN with) (NP (NP (NN MP)) (PRN (-LRB- -LRB-) (NP (NP (CD 78.26)) (NP (CD ±) (CD 0668) (NN %))) (-RRB- -RRB-)))))) (, ,)) (VP (VBD performed) (NP (NP (DT the) (NNP DFR) (NN task)) (ADJP (RB significantly) (JJR better))) (PP (IN than) (NP (DT all) (JJ other) (JJ lesioned) (NNS animals)))))))) (: ;) (S (ADVP (RB however)) (, ,) (NP (DT these) (CD two) (NNS groups)) (VP (VBD were) (RB not) (ADJP (RB significantly) (JJ different) (PP (IN from) (NP (DT each) (JJ other)))))) (. .)))
		TregexPattern NPpattern = TregexPattern.compile("VP");
		for (CoreSentence sentence : document.sentences()) {
			Tree constituencyParse = sentence.constituencyParse();
			TregexMatcher matcher = NPpattern.matcher(constituencyParse);
			while (matcher.findNextMatchingNode()) {
				String term = SentenceUtils.listToString(matcher.getMatch().yield());

				nps.add(new TermIndexPair(term, matcher.getMatch().yieldWords().get(0).beginPosition()));
			}
		}
		return nps;
	}

}
