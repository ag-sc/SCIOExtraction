package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class NPChunkerServer {

	public static int maxLength = 25;

	private File npChunkerDir;

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
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				//
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())
				//
				.build();
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "data/slot_filling/defined_experimental_group/instances/";
			args[1] = "prediction/test2";
			args[2] = "4";
//			args[0] = "prediction/instances";
//			args[1] = "prediction/test";
//			args[2] = "4";
		}

		final File instanceDirectory = new File(args[0]);

		List<String> instanceNames = Arrays.asList(instanceDirectory.list());
		Collections.sort(instanceNames);

//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(100)
//				.build();
		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(instanceNames).build();

		InstanceProvider.maxNumberOfAnnotations = 1000;
		InstanceProvider.removeEmptyInstances = false;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		List<Instance> instances = instanceProvider.getTrainingInstances();

		File npChunkerDir = new File(args[1]);

		new NPChunkerServer(npChunkerDir, instances, Integer.parseInt(args[2]));

	}

	File chunks;

	public NPChunkerServer(final File cacheDir, final List<Instance> instances, int numOfthreads) throws IOException {
		Properties props = new Properties();
		props.setProperty("parse.nthreads", "" + numOfthreads);
		props.setProperty("pos.nthreads", "" + numOfthreads);
		props.setProperty("annotators", "tokenize,ssplit,pos,parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		if (cacheDir != null)
			npChunkerDir = cacheDir;

		npChunkerDir.mkdirs();

		final int max = instances.size();
		int count = 0;

		for (Instance instance : instances) {
			count++;
			AutomatedSectionifcation sectionifcation = AutomatedSectionifcation.getInstance(instance);

			chunks = new File(npChunkerDir, instance.getDocument().documentID);
			if (chunks.exists()) {
				continue;
			}

			System.out.println(count + "/" + max + "\t" + instance.getDocument().documentID + "\t"
					+ instance.getDocument().getNumberOfSentences());

			List<TermIndexPair> nps = new ArrayList<>();
			List<TermIndexPair> vps = new ArrayList<>();
			final int maxSent = instance.getDocument().getNumberOfSentences();
			int docOffset = 0;
			for (int j = 0; j < maxSent; j++) {

				System.out.print(".");
				if (j % 50 == 0)
					System.out.println(j + "/" + maxSent);

				ESection section = sectionifcation.getSection(j);

				docOffset = instance.getDocument().getSentenceByIndex(j).get(0).getDocCharOffset();

				if (section == ESection.REFERENCES)
					continue;

				if (instance.getDocument().getSentenceByIndex(j).size() > 100) {
					System.out.println();
					System.out.println("Skip\t" + instance.getDocument().documentID + "\t" + j + "\t"
							+ instance.getDocument().getSentenceByIndex(j).size());
					continue;
				}

				CoreDocument d = new CoreDocument(instance.getDocument().getContentOfSentence(j));
				pipeline.annotate(d);
				storeNPs(d, nps, docOffset);
				storeVPs(d, vps, docOffset);

			}
			System.out.println();
			if (nps.isEmpty() || vps.isEmpty())
				System.out.println("###WARN Empty Sets...");
			System.out.print("Write data...");
			PrintStream ps = new PrintStream(chunks);
			nps.forEach(np -> ps.println("@NP" + SPLITTER + np.term + SPLITTER + np.index));
			vps.forEach(np -> ps.println("@VP" + SPLITTER + np.term + SPLITTER + np.index));
			ps.close();
			System.out.println("\tdone!");


		}
	}

	private List<TermIndexPair> storeNPs(CoreDocument d, List<TermIndexPair> nps, int docOffset) throws IOException {

		TregexPattern NPpattern = TregexPattern.compile("@NP");
		for (CoreSentence sentence : d.sentences()) {
			Tree constituencyParse = sentence.constituencyParse();
			TregexMatcher matcher = NPpattern.matcher(constituencyParse);
			while (matcher.findNextMatchingNode()) {
				String term = SentenceUtils.listToString(matcher.getMatch().yield());
				if (term.length() > 80)
					continue;
//				if (!term.matches(".+(group|animals|rats|mice|rats|cats|dogs)"))
//					continue;

				nps.add(new TermIndexPair(term, docOffset + matcher.getMatch().yieldWords().get(0).beginPosition()));
			}
		}
		return nps;
	}

	private List<TermIndexPair> storeVPs(CoreDocument d, List<TermIndexPair> vps, int docOffset) throws IOException {
		TregexPattern VPpattern = TregexPattern.compile("@VP");
		for (CoreSentence sentence : d.sentences()) {
			Tree constituencyParse = sentence.constituencyParse();
			TregexMatcher matcher = VPpattern.matcher(constituencyParse);
			while (matcher.findNextMatchingNode()) {
				String term = SentenceUtils.listToString(matcher.getMatch().yield());

				if (term.length() > 80)
					continue;

//				if (!term.matches(".+(receiv|inject|contus|transplant|injur|train|treat).+"))
//					continue;

				vps.add(new TermIndexPair(term, docOffset + matcher.getMatch().yieldWords().get(0).beginPosition()));
			}
		}
		return vps;
	}

}
