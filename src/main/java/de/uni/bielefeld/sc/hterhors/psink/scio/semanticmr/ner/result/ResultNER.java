package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
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
public class ResultNER {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * Start the named entity recognition and linking procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
//		args = new String[]{"0","6"};

		System.out.println(new Score(3, 2,2));
		
		
//		new ResultNER(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}

	public ResultNER(int from, int to) {
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("Result"))
				/**
				 * Finally, we build the systems scope.
				 */
				.build();
//		String modelName = "Result_" + new Random().nextInt();
		String modelName = "Result_-780036971";
		log.info("modelName: " + modelName);
		log.info("NOTE: TRAIN ON ABSTRACT ONLY");

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(90)
				.setSeed(1000L).setDevelopmentProportion(10).setCorpusSizeFraction(1F).build();
//		AbstractCorpusDistributor originalCorpusDistributor = new OriginalCorpusDistributor.Builder()
//				.setCorpusSizeFraction(1F).build();

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 3000;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result), corpusDistributor);

		List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		ResultNERLPredictor predictor = new ResultNERLPredictor(modelName, trainingInstanceNames, developInstanceNames,
				testInstanceNames);

		predictor.trainOrLoadModel();

		Map<Instance, State> resultsAll = new HashMap<>();
		int c = -1;
		for (Instance i : predictor.instanceProvider.getDevelopmentInstances()) {
			System.out.println(i.getName());
			c++;
			if (!(c >= from && c <= to))
				continue;

			System.out.println(c);

			Map<Instance, State> results = predictor.crf.predict(new ArrayList<>(Arrays.asList(i)),
					predictor.maxStepCrit, predictor.noModelChangeCrit);

			resultsAll.putAll(results);

			JsonNerlaIO io = new JsonNerlaIO(true);
			File nerlaALLDiractory = new File("predicted/nerla");
			if (!nerlaALLDiractory.exists())
				nerlaALLDiractory.mkdirs();
			log.info("Write nerlas to: " + nerlaALLDiractory);
			for (Entry<Instance, State> result : results.entrySet()) {

				try {

					List<JsonEntityAnnotationWrapper> wrappedAnnotation = result.getValue().getCurrentPredictions()
							.getAbstractAnnotations().stream()
							.map(d -> new JsonEntityAnnotationWrapper(d.asInstanceOfDocumentLinkedAnnotation()))
							.collect(Collectors.toList());

					io.writeNerlas(new File(nerlaALLDiractory, result.getKey().getName() + ".nerla.json"),
							wrappedAnnotation);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		log.info("Final Score: "
				+ AbstractSemReadProject.evaluate(log, resultsAll, predictor.evaluationObjectiveFunction));

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
