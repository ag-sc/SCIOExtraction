package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjuryRestrictionProvider.EInjuryModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.specs.InjurySpecs;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaSlotFilling;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.DurationNormalization;

/**
 * Slot filling for injuries.
 * 
 * 
 * Mean Score: Score [getF1()=0.416, getPrecision()=0.521, getRecall()=0.347,
 * tp=76, fp=70, fn=143, tn=0] CRFStatistics [context=Train,
 * getTotalDuration()=200631] CRFStatistics [context=Test,
 * getTotalDuration()=6597] Compute coverage... Coverage Training: Score
 * [getF1()=0.950, getPrecision()=0.985, getRecall()=0.917, tp=719, fp=11,
 * fn=65, tn=0] Compute coverage... No states were generated for instance: N156
 * Kalincik 2010 2 Coverage Development: Score [getF1()=0.814,
 * getPrecision()=0.905, getRecall()=0.740, tp=162, fp=17, fn=57, tn=0]
 * Injury-520642072
 * 
 * 
 * @author hterhors
 *
 */
public class InjurySlotFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new InjurySlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	private final File instanceDirectory = new File("src/main/resources/slotfilling/injury/corpus/instances/");

	public static EInjuryModifications rule;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public InjurySlotFilling() throws IOException {

		SystemScope scope = SystemScope.Builder.getScopeHandler().addScopeSpecification(InjurySpecs.systemsScope)
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization()).build();

//		SpecificationWriter w = new SpecificationWriter(scope);
//		w.writeEntitySpecificationFile(new File("src/main/resources/slotfilling/injury/entities.csv"),
//				EntityType.get("Injury"));
//		w.writeHierarchiesSpecificationFile(new File("src/main/resources/slotfilling/injury/hierarchies.csv"),
//				EntityType.get("Injury"));
//		w.writeSlotsSpecificationFile(new File("src/main/resources/slotfilling/injury/slots.csv"),
//				EntityType.get("Injury"));
//		w.writeStructuresSpecificationFile(ResultSpecifications.structures,new File("src/main/resources/slotfilling/injury/structures.csv"),
//				EntityType.get("Injury"));
//
//		System.exit(1);

		PrintStream resultsOut = new PrintStream(new File("results/injuryResults.csv"));

		resultsOut.println(header);
//		List<String> names = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());
		VertebralAreaSlotFilling.rule = EVertebralAreaModifications.NO_MODIFICATION;
		for (EInjuryModifications rule : EInjuryModifications.values()) {
			SlotType.includeAll();
			InjurySlotFilling.rule = EInjuryModifications.ROOT_DEVICE_LOCATION_ANAESTHESIA;
//			InjurySlotFilling.rule = rule;

//			AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
//					.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

//			AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
//					.setCorpusSizeFraction(1F).build();

			List<String> docs = Files.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

			Collections.shuffle(docs, new Random(100L));

			int percent = (int) ((((double) docs.size()) / 100D) * 80D);
			List<String> tn = docs.subList(0, percent);
			List<String> dn = docs.subList(percent, docs.size());

			AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
					.setTrainingInstanceNames(tn).setDevelopInstanceNames(dn).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					InjuryRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			String modelName = "Injury" + new Random().nextInt(10000);

			InjurySlotFillingPredictor predictor = new InjurySlotFillingPredictor(modelName, scope,
					trainingInstanceNames, developInstanceNames, testInstanceNames);

//			SlotType.get("hasInjuryDevice").include();
//			SlotType.get("hasInjuryLocation").exclude();
//
//			SlotType.get("hasInjuryArea").exclude();
//			SlotType.get("hasInjuryPostsurgicalCare").exclude();
//			SlotType.get("hasAnimalCareCondition").exclude();
//			SlotType.get("hasInjuryIntensity").exclude();
//			SlotType.get("hasDirection").exclude();
//			SlotType.get("hasMedication").exclude();
//			SlotType.get("hasInjuryAnaesthesia").exclude();

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Score standard = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			SlotType.excludeAll();
			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);


			log.info("standard: " + standard);
			log.info("only root: " + score);

			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());

			resultsOut.println(toResults(rule, standard, "standard"));
			resultsOut.println(toResults(rule, score, "onyl root"));

			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
			log.info("Coverage Training: " + trainCoverage);
			resultsOut.println(toResults(rule, trainCoverage, "coverage on train"));
			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
			log.info("Coverage Development: " + devCoverage);
			resultsOut.println(toResults(rule, devCoverage, "coverage on dev"));

			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */
			log.info("modelName: " + predictor.modelName);
			break;
		}

		resultsOut.flush();
		resultsOut.close();
	}

	private String toResults(EInjuryModifications rule, Score score, String context) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall()) + "\t"
				+ context;
	}
}
