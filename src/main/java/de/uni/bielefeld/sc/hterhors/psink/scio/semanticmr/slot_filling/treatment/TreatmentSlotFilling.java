package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.TreatmentRestrictionProvider.ETreatmentModifications;

/**
 * 
 * @author hterhors 
 */
public class TreatmentSlotFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new TreatmentSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public TreatmentSlotFilling() throws IOException {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Treatment")).apply()
				.registerNormalizationFunction(new DosageNormalization()).build();

		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.treatment);
		PrintStream resultsOut = new PrintStream(new File("results/treatmentResults.csv"));

		resultsOut.println(header);

		/**
		 * 
		 * 
		 * 
		 * 
		 * 
		 * TODO: REMOVE QUICK FIX IN EntityTemplate Line 359
		 * 
		 */
		Map<String, Score> scoreMap = new HashMap<>();

		for (ETreatmentModifications rule : ETreatmentModifications.values()) {
//			this.rule = rule;
			rule = ETreatmentModifications.DOSAGE_DELIVERY_METHOD_APPLICATION_INSTRUMENT_DIRECTION;

//			AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
//					.setCorpusSizeFraction(1F).build();

			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
					.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					TreatmentRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

//			String modelName = "Treatment-1842612192";
			String modelName = "Treatment" + new Random().nextInt();

			TreatmentSlotFillingPredictor predictor = new TreatmentSlotFillingPredictor(modelName,
					trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
//
//			predictor.trainOrLoadModel();
//
//			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();
//
//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);
//
//			resultsOut.println(toResults(rule, score));
//			log.info("results: " + toResults(rule, score));
//			/**
//			 * Finally, we evaluate the produced states and print some statistics.
//			 */
//
//			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//			log.info("Coverage Training: " + trainCoverage);
//
//			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//			log.info("Coverage Development: " + devCoverage);
//
//			log.info("results: " + toResults(rule, score));

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

//			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);
			slotTypesToConsider.add(SCIOSlotTypes.hasDirection);
			slotTypesToConsider.add(SCIOSlotTypes.hasApplicationInstrument);
			slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
			slotTypesToConsider.add(SCIOSlotTypes.hasCompound);
			slotTypesToConsider.add(SCIOSlotTypes.hasVoltage);
			slotTypesToConsider.add(SCIOSlotTypes.hasRehabMedication);
			slotTypesToConsider.add(SCIOSlotTypes.hasElectricFieldStrength);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(false);

			log.info("---------------------------------------");

//			PerSlotEvaluator.evalRoot(EScoreType.MICRO, finalStates, coverageStates, evaluator);
//
//			PerSlotEvaluator.evalProperties(EScoreType.MICRO, finalStates, coverageStates, slotTypesToConsider,
//					evaluator);
//
//			PerSlotEvaluator.evalCardinality(EScoreType.MICRO, finalStates, coverageStates);

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator, scoreMap);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

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
			break;
		}

		resultsOut.flush();
		resultsOut.close();

	}

//	 multi state 2-6 compound as init + BEAM SEARCH
//	results: ROOT	0.56	0.42	0.83

//	 multi state mean+-std dev (=2-4) compound as init
//	results: ROOT	0.65	0.57	0.76

//	 always 4 compound as init
//	results: ROOT	0.65	0.57	0.76
//	Compute coverage...
//	Coverage Training: Score [getF1()=0.877, getPrecision()=0.799, getRecall()=0.971, tp=534, fp=134, fn=16, tn=0]
//	Compute coverage...
//	Coverage Development: Score [getF1()=0.860, getPrecision()=0.808, getRecall()=0.918, tp=135, fp=32, fn=12, tn=0]
//	results: ROOT	0.65	0.57	0.76
//	modelName: Treatment1807447630

	private String toResults(ETreatmentModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}
}
