package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.investigationMethod;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.base.Sys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.jsonldjava.shaded.com.google.common.base.Predicate;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.NerlaObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.LiteralAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.SectionizedEntityRecLinkExplorer;

/**
 * @author hterhors Final Score: Score [ getF1()=0.170, getPrecision()=0.107,
 *         getRecall()=0.404, tp=72, fp=599, fn=106, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=1699948] CRFStatistics
 *         [context=Test, getTotalDuration()=302525] modelName:
 *         GroupName_895041394
 */
public class InvestigationMethodNER {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * Start the named entity recognition and linking procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new InvestigationMethodNER();
	}

	public InvestigationMethodNER() {
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("InvestigationMethod"))
				/**
				 * Finally, we build the systems scope.
				 */
				.build();
//		String modelName = "NERLA1387292063";
		String modelName = "InvestigationMethod" + new Random().nextInt();
		log.info("modelName: " + modelName);

		AbstractCorpusDistributor originalCorpusDistributor = new ShuffleCorpusDistributor.Builder().setSeed(1000L)
				.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(0.1F).build();
//		AbstractCorpusDistributor originalCorpusDistributor = new OriginalCorpusDistributor.Builder()
//				.setCorpusSizeFraction(1F).build();
		SectionizedEntityRecLinkExplorer.MAX_WINDOW_SIZE = 8;

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod),
				originalCorpusDistributor);

		List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		InvestigationMethodNERLPredictor predictor = new InvestigationMethodNERLPredictor(modelName,
				trainingInstanceNames, developInstanceNames, testInstanceNames);

		if (true) {

			predictor.trainOrLoadModel();

			Map<Instance, State> results = predictor.crf.predict(
					predictor.instanceProvider.getRedistributedDevelopmentInstances(), predictor.maxStepCrit,
					predictor.noModelChangeCrit);

			log.info("Final Score: "
					+ AbstractSemReadProject.evaluate(log, results, predictor.evaluationObjectiveFunction));

			persentenceEvaluation(results);

			log.info(predictor.crf.getTrainingStatistics());
			log.info(predictor.crf.getTestStatistics());
		}

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
		log.info("modelName: " + modelName);
	}

	private void persentenceEvaluation(Map<Instance, State> results) {
		System.out.println("Per sentence evaluation...");
		NerlaEvaluator evaluator = new NerlaEvaluator(EEvaluationDetail.LITERAL);

		Score s = new Score();
		for (Instance instance : results.keySet()) {
			System.out.println(instance.getName());
			List<LiteralAnnotation> goldData = instance.getGoldAnnotations().getAbstractAnnotations().stream()
					.map(a -> a.asInstanceOfDocumentLinkedAnnotation())
					.map(d -> AnnotationBuilder.toAnnotation(d.getEntityType().name, "" + d.getSentenceIndex()))
					.distinct().sorted().collect(Collectors.toList());
			List<LiteralAnnotation> predictedData = results.get(instance).getCurrentPredictions()
					.getAbstractAnnotations().stream().map(a -> a.asInstanceOfDocumentLinkedAnnotation())
					.map(d -> AnnotationBuilder.toAnnotation(d.getEntityType().name, "" + d.getSentenceIndex()))
					.distinct().sorted().collect(Collectors.toList());

			System.out.println("----GOLD----");
			for (LiteralAnnotation literalAnnotation : goldData) {
				System.out.println(literalAnnotation.toPrettyString());
			}
			System.out.println("----PREDICT----");
			for (LiteralAnnotation literalAnnotation : predictedData) {
				System.out.println(literalAnnotation.toPrettyString());
			}

			Score score = evaluator.scoreMultiValues(goldData, predictedData, EScoreType.MICRO);
			System.out.println(instance.getName() + ": " + score);
			s.add(score);
			System.out.println("-------------------");

		}
		System.out.println(s);
	}

}
