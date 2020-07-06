package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;

public class AnaestheticSlotFilling {

	/**
	 * Compute coverage...
	 * 
	 * Coverage Training: Score [getF1()=0.741, getPrecision()=1.000,
	 * getRecall()=0.589, tp=347, fp=0, fn=242, tn=0]
	 * 
	 * Compute coverage...
	 * 
	 * Coverage Development: Score [getF1()=0.694, getPrecision()=0.987,
	 * getRecall()=0.536, tp=75, fp=1, fn=65, tn=0]
	 * 
	 * results: ROOT_DELIVERY_METHOD_DOSAGE 0.58 0.92 0.42
	 * 
	 * modelName: Anaesthetic1290337507
	 * 
	 * CRFStatistics [context=Train, getTotalDuration()=28276]
	 * 
	 * CRFStatistics [context=Test, getTotalDuration()=411]
	 * 
	 * 
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new AnaestheticSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/anaesthetic/instances/");

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public AnaestheticSlotFilling() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Anaesthetic"))
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

		PrintStream resultsOut = new PrintStream(new File("results/anaestheticResults.csv"));
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		resultsOut.println(header);

		for (EAnaestheticModifications rule : EAnaestheticModifications.values()) {
//			DeliveryMethodFilling.rule =rule;
			rule = EAnaestheticModifications.ROOT_DELIVERY_METHOD_DOSAGE;
			/**
			 * The instance provider reads all json files in the given directory. We can set
			 * the distributor in the constructor. If not all instances should be read from
			 * the file system, we can add an additional parameter that specifies how many
			 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
			 * ShuffleCorpusDistributor, we initially set a limit to the number of files
			 * that should be read.
			 */
			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					AnaestheticRestrictionProvider.getByRule(rule));

			String modelName = "Anaesthetic" + new Random().nextInt();

			AnaestheticPredictor predictor = new AnaestheticPredictor(modelName,
					instanceProvider.getRedistributedTrainingInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getRedistributedDevelopmentInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					rule);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			resultsOut.println(toResults(rule, score));

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
//			slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(false);

			System.out.println("---------------------------------------");

			PerSlotEvaluator.evalRoot(EScoreType.MICRO, finalStates, coverageStates, evaluator);

			PerSlotEvaluator.evalProperties(EScoreType.MICRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator);

			PerSlotEvaluator.evalCardinality(EScoreType.MICRO, finalStates, coverageStates);

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator);

			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

//			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(true);
//			log.info("Coverage Training: " + trainCoverage);
//
//			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(true);
//			log.info("Coverage Development: " + devCoverage);

			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */
//			log.info("results: " + toResults(rule, score));
			log.info("modelName: " + predictor.modelName);
			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());

			/**
			 * TODO: Compare results with results when changing some parameter. Implement
			 * more sophisticated feature-templates.
			 */
			break;
		}
		resultsOut.flush();
		resultsOut.close();
	}

	private String toResults(EAnaestheticModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}

}

//MICRO	Root = 0.752	0.688	0.830	0.936	0.936	0.936
//MICRO	hasDosage = 0.521	0.688	0.419	0.936	0.936	0.936
//MICRO	Cardinality = 0.838	0.766	0.925	1.000	1.000	1.000
//MACRO	Root = 0.774	0.688	0.885	0.944	0.936	0.955
//MACRO	hasDosage = 0.540	0.688	0.445	0.948	0.936	0.955
//MACRO	Cardinality = 0.851	0.766	0.958	1.000	1.000	1.000
//MACRO	Overall = 0.774	0.688	0.885	0.944	0.936	0.955
//modelName: Anaesthetic1773861614
//CRFStatistics [context=Train, getTotalDuration()=10734]
//CRFStatistics [context=Test, getTotalDuration()=31]