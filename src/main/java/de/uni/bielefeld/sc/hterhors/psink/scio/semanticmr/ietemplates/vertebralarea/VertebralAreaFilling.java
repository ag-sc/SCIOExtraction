package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.specs.VertebralAreaSpecs;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class VertebralAreaFilling {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new VertebralAreaFilling();
	}

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("src/main/resources/slotfilling/vertebral_area/corpus/instances/");
	public static EVertebralAreaModifications rule;

	public VertebralAreaFilling() throws IOException {

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
				.addScopeSpecification(VertebralAreaSpecs.systemsScopeReader)
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		rule = EVertebralAreaModifications.NO_MODIFICATION;

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(80).setDevelopmentProportion(20).setTestProportion(0).setCorpusSizeFraction(1F)
				.build();

		PrintStream resultsOut = new PrintStream(new File("results/vertebralAreaResults.csv"));

		/**
		 * The instance provider reads all json files in the given directory. We can set
		 * the distributor in the constructor. If not all instances should be read from
		 * the file system, we can add an additional parameter that specifies how many
		 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
		 * ShuffleCorpusDistributor, we initially set a limit to the number of files
		 * that should be read.
		 */
		InstanceProvider instanceProvider;

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
				VertebralAreaRestrictionProvider.getByRule(rule));

		String modelName = "VertebralArea" + new Random().nextInt(10000);

		VertebralAreaPredictor predictor = new VertebralAreaPredictor(modelName, scope,
				instanceProvider.getRedistributedTrainingInstances().stream().map(t -> t.getName())
						.collect(Collectors.toList()),
				instanceProvider.getRedistributedDevelopmentInstances().stream().map(t -> t.getName())
						.collect(Collectors.toList()),
				instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
						.collect(Collectors.toList()));

		predictor.trainOrLoadModel();

		Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

		Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

		resultsOut.println(toResults(rule, score));
		/**
		 * Finally, we evaluate the produced states and print some statistics.
		 */

		final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
		log.info("Coverage Training: " + trainCoverage);

		final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
		log.info("Coverage Development: " + devCoverage);

		/**
		 * Computes the coverage of the given instances. The coverage is defined by the
		 * objective mean score that can be reached relying on greedy objective function
		 * sampling strategy. The coverage can be seen as the upper bound of the system.
		 * The upper bound depends only on the exploration strategy, e.g. the provided
		 * NER-annotations during slot-filling.
		 */
		log.info("modelName: " + predictor.modelName);
		/**
		 * TODO: Compare results with results when changing some parameter. Implement
		 * more sophisticated feature-templates.
		 */

		resultsOut.flush();
		resultsOut.close();
	}

	private String toResults(EVertebralAreaModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}

}
