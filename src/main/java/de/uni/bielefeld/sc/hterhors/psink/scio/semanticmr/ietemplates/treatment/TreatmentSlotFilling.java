package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.tools.specifications.SpecificationWriter;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentRestrictionProvider.ETreatmentModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.specs.TreatmentSpecs;

/**
 * 
 * @author hterhors
 *
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
	private final File instanceDirectory = new File("src/main/resources/slotfilling/treatment/corpus/instances/");

	public static ETreatmentModifications rule;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public TreatmentSlotFilling() throws IOException {
		SystemScope scope = SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(TreatmentSpecs.systemsScopeReader).build();

//		SpecificationWriter w = new SpecificationWriter(scope);
//		w.writeEntitySpecificationFile(new File("src/main/resources/slotfilling/treatment/entities.csv"), EntityType.get("Treatment"));
//		w.writeHierarchiesSpecificationFile(new File("src/main/resources/slotfilling/treatment/hierarchies.csv"), EntityType.get("Treatment"));
//		w.writeSlotsSpecificationFile(new File("src/main/resources/slotfilling/treatment/slots.csv"), EntityType.get("Treatment"));
////		w.writeStructuresSpecificationFile(null, EntityType.get("Treatment"));
//	
//		System.exit(1);

		PrintStream resultsOut = new PrintStream(new File("results/treatmentResults.csv"));

		resultsOut.println(header);

		for (ETreatmentModifications rule : ETreatmentModifications.values()) {
			TreatmentSlotFilling.rule = rule;

			AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
					.setCorpusSizeFraction(1F).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					TreatmentRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

//			String modelName = "Treatment819785968";
			String modelName = "Treatment" + new Random().nextInt();

			TreatmentSlotFillingPredictor predictor = new TreatmentSlotFillingPredictor(modelName, scope,
					trainingInstanceNames, developInstanceNames, testInstanceNames);

			predictor.trainOrLoadModel();

			Score score = predictor.evaluateOnDevelopment();

			resultsOut.println(toResults(rule, score));
			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(true);
			log.info("Coverage Training: " + trainCoverage);

			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
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

		resultsOut.flush();
		resultsOut.close();

	}

	private String toResults(ETreatmentModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}
}
