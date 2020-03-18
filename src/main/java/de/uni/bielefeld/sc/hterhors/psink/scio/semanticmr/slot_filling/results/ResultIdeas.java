package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.results;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.Set;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.AutomatedSectionifcation;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.AutomatedSectionifcation.ESection;

/**
 * Collection of some ideas to tackle results:
 * 
 * 0) All sentences in result section 1) Filter sentences with
 * InvestigationMethods 2) Filter sentences with Trend
 * 
 * @author hterhors
 *
 *
 *
 *         Collect annotation on sentences in result section and print
 *
 */

public class ResultIdeas {
	private final File instanceDirectory;

	private InstanceProvider instanceProvider;
	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;

	private String modelName;
	private File outputFile;

	private final String rand;

	public static void main(String[] args) {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build();

		new ResultIdeas();
	}

	public ResultIdeas() {

		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 200;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 200;

		rand = String.valueOf(new Random().nextInt(100000));

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 80;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();

		Map<Instance, Map<Integer, List<EntityType>>> pred = buildFromAnnotatedData();

		Map<Instance, List<Map<Integer, List<EntityType>>>> gold = resultsPerSentenceGOLD();

		for (Instance instance : pred.keySet()) {

			Set<Integer> predSentences = new HashSet<>(pred.get(instance).keySet());

			Set<Integer> goldSentences = new HashSet<>(
					gold.get(instance).stream().flatMap(a -> a.keySet().stream()).collect(Collectors.toSet()));

			System.out.println(predSentences);
			System.out.println(goldSentences);

			System.out.println(instance.getName() + "\t" + prf1(goldSentences, predSentences));

		}

	}

	public Score prf1(Collection<Integer> annotations, Collection<Integer> otherAnnotations) {

		int tp = 0;
		int fp = 0;
		int fn = 0;

		outer: for (Integer a : annotations) {
			for (Integer oa : otherAnnotations) {
				if (oa == a) {
					tp++;
					continue outer;
				}
			}

			fn++;
		}

		fp = Math.max(otherAnnotations.size() - tp, 0);

		return new Score(tp, fp, fn);

	}

	private Map<Instance, Map<Integer, List<EntityType>>> buildFromAnnotatedData() {
		Map<Instance, Set<AbstractAnnotation>> investigationMethodAnnotations = new HashMap<>();

		JSONNerlaReader nerlaIMJSONReader = new JSONNerlaReader(
				new File("data/slot_filling/investigation_method/regex_nerla"));

		for (Instance instance : instanceProvider.getInstances()) {

			investigationMethodAnnotations.put(instance, new HashSet<>(nerlaIMJSONReader.getForInstance(instance)));
		}
		Map<Instance, Set<AbstractAnnotation>> trendAnnotations = new HashMap<>();

		JSONNerlaReader nerlaTrendJSONReader = new JSONNerlaReader(new File("data/slot_filling/trend/regex_nerla"));

		for (Instance instance : instanceProvider.getInstances()) {

			trendAnnotations.put(instance, new HashSet<>(nerlaTrendJSONReader.getForInstance(instance)));
		}

		Map<Instance, Map<Integer, List<EntityType>>> collectAnnotations = new HashMap<>();

		for (Instance instance : instanceProvider.getInstances()) {
			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

			collectAnnotations.put(instance, new HashMap<>());
			/**
			 * invest method per sentence
			 */
			for (AbstractAnnotation investMeth : investigationMethodAnnotations.get(instance)) {

				if (sectionification.getSection(
						investMeth.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()) != ESection.RESULTS)
					continue;
				collectAnnotations.get(instance).putIfAbsent(
						investMeth.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(), new ArrayList<>());
				collectAnnotations.get(instance)
						.get(investMeth.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.add(investMeth.getEntityType());
			}
			/**
			 * trends per sentence
			 */
			for (AbstractAnnotation trend : trendAnnotations.get(instance)) {
				if (sectionification.getSection(
						trend.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()) != ESection.RESULTS)
					continue;
				collectAnnotations.get(instance).putIfAbsent(
						trend.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(), new ArrayList<>());
				collectAnnotations.get(instance).get(trend.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
						.add(trend.getEntityType());
			}

		}

		Map<Instance, Map<Integer, List<EntityType>>> results = new HashMap<>();

		for (Instance instance : collectAnnotations.keySet()) {

			for (Entry<Integer, List<EntityType>> annotationsPerSentence : collectAnnotations.get(instance)
					.entrySet()) {

				if (annotationsPerSentence.getValue().size() < 2)
					continue;

				if (!containsRelatedClassOf(annotationsPerSentence.getValue(), EntityType.get("Trend"))) {
					continue;
				}
				if (!containsSubClassOf(annotationsPerSentence.getValue(), EntityType.get("InvestigationMethod"))) {
					continue;
				}

				System.out.println(instance.getName() + "\t" + annotationsPerSentence.getKey() + "\t"
						+ annotationsPerSentence.getValue());

				results.put(instance, new HashMap<>());
				results.get(instance).put(annotationsPerSentence.getKey(), annotationsPerSentence.getValue());

			}
		}
		return results;
	}

	private Map<Instance, List<Map<Integer, List<EntityType>>>> resultsPerSentenceGOLD() {
		Map<Instance, List<Map<Integer, List<EntityType>>>> results = new HashMap<>();
		for (Instance instance : instanceProvider.getInstances()) {

			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);

			for (AbstractAnnotation result : instance.getGoldAnnotations().getAnnotations()) {
				Map<Integer, List<EntityType>> entitiesPerSentence = new HashMap<>();
				collectAnnotationForResult(sectionification, entitiesPerSentence, result);

				results.putIfAbsent(instance, new ArrayList<>());
				results.get(instance).add(entitiesPerSentence);

			}

		}

		int numberOfResults = 0;
		int reminingResults = 0;

		for (Instance instance : results.keySet()) {
			numberOfResults += results.get(instance).size();

			for (Map<Integer, List<EntityType>> entitiesPerSentence : results.get(instance)) {

				boolean add = false;
				for (Integer sentence : entitiesPerSentence.keySet()) {
					List<EntityType> annotations = entitiesPerSentence.get(sentence);
					Collections.sort(annotations);

					if (!containsRelatedClassOf(annotations, EntityType.get("Trend"))) {
						continue;
					}
					if (!containsSubClassOf(annotations, EntityType.get("InvestigationMethod"))) {
						continue;
					}

					add = true;

					System.out.println(
							"ADD: " + instance.getName() + "\t" + sentence + "\t" + entitiesPerSentence.get(sentence));
				}
				if (add)
					reminingResults++;
				System.out.println();
			}
		}

		System.out.println("numberOfResults  = " + numberOfResults);
		System.out.println("reminingResults  = " + reminingResults);
		return results;
	}

	private boolean containsRelatedClassOf(List<EntityType> annotations, EntityType entityType) {

		for (EntityType et : entityType.getRelatedEntityTypes()) {
			if (annotations.contains(et))
				return true;

		}
		return false;
	}

	private boolean containsSubClassOf(List<EntityType> annotations, EntityType entityType) {

		for (EntityType et : entityType.getTransitiveClosureSubEntityTypes()) {
			if (annotations.contains(et))
				return true;

		}
		return false;
	}

	/**
	 * @param entitiesPerSentence
	 * 
	 */

	public void collectAnnotationForResult(AutomatedSectionifcation sectionification,
			Map<Integer, List<EntityType>> entitiesPerSentence, AbstractAnnotation et) {

		if (et.isInstanceOfDocumentLinkedAnnotation() && sectionification
				.getSection(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()) == ESection.RESULTS) {

			entitiesPerSentence.putIfAbsent(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(),
					new ArrayList<>());

			entitiesPerSentence.get(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex())
					.add(et.getEntityType());

		}
		if (et.isInstanceOfEntityTemplate()) {
			collectAnnotationForResult(sectionification, entitiesPerSentence,
					et.asInstanceOfEntityTemplate().getRootAnnotation());
			for (Entry<SlotType, SingleFillerSlot> instance2 : et.asInstanceOfEntityTemplate().getSingleFillerSlots()
					.entrySet()) {
				collectAnnotationForResult(sectionification, entitiesPerSentence, instance2.getValue().getSlotFiller());

			}
			for (Entry<SlotType, MultiFillerSlot> instance2 : et.asInstanceOfEntityTemplate().getMultiFillerSlots()
					.entrySet()) {
				for (AbstractAnnotation instance : instance2.getValue().getSlotFiller()) {
					collectAnnotationForResult(sectionification, entitiesPerSentence, instance);
				}
			}
		}
	}

}
