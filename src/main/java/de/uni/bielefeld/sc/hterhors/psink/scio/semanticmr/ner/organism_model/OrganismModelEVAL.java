package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.organism_model;

import java.io.File;
import java.io.IOException;
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
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
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
public class OrganismModelEVAL {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * Start the named entity recognition and linking procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new OrganismModelEVAL();
	}

	public OrganismModelEVAL() {
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
		log.info("modelName: " + modelName);

		AbstractCorpusDistributor originalCorpusDistributor = new OriginalCorpusDistributor.Builder()
				.setCorpusSizeFraction(1F).build();

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.organismModel),
				originalCorpusDistributor);

		List<String> trainingInstanceNames = instanceProvider.getTrainingInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getDevelopmentInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		JSONNerlaReader nerlaJSONReader = new JSONNerlaReader(getExternalNerlaFile(),
				new HashSet<>(instanceProvider.getInstances()));

		Map<Instance, Set<AbstractAnnotation>> goldAnnotations = new HashMap<>();
		Map<Instance, Set<AbstractAnnotation>> predictAnnotations = new HashMap<>();

		NerlaEvaluator evalDocLinked = new NerlaEvaluator(EEvaluationDetail.DOCUMENT_LINKED);
		NerlaEvaluator evalLiteral = new NerlaEvaluator(EEvaluationDetail.LITERAL);
		NerlaEvaluator evalEntityType = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		Score sTokenAll = new Score();
		Score sLiteralAll = new Score();
		Score sTypeAll = new Score();

		for (Instance instance : instanceProvider.getInstances()) {
			predictAnnotations.put(instance, new HashSet<>());
			predictAnnotations.get(instance).addAll(nerlaJSONReader.getForInstance(instance));

			goldAnnotations.put(instance, new HashSet<>());
			goldAnnotations.get(instance).addAll(instance.getGoldAnnotations().getAnnotations());

			Score sToken = evalDocLinked.scoreMultiValues(goldAnnotations.get(instance),
					predictAnnotations.get(instance), EScoreType.MICRO);
			sTokenAll.add(sToken);

			Score sLiteral = evalLiteral.scoreMultiValues(goldAnnotations.get(instance),
					predictAnnotations.get(instance), EScoreType.MICRO);
			sLiteralAll.add(sLiteral);
			Score sType = evalEntityType.scoreMultiValues(goldAnnotations.get(instance),
					predictAnnotations.get(instance), EScoreType.MICRO);
			sTypeAll.add(sType);
		}
		System.out.println(sTokenAll);
		System.out.println(sLiteralAll);
		System.out.println(sTypeAll);
		System.exit(1);

		OrganismModelNERLPredictor predictor = new OrganismModelNERLPredictor(modelName, trainingInstanceNames,
				developInstanceNames, testInstanceNames);

		predictor.trainOrLoadModel();

		Map<Instance, State> results = predictor.crf.predict(predictor.instanceProvider.getDevelopmentInstances(),
				predictor.maxStepCrit, predictor.noModelChangeCrit);

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

	protected File getExternalNerlaFile() {
		// Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE 0.95 0.96 0.94
		// modelName: OrganismModel-1205615375
//		if (modus == ENERModus.GOLD)
//			return new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR,
//					AnnotationsCorpusBuilderBib.toDirName(SCIOEntityTypes.organismModel));
//		else
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.organismModel);

		// Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE 0.85 0.88 0.83
		// modelName: OrganismModel-673015650
		// return new
		// File("src/main/resources/additional_nerla/organism_model/LITERAL");
//		Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE	0.87	0.91	0.83
//		modelName: OrganismModel1469327283
//		return new File("src/main/resources/additional_nerla/organism_model/DOCUMENT_LINKED");
	}
}
