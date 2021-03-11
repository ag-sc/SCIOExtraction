package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.investigation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.exce.DocumentLinkedAnnotationMismatchException;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.NPChunker.TermIndexPair;

public class CollectExpGroupNames {
	public static int maxLength = 25;

	public static class PatternIndexPair {
		public final int impact;
		public final Pattern pattern;
		public final int index;
		private static Set<Integer> indicies = new HashSet<>();
		public final List<Integer> groups;

		public PatternIndexPair(int index, Set<Integer> groups, Pattern pattern) {
			this.pattern = pattern;
			this.index = index;
			this.impact = 1;
			this.groups = new ArrayList<>(groups);
			Collections.sort(this.groups);
			if (!indicies.add(index)) {
				throw new IllegalArgumentException("Duplicate index: " + index);
			}

		}

		public PatternIndexPair(int index, Set<Integer> groups, Pattern pattern, int impact) {
			this.pattern = pattern;
			this.impact = impact;
//			this.impact = 1;
			this.index = index;
			this.groups = new ArrayList<>(groups);
			Collections.sort(this.groups);
			if (!indicies.add(index)) {
				throw new IllegalArgumentException("Duplicate index: " + index);
			}

		}

		public PatternIndexPair(int index, Pattern pattern) {
			this.pattern = pattern;
			this.index = index;
			this.impact = 1;
			this.groups = new ArrayList<>(new HashSet<>(Arrays.asList(0)));
			Collections.sort(this.groups);
			if (!indicies.add(index)) {
				throw new IllegalArgumentException("Duplicate index: " + index);
			}

		}

		public PatternIndexPair(int index, Pattern pattern, int impact) {
			this.pattern = pattern;
			this.impact = impact;
//			this.impact = 1;
			this.index = index;
			this.groups = new ArrayList<>(new HashSet<>(Arrays.asList(0)));

			Collections.sort(this.groups);
			if (!indicies.add(index)) {
				throw new IllegalArgumentException("Duplicate index: " + index);
			}

		}

	}

	static class Finding {
		final public String term;
		final public PatternIndexPair pattern;

		public Finding(String finding, PatternIndexPair pattern) {
			this.term = finding;
			this.pattern = pattern;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((term == null) ? 0 : term.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Finding other = (Finding) obj;
			if (term == null) {
				if (other.term != null)
					return false;
			} else if (!term.equals(other.term))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return term + "\t" + pattern.index;
		}

	}

	public static void main(String[] args) throws Exception {
		
		for (PatternIndexPair string : pattern) {
			System.out.println(string.pattern);
		}
		
		System.exit(1);
		
		CollectExpGroupNames pg = new CollectExpGroupNames(new File("src/main/resources/slotfilling/corpus_docs.csv"));

		/**
		 * Based on regex
		 */
		Map<String, HashMap<Integer, Cluster>> clusters = pg.collectClusters();

		Map<String, Set<AbstractAnnotation>> treatments = pg.getTreatments();
		Map<String, Set<AbstractAnnotation>> orgModels = pg.getOrganismModels();
		Map<String, Set<AbstractAnnotation>> injuries = pg.getInjuryModels();
		pg.merge(clusters, treatments, orgModels, injuries);
	}

	private Map<String, Set<AbstractAnnotation>> getTreatments() {
		Map<String, Set<AbstractAnnotation>> treatmentsPerDocument = new HashMap<>();
		for (Instance instance : instanceProvider.getTrainingInstances()) {

			Set<AbstractAnnotation> treatments = new HashSet<>();
			for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
				treatments.addAll(expGroup.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream()
						.filter(a -> a != null).collect(Collectors.toList()));
			}

			treatmentsPerDocument.put(instance.getName(), treatments);

		}

		return treatmentsPerDocument;
	}

	private Map<String, Set<AbstractAnnotation>> getOrganismModels() {
		Map<String, Set<AbstractAnnotation>> orgModelsPerDocument = new HashMap<>();
		for (Instance instance : instanceProvider.getTrainingInstances()) {

			Set<AbstractAnnotation> orgModels = new HashSet<>();
			for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
				if (expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).containsSlotFiller())
					orgModels.add(expGroup.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller());
			}

			orgModelsPerDocument.put(instance.getName(), orgModels);

		}

		return orgModelsPerDocument;
	}

	private Map<String, Set<AbstractAnnotation>> getInjuryModels() {
		Map<String, Set<AbstractAnnotation>> injuriesPerDocument = new HashMap<>();
		for (Instance instance : instanceProvider.getTrainingInstances()) {

			Set<AbstractAnnotation> injuries = new HashSet<>();
			for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
				if (expGroup.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).containsSlotFiller())
					injuries.add(expGroup.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller());
			}

			injuriesPerDocument.put(instance.getName(), injuries);

		}

		return injuriesPerDocument;
	}

	static private final Map<String, String> syns = new HashMap<>();
	static {
		syns.put("mock", "sham");
		syns.put("loaded", "encapsulated");
		syns.put("media", "vehicle");
		syns.put("medium", "vehicle");
		syns.put("veh", "vehicle");
		syns.put("schwann", "SC");
		syns.put("ensheating", "OEC");
		syns.put("EC", "OEC");
		syns.put("hOEC", "OEC");
		syns.put("POEG", "OEC");
		syns.put("OMPC", "OEC");
		syns.put("NSC", "OEC");
		syns.put("OEC-M", "OEC");
		syns.put("OECM", "OEC");
	}

	private static final Set<String> SPECIAL_KEEPWORDS = new HashSet<>(
			Arrays.asList("media", "single", "alone", "only", "control", "sham", "low", "high"));

	private static final Set<String> ADDITIONAL_STOPWORDS = new HashSet<>(Arrays.asList("transplantation", "untreated",
			"is", "untrained", "blank", "transplanted", "transection", "grafts", "normal", "injection", "injections",
			"cultured", "cords", "uninfected", "injected", "additional", "ca", "observed", "grafted", "graft", "cells",
			"are", "effects", "gray", "cord", "spinal", "identifi", "cation", "n", "treated", "treatment", "",
			"received", "the", "injured", "all", "lesioned", "fi", "rst", "first", "second", "third", "fourth", "group",
			"animals", "rats", "in", "same", "individual", "groups", "were"));

	private static Set<String> ALL_STOPWORDS;

	static {
		try {
			ALL_STOPWORDS = new HashSet<>(Files.readAllLines(new File("src/main/resources/top1000.csv").toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		ALL_STOPWORDS.addAll(ADDITIONAL_STOPWORDS);
		ALL_STOPWORDS.removeAll(SPECIAL_KEEPWORDS);
	}

	private final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	InstanceProvider instanceProvider;
	Map<String, Integer> countExpGroups = new HashMap<>();
	Map<String, Integer> countTreatments = new HashMap<>();
	Map<String, Integer> countInjuries = new HashMap<>();
	Map<String, Integer> countOrganismModels = new HashMap<>();

	public CollectExpGroupNames(File file) throws Exception {

		SystemScope.verbose = false;
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup"))
				.apply().registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build();

		InstanceProvider.maxNumberOfAnnotations = 100;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;
		InstanceProvider.verbose = false;

		AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
				.setTrainingInstanceNames(Files.readAllLines(file.toPath())).build();
//		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
//				.build();

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		instanceProvider.getTrainingInstances().stream().forEach(i -> {
			countExpGroups.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
							.filter(b -> b != null && (b.asInstanceOfEntityTemplate()
									.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).containsSlotFiller()
									|| b.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).containsSlotFiller()
									|| b.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
											.containsSlotFiller()))
							.count()));

			countInjuries.put(i.getName(), Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
					.filter(zzz -> zzz != null).map(b -> b.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).getSlotFiller())
					.filter(zzz -> zzz != null).distinct().count()));

			countOrganismModels.put(i.getName(), Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
					.filter(zzz -> zzz != null).map(b -> b.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel).getSlotFiller())
					.filter(zzz -> zzz != null).distinct().count()));

			countTreatments.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream().filter(zzz -> zzz != null)
							.flatMap(b -> b.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.filter(zzz -> zzz != null).distinct().count()));

		});

	}

	private final static boolean includeOrganismModels = false;
	private final static boolean includeInjuryModels = false;

	private void merge(Map<String, HashMap<Integer, Cluster>> clusters, Map<String, Set<AbstractAnnotation>> treatments,
			Map<String, Set<AbstractAnnotation>> organismModels, Map<String, Set<AbstractAnnotation>> injuryModels) {
		Score overallFullRandomScore = new Score();
		Score overallFullHeuristicsScore = new Score();

		Score overallSimpleEvalRandomScore = new Score();
		Score overallSimpleEvalHeuristicsScore = new Score();
		final NerlaEvaluator simpleEval = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);
		final CartesianEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE);

		for (Instance instance : instanceProvider.getTrainingInstances()) {

//			if (!instance.getName().startsWith("N253"))
//				continue;

			System.out.println("########\t" + instance.getName() + "\t########");
			System.out.println("Number of Exp Groups :" + countExpGroups.get(instance.getName()));
			System.out.println("Number of Treatments :" + countTreatments.get(instance.getName()));
			System.out.println("Number of OrganismModels :" + countOrganismModels.get(instance.getName()));
			System.out.println("Number of Injuries :" + countInjuries.get(instance.getName()));

			List<EntityTemplate> goldAnnotations = instance.getGoldAnnotations().<EntityTemplate>getAnnotations()
					.stream()
					.filter(b -> b != null && (b.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel).containsSlotFiller()
							|| b.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel)
									.containsSlotFiller()
							|| b.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType)
									.containsSlotFiller()))
					.collect(Collectors.toList());

			goldAnnotations.forEach(a -> a.clearSlot(SCIOSlotTypes.hasGroupName));
			goldAnnotations.forEach(a -> a.clearSlotOfName("hasNNumber"));

			if (!includeInjuryModels)
				goldAnnotations.forEach(a -> a.clearSlotOfName("hasInjuryModel"));
			if (!includeOrganismModels)
				goldAnnotations.forEach(a -> a.clearSlot(SCIOSlotTypes.hasOrganismModel));

			List<EntityTemplate> predictedAnnotationsBaseline = goldAnnotations.stream().map(a -> {
				EntityTemplate clone = a.deepCopy();
				clone.clearSlotOfName("hasNNumber");
				clone.clearSlot(SCIOSlotTypes.hasGroupName);
				clone.clearSlotOfName("hasInjuryModel");
				clone.clearSlotOfName("hasOrganismModel");
				clone.clearSlot(SCIOSlotTypes.hasTreatmentType);
				return clone;
			}).collect(Collectors.toList());

			List<EntityTemplate> predictedAnnotationsHeuristics = predictedAnnotationsBaseline.stream()
					.map(a -> a.deepCopy()).collect(Collectors.toList());

			fillRandom(predictedAnnotationsBaseline, clusters.get(instance.getName()),
					treatments.get(instance.getName()), organismModels.get(instance.getName()),
					injuryModels.get(instance.getName()));

			fillHeuristics(predictedAnnotationsHeuristics, clusters.get(instance.getName()),
					treatments.get(instance.getName()), organismModels.get(instance.getName()),
					injuryModels.get(instance.getName()));

//			goldAnnotations.forEach(g -> System.out.println(g.toPrettyString()));
//			System.out.println("--------");
//			predictedAnnotationsHeuristics.forEach(g -> System.out.println(g.toPrettyString()));
//			System.out.println("--------");
//			predictedAnnotationsBaseline.forEach(g -> System.out.println(g.toPrettyString()));

			List<Integer> randomBestAssignemnt = evaluator.getBestAssignment(goldAnnotations,
					predictedAnnotationsBaseline, EScoreType.MICRO);
			Score simpleRandomS = simpleEvaluate(false, simpleEval, randomBestAssignemnt, goldAnnotations,
					predictedAnnotationsBaseline);
			overallSimpleEvalRandomScore.add(simpleRandomS);
			System.out.println("---");
			List<Integer> heuristicBestAssignment = evaluator.getBestAssignment(goldAnnotations,
					predictedAnnotationsHeuristics, EScoreType.MICRO);
			Score simpleHeuristicS = simpleEvaluate(true, simpleEval, heuristicBestAssignment, goldAnnotations,
					predictedAnnotationsHeuristics);
			overallSimpleEvalHeuristicsScore.add(simpleHeuristicS);

			System.out.println("Simple Random: " + simpleRandomS);
			System.out.println("Simple Heuristics: " + simpleHeuristicS);

			Score fullRandomS = evaluator.scoreMultiValues(goldAnnotations, predictedAnnotationsBaseline,
					EScoreType.MICRO);
			overallFullRandomScore.add(fullRandomS);

			Score fullHeuristicS = evaluator.scoreMultiValues(goldAnnotations, predictedAnnotationsHeuristics,
					EScoreType.MICRO);
			overallFullHeuristicsScore.add(fullHeuristicS);

			System.out.println("Full Random: " + fullRandomS);
			System.out.println("Full Heuristics: " + fullHeuristicS);

		}
		System.out.println();
		System.out.println();
		System.out.println("******************SIMPLE************************");
		System.out.println("overall simple RandomScore = " + overallSimpleEvalRandomScore);
		System.out.println("overall simple HeuristicsScore = " + overallSimpleEvalHeuristicsScore);
		System.out.println("******************************************");
		System.out.println();
		System.out.println("******************FULL************************");
		System.out.println("overall full RandomScore = " + overallFullRandomScore);
		System.out.println("overall full HeuristicsScore = " + overallFullHeuristicsScore);
		System.out.println("******************************************");

	}

	private Score simpleEvaluate(boolean print, NerlaEvaluator evaluator, List<Integer> bestAssignment,
			List<EntityTemplate> goldAnnotations, List<EntityTemplate> predictedAnnotationsBaseline) {
		Score simpleScore = new Score();

		for (int goldIndex = 0; goldIndex < bestAssignment.size(); goldIndex++) {
			final int predictIndex = bestAssignment.get(goldIndex);
			/*
			 * Treatments
			 */
			List<AbstractAnnotation> goldTreatments = new ArrayList<>(
					goldAnnotations.get(goldIndex).getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());

			List<AbstractAnnotation> predictTreatments = new ArrayList<>(predictedAnnotationsBaseline.get(predictIndex)
					.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller());
			Score s;
			if (goldTreatments.isEmpty() && predictTreatments.isEmpty())
				s = new Score(1, 0, 0);
			else
				s = evaluator.prf1(goldTreatments, predictTreatments);

			if (print && s.getF1() != 1.0D) {
				System.out.println("Compare: g" + goldIndex);
				goldTreatments.forEach(g -> System.out.println(g.toPrettyString()));
				System.out.println("With: p" + predictIndex);
				predictTreatments.forEach(p -> System.out.println(p.toPrettyString()));
				System.out.println("Score: " + s);
				System.out.println("-----");
			}

			simpleScore.add(s);
			/*
			 * OrganismModel
			 */
			List<AbstractAnnotation> goldOrganismModel = Arrays.asList(
					goldAnnotations.get(goldIndex).getSingleFillerSlotOfName("hasOrganismModel").getSlotFiller())
					.stream().filter(a -> a != null).collect(Collectors.toList());
			List<AbstractAnnotation> predictOrganismModel = Arrays.asList(predictedAnnotationsBaseline.get(predictIndex)
					.getSingleFillerSlotOfName("hasOrganismModel").getSlotFiller()).stream().filter(a -> a != null)
					.collect(Collectors.toList());

			simpleScore.add(evaluator.prf1(goldOrganismModel, predictOrganismModel));

			/*
			 * InjuryModel
			 */
			List<AbstractAnnotation> goldInjuryModel = Arrays
					.asList(goldAnnotations.get(goldIndex).getSingleFillerSlotOfName("hasInjuryModel").getSlotFiller())
					.stream().filter(a -> a != null).collect(Collectors.toList());
			List<AbstractAnnotation> predictInjuryModel = Arrays.asList(predictedAnnotationsBaseline.get(predictIndex)
					.getSingleFillerSlotOfName("hasInjuryModel").getSlotFiller()).stream().filter(a -> a != null)
					.collect(Collectors.toList());

			simpleScore.add(evaluator.prf1(goldInjuryModel, predictInjuryModel));

		}

		return simpleScore;
	}

	private void fillRandom(List<EntityTemplate> predictedAnnotations, HashMap<Integer, Cluster> clusters,
			Set<AbstractAnnotation> treatments, Set<AbstractAnnotation> orgModels, Set<AbstractAnnotation> injuries) {

		List<AbstractAnnotation> tr = new ArrayList<>(treatments);
		List<AbstractAnnotation> or = new ArrayList<>(orgModels);
		List<AbstractAnnotation> in = new ArrayList<>(injuries);
		for (int i = 0; i < predictedAnnotations.size() && i < clusters.size(); i++) {
			Collections.shuffle(or, new Random());
			Collections.shuffle(in, new Random());

			EntityTemplate t = predictedAnnotations.get(i);

			if (includeInjuryModels)
				t.setSingleSlotFiller(SCIOSlotTypes.hasInjuryModel, in.get(0));
			if (includeOrganismModels)
				t.setSingleSlotFiller(SCIOSlotTypes.hasOrganismModel, or.get(0));

//			for (int j = 0; j < clusters.get(i).terms.size() && j < tr.size(); j++) {
			Collections.shuffle(tr, new Random());
			t.addMultiSlotFiller(SCIOSlotTypes.hasTreatmentType, tr.get(0));
//			}

		}

	}

	/**
	 * Fills slots by heuristics.
	 * 
	 * @param predictedAnnotations
	 * @param clusters
	 * @param treatments
	 * @param orgModels
	 * @param injuries
	 */
	private void fillHeuristics(List<EntityTemplate> predictedAnnotations, HashMap<Integer, Cluster> clusters,
			Set<AbstractAnnotation> treatments, Set<AbstractAnnotation> orgModels, Set<AbstractAnnotation> injuries) {

		List<AbstractAnnotation> tr = new ArrayList<>(treatments);
		List<AbstractAnnotation> or = new ArrayList<>(orgModels);
		List<AbstractAnnotation> in = new ArrayList<>(injuries);

		/*
		 * Create mapping from clusters to expGroup.
		 */
		final Map<Set<Finding>, EntityTemplate> remainingPredictedAnnotations = new HashMap<>();

		for (Entry<Integer, Cluster> cluster : clusters.entrySet()) {
			remainingPredictedAnnotations.put(cluster.getValue().terms, predictedAnnotations.get(cluster.getKey()));
		}

		for (EntityTemplate expGroup : remainingPredictedAnnotations.values()) {
			if (includeInjuryModels)
				expGroup.setSingleSlotFiller(SCIOSlotTypes.hasInjuryModel, pickRandom(in, new Random()));
			if (includeOrganismModels)
				expGroup.setSingleSlotFiller(SCIOSlotTypes.hasOrganismModel, pickRandom(or, new Random()));

		}

		/**
		 * HEURISTIC findings contains injury/lesion & only/alone / non
		 * 
		 */
		for (Iterator<Entry<Set<Finding>, EntityTemplate>> it = remainingPredictedAnnotations.entrySet().iterator(); it
				.hasNext();) {

			final Set<String> keyWords = it.next().getKey().stream().map(f -> f.term).collect(Collectors.toSet());
			if (keyWords.contains("non")
					|| ((keyWords.contains("laminectomy") || keyWords.contains("lesion") || keyWords.contains("injury"))
							&& (keyWords.contains("only") || keyWords.contains("alone")))) {
				it.remove();
			}

		}

		/**
		 * HEURISTIC if control + vehicle exist control is sham and vehicle is control.
		 */
		{
			boolean containsVehicle = false;
			boolean containsControl = false;
			boolean containsSham = false;
			for (Iterator<Entry<Set<Finding>, EntityTemplate>> it = remainingPredictedAnnotations.entrySet()
					.iterator(); it.hasNext();) {

				final Set<String> keyWords = it.next().getKey().stream().map(f -> f.term).collect(Collectors.toSet());
				containsVehicle |= keyWords.contains("vehicle");
				containsControl |= keyWords.contains("control");
				containsSham |= keyWords.contains("sham");

			}
			if (containsSham) {
				for (Iterator<Entry<Set<Finding>, EntityTemplate>> it = remainingPredictedAnnotations.entrySet()
						.iterator(); it.hasNext();) {
					/*
					 * Remove sham -> no treatment
					 */
					final Set<String> keyWords = it.next().getKey().stream().map(f -> f.term)
							.collect(Collectors.toSet());
					if (keyWords.contains("sham"))
						it.remove();

				}
			} else {
				if (containsVehicle && containsControl) {
					for (Iterator<Entry<Set<Finding>, EntityTemplate>> it = remainingPredictedAnnotations.entrySet()
							.iterator(); it.hasNext();) {
						final Set<String> keyWords = it.next().getKey().stream().map(f -> f.term)
								.collect(Collectors.toSet());
						/*
						 * remove control if vehicle also exists
						 */
						if (keyWords.contains("control"))
							it.remove();
					}
				}
			}
		}

		/**
		 * HEUSRISTIC CyclosporinA used in every group if it exists.
		 */
		{
			Optional<AbstractAnnotation> cycloOpt = tr.stream().filter(a -> {
				if (a.asInstanceOfEntityTemplate().getEntityType() == SCIOEntityTypes.compoundTreatment) {
					return a.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller()
							.asInstanceOfEntityTemplate().getRootAnnotation().entityType == EntityType
									.get("CyclosporineA");
				} else {
					return a.asInstanceOfEntityTemplate().getRootAnnotation().getEntityType() == EntityType
							.get("CyclosporineA");
				}
			}).findFirst();

			if (cycloOpt.isPresent()) {
				System.out.println("Found Cyclosporine Treatment...");
				AbstractAnnotation cyclo = cycloOpt.get();
				for (EntityTemplate abstractAnnotation : remainingPredictedAnnotations.values()) {
					abstractAnnotation.addMultiSlotFiller(SCIOSlotTypes.hasTreatmentType, cyclo);
				}

				tr.remove(cyclo);
			}
		}

		/**
		 * HEURISTIC medium/media = vehicle treatment && USE vehicle only once! Only for
		 * single keyword clusters.
		 */
//		{
//			Optional<AbstractAnnotation> vehicleOpt = tr.stream().filter(a -> {
//				if (a.asInstanceOfEntityTemplate().getEntityType() == SCIOEntityTypes.compoundTreatment) {
//					return a.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller()
//							.asInstanceOfEntityTemplate().getRootAnnotation().entityType
//									.getTransitiveClosureSuperEntityTypes().contains(EntityType.get("Vehicle"));
//				}
//				return false;
//			}).findFirst();
//
//			if (vehicleOpt.isPresent()) {
//				System.out.println("Found Vehicle Treatment...");
//				AbstractAnnotation vehicle = vehicleOpt.get();
//
//				for (Iterator<Entry<Set<Finding>, EntityTemplate>> it = remainingPredictedAnnotations.entrySet()
//						.iterator(); it.hasNext();) {
//					Entry<Set<Finding>, EntityTemplate> e = it.next();
//					final Set<String> keyWords = e.getKey().stream().map(f -> f.term).collect(Collectors.toSet());
//					if (keyWords.size() == 1 && (keyWords.contains("media") || keyWords.contains("medium")
//							|| keyWords.contains("vehicle") || keyWords.contains("veh"))) {
//						e.getValue().addMultiSlotFiller(SCIOSlotTypes.hasTreatmentType, vehicle);
//
//						/*
//						 * remove vehicle from exp groups.
//						 */
//						it.remove();
//					}
//
//				}
//				/*
//				 * remove vehicle from treatments.
//				 */
//				tr.remove(vehicle);
//			}
//		}

		if (remainingPredictedAnnotations.size() == 1) {
			/**
			 * HEURISTIC if just one remains add all treatments
			 */

			for (EntityTemplate expGroup : remainingPredictedAnnotations.values()) {
				for (AbstractAnnotation treatment : tr) {
					expGroup.addMultiSlotFiller(SCIOSlotTypes.hasTreatmentType, treatment);
				}
			}
			return;

		} else if (tr.size() == 2 && remainingPredictedAnnotations.size() == 2) {

			/**
			 * HEURISTIC If treatments = 2 and exp group = 2 than each exp group receive
			 * exactly one treatment.
			 */
			int i = 0;
			for (AbstractAnnotation expGroup : remainingPredictedAnnotations.values()) {
				expGroup.asInstanceOfEntityTemplate().addMultiSlotFiller(SCIOSlotTypes.hasTreatmentType, tr.get(i++));
			}

			return;
		} else {

			for (Entry<Set<Finding>, EntityTemplate> pair : remainingPredictedAnnotations.entrySet()) {

				/**
				 * HEURISTIC: Select by heuristics
				 */
				Set<AbstractAnnotation> picked = selectByHeuristic(pair.getKey(), tr);

				for (AbstractAnnotation treatment : picked) {
					pair.getValue().addMultiSlotFiller(SCIOSlotTypes.hasTreatmentType, treatment);
				}

			}
			return;
		}
	}

	/**
	 * HEURISTIC pick from treatments based on several heuristics.
	 * 
	 * @param key
	 * @param tr
	 * @return
	 */
	private Set<AbstractAnnotation> selectByHeuristic(Set<Finding> key, List<AbstractAnnotation> tr) {

		Set<String> keyWords = key.stream().map(k -> k.term).collect(Collectors.toSet());

		if (keyWords.contains("control")) {
			keyWords.remove("control");
			keyWords.add("vehicle");
		}

		/**
		 * HEURISTIC Saline = Vehicle if vehicle does not exist.
		 */
		{
			boolean containsVehicle = false;

			for (AbstractAnnotation treatment : tr) {
				Set<String> terms = unify(getTreatmentTerms(treatment));
				containsVehicle |= terms.contains("vehicle");
			}

			/*
			 * If no vehicle treatment exists take saline...
			 */
			if (!containsVehicle && keyWords.contains("vehicle")) {
				keyWords.remove("vehicle");
				keyWords.add("saline");
			}
		}

		List<AbstractAnnotation> sortedTreatments = new ArrayList<>(tr);

		Collections.sort(sortedTreatments, new Comparator<AbstractAnnotation>() {

			@Override
			public int compare(AbstractAnnotation o1, AbstractAnnotation o2) {
				Set<String> treatment1 = unify(getTreatmentTerms(o1));
				Set<String> treatment2 = unify(getTreatmentTerms(o2));
				return -Double.compare(keyWords.stream().filter(k -> treatment1.contains(k)).count(),
						keyWords.stream().filter(k -> treatment2.contains(k)).count());
			}

		});
		System.out.println("Match: ");
		System.out.println(keyWords);
		sortedTreatments.subList(0, Math.min(key.size(), sortedTreatments.size()))
				.forEach(g -> System.out.println(g.toPrettyString()));
		System.out.println("------------------");
		return new HashSet<>(sortedTreatments.subList(0, Math.min(key.size(), sortedTreatments.size())));
	}

	/**
	 * Unifies a set of strings. this includes e.g. synonym replacements lowercasing
	 * etc.
	 * 
	 * @param strings
	 * @return unified set of string
	 */
	private Set<String> unify(Set<String> strings) {
		return strings.stream().map(s -> toLowerIfNotUpper(s)).map(s -> toSingular(s))
				.map(term -> syns.getOrDefault(term, term)).collect(Collectors.toSet());

	}

	/**
	 * HEURISTIC
	 * 
	 * @param finding
	 * @return
	 */
	private String toSingular(String finding) {
		if (finding.endsWith("s"))
			return finding.substring(0, finding.length() - 1);
		if (finding.endsWith("ies"))
			return finding.substring(0, finding.length() - 3) + "y";
		return finding;
	}

	/**
	 * Converts a string to lowercase if it is not in uppercase
	 * 
	 * @param s
	 * @return
	 */
	private String toLowerIfNotUpper(String s) {
		if (s.matches("[a-z]?[A-Z\\d\\W]+s?"))
			return s;

		return s.toLowerCase();
	}

	private Set<String> getTreatmentTerms(AbstractAnnotation treatment) {
		Set<String> treatmentTerms = new HashSet<>();

		if (treatment.asInstanceOfEntityTemplate().getEntityType() == SCIOEntityTypes.compoundTreatment) {

			AbstractAnnotation compoundRoot = treatment.asInstanceOfEntityTemplate()
					.getSingleFillerSlot(SCIOSlotTypes.hasCompound).getSlotFiller().asInstanceOfEntityTemplate()
					.getRootAnnotation();

			if (compoundRoot.isInstanceOfLiteralAnnotation()) {
				treatmentTerms.addAll(Arrays
						.asList(compoundRoot.asInstanceOfLiteralAnnotation().textualContent.surfaceForm.split(" ")));
			}
			treatmentTerms.add(compoundRoot.getEntityType().name);

		}
		if (treatmentTerms.isEmpty()) {
			AbstractAnnotation treatmentRoot = treatment.asInstanceOfEntityTemplate().getRootAnnotation();

			if (treatmentRoot.isInstanceOfLiteralAnnotation()) {
				treatmentTerms.addAll(Arrays
						.asList(treatmentRoot.asInstanceOfLiteralAnnotation().textualContent.surfaceForm.split(" ")));
			}

			treatmentTerms.add(treatment.getEntityType().name);
		}

		return treatmentTerms;
	}

	private AbstractAnnotation pickRandom(List<AbstractAnnotation> or, Random random) {
		Collections.shuffle(or, random);
		return or.get(0);
	}

	private static class Cluster implements Comparable<Cluster> {

		final Set<Finding> terms;
		int value;

		public Cluster(Set<Finding> finding, int value) {
			this.terms = finding;
			this.value = value;
		}

		public Cluster(Entry<Set<Finding>, Integer> e) {
			this(e.getKey(), e.getValue());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((terms == null) ? 0 : terms.hashCode());
			result = prime * result + value;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Cluster other = (Cluster) obj;
			if (terms == null) {
				if (other.terms != null)
					return false;
			} else if (!terms.equals(other.terms))
				return false;
			if (value != other.value)
				return false;
			return true;
		}

		@Override
		public int compareTo(Cluster o) {
			return -Integer.compare(this.value, o.value);
		}

		@Override
		public String toString() {
			return "Cluster [terms=" + terms + ", value=" + value + "]";
		}

	}

	public static final PatternIndexPair[] pattern = new PatternIndexPair[] { new PatternIndexPair(0,
			Pattern.compile("(\\W)[\\w-\\+ '[^\\x20-\\x7E]]{3,20} (treated|grafted|transplanted|(un)?trained)(?=\\W)",
					Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(1, Pattern.compile(
					" ([\\w']+?( with | and | plus | ?(\\+|-|/) ?))*[\\w']+?(-|[^\\x20-\\x7E])(animals|mice|rats|cats|dogs|transplantation)",
					Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(2, new HashSet<>(Arrays.asList(0, 1)),
					Pattern.compile("([^ ]+? (with|and|plus| ?(\\+|-|/) ?) [^ ]+?) ?\\((n)\\W?=\\W?\\d{1,2}\\)",
							Pattern.CASE_INSENSITIVE),
					3),
			new PatternIndexPair(3, new HashSet<>(Arrays.asList(0, 1)),
					Pattern.compile("received both ([^ ]+ (with|/|and|plus| ?(\\+|-) ?) [^ ]+)",
							Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(4, new HashSet<>(Arrays.asList(0, 3)),
					Pattern.compile("((only|or) )?([a-z][^ ]+?) ?\\((n)\\W?=\\W?\\d{1,2}\\)", Pattern.CASE_INSENSITIVE),
					5),
			new PatternIndexPair(5, new HashSet<>(Arrays.asList(2)),
					Pattern.compile(
							"(a|the|in) ([\\w-\\+ ']{3,20} (group|animals|mice|rats|cats|dogs|transplantation))",
							Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(6, new HashSet<>(Arrays.asList(3)),
					Pattern.compile(
							"(,( ?and ?)?|;)([\\w-\\+ ']{3,20} ?(group|animals|mice|rats|cats|dogs|transplantation))",
							Pattern.CASE_INSENSITIVE),
					2),
			new PatternIndexPair(7, new HashSet<>(Arrays.asList(2)), Pattern.compile(
					"(\\)|;|:) ?((\\(\\w\\) ?)?([\\w-\\+ ',\\.]|[^\\x20-\\x7E]){5,100})(\\( ?)?n\\W?=\\W?\\d{1,2}( ?\\))?(?=(,|\\.|;))",
					Pattern.CASE_INSENSITIVE), 5),
			new PatternIndexPair(8, new HashSet<>(Arrays.asList(0, 3)), Pattern.compile(
					"in(jured)? (animals|mice|rats|cats|dogs).{1,10}(receiv.{3,20}(,|;|\\.| injections?| treatments?))",
					Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(9, new HashSet<>(Arrays.asList(0, 2)), Pattern.compile(
					"(the|a|\\)|in) ([\\w-\\+ ']{3,20} (treated|grafted|transplanted|(un)?trained) ((control |sham )?((injury )?(only )?))? (group|animals|mice|rats|cats|dogs|transplantation))",
					Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(10, Pattern.compile(
					"([\\w']+?( and | plus | ?(\\+|-|/|[^\\x20-\\x7E]) ?))*[\\w']+?(-|[^\\x20-\\x7E]| ){1,2}(treated\\W|grafted\\W|transplanted\\W|(un)?trained\\W)((control |sham )?((injury )?(only )?))?(group|transplantation|animals|mice|rats|cats|dogs)",
					Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(11, Pattern.compile(
					"((control |sham )?((injury )?(only )?))?(group|animals|mice|rats|cats|dogs) that were (treated|grafted|transplanted|(un)?trained) with.+? ",
					Pattern.CASE_INSENSITIVE)),
			new PatternIndexPair(12,
					Pattern.compile("([\\w']+?( with | and | plus | ?(\\+|-|/) ?))*[\\w']+? ?treatment")),
			new PatternIndexPair(13, Pattern.compile(
					"((control|sham) ((injury )?(only )?))(treatment|grafting|transplantation|training|operation)")),

	};

	Pattern etAlPattern = Pattern.compile("et al");

	private Map<String, HashMap<Integer, Cluster>> collectClusters() throws Exception {

		Map<String, HashMap<Integer, Cluster>> clustersPerInstance = new HashMap<>();

		PatternIndexPair nullPattern = new PatternIndexPair(10000, Pattern.compile(""));
		for (Instance instance : instanceProvider.getTrainingInstances()) {
			System.out.println("########\t" + instance.getName() + "\t########");

//			if (!instance.getName().startsWith("N253"))
//				continue;

			List<Finding> findings = new ArrayList<>();

			Set<Integer> keyPoints = getKeyPoints(instance);

			Integer referencePoint = getReferencePoint(instance);
			System.out.println("ReferencePoint = " + referencePoint);

			/*
			 * Add Abstract approx. 10 sentences.
			 */
			keyPoints.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

			System.out.println("Keypoints: " + keyPoints);

			System.out.println("Number of Exp Groups :" + countExpGroups.get(instance.getName()));
			System.out.println("Number of Treatments :" + countTreatments.get(instance.getName()));
			System.out.println("Number of OrganismModels :" + countOrganismModels.get(instance.getName()));
			System.out.println("Number of Injuries :" + countInjuries.get(instance.getName()));
			int i = 0;

			/**
			 * TODO: Use Regexp or NPChunker
			 */
//			List<TermIndexPair> doubleGroupNames = new ArrayList<>();
//
//			for (int j = 0; j < groupNames.size() - 1; j++) {
//				for (int k = j + 1; k < groupNames.size(); k++) {
//					if (groupNames.get(j).term.toLowerCase().equals(groupNames.get(k).term.toLowerCase())) {
//						doubleGroupNames.add(groupNames.get(j));
//						break;
//					}
//				}
//			}
//
//			doubleGroupNames.forEach(System.out::println);
//
			/**
			 * HEURISTIC NPCHUNKS
			 */
			NPChunker chunker = new NPChunker();
			chunker.apply(instance.getDocument());
			List<TermIndexPair> groupNames = chunker.getNPs();
			for (TermIndexPair groupName : groupNames) {
				if (groupName.term.matches(".+(group|animals|rats|mice|rats|cats|dogs)"))
					addFinding(keyPoints, referencePoint, instance, findings, nullPattern, groupName.term,
							groupName.index);
			}

//			/**
//			 * HEURISTIC REG EX
//			 */
//			for (PatternIndexPair p : pattern) {
//				System.out.println("Pattern i = " + p.index + ", group = " + p.groups + ":\t" + p.pattern.pattern());
//				Matcher m = p.pattern.matcher(instance.getDocument().documentContent);
//
//				while (m.find()) {
//					for (Integer group : p.groups) {
//						System.out.print("Group: " + group + "\t");
//						addFinding(keyPoints, referencePoint, instance, findings, p, m.group(group), m.start(group));
//					}
//
//				}
//				i++;
//			}

			List<Set<Finding>> BOWFindings = toBOWFindings(findings);

			BOWFindings.forEach(System.out::println);

			Set<String> increaseImpact = new HashSet<>(Arrays.asList("sham", "control", "vehicle"));
			HashMap<Set<Finding>, Integer> countBOW = new HashMap<>();

			/**
			 * HEURISTIC: Increase sham / control groups.
			 */
			boolean containsSham = false;
			boolean containsVehicle = false;
			for (Set<Finding> bow : BOWFindings) {
				if (bow.size() == 1 && increaseImpact.contains(bow.iterator().next().term))
					containsSham |= bow.iterator().next().term.equals("sham");
				if (bow.size() == 1 && increaseImpact.contains(bow.iterator().next().term))
					containsVehicle |= bow.iterator().next().term.equals("control");
			}
			/**
			 * HEURISTIC Favor sham over control
			 */
			if (containsSham)
				increaseImpact.remove("control");

			/**
			 * HEURISTIC Favor control over vehicle
			 */
			if (containsVehicle)
				increaseImpact.remove("vehicle");

			for (Set<Finding> bow : BOWFindings) {
				int impact = bow.iterator().next().pattern.impact;

				Set<String> findingStrings = bow.stream().map(f -> f.term).collect(Collectors.toSet());

				if (bow.size() == 1 && increaseImpact.contains(bow.iterator().next().term)) {
					impact = 10;
				}

				if (findingStrings.contains("injury")
						&& (findingStrings.contains("only") || findingStrings.contains("alone"))) {
					PatternIndexPair p = bow.iterator().next().pattern;
					bow.clear();
					bow.add(new Finding("sham", p));
				}

				/**
				 * HEURISTIC unify findings
				 */
				Set<Finding> unifiedFindings = unifyFindings(bow);

				if (unifiedFindings.isEmpty())
					continue;

				countBOW.put(unifiedFindings, countBOW.getOrDefault(unifiedFindings, 0) + impact);
			}

			List<Cluster> sortCountBow = new ArrayList<>();

			for (Entry<Set<Finding>, Integer> e : countBOW.entrySet()) {
				sortCountBow.add(new Cluster(e));
			}

			Collections.sort(sortCountBow);

			System.out.println("----------------");
			for (Cluster e : sortCountBow) {
				System.out.println(e.terms + "\t" + e.value);
			}

			System.out.println("----------------");
			HashMap<Integer, Cluster> clusters = new HashMap<>();
			for (int c = 0; c < countExpGroups.get(instance.getName()) && c < sortCountBow.size(); c++)
				clusters.put(c, sortCountBow.get(c));

			/**
			 * HEURISTIC resolve co / combinatorial co or combinatorial = pairs of
			 * treatments. = combination of highest ranking (without control and sham) in
			 * the sorted cluster list.
			 */
			{
				final Set<String> combinatorialTerms = new HashSet<>(
						Arrays.asList("co", "combinatorial", "combination", "combine", "combined"));

				/*
				 * Find first combination.
				 */
				Optional<Entry<Integer, Cluster>> o = clusters.entrySet().stream()
						.filter(e -> e.getValue().terms.size() > 1).findFirst();

				Optional<Integer> combClusterImpact = sortCountBow.stream()
						.filter(e -> e.terms.size() == 1 && combinatorialTerms.contains(e.terms.iterator().next().term))
						.map(e -> e.value).reduce(Integer::sum);

				if (combClusterImpact.isPresent() && combClusterImpact.get() > 0) {

					if (o.isPresent()) {
						int combinatorialIndex = o.get().getKey();

						sortCountBow.get(combinatorialIndex).value += combClusterImpact.get();

						for (Iterator<Cluster> iterator = sortCountBow.iterator(); iterator.hasNext();) {
							Cluster cluster = iterator.next();
							if (cluster.terms.size() == 1
									&& combinatorialTerms.contains(cluster.terms.iterator().next().term)) {
								System.out.println("Remove: " + cluster);
								iterator.remove();
							}
						}
					} else {

						for (Iterator<Cluster> iterator = sortCountBow.iterator(); iterator.hasNext();) {
							Cluster cluster = iterator.next();
							if (cluster.terms.size() == 1
									&& combinatorialTerms.contains(cluster.terms.iterator().next().term)) {
								System.out.println("Remove: " + cluster);
								iterator.remove();
							}
						}

						Collections.sort(sortCountBow);
						for (int c = 0; c < countExpGroups.get(instance.getName()) && c < sortCountBow.size(); c++)
							clusters.put(c, sortCountBow.get(c));
						/**
						 * HEURISTIC Create new combination from two best non control cluster terms.
						 */
						Set<Finding> combineTerms = new HashSet<>();
						for (Entry<Integer, Cluster> cluster : clusters.entrySet()) {
							if (isControl(cluster.getValue()))
								continue;
							else
								// contains only 1 term
								combineTerms.addAll(cluster.getValue().terms);
							if (combineTerms.size() == 2) {
								sortCountBow.add(new Cluster(combineTerms, combClusterImpact.get()));
								break;
							}
						}
					}

					Collections.sort(sortCountBow);
					System.out.println("-------Resolve Combinatorial---------");
					for (Cluster e : sortCountBow) {
						System.out.println(e.terms + "\t" + e.value);
					}

					for (int c = 0; c < countExpGroups.get(instance.getName()) && c < sortCountBow.size(); c++)
						clusters.put(c, sortCountBow.get(c));
				}

			}

//			Set<Finding>, Integer> combinatorial =

			clusters.entrySet().forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
			System.out.println("########\t" + instance.getName() + "\t########");
			clustersPerInstance.put(instance.getName(), clusters);
		}
//		countWords.entrySet().forEach(e -> System.)out.println(e.getKey() + "\t" + e.getValue()));
		return clustersPerInstance;
	}

	public void addFinding(Set<Integer> keyPoints, Integer referencePoint, Instance instance, List<Finding> findings,
			PatternIndexPair p, String group, int start) {
		Finding finding = new Finding(group, p);
		try {
			int senIndex = instance.getDocument().getTokenByCharStartOffset(start).getSentenceIndex();

			/**
			 * HEURISTIC reference
			 */
//			if (senIndex > referencePoint)
//				return;

			/**
			 * HEURISTIC keypoints
			 */
//			boolean cont = false;
//			for (Integer keyPoint : keyPoints) {
//				if (senIndex >= keyPoint
////						) {
//						&& senIndex < keyPoint + 50) {
//					cont = true;
//					break;
//				}
//
//			}
//
//			if (!cont) {
//				System.out.println("OOB Discard: " + senIndex + "\t" + finding.term);
//				return;
//			}

			/**
			 * HEURISTIC et al discard
			 */
			String sentence = listToString(instance.getDocument().getSentenceByIndex(senIndex));
			if (etAlPattern.matcher(sentence).find()) {
//				System.out.println("ETAL Discard: " + senIndex + "\t" + finding.term);
				return;
			}

			/**
			 * HEURISTIC 0
			 */
			if (finding.term.matches("(the|with) (treated|grafted|transplanted|(un)?trained)")) {
				return;
			}

			/**
			 * HEURISTIC 1
			 */
			List<Finding> splitFindings = new ArrayList<>();
//			if (false) {
//				for (String split : finding.term.trim().split("\\Wor\\W")) {
//					splitFindings.add(new Finding(split, finding.pattern));
////				System.out.println(senIndex + "\t" + split);
//				}
//			} else {
			splitFindings.add(finding);
//			}

			for (Finding splitF : splitFindings) {
				/**
				 * HEURISTIC 2
				 */
				if (p != null && p.index == 3)
					/*
					 * do not split and for explicit "and"-groups
					 */
					findings.add(splitF);
				else
					for (String split : splitF.term.trim().split("\\Wand\\W")) {
						findings.add(new Finding(split, finding.pattern));
//						System.out.println(senIndex + "\t" + split);
					}

			}

		} catch (DocumentLinkedAnnotationMismatchException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private boolean isControl(Cluster cluster) {
		return cluster.terms.stream().map(f -> f.term).filter(a -> a.equals("control") || a.equals("sham")).findAny()
				.isPresent();
	}

	private Set<Finding> unifyFindings(Set<Finding> findings) {

		Set<Finding> unified = new HashSet<>();

		for (Finding finding : findings) {

			String findingString = finding.term;

			if (findingString.equals("only"))
				continue;
			if (findingString.equals("alone"))
				continue;
			/*
			 * Unify
			 */
			findingString = toLowerIfNotUpper(findingString);
			findingString = toSingular(findingString);
			findingString = syns.getOrDefault(findingString, findingString);

			/*
			 * Keep pattern
			 */
			unified.add(new Finding(findingString, finding.pattern));
		}
		return unified;
	}

	final private static String referencesKeypointTerm = "references";
	final private static String resultsKeyPointTerm = "results";

	final public static Set<String> STOP_TERM_LIST = new HashSet<>(
			Arrays.asList("spinal", "injured", "lesion", "experimental", "group", "animals", "rats", "cats", "SCI"));

	private Integer getReferencePoint(Instance instance) {
		/*
		 * Search for mentioned references point
		 */
		int refPoint = 0;
		int iRef = 0;
		int iRes = 0;
		for (DocumentToken docToken : instance.getDocument().tokenList) {
			if (docToken.getText().matches("R(E|e)(F|f)(E|e)(R|r)(E|e)(N|n)(C|c)(E|e)(S|s)")) {

				refPoint = update(refPoint, docToken.getSentenceIndex());
			}
			if (docToken.getText().matches("R(E|e)(S|s)(U|u)(L|l)(T|t)(S|s)")) {

				refPoint = update(refPoint, docToken.getSentenceIndex());
			}

			if (docToken.getText().toLowerCase().equals(String.valueOf(referencesKeypointTerm.charAt(iRef))))
				iRef++;
			else
				iRef = 0;

			if (iRef == referencesKeypointTerm.length()) {
				iRef = 0;
				refPoint = update(refPoint, docToken.getSentenceIndex());
			}

			if (docToken.getText().toLowerCase().equals(String.valueOf(resultsKeyPointTerm.charAt(iRes))))
				iRes++;
			else
				iRes = 0;

			if (iRes == resultsKeyPointTerm.length()) {
				iRes = 0;
				refPoint = update(refPoint, docToken.getSentenceIndex());
			}

		}
		if (refPoint != 0)
			return refPoint;

		return Integer.MAX_VALUE;
	}

	private int update(int refPoint, int senIndex) {
		return Math.max(refPoint, senIndex);
	}

	private List<Set<Finding>> toBOWFindings(List<Finding> findings) {

		List<Set<Finding>> BOWFindings = new ArrayList<>();

		for (Finding finding : findings) {

			Set<Finding> tokens = new HashSet<>();
			for (String token : Arrays.asList(finding.term.split("\\W"))) {

				/**
				 * HEURISTIC 3
				 */
				if (token.matches("\\d+"))
					continue;

				/**
				 * HEURISTIC 4
				 */
				tokens.add(new Finding(toLowerIfNotUpper(token), finding.pattern));
			}

			/**
			 * HEURISTIC 5
			 */
			for (Iterator<Finding> fi = tokens.iterator(); fi.hasNext();) {
				Finding f = fi.next();
				if (ALL_STOPWORDS.contains(f.term)) {
					fi.remove();
				}

			}

			if (tokens.isEmpty())
				continue;

			BOWFindings.add(tokens);

		}

		return BOWFindings;

	}

	public Set<Integer> getKeyPoints(Instance instance) {
		Set<Integer> keyPoints = new HashSet<>();

		Set<Integer> exp = instance.getGoldAnnotations().getAnnotations().stream().filter(a -> a != null)
				.map(a -> a.asInstanceOfEntityTemplate().getRootAnnotation())
				.filter(a -> a != null && a.isInstanceOfDocumentLinkedAnnotation())
				.map(a -> a.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex() + 1)
				.collect(Collectors.toSet());

//			exp.forEach(a -> System.out.println(a + "\tExp Root"));
		Set<Integer> model = instance.getGoldAnnotations().getAnnotations().stream().filter(a -> a != null)
				.map(a -> a.asInstanceOfEntityTemplate().getSingleFillerSlotOfName("hasOrganismModel"))
				.filter(a -> a != null && a.containsSlotFiller())

				.flatMap(a -> Stream.concat(
						a.getSlotFiller().asInstanceOfEntityTemplate().streamSingleFillerSlotValues(),
						a.getSlotFiller().asInstanceOfEntityTemplate().flatStreamMultiFillerSlotValues()))

				.filter(a -> a.isInstanceOfDocumentLinkedAnnotation())
				.map(a -> a.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex() + 1)
				.collect(Collectors.toSet());

//		model.forEach(a -> System.out.println(a + "\tOrganismModel"));

		if (!model.isEmpty())
			keyPoints.add(model.stream().min(Integer::compare).get());
		/*
		 * Check if each exp annotation is at least after one slot filler of the
		 * organism model.
		 */
		boolean after = true;
		for (Integer e : exp) {
			boolean b = false;
			for (Integer m : model) {
				if (e >= m) {
					b |= true;
				}
			}
			after &= b;
		}
//			System.out.println(after);
		int i = 1;

		sentences: for (List<DocumentToken> sentence : instance.getDocument().getSentences()) {

			final String text = sentence.get(0).getText();

			Pattern materials = Pattern.compile("Materials?|Methods?");

			Matcher mm = materials.matcher(text);
			if (mm.find()) {
				keyPoints.add(i);
//				System.out.println(i + "\t" + sentence.get(0).getDocCharOffset() + "\t" + mm.group());
			}

			Pattern experimentals = Pattern.compile("Experimentals?");
			Matcher me = experimentals.matcher(text);
			if (me.find()) {
				if (sentence.size() > 1) {
					final String nextWord = sentence.get(1).getText();

					if (!(nextWord.equals("procedures") || nextWord.equals("procedure"))
							&& nextWord.matches("[a-z].*")) {
						break sentences;
					}

				}
				keyPoints.add(i);
//				System.out.println(i + "\t" + sentence.get(0).getDocCharOffset() + "\t" + me.group());
			}

			i++;
		}
		return keyPoints;
	}

	private String listToString(List<DocumentToken> sentenceByIndex) {
		StringBuffer sb = new StringBuffer();

		for (DocumentToken documentToken : sentenceByIndex) {
			sb.append(documentToken.getText()).append(" ");
		}

		return sb.toString().trim();
	}

	public static boolean contains(String expSurfaceForm, String treatSurfaceForm) {
		for (String element : treatSurfaceForm.split("\\W")) {
			if (expSurfaceForm.contains(element))
				return true;
		}
		return false;
	}
}
