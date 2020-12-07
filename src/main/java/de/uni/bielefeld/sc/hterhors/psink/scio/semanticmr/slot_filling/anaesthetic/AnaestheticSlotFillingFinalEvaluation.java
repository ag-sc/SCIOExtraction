package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;

public class AnaestheticSlotFillingFinalEvaluation {

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
		if( args.length==0)
			new AnaestheticSlotFillingFinalEvaluation(1000L, "PREDICT");
		else
		new AnaestheticSlotFillingFinalEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/anaesthetic/instances/");
	ENERModus modus;

	public AnaestheticSlotFillingFinalEvaluation(long randomSeed, String modusName) throws IOException {

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
				.apply().registerNormalizationFunction(new DosageNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		Map<String, Score> scoreMap = new HashMap<>();

		EAnaestheticModifications rule = EAnaestheticModifications.ROOT_DELIVERY_METHOD_DOSAGE;

		Random random = new Random(randomSeed);
		modus = ENERModus.valueOf(modusName);
		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

			long seed = random.nextLong();
			log.info("RUN SEED:" + seed);
			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(seed)
					.setTrainingProportion(90).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					AnaestheticRestrictionProvider.getByRule(rule));

			String modelName = modusName + "_Anaesthetic_DissFinal_" + seed;

			AnaestheticPredictor predictor = new AnaestheticPredictor(modelName,
					instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					instanceProvider.getTestInstances().stream().map(t -> t.getName())
//							.filter(n -> names.contains(n))
							.collect(Collectors.toList()),
					rule, modus);

			predictor.trainOrLoadModel();
//
			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
			slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.anaesthetic,
					false);

			System.out.println("---------------------------------------");

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator, scoreMap);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */
			log.info("modelName: " + predictor.modelName);
			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());

			/**
			 * TODO: Compare results with results when changing some parameter. Implement
			 * more sophisticated feature-templates.
			 */
		}

		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("	*************************");

	}

}
