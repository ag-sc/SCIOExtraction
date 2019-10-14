package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.investigation;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.converters.AbstractArrayConverter;
import org.apache.commons.collections.set.SynchronizedSet;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.of.IObjectiveFunction;
import de.hterhors.semanticmr.crf.of.SlotFillingObjectiveFunction;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.exce.DocumentLinkedAnnotationMismatchException;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;

public class CollectExpGroupNames {
	static class PatternIndexPair {
		public final int impact;
		public final Pattern pattern;
		public final int index;
		private static Set<Integer> indicies = new HashSet<>();

		public PatternIndexPair(int index, Pattern pattern) {
			this.pattern = pattern;
			this.index = index;
			this.impact = 1;
			if (!indicies.add(index)) {
				throw new IllegalArgumentException("Duplicate index: " + index);
			}

		}

		public PatternIndexPair(int index, Pattern pattern, int impact) {
			this.pattern = pattern;
			this.impact = impact;
			this.index = index;

			if (!indicies.add(index)) {
				throw new IllegalArgumentException("Duplicate index: " + index);
			}

		}

	}

	static class Finding {
		final public String finding;
		final public PatternIndexPair pattern;

		public Finding(String finding, PatternIndexPair pattern) {
			this.finding = finding;
			this.pattern = pattern;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((finding == null) ? 0 : finding.hashCode());
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
			if (finding == null) {
				if (other.finding != null)
					return false;
			} else if (!finding.equals(other.finding))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return finding + "\t" + pattern.index;
		}

	}

	public static void main(String[] args) throws Exception {
		CollectExpGroupNames pg = new CollectExpGroupNames(new File("src/main/resources/slotfilling/corpus_docs.csv"));
		Map<String, HashMap<Integer, Entry<Set<Finding>, Integer>>> clusters = pg.collectClusters();
		Map<String, Set<AbstractAnnotation>> treatments = pg.getTreatments();
		Map<String, Set<AbstractAnnotation>> orgModels = pg.getOrganismModels();
		Map<String, Set<AbstractAnnotation>> injuries = pg.getInjuryModels();
		pg.merge(clusters, treatments, orgModels, injuries);
	}

	private Map<String, Set<AbstractAnnotation>> getTreatments() {
		Map<String, Set<AbstractAnnotation>> treatmentsPerDocument = new HashMap<>();
		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {

			Set<AbstractAnnotation> treatments = new HashSet<>();
			for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
				treatments.addAll(expGroup.getMultiFillerSlot(SlotType.get("hasTreatmentType")).getSlotFiller());
			}

			treatmentsPerDocument.put(instance.getName(), treatments);

		}

		return treatmentsPerDocument;
	}

	private Map<String, Set<AbstractAnnotation>> getOrganismModels() {
		Map<String, Set<AbstractAnnotation>> orgModelsPerDocument = new HashMap<>();
		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {

			Set<AbstractAnnotation> orgModels = new HashSet<>();
			for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
				orgModels.add(expGroup.getSingleFillerSlot(SlotType.get("hasOrganismModel")).getSlotFiller());
			}

			orgModelsPerDocument.put(instance.getName(), orgModels);

		}

		return orgModelsPerDocument;
	}

	private Map<String, Set<AbstractAnnotation>> getInjuryModels() {
		Map<String, Set<AbstractAnnotation>> injuriesPerDocument = new HashMap<>();
		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {

			Set<AbstractAnnotation> injuries = new HashSet<>();
			for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
				injuries.add(expGroup.getSingleFillerSlot(SlotType.get("hasInjuryModel")).getSlotFiller());
			}

			injuriesPerDocument.put(instance.getName(), injuries);

		}

		return injuriesPerDocument;
	}

	private static final Set<String> SPECIAL_KEEPWORDS = new HashSet<>(Arrays.asList("control", "sham", "low", "high"));

	private static final Set<String> ADDITIONAL_STOPWORDS = new HashSet<>(Arrays.asList("transection", "grafts",
			"normal", "injection", "injections", "cultured", "uninfected", "injected", "additional", "ca", "observed",
			"grafted", "cells", "are", "effects", "gray", "cord", "spinal", "identifi", "cation", "n", "treated",
			"treatment", "", "received", "the", "injured", "all", "lesioned", "fi", "rst", "first", "second", "third",
			"fourth", "group", "animals", "rats", "in", "same", "individual", "groups", "were"));

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
		SystemScope scope = SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(ExperimentalGroupSpecifications.systemsScope).apply()
				.registerNormalizationFunction(new WeightNormalization())
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

		instanceProvider.getRedistributedTrainingInstances().stream().forEach(i -> {
			countExpGroups.put(i.getName(), Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
					.filter(b -> b != null && (b.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SlotType.get("hasInjuryModel")).containsSlotFiller()
							|| b.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasOrganismModel"))
									.containsSlotFiller()
							|| b.asInstanceOfEntityTemplate().getMultiFillerSlot(SlotType.get("hasTreatmentType"))
									.containsSlotFiller()))
					.count()));

			countInjuries.put(i.getName(), Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
					.filter(zzz -> zzz != null).map(b -> b.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SlotType.get("hasInjuryModel")).getSlotFiller())
					.filter(zzz -> zzz != null).distinct().count()));

			countOrganismModels.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream().filter(zzz -> zzz != null)
							.map(b -> b.asInstanceOfEntityTemplate()
									.getSingleFillerSlot(SlotType.get("hasOrganismModel")).getSlotFiller())
							.filter(zzz -> zzz != null).distinct().count()));

			countTreatments.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream().filter(zzz -> zzz != null)
							.flatMap(b -> b.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SlotType.get("hasTreatmentType")).getSlotFiller().stream())
							.filter(zzz -> zzz != null).distinct().count()));

		});

	}

	private void merge(Map<String, HashMap<Integer, Entry<Set<Finding>, Integer>>> clusters,
			Map<String, Set<AbstractAnnotation>> treatments, Map<String, Set<AbstractAnnotation>> organismModels,
			Map<String, Set<AbstractAnnotation>> injuryModels) {

		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {

			if (!instance.getName().startsWith("N075"))
				continue;

			System.out.println("########\t" + instance.getName() + "\t########");
			System.out.println("Number of Exp Groups :" + countExpGroups.get(instance.getName()));
			System.out.println("Number of Treatments :" + countTreatments.get(instance.getName()));
			System.out.println("Number of OrganismModels :" + countOrganismModels.get(instance.getName()));
			System.out.println("Number of Injuries :" + countInjuries.get(instance.getName()));

			System.out.println(countTreatments.get(instance.getName()));
			System.out.println(countOrganismModels.get(instance.getName()));
			System.out.println(countInjuries.get(instance.getName()));

			final CartesianEvaluator evaluator = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE);

			List<EntityTemplate> goldAnnotations = instance.getGoldAnnotations().<EntityTemplate>getAnnotations()
					.stream()
					.filter(b -> b != null && (b.asInstanceOfEntityTemplate()
							.getSingleFillerSlot(SlotType.get("hasInjuryModel")).containsSlotFiller()
							|| b.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasOrganismModel"))
									.containsSlotFiller()
							|| b.asInstanceOfEntityTemplate().getMultiFillerSlot(SlotType.get("hasTreatmentType"))
									.containsSlotFiller()))
					.collect(Collectors.toList());

			List<EntityTemplate> predictedAnnotations = goldAnnotations
					.stream().map(a -> {
						EntityTemplate clone = a.deepCopy();
						clone.getSingleFillerSlot("hasOrganismModel").removeFiller();
						clone.getSingleFillerSlot("hasInjuryModel").removeFiller();
						clone.getMultiFillerSlot("hasTreatmentType").removeAll();
						return clone;
					}).collect(Collectors.toList());

			fillRandom(predictedAnnotations, clusters.get(instance.getName()), treatments.get(instance.getName()),
					organismModels.get(instance.getName()), injuryModels.get(instance.getName()));

			System.out.println(evaluator.scoreMultiValues(goldAnnotations, predictedAnnotations));
			System.exit(1);

			for (EntityTemplate expGroup : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {

				if (!(expGroup.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasInjuryModel"))
						.containsSlotFiller()
						|| expGroup.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasOrganismModel"))
								.containsSlotFiller()
						|| expGroup.asInstanceOfEntityTemplate().getMultiFillerSlot(SlotType.get("hasTreatmentType"))
								.containsSlotFiller()))
					continue;

				System.out.println("+++++++++++++++++++++++++++++++++++++++++");

				String expSurfaceForm = "";
				if (expGroup.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
					DocumentLinkedAnnotation expRoot = expGroup.getRootAnnotation()
							.asInstanceOfDocumentLinkedAnnotation();
					expSurfaceForm = expRoot.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm();
					System.out.println("ExpRoot: "
							+ expRoot.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex()
							+ "\t" + expRoot.getEntityType().entityName + "\t" + expSurfaceForm);
					System.out.println();
					System.out.println(expRoot.getSentenceOfAnnotation());
					System.out.println();
				}
				Set<AbstractAnnotation> groupNames = expGroup.getMultiFillerSlot("hasGroupName").getSlotFiller();
				System.out.println("GroupNames:");
				groupNames.stream().map(g -> "GroupName: " + g.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm())
						.forEach(System.out::println);

				System.out.println("------------treatments-------------");
				for (AbstractAnnotation treatment : treatments.get(instance.getName())) {
					AbstractAnnotation treatRoot;
					if (treatment.getEntityType() == EntityType.get("CompoundTreatment")) {
						treatRoot = treatment.asInstanceOfEntityTemplate().getSingleFillerSlot("hasCompound")
								.getSlotFiller().asInstanceOfEntityTemplate().getRootAnnotation();
					} else {
						treatRoot = treatment.asInstanceOfEntityTemplate().getRootAnnotation();
					}

					if (treatRoot.isInstanceOfDocumentLinkedAnnotation()) {
						System.out.println(
								treatRoot.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getSentenceIndex()
										+ "\t" + treatRoot.getEntityType().entityName + "\t"
										+ treatRoot.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm() + "\t"
										+ contains(expSurfaceForm,
												treatRoot.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()));
					}

				}

				System.out.println();
			}

		}
	}

	private void fillRandom(List<EntityTemplate> predictedAnnotations,
			HashMap<Integer, Entry<Set<Finding>, Integer>> clusters, Set<AbstractAnnotation> treatments,
			Set<AbstractAnnotation> orgModels, Set<AbstractAnnotation> injuries) {

		List<AbstractAnnotation> tr = new ArrayList<>(treatments);
		List<AbstractAnnotation> or = new ArrayList<>(orgModels);
		List<AbstractAnnotation> in = new ArrayList<>(injuries);
		for (int i = 0; i < predictedAnnotations.size(); i++) {

			Collections.shuffle(tr);
			Collections.shuffle(or);
			Collections.shuffle(in);

			EntityTemplate t = predictedAnnotations.get(i);

			t.setSingleSlotFiller(SlotType.get("hasInjuryModel"), in.get(0));
			t.setSingleSlotFiller(SlotType.get("hasOrganismModel"), or.get(0));

			for (int j = 0; j < clusters.get(i).getKey().size(); j++) {
				t.addMultiSlotFiller(SlotType.get("hasTreatmentType"), tr.get(j));
			}

		}

	}

	private Map<String, HashMap<Integer, Entry<Set<Finding>, Integer>>> collectClusters() throws Exception {

		String number = "(one|two|three|four|five|six|seven|eight|nine|ten|\\d{1,2})";
		PatternIndexPair[] pattern = new PatternIndexPair[] {
				new PatternIndexPair(0, Pattern.compile(
						"(\\W)[\\w-\\+ '[^\\x20-\\x7E]]{3,20} (treated|grafted|transplanted|(un)?trained)(?=\\W)",
						Pattern.CASE_INSENSITIVE)),
				new PatternIndexPair(1, Pattern.compile(
						" ([\\w']+?( with | and | plus | ?(\\+|-|/) ?))*[\\w']+?(-|[^\\x20-\\x7E])(animals|mice|rats|cats|dogs)",
						Pattern.CASE_INSENSITIVE)),
				new PatternIndexPair(2,
						Pattern.compile("[^ ]+? (with|and|plus| ?(\\+|-|/) ?) [^ ]+? ?\\((n)\\W?=\\W?\\d{1,2}\\)",
								Pattern.CASE_INSENSITIVE),
						3),
				new PatternIndexPair(3,
						Pattern.compile("received both [^ ]+ (with|/|and|plus| ?(\\+|-) ?) [^ ]+",
								Pattern.CASE_INSENSITIVE)),
				new PatternIndexPair(4,
						Pattern.compile("((only|or) )?[a-z][^ ]+? ?\\((n)\\W?=\\W?\\d{1,2}\\)",
								Pattern.CASE_INSENSITIVE),
						5),
				new PatternIndexPair(5, Pattern.compile(
						"(a|the|in) [\\w-\\+ ']{3,20} (group|animals|mice|rats|cats|dogs)", Pattern.CASE_INSENSITIVE)),
//				Pattern.compile("\\([A-Z][\\w-\\+']{2,30}\\)"),
				new PatternIndexPair(6,
						Pattern.compile("(,|;)[\\w-\\+ ']{3,20} (group|animals|mice|rats|cats|dogs)",
								Pattern.CASE_INSENSITIVE)),
				new PatternIndexPair(7, Pattern.compile(
						"(\\)|;|:) ?(\\(\\w\\) ?)?([\\w-\\+ ',\\.]|[^\\x20-\\x7E]){5,100}(\\( ?)?n\\W?=\\W?\\d{1,2}( ?\\))?(?=(,|\\.|;))",
//						"(\\)|;|:) ?(\\(\\w\\) ?)?([\\w-\\+ ',\\.]|[^\\x20-\\x7E]){5,100}(\\( ?)?((n\\W?=\\W?\\d{1,2})|("+number +" ?(animals|mice|rats|cats|dogs)))( ?\\))?(?=(,|\\.|;))",
						Pattern.CASE_INSENSITIVE), 5),
				new PatternIndexPair(8, Pattern.compile(
						"in(jured)? (animals|mice|rats|cats|dogs).{1,10}receiv.{3,20}(,|;|\\.| injections?| treatments?)",
						Pattern.CASE_INSENSITIVE)),
				new PatternIndexPair(9, Pattern.compile(
						"(the|a|\\)|in) [\\w-\\+ ']{3,20} (treated|grafted|transplanted|(un)?trained) ((control |sham )?((injury )?(only )?))? (group|animals|mice|rats|cats|dogs)",
						Pattern.CASE_INSENSITIVE)),
				new PatternIndexPair(10, Pattern.compile(
						"([\\w']+?( and | plus | ?(\\+|-|/|[^\\x20-\\x7E]) ?))*[\\w']+?(-|[^\\x20-\\x7E]| ){1,2}(treated\\W|grafted\\W|transplanted\\W|(un)?trained\\W)((control |sham )?((injury )?(only )?))?(group|animals|mice|rats|cats|dogs)",
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
		Map<String, Integer> countWords = new HashMap<>();
		Map<String, HashMap<Integer, Entry<Set<Finding>, Integer>>> clustersPerInstance = new HashMap<>();

		for (Instance instance : instanceProvider.getRedistributedTrainingInstances()) {
			System.out.println("########\t" + instance.getName() + "\t########");

//			if (!instance.getName().startsWith("N075"))
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
			for (PatternIndexPair p : pattern) {
				System.out.println("Pattern " + p.index + ":\t" + p.pattern.pattern());
				Matcher m = p.pattern.matcher(instance.getDocument().documentContent);

				while (m.find()) {
					Finding finding = new Finding(m.group(), p);
					try {
						int senIndex = instance.getDocument().getTokenByCharOffset(m.start()).getSentenceIndex();

						boolean cont = false;
						for (Integer keyPoint : keyPoints) {
							if (senIndex >= keyPoint && senIndex < referencePoint) {
//								&& senIndex < keyPoint + 50) {
								cont = true;
								break;
							}

						}

						if (!cont) {
							System.out.println("OOB Discard: " + senIndex + "\t" + finding.finding);
							continue;
						}

						String sentence = listToString(instance.getDocument().getSentenceByIndex(senIndex));
						if (etAlPattern.matcher(sentence).find()) {
							System.out.println("ETAL Discard: " + senIndex + "\t" + finding.finding);
							continue;
						}

						for (String splits : finding.finding.split("\\W")) {
							countWords.put(splits, countWords.getOrDefault(splits, 0) + 1);
						}

						/**
						 * HEURISTIC 0
						 */
						if (finding.finding.matches("(the|with) (treated|grafted|transplanted|(un)?trained)")) {
							continue;
						}

						/**
						 * HEURISTIC 1
						 */
						List<Finding> splitFindings = new ArrayList<>();
						for (String split : finding.finding.trim().split("\\Wor\\W")) {
							splitFindings.add(new Finding(split, finding.pattern));
							System.out.println(senIndex + "\t" + split);
						}

						for (Finding splitF : splitFindings) {
							/**
							 * HEURISTIC 2
							 */
							if (p.index == 3)
								/*
								 * do not split and for explicit "and"-groups
								 */
								findings.add(splitF);
							else
								for (String split : splitF.finding.trim().split("\\Wand\\W")) {
									findings.add(new Finding(split, finding.pattern));
									System.out.println(senIndex + "\t" + split);
								}

						}

					} catch (DocumentLinkedAnnotationMismatchException ex) {

					}

				}
				i++;
			}

			List<Set<Finding>> BOWFindings = toBOWFindings(findings);
			for (Set<Finding> set : BOWFindings) {
				System.out.println(set);
			}

			HashMap<Set<Finding>, Integer> countBOW = new HashMap<>();
			for (Set<Finding> bow : BOWFindings) {
				countBOW.put(bow, countBOW.getOrDefault(bow, 0) + bow.iterator().next().pattern.impact);
			}
			List<Entry<Set<Finding>, Integer>> sortCountBow = new ArrayList<>(countBOW.entrySet());
			Collections.sort(sortCountBow, new Comparator<Entry<Set<Finding>, Integer>>() {
				@Override
				public int compare(Entry<Set<Finding>, Integer> o1, Entry<Set<Finding>, Integer> o2) {
					return -Integer.compare(o1.getValue(), o2.getValue());
				}
			});

			System.out.println("----------------");
			for (Entry<Set<Finding>, Integer> e : sortCountBow) {
				System.out.println(e.getKey() + "\t" + e.getValue());
			}

			/**
			 * TODO: Implement cluster rules
			 *
			 * CLUSTER RULES:
			 * 
			 * sham / control -> must include
			 * 
			 * schwann -> = SC
			 * 
			 * remove plural s
			 * 
			 * ensheating -> OEC
			 * 
			 * more impact to pairs
			 * 
			 * single DMEM / saline = sham or control ?
			 * 
			 * single terms remove: encapsul*, transplantat*
			 * 
			 * co or combinatorial = pairs of treatments.
			 * 
			 */

			System.out.println("----------------");
			HashMap<Integer, Entry<Set<Finding>, Integer>> clusters = new HashMap<>();

			for (int c = 0; c < countExpGroups.get(instance.getName()) && c < sortCountBow.size(); c++)
				clusters.put(c, sortCountBow.get(c));

			clusters.entrySet().forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
			System.out.println("########\t" + instance.getName() + "\t########");
			clustersPerInstance.put(instance.getName(), clusters);
		}
//		countWords.entrySet().forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
		return clustersPerInstance;
	}

	final private static String references = "references";

	private Integer getReferencePoint(Instance instance) {
		/*
		 * Search for mentioned references point
		 */
		int refPoint = 0;
		int i = 0;
		for (DocumentToken docToken : instance.getDocument().tokenList) {
			if (docToken.getText().matches("R(E|e)(F|f)(E|e)(R|r)(E|e)(N|n)(C|c)(E|e)(S|s)")) {

				refPoint = update(refPoint, docToken.getSentenceIndex());
			}

			if (docToken.getText().toLowerCase().equals(String.valueOf(references.charAt(i))))
				i++;
			else
				i = 0;

			if (i == references.length()) {
				i = 0;
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
			for (String token : Arrays.asList(finding.finding.split("\\W"))) {

				/**
				 * HEURISTIC 3
				 */
				if (token.matches("\\d+"))
					continue;

				/**
				 * HEURISTIC 4
				 */
				if (token.matches("[A-Z\\d]+s?")) {
					tokens.add(new Finding(token, finding.pattern));
				} else {
					tokens.add(new Finding(token.toLowerCase(), finding.pattern));
				}
			}

			/**
			 * HEURISTIC 5
			 */
			for (Iterator<Finding> fi = tokens.iterator(); fi.hasNext();) {
				Finding f = fi.next();
				if (ALL_STOPWORDS.contains(f.finding)) {
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
				.map(a -> a.asInstanceOfEntityTemplate().getSingleFillerSlot("hasOrganismModel"))
				.filter(a -> a != null && a.containsSlotFiller())
				.flatMap(a -> a.getSlotFiller().asInstanceOfEntityTemplate().getAllSlotFillerValues().stream())
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

	private void checkForCommonPhrases(Integer numberOfExpGroups, Instance instance) {

		System.out.println("Searched number: " + numberOfExpGroups);

		Pattern p1 = Pattern.compile("\\(n\\W?=\\W?[0-9]{1,2}(,|;)\\W?[A-Z]{2,5}(\\)|,|;)|n\\W?=\\W?[0-9]{1,2}");
		Matcher m1 = p1.matcher(instance.getDocument().documentContent);
		int n = 0;
		while (m1.find()) {
			n++;
			System.out.println(m1.group());
		}
		System.out.println("Count n = x : " + n);

		Pattern pg1 = Pattern.compile("\\W.{3,30}\\(n\\W?=\\W?[0-9]{1,2}\\)");
		Matcher mg1 = pg1.matcher(instance.getDocument().documentContent);

		Set<String> groups1 = new HashSet<>();
		while (mg1.find()) {
			n++;
			System.out.println(mg1.group());
			groups1.add(mg1.group());
		}
		System.out.println("Count groups1 : " + groups1.size());

		Pattern p2 = Pattern.compile("(the|a|,|;|and)\\W.{3,30}\\W((((un)?treated|injured)(mice|rats|animals))|group)");
		Matcher m2 = p2.matcher(instance.getDocument().documentContent);
		Map<Set<String>, Integer> countBOW = new HashMap<>();
		Set<String> groups = new HashSet<>();
		while (m2.find()) {
//			if (m2.group().length() > 30)
//				continue;
			System.out.println(m2.group());

			groups.add(m2.group());

			// if (m2.group().contains("the") && m2.group().contains("and"))
//				continue;

			Set<String> groupBOW = new HashSet<>(Arrays.asList(m2.group().split("\\s")));

//			groupBOW.removeAll(STOPWORDS);

//			if (groupBOW.contains("or"))
//				continue;

			countBOW.put(groupBOW, countBOW.getOrDefault(groupBOW, 0) + 1);

		}
		System.out.println("Count Groups : " + groups.size());

		List<Entry<Set<String>, Integer>> sortBOW = new ArrayList<>(countBOW.entrySet());

		Collections.sort(sortBOW, new Comparator<Entry<Set<String>, Integer>>() {

			@Override
			public int compare(Entry<Set<String>, Integer> o1, Entry<Set<String>, Integer> o2) {
				return -Integer.compare(o1.getValue(), o2.getValue());
			}

		});
		System.out.println("----------------");
		sortBOW.forEach(System.out::println);

		System.out.println("----------------");

		Pattern p3 = Pattern.compile("\\(([1-9]|[ivIV]{1,2}|[a-zA-Z])\\)");
		Matcher m3 = p3.matcher(instance.getDocument().documentContent);
		Set<String> listings = new HashSet<>();
		while (m3.find()) {
			System.out.println(m3.group());
			listings.add(m3.group());

		}
		System.out.println("Count listings : " + listings.size());

		Pattern p4 = Pattern.compile("((fi)?rst|second|third|fourth|(fi)?fth|sixth|seventh)\\Wgroup");
		Matcher m4 = p4.matcher(instance.getDocument().documentContent);
		Set<String> enumerations = new HashSet<>();
		while (m4.find()) {
			System.out.println(m4.group());
			enumerations.add(m4.group());

		}
		System.out.println("Count enumerations : " + enumerations.size());

		Pattern p5 = Pattern.compile("(two|three|four|five|six|seven)\\W(experimental\\W)?groups");
		Matcher m5 = p5.matcher(instance.getDocument().documentContent);
		int c = 0;
		while (m5.find()) {
			System.out.println(m5.group());
			c++;
		}
		System.out.println("Count Counts : " + c);

		Pattern p6 = Pattern.compile("(sham|control)(\\((.{1,15})\\))?\\W(group|animals|controls?|)");
		Matcher m6 = p6.matcher(instance.getDocument().documentContent);
		Set<String> shams = new HashSet<>();
		while (m6.find()) {
			System.out.println(m6.group());
			shams.add(m6.group());
		}
		System.out.println("Count Shams : " + shams.size());

		Pattern p7 = Pattern.compile("(g|G)roup\\W\\(?([A-Z]?[1-9]|[ivIV]{1,2}|\\([a-zA-Z]\\))\\)? ");
		Matcher m7 = p7.matcher(instance.getDocument().documentContent);
		Set<String> gn = new HashSet<>();
		while (m7.find()) {
			System.out.println(m7.group());
			gn.add(m7.group());
		}
		System.out.println("Count GroupNames : " + gn.size());

		Pattern p8 = Pattern.compile("received.{1,20}inject.{1,5}");
		Matcher m8 = p8.matcher(instance.getDocument().documentContent);
		int rec = 0;
		while (m8.find()) {
			System.out.println(m8.group());
			rec++;
		}
		System.out.println("Count received : " + rec);
	}

	public static boolean contains(String expSurfaceForm, String treatSurfaceForm) {
		for (String element : treatSurfaceForm.split("\\W")) {
			if (expSurfaceForm.contains(element))
				return true;
		}
		return false;
	}
}
