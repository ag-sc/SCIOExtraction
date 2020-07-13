package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia;

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
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthesia.AnaestheticRestrictionProvider.EAnaestheticModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.evaluation.PerSlotEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

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
	String dataRandomSeed;

	List<Instance> trainingInstances;
	List<Instance> devInstances;
	List<Instance> testInstances;
	final ENERModus modus;

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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply().registerNormalizationFunction(new DosageNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.setSeed(1000L).setTrainingProportion(80).setDevelopmentProportion(20).build();
		modus = ENERModus.PREDICT;
//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

//		MACRO	Root = 0.561	0.688	0.474	0.570	0.688	0.489	0.984	1.000	0.969
//				MACRO	hasDeliveryMethod = 0.369	0.381	0.357	0.411	0.425	0.398	0.897	0.897	0.897
//				MACRO	hasDosage = 0.854	1.000	0.745	0.868	1.000	0.769	0.984	1.000	0.969
//				MACRO	Cardinality = 0.850	1.000	0.740	0.864	1.000	0.763	0.984	1.000	0.969
//				MACRO	Overall = 0.654	0.841	0.536	0.687	0.868	0.571	0.953	0.969	0.937
//				modelName: Anaesthetic172199658
//				CRFStatistics [context=Train, getTotalDuration()=16889]
//				CRFStatistics [context=Test, getTotalDuration()=210]

//		MACRO	Root = 0.593	0.719	0.505	0.593	0.719	0.505	1.000	1.000	1.000
//				MACRO	hasDeliveryMethod = 0.473	0.520	0.433	0.473	0.520	0.433	1.000	1.000	1.000
//				MACRO	hasDosage = 0.854	1.000	0.745	0.854	1.000	0.745	1.000	1.000	1.000
//				MACRO	Cardinality = 0.850	1.000	0.740	0.850	1.000	0.740	1.000	1.000	1.000
//				MACRO	Overall = 0.694	0.852	0.586	0.694	0.852	0.586	1.000	1.000	1.000
//				modelName: Anaesthetic-345509746
//				CRFStatistics [context=Train, getTotalDuration()=17000]
//				CRFStatistics [context=Test, getTotalDuration()=116]

		

		PrintStream resultsOut = new PrintStream(new File("results/anaestheticResults.csv"));
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		resultsOut.println(header);
		Map<String, Score> scoreMap = new HashMap<>();

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

			dataRandomSeed = "" + new Random().nextInt();
			String modelName = "Anaesthetic" + dataRandomSeed;

			trainingInstances = instanceProvider.getRedistributedTrainingInstances();
			devInstances = instanceProvider.getRedistributedDevelopmentInstances();
			testInstances = instanceProvider.getRedistributedTestInstances();

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
					rule, modus);

//			predictor.setOrganismModel(predictOrganismModel(instanceProvider.getInstances()));

			predictor.trainOrLoadModel();
//
			Map<Instance, State> coverageStates = predictor.coverageOnDevelopmentInstances(SCIOEntityTypes.anaesthetic,
					true);

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();
////

			Set<SlotType> slotTypesToConsider = new HashSet<>();
			slotTypesToConsider.add(SCIOSlotTypes.hasDosage);
			slotTypesToConsider.add(SCIOSlotTypes.hasDeliveryMethod);

			AbstractEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
					EEvaluationDetail.LITERAL);
//

			System.out.println("---------------------------------------");

			PerSlotEvaluator.evalRoot(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

			PerSlotEvaluator.evalProperties(EScoreType.MACRO, finalStates, coverageStates, slotTypesToConsider,
					evaluator, scoreMap);

			PerSlotEvaluator.evalCardinality(EScoreType.MACRO, finalStates, coverageStates, scoreMap);

			PerSlotEvaluator.evalOverall(EScoreType.MACRO, finalStates, coverageStates, evaluator, scoreMap);

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

	private Map<String, Set<AbstractAnnotation>> predictOrganismModel(List<Instance> instances) {

		EOrgModelModifications rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;

		/**
		 * Predict OrganismModels
		 */
		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		OrganismModelRestrictionProvider.applySlotTypeRestrictions(rule);

		List<String> trainingInstanceNames = trainingInstances.stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = devInstances.stream().map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = testInstances.stream().map(t -> t.getName()).collect(Collectors.toList());
		// + modelName
		OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(
//				"OrganismModel_Anaesthetic_FIX",
//				trainingInstanceNames, developInstanceNames,
				"OrganismModel_Anaesthetic_" + dataRandomSeed, trainingInstanceNames, developInstanceNames,
				testInstanceNames, rule, modus);
		predictor.trainOrLoadModel();

		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = predictor.predictInstances(instances, 1)
				.entrySet().stream().collect(Collectors.toMap(a -> a.getKey().getName(), a -> a.getValue()));

		SlotType.restoreExcludance(x);
		return organismModelAnnotations;
	}
}

//MACRO	Root = 0.774	0.688	0.885	0.944	0.936	0.955
//MACRO	hasDosage = 0.540	0.688	0.445	0.948	0.936	0.955
//MACRO	Cardinality = 0.851	0.766	0.958	1.000	1.000	1.000
//MACRO	Overall = 0.774	0.688	0.885	0.944	0.936	0.955
//modelName: Anaesthetic1773861614
//CRFStatistics [context=Train, getTotalDuration()=10734]
//CRFStatistics [context=Test, getTotalDuration()=31]

//MACRO	Root = 0.582	0.719	0.490	0.592	0.719	0.505	0.984	1.000	0.969
//MACRO	hasDeliveryMethod = 0.430	0.517	0.368	0.479	0.577	0.410	0.897	0.897	0.897
//MACRO	hasDosage = 0.854	1.000	0.745	0.868	1.000	0.769	0.984	1.000	0.969
//MACRO	Cardinality = 0.850	1.000	0.740	0.864	1.000	0.763	0.984	1.000	0.969
//MACRO	Overall = 0.668	0.771	0.589	0.701	0.796	0.629	0.953	0.969	0.937
//modelName: Anaesthetic1113428331
//CRFStatistics [context=Train, getTotalDuration()=20215]
//CRFStatistics [context=Test, getTotalDuration()=109]