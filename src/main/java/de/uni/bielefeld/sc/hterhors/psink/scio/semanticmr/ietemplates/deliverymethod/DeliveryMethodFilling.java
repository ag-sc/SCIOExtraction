package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.deliverymethod.specs.DeliveryMethodSpecs;

/**
 * GREEDY EVAL! Slot filling for delivery method.
 * 
 * ROOT Time: 8867 mean score: Score [getF1()=0.760, getPrecision()=0.760,
 * getRecall()=0.760, tp=38, fp=12, fn=12, tn=0] CRFStatistics [context=Train,
 * getTotalDuration()=95621] CRFStatistics [context=Test,
 * getTotalDuration()=8867] Compute coverage... Coverage Training: Score
 * [getF1()=1.000, getPrecision()=1.000, getRecall()=1.000, tp=143, fp=0, fn=0,
 * tn=0] Compute coverage... Coverage Development: Score [getF1()=0.920,
 * getPrecision()=0.920, getRecall()=0.920, tp=46, fp=4, fn=4, tn=0] modelName:
 * DeliveryMethod708621930
 * 
 * 
 * ROOT LOCATION Time: 13968 mean score: Score [getF1()=0.478,
 * getPrecision()=0.512, getRecall()=0.449, tp=44, fp=42, fn=54, tn=0]
 * CRFStatistics [context=Train, getTotalDuration()=325204] CRFStatistics
 * [context=Test, getTotalDuration()=13969] Compute coverage... Coverage
 * Training: Score [getF1()=0.994, getPrecision()=1.000, getRecall()=0.988,
 * tp=257, fp=0, fn=3, tn=0] Compute coverage... Coverage Development: Score
 * [getF1()=0.805, getPrecision()=0.958, getRecall()=0.694, tp=68, fp=3, fn=30,
 * tn=0] modelName: DeliveryMethod-1603701560
 * 
 * 
 * ROOT LOCATION DURATION
 * 
 * Time: 18560 mean score: Score [getF1()=0.480, getPrecision()=0.522,
 * getRecall()=0.443, tp=47, fp=43, fn=59, tn=0] CRFStatistics [context=Train,
 * getTotalDuration()=355932] CRFStatistics [context=Test,
 * getTotalDuration()=18560] Compute coverage... Coverage Training: Score
 * [getF1()=0.994, getPrecision()=1.000, getRecall()=0.989, tp=271, fp=0, fn=3,
 * tn=0] Compute coverage... Coverage Development: Score [getF1()=0.782,
 * getPrecision()=0.959, getRecall()=0.660, tp=70, fp=3, fn=36, tn=0] modelName:
 * DeliveryMethod1911796546
 * 
 * @author hterhors
 *
 *
 *         mean score: Score [getF1()=0.778, getPrecision()=0.778,
 *         getRecall()=0.778, tp=7, fp=2, fn=2, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=173393] CRFStatistics
 *         [context=Test, getTotalDuration()=504] Compute coverage...
 * 
 * 
 * 
 *         Time: 1284 mean score: Score [getF1()=0.500, getPrecision()=0.500,
 *         getRecall()=0.500, tp=10, fp=10, fn=10, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=584119] CRFStatistics
 *         [context=Test, getTotalDuration()=1284] Compute coverage...
 * 
 *         Time: 7664 mean score: Score [getF1()=0.554, getPrecision()=0.605,
 *         getRecall()=0.511, tp=23, fp=15, fn=22, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=769097] CRFStatistics
 *         [context=Test, getTotalDuration()=7664] Compute coverage... Coverage
 *         Training: Score [getF1()=0.991, getPrecision()=1.000,
 *         getRecall()=0.983, tp=399, fp=0, fn=7, tn=0] Compute coverage...
 *         Coverage Development: Score [getF1()=0.816, getPrecision()=1.000,
 *         getRecall()=0.689, tp=31, fp=0, fn=14, tn=0] modelName:
 *         DeliveryMethod-668144621
 * 
 */
public class DeliveryMethodFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new DeliveryMethodFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("src/main/resources/slotfilling/delivery_method/corpus/instances/");

	public static EDeliveryMethodModifications rule;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public DeliveryMethodFilling() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		SystemScope scope = SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DeliveryMethodSpecs.systemsScopeReader)
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.setSeed(1000L).setTrainingProportion(80).setDevelopmentProportion(20).build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		PrintStream resultsOut = new PrintStream(new File("results/deliveryResults.csv"));
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		resultsOut.println(header);

		for (EDeliveryMethodModifications rule : EDeliveryMethodModifications.values()) {
			DeliveryMethodFilling.rule = rule;
			/**
			 * The instance provider reads all json files in the given directory. We can set
			 * the distributor in the constructor. If not all instances should be read from
			 * the file system, we can add an additional parameter that specifies how many
			 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
			 * ShuffleCorpusDistributor, we initially set a limit to the number of files
			 * that should be read.
			 */
			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					DeliveryMethodRestrictionProvider.getByRule(rule));

			String modelName = "DeliveryMethod" + new Random().nextInt();

			DeliveryMethodPredictor deliveryMethodPrediction = new DeliveryMethodPredictor(modelName, scope,
					instanceProvider.getRedistributedTrainingInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getRedistributedDevelopmentInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()));

			deliveryMethodPrediction.trainOrLoadModel();

			Score score = deliveryMethodPrediction.evaluateOnDevelopment();

			resultsOut.println(toResults(rule, score));

			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

//			final Score trainCoverage = deliveryMethodPrediction.computeCoverageOnTrainingInstances(false);
//			log.info("Coverage Training: " + trainCoverage);
//
//			final Score devCoverage = deliveryMethodPrediction.computeCoverageOnDevelopmentInstances(false);
//			log.info("Coverage Development: " + devCoverage);

			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */
			log.info("modelName: " + deliveryMethodPrediction.modelName);
			/**
			 * TODO: Compare results with results when changing some parameter. Implement
			 * more sophisticated feature-templates.
			 */
		}
		resultsOut.flush();
		resultsOut.close();
	}

	private String toResults(EDeliveryMethodModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}

}
