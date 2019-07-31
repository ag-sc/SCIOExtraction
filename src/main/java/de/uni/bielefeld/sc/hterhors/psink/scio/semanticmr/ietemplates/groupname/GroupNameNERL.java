package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.groupname;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.groupname.specs.GroupNameSpecs;

/**
 * Example of how to perform named entity recognition and linking.
 * 
 * @author hterhors
 *
 */
public class GroupNameNERL {
	private static Logger log = LogManager.getFormatterLogger("NERL");

	/**
	 * Start the named entity recognition and linking procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new GroupNameNERL();
	}

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory = new File("src/main/resources/nerl/group_name/corpus/instances/");

	public GroupNameNERL() {

		String modelName = "NERLA" + new Random().nextInt();
		SystemScope scope = SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(GroupNameSpecs.systemsScope)
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

//		NerlaEvaluator eval = new NerlaEvaluator(EEvaluationDetail.LITERAL);
//		Document d = new Document("1", "Hello Hello");
//
//		System.out.println(eval.prf1(AnnotationBuilder.toAnnotation(d, "GroupName", "Hello", 0),
//				AnnotationBuilder.toAnnotation(d, "GroupName", "Hello", 6)));
//

		AbstractCorpusDistributor originalCorpusDistributor = new OriginalCorpusDistributor.Builder()
				.setCorpusSizeFraction(0.25F).build();

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 20;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, originalCorpusDistributor);

		List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		GroupNameNERLPredictor predictor = new GroupNameNERLPredictor(modelName, scope, trainingInstanceNames,
				developInstanceNames, testInstanceNames);

		predictor.trainOrLoadModel();

		predictor.evaluateOnDevelopment();

		/**
		 * Finally, we evaluate the produced states and print some statistics.
		 */

		final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
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
	}

}
