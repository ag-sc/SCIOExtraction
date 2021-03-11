package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * TODO: CHANGE INITALIZER TO GENERIC !!! FOR FINAL EVALUATION
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */

/**
 * MIX
 */
//MACRO	Root = 0.747	0.882	0.647	0.806	0.938	0.710	0.926	0.941	0.912
//MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//MACRO	hasLocations = 0.413	0.529	0.338	0.578	0.711	0.493	0.714	0.745	0.686
//MACRO	Cardinality = 0.847	1.000	0.735	0.915	1.062	0.806	0.926	0.941	0.912
//MACRO	Overall = 0.580	0.706	0.492	0.712	0.831	0.630	0.813	0.849	0.781
//CRFStatistics [context=Train, getTotalDuration()=63953]
//CRFStatistics [context=Test, getTotalDuration()=222]
//modelName: PREDICT_DeliveryMethod_Final_-470456323649602622
//
//
//

/**
 * Greedy model
 */
//MACRO	Root = 0.369	0.471	0.304	0.399	0.500	0.333	0.926	0.941	0.912
//MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//MACRO	hasLocations = 0.039	0.059	0.029	0.055	0.079	0.043	0.714	0.745	0.686
//MACRO	Cardinality = 0.431	0.529	0.363	0.465	0.562	0.398	0.926	0.941	0.912
//MACRO	Overall = 0.229	0.382	0.164	0.282	0.450	0.210	0.813	0.849	0.781
//CRFStatistics [context=Train, getTotalDuration()=60357]
//CRFStatistics [context=Test, getTotalDuration()=126]
//modelName: PREDICT_DeliveryMethod_Final_-470456323649602622
//

public class DeliveryMethodSlotFillingMajorityEvaluation {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new DeliveryMethodSlotFillingMajorityEvaluation(1000L, "PREDICT");
		else
			new DeliveryMethodSlotFillingMajorityEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/delivery_method/instances/");

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;
//	long seed;
	ENERModus modus;

	public DeliveryMethodSlotFillingMajorityEvaluation(long randomSeed, String modusName) throws IOException {

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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply().registerNormalizationFunction(new DurationNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		SlotType.excludeAll();

		Map<String, Score> scoreMap = new HashMap<>();
		EDeliveryMethodModifications rule = EDeliveryMethodModifications.ROOT_LOCATION_DURATION;

		SCIOSlotTypes.hasDuration.include();
		SCIOSlotTypes.hasLocations.include();
		modus = ENERModus.valueOf(modusName);
//		Random random = new Random(randomSeed);

		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

//			seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new TenFoldCrossCorpusDistributor.Builder()
					.setSeed(randomSeed).setFold(i).setTrainingProportion(90).setDevelopmentProportion(10)
					.setCorpusSizeFraction(1F).build();

			/**
			 * The instance provider reads all json files in the given directory. We can set
			 * the distributor in the constructor. If not all instances should be read from
			 * the file system, we can add an additional parameter that specifies how many
			 * instances should be read. NOTE: in contrast to the corpusSizeFraction in the
			 * ShuffleCorpusDistributor, we initially set a limit to the number of files
			 * that should be read.
			 */
			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDuration);
			slotTypesToConsider.add(SCIOSlotTypes.hasLocations);
			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					DeliveryMethodRestrictionProvider.getByRule(rule));

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