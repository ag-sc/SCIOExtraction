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
import de.hterhors.semanticmr.corpus.distributor.TenFoldCrossCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Annotations;
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

public class AnaestheticSlotFillingMajorityEvaluation {

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
			new AnaestheticSlotFillingMajorityEvaluation(1000L, "GOLD");
		else
		new AnaestheticSlotFillingMajorityEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/anaesthetic/instances/");
	ENERModus modus;

	public AnaestheticSlotFillingMajorityEvaluation(long randomSeed, String modusName) throws IOException {

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

//		Random random = new Random(randomSeed);
		modus = ENERModus.valueOf(modusName);
		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

//			long seed = random.nextLong();
//			log.info("RUN SEED:" + seed);
			AbstractCorpusDistributor corpusDistributor = new TenFoldCrossCorpusDistributor.Builder().setSeed(randomSeed).setFold(i)
					.setTrainingProportion(90).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
			slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);
			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					AnaestheticRestrictionProvider.getByRule(rule));
			Map<Instance, State> coverageStates;

			Map<SlotType, Map<String, Integer>> majority = new HashMap<>();
			Map<String, Integer> majorityET = new HashMap<>();

			for (Instance trainingInstance : instanceProvider.getTrainingInstances()) {

				for (AbstractAnnotation orgModel : trainingInstance.getGoldAnnotations().getAnnotations()) {

					majorityET.put(orgModel.getEntityType().name,
							majorityET.getOrDefault(orgModel.getEntityType().name, 0) + 1);

					for (SlotType slotType : slotTypesToConsider) {

						if (!orgModel.asInstanceOfEntityTemplate().hasSlotOfType(slotType))
							continue;

						if (slotType.isSingleValueSlot()) {

							SingleFillerSlot sfs = orgModel.asInstanceOfEntityTemplate().getSingleFillerSlot(slotType);
							String key;
							if (sfs.containsSlotFiller()) {
								key = sfs.getSlotFiller().getEntityType().name;
							} else {
								key = "null";
							}
							majority.putIfAbsent(slotType, new HashMap<>());
							majority.get(slotType).put(key, majority.get(slotType).getOrDefault(key, 0) + 1);
						}

						if (!slotType.isSingleValueSlot()) {

							MultiFillerSlot sfs = orgModel.asInstanceOfEntityTemplate().getMultiFillerSlot(slotType);

							for (AbstractAnnotation slotType2 : sfs.getSlotFiller()) {

								String key;
								key = slotType2.getEntityType().name;
								majority.putIfAbsent(slotType, new HashMap<>());
								majority.get(slotType).put(key, majority.get(slotType).getOrDefault(key, 0) + 1);
							}
						}
					}

				}

			}

			Map<Instance, State> finalStates = new HashMap<>();

			for (Instance instance : instanceProvider.getDevelopmentInstances()) {
				finalStates.put(instance, new State(instance, new Annotations(buildMajority(majorityET, majority))));
			}

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);

			coverageStates = finalStates;
			System.out.println("---------------------------------------");

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator, scoreMap);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

		}
		log.info("\n\n\n*************************");

		for (Entry<String, Score> sm : scoreMap.entrySet()) {
			log.info(sm.getKey() + "\t" + sm.getValue().toTSVString());
		}

		log.info("	*************************");
	}

	private AbstractAnnotation buildMajority(Map<String, Integer> majorityET,
			Map<SlotType, Map<String, Integer>> majority) {

		EntityTemplate ee = new EntityTemplate(max(majorityET).entityType);
		for (Entry<SlotType, Map<String, Integer>> m : majority.entrySet()) {

			if (m.getKey().isSingleValueSlot())
				ee.setSingleSlotFiller(m.getKey(), max(m.getValue()));
			else {
				ee.addMultiSlotFiller(m.getKey(), max(m.getValue()));
			}
		}

		return ee;

	}

	private EntityTypeAnnotation max(Map<String, Integer> value) {
		int maxValue = 0;
		String ent = null;
		for (Entry<String, Integer> e : value.entrySet()) {

			if (e.getValue() >= maxValue) {
				ent = e.getKey();
				maxValue = e.getValue();
			}
		}
		if (ent == null || ent.equals("null"))
			return null;
		else
			return EntityTypeAnnotation.get(EntityType.get(ent));
	}
}