package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

/************************
 * Cardinality-Coverage 0.996 1.000 0.993 hasInjuryAnaesthesia-Absolute 0.368
 * 0.899 0.231 Cardinality-Absolute 0.970 1.000 0.942 Root-Coverage 0.937 0.940
 * 0.935 hasInjuryAnaesthesia-Coverage 0.487 0.938 0.328
 * hasInjuryLocation-Relative 0.804 0.768 0.844 Root-Absolute 0.891 0.920 0.864
 * hasInjuryDevice-Relative 0.942 0.954 0.930 Overall-Relative 0.853 0.896 0.814
 * Root-Relative 0.950 0.978 0.924 hasInjuryDevice-Absolute 0.717 0.826 0.634
 * hasInjuryLocation-Absolute 0.678 0.737 0.627 hasInjuryDevice-Coverage 0.763
 * 0.866 0.682 Overall-Coverage 0.662 0.982 0.500 Cardinality-Relative 0.974
 * 1.000 0.949 hasInjuryLocation-Coverage 0.836 0.960 0.741 Overall-Absolute
 * 0.557 0.880 0.407 hasInjuryAnaesthesia-Relative 0.807 0.957 0.697
 *************************
 * Slot filling for injuries.
 */
public class InjurySlotFillingMajorityEvaluation {
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new InjurySlotFillingMajorityEvaluation(1000L, "GOLD");
		else
			new InjurySlotFillingMajorityEvaluation(1000L, args[0]);
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private File instanceDirectory;

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");
	ENERModus modus;

	public InjurySlotFillingMajorityEvaluation(long randomSeed, String modusName) throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Injury")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())

				.build();

		instanceDirectory = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.injury);

//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		Map<String, Score> scoreMap = new HashMap<>();

		EInjuryModifications rule = EInjuryModifications.ROOT_DEVICE_LOCATION_ANAESTHESIA;

//		Random random = new Random(randomSeed);
		modus = ENERModus.valueOf(modusName);
		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

//			long seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new TenFoldCrossCorpusDistributor.Builder()
					.setSeed(randomSeed).setFold(i).setTrainingProportion(90).setDevelopmentProportion(10)
					.setCorpusSizeFraction(1F).build();

//			AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
//					.setCorpusSizeFraction(1F).build();

			InjuryRestrictionProvider.applySlotTypeRestrictions(rule);

			Set<SlotType> slotTypesToConsider = new HashSet<>();

			slotTypesToConsider.add(SCIOSlotTypes.hasInjuryDevice);
			slotTypesToConsider.add(SCIOSlotTypes.hasInjuryLocation);
			slotTypesToConsider.add(SCIOSlotTypes.hasAnaesthesia);
			slotTypesToConsider.add(SCIOSlotTypes.hasInjuryIntensity);
			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					InjuryRestrictionProvider.getByRule(rule));

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
