package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.investigation_method;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AnalyzeComplexity;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.investigation_method.InvestigationMethodRestrictionProvider.EInvestigationMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

/**
 * 
 * @author hterhors
 * 
 */
public class InvestigationMethodSlotFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new InvestigationMethodSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

	public InvestigationMethodSlotFilling() throws IOException {

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
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("InvestigationMethod"))
				/**
				 * Finally, we build the systems scope.
				 */
				.build();
		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod);
//		SpecificationWriter w = new SpecificationWriter(scope);
//		w.writeEntitySpecificationFile(new File("src/main/resources/slotfilling/investigation_method/entities.csv"),
//				EntityType.get("InvestigationMethod"));
//		w.writeHierarchiesSpecificationFile(
//				new File("src/main/resources/slotfilling/investigation_method/hierarchies.csv"),
//				EntityType.get("InvestigationMethod"));
//		w.writeSlotsSpecificationFile(new File("src/main/resources/slotfilling/investigation_method/slots.csv"),
//				EntityType.get("InvestigationMethod"));
//		w.writeStructuresSpecificationFile(
//				new File("src/main/resources/slotfilling/result/specifications/structures.csv"),
//				new File("src/main/resources/slotfilling/investigation_method/structures.csv"),
//				EntityType.get("InvestigationMethod"));
//

		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.build();

//		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
//				.setSeed(1000L).setDevelopmentProportion(20).setTestProportion(20).setCorpusSizeFraction(1F).build();
		InstanceProvider.maxNumberOfAnnotations = 100;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
				getGoldModificationRules());

		List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		String modelName = "InvestigationMethod" + new Random().nextInt();

		Stats.countVariables(1, instanceProvider.getInstances());

//		System.exit(1);

		AnalyzeComplexity.analyze(SCIOEntityTypes.investigationMethod
				, new HashSet<>(), instanceProvider.getInstances(),
				new SlotFillingObjectiveFunction(EScoreType.MICRO,
						new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE, EEvaluationDetail.ENTITY_TYPE))
								.getEvaluator());

		InvestigationMethodSlotFillingPredictor predictor = new InvestigationMethodSlotFillingPredictor(modelName,
				trainingInstanceNames, developInstanceNames, testInstanceNames, EInvestigationMethodModifications.ROOT,
				ENERModus.GOLD);

//		predictor.trainOrLoadModel();
//
//		predictor.evaluateOnDevelopment();

		/**
		 * Finally, we evaluate the produced states and print some statistics.
		 */

		final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(true);
		log.info("Coverage Training: " + trainCoverage);

		final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(true);
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
	}

	private Collection<GoldModificationRule> getGoldModificationRules() {

		List<GoldModificationRule> list = new ArrayList<>();
//
//		list.add(
//				new GoldModificationRule() {

//			@Override
//			public AbstractAnnotation modify(AbstractAnnotation goldAnnotation) {
//				if (goldAnnotation.getEntityType() == EntityType.get("Trend"))
//					if (goldAnnotation.asInstanceOfEntityTemplate().isEmpty())
//						return null;
//
//				return goldAnnotation;
//			}
//		});

		return list;
	}
}
