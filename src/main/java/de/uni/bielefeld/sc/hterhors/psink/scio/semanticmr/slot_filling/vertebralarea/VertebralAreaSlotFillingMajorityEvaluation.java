package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class VertebralAreaSlotFillingMajorityEvaluation {
	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0)
			new VertebralAreaSlotFillingMajorityEvaluation(1000L, "GOLD");
		else
			new VertebralAreaSlotFillingMajorityEvaluation(1000L, args[0]);
	}

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("data/slot_filling/vertebral_area/instances/");

	ENERModus modus;

	public VertebralAreaSlotFillingMajorityEvaluation(long randomSeed, final String modusName) throws IOException {
		InstanceProvider.removeEmptyInstances = false;

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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("VertebralArea"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		Random random = new Random(randomSeed);
		Map<String, Score> scoreMap = new HashMap<>();
		modus = ENERModus.valueOf(modusName);

		Set<SlotType> slotTypesToConsider = new HashSet<>();
		slotTypesToConsider.add(SCIOSlotTypes.hasUpperVertebrae);
		slotTypesToConsider.add(SCIOSlotTypes.hasLowerVertebrae);
		for (int i = 0; i < 10; i++) {
			log.info("RUN ID:" + i);

//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

			EVertebralAreaModifications rule = EVertebralAreaModifications.NO_MODIFICATION;

			long seed = random.nextLong();
			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(seed)
					.setTrainingProportion(90).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();

//		InstanceProvider.removeEmptyInstances = false;

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
			Map<Instance, State> coverageStates;

			Map<SlotType, Map<String, Integer>> majority = new HashMap<>();
			Map<String, Integer> majorityET = new HashMap<>();

			for (Instance trainingInstance : instanceProvider.getTrainingInstances()) {

				for (AbstractAnnotation orgModel : trainingInstance.getGoldAnnotations().getAnnotations()) {

					majorityET.put(orgModel.getEntityType().name,
							majorityET.getOrDefault(orgModel.getEntityType().name, 0) + 1);

					for (SlotType slotType : slotTypesToConsider) {

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

			ee.setSingleSlotFiller(m.getKey(), max(m.getValue()));
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