package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.organism_model;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;

/**
 * Example of how to perform named entity recognition and linking.
 * 
 * @author hterhors Final Score: Score [ getF1()=0.170, getPrecision()=0.107,
 *         getRecall()=0.404, tp=72, fp=599, fn=106, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=1699948] CRFStatistics
 *         [context=Test, getTotalDuration()=302525] modelName:
 *         GroupName_895041394
 */
public class OrganismModelNER {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * Start the named entity recognition and linking procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new OrganismModelNER();
	}

	public OrganismModelNER() {
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("OrganismModel"))
				/**
				 * Finally, we build the systems scope.
				 */
				.build();
		String modelName = "OM_" + new Random().nextInt();
//		String modelName = "OM_-426874068" ;
		log.info("modelName: " + modelName);

		AbstractCorpusDistributor originalCorpusDistributor = new OriginalCorpusDistributor.Builder()
				.setCorpusSizeFraction(0.1F).build();

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.organismModel),
				originalCorpusDistributor);

		List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		OrganismModelNERLPredictor predictor = new OrganismModelNERLPredictor(modelName, trainingInstanceNames, developInstanceNames,
				testInstanceNames);

		predictor.trainOrLoadModel();

		Map<Instance, State> results = predictor.crf.predict(
				predictor.instanceProvider.getDevelopmentInstances(), predictor.maxStepCrit,
				predictor.noModelChangeCrit);

		log.info(
				"Final Score: " + AbstractSemReadProject.evaluate(log, results, predictor.evaluationObjectiveFunction));

		log.info(predictor.crf.getTrainingStatistics());
		log.info(predictor.crf.getTestStatistics());

		/**
		 * Finally, we evaluate the produced states and print some statistics.
		 */
//
//		final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//		log.info("Coverage Training: " + trainCoverage);
//
//		final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//		log.info("Coverage Development: " + devCoverage);

		/**
		 * Computes the coverage of the given instances. The coverage is defined by the
		 * objective mean score that can be reached relying on greedy objective function
		 * sampling strategy. The coverage can be seen as the upper bound of the system.
		 * The upper bound depends only on the exploration strategy, e.g. the provided
		 * NER-annotations during slot-filling.
		 */
		log.info("modelName: " + modelName);
	}

}
