package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.heuristics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.helper.LevenShteinSimilarities;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.groupnames.helper.GroupNamePair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.clustering.weka.WEKAClustering;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.expgroup.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;

public class Heuristic {

	final Map<Instance, Set<DocumentLinkedAnnotation>> annotations;
	private final WEKAClustering gnc;

	public Heuristic(Map<Instance, Set<DocumentLinkedAnnotation>> annotations, List<Instance> trainingInstances) {
		this.annotations = annotations;

		gnc = new WEKAClustering();

		try {
			gnc.trainOrLoad(trainingInstances);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<Instance, State> predictByHeuristic(List<Instance> instances) {

		Map<Instance, State> results = new HashMap<>();

		for (Instance instance : instances) {

			Set<DocumentLinkedAnnotation> documentAnnotations = this.annotations.get(instance);

			Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = toSentencebasedAnnotations(
					documentAnnotations);

			List<ResultData> trendInvResultData = extractTrendInvMethodResultData(entitiesPerSentence);

			if (SCIOSlotTypes.hasTargetGroup.isIncluded())
				addGroupData(trendInvResultData, instance, entitiesPerSentence);

			List<AbstractAnnotation> predictions = new ArrayList<>();

			for (ResultData resultData : trendInvResultData) {

				EntityTemplate result = resultData.toResult();

				if (result != null)
					predictions.add(result);

			}
			results.put(instance, new State(instance, new Annotations(predictions)));
		}

		return results;

	}

	private void addGroupData(List<ResultData> trendInvResultData, Instance instance,
			Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence) {

		Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup = extractGroupNameMapping(instance);

		int numberofGroups = new HashSet<>(namePerGroup.values()).size();
//		System.out.println("Number of groups: " + numberofGroups);
//		System.out.println("Number of results: " + trendInvResultData.size());

		final int range = 2;
		for (ResultData resultData : trendInvResultData) {

			int trendSentenceIndex = getSentenceIndexOfTrend(resultData);

			/**
			 * Look behind trend:
			 * 
			 * For the last range number of sentences look for group names / experimental
			 * groups.
			 */

			Set<EntityTemplate> defExpGroups = new HashSet<>();

			for (int i = trendSentenceIndex; i >= trendSentenceIndex - range; i--) {

				Set<DocumentLinkedAnnotation> annotationsForSentence = entitiesPerSentence.get(i);
				if (annotationsForSentence == null)
					continue;

				Set<DocumentLinkedAnnotation> names = new HashSet<>();

				List<DocumentLinkedAnnotation> groupNames = getRelatedClassesOf(annotationsForSentence,
						EntityType.get("GroupName"));

				List<DocumentLinkedAnnotation> groups = getSubClassOf(annotationsForSentence,
						EntityType.get("DefinedExperimentalGroup"));

				names.addAll(groupNames);
				names.addAll(groups);
				System.out.println(namePerGroup.keySet().stream()
						.map(d -> d.getSurfaceForm() + " " + d.getEndDocCharOffset()).collect(Collectors.toSet()));
				System.out.println(names.stream().map(d -> d.getSurfaceForm() + " " + d.getEndDocCharOffset())
						.collect(Collectors.toSet()));

				for (DocumentLinkedAnnotation groupNameAnnotation : names) {
					if (!addDirectMatch(namePerGroup, defExpGroups, groupNameAnnotation))
						System.out.println("In");
					System.out.println(groupNameAnnotation.getSurfaceForm());
					System.out.println("Cluster: "
							+ addClusterMatch(namePerGroup, defExpGroups, groupNameAnnotation).getSurfaceForm());
					System.out.println(
							"Fuzzy: " + addLevenshteinFuzzyMatch(namePerGroup, defExpGroups, groupNameAnnotation)
									.getSurfaceForm());
					System.out.println();
				}

				if (!defExpGroups.isEmpty())
					break;

//				System.out.println(namePerGroup.keySet());
//				System.out.println(names.stream().map(d -> d.getSurfaceForm()).collect(Collectors.toSet()));
			}

			Iterator<EntityTemplate> it = defExpGroups.iterator();

			if (it.hasNext())
				resultData.group1 = it.next();
//			else
//				System.out.println("Less than 1!");

			if (it.hasNext())
				resultData.group2 = it.next();
//			else
//				System.out.println("Less than 2!");

//			if (it.hasNext())
//				System.out.println("More than 2!");
		}

	}

	private boolean addDirectMatch(Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			Set<EntityTemplate> defExpGroups, DocumentLinkedAnnotation groupNameAnnotation) {
		EntityTemplate g = namePerGroup.get(groupNameAnnotation);
		if (g != null) {
			defExpGroups.add(g);
			return true;
		}
		return false;
	}

	private DocumentLinkedAnnotation addLevenshteinFuzzyMatch(
			Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup, Set<EntityTemplate> defExpGroups,
			DocumentLinkedAnnotation groupNameAnnotation) {
//		Final Score: Score [getF1()=0.448, getPrecision()=0.895, getRecall()=0.299, tp=5832, fp=686, fn=13671, tn=0]

		DocumentLinkedAnnotation bestFuzzyMatch = null;
		double sim = 0;

		for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

			double s = LevenShteinSimilarities.levenshteinSimilarity(groupNameAnnotation.getSurfaceForm(),
					name.getSurfaceForm(), 100);

			if (s >= sim) {
				sim = s;
				bestFuzzyMatch = name;
			}
		}

		if (sim >= 0.3D && bestFuzzyMatch != null) {
//			System.out.println("Match: " + bestFuzzyMatch + "\t" + groupNameAnnotation.getSurfaceForm());
			defExpGroups.add(namePerGroup.get(bestFuzzyMatch));
		}

		return bestFuzzyMatch;
	}

	private DocumentLinkedAnnotation addClusterMatch(Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			Set<EntityTemplate> defExpGroups, DocumentLinkedAnnotation groupNameAnnotation) {

//	Final Score: Score [getF1()=0.446, getPrecision()=0.889, getRecall()=0.298, tp=5804, fp=727, fn=13699, tn=0]
		DocumentLinkedAnnotation bestFuzzyMatch = null;
		double sim = 0;

		for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

			GroupNamePair gnp = new GroupNamePair(groupNameAnnotation, name, false, 0);
			double s = gnc.classifyDocument(gnp);

			if (s >= sim) {
				sim = s;
				bestFuzzyMatch = name;
//				System.out.println(
//						sim + "\tMatch: " + bestFuzzyMatch.getSurfaceForm() + "\t" + groupNameAnnotation.getSurfaceForm());
			}
		}

		if (bestFuzzyMatch != null) {
//			System.out.println(
//					sim + "\tMatch: " + bestFuzzyMatch.getSurfaceForm() + "\t" + groupNameAnnotation.getSurfaceForm());
			defExpGroups.add(namePerGroup.get(bestFuzzyMatch));
		}
		return bestFuzzyMatch;
	}

	/**
	 * Returns the sentence index of the trend or -1 if there is no trend at all.
	 * this should not happen.
	 * 
	 * @param resultData
	 * @return
	 */
	private int getSentenceIndexOfTrend(ResultData resultData) {
		int trendSentenceIndex = -1;
		if (resultData.difference != null)
			trendSentenceIndex = resultData.difference.getSentenceIndex();
		else if (resultData.pValue != null)
			trendSentenceIndex = resultData.pValue.getSentenceIndex();
		else if (resultData.significance != null)
			trendSentenceIndex = resultData.significance.getSentenceIndex();
		else if (resultData.trend != null)
			trendSentenceIndex = resultData.trend.getSentenceIndex();
		return trendSentenceIndex;
	}

	private Map<DocumentLinkedAnnotation, EntityTemplate> extractGroupNameMapping(Instance instance) {
		Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup = new HashMap<>();

		for (AbstractAnnotation resultAnnotation : instance.getGoldAnnotations().getAnnotations()) {
			Result result = new Result(resultAnnotation);

			List<DefinedExperimentalGroup> groups = result.getDefinedExperimentalGroups();

			for (DefinedExperimentalGroup group : groups) {
				for (DocumentLinkedAnnotation groupName : group.getGroupNames()) {
					namePerGroup.put(groupName, group.get());
				}
			}
		}
		return namePerGroup;
	}

	class ResultData {

		public EntityTemplate group1;
		public EntityTemplate group2;
		public DocumentLinkedAnnotation trend;
		public DocumentLinkedAnnotation difference;
		public DocumentLinkedAnnotation significance;
		public DocumentLinkedAnnotation pValue;
		public DocumentLinkedAnnotation invMethod;

		public EntityTemplate toResult() {

			if (SCIOSlotTypes.hasTargetGroup.isIncluded() && group1 == null && group2 == null)
				return null;

			EntityTemplate result = new EntityTemplate(SCIOEntityTypes.result)
					.setSingleSlotFiller(SCIOSlotTypes.hasInvestigationMethod, invMethod);

			EntityTemplate tr = null;

			if (trend == null && difference == null && significance == null && pValue == null)
				throw new IllegalStateException("Trend is null");

			if (trend != null)
				tr = new EntityTemplate(trend);

			if (difference != null) {

				if (tr == null)
					tr = new EntityTemplate(SCIOEntityTypes.trend);

				tr.setSingleSlotFiller(SCIOSlotTypes.hasDifference, difference);
			}

			EntityTemplate sig = null;
			if (significance != null) {

				if (tr == null)
					tr = new EntityTemplate(SCIOEntityTypes.trend);

				sig = new EntityTemplate(significance);
			}

			if (pValue != null) {

				if (tr == null)
					tr = new EntityTemplate(SCIOEntityTypes.trend);
				if (sig == null)
					sig = new EntityTemplate(SCIOEntityTypes.significance);

				sig.setSingleSlotFiller(SCIOSlotTypes.hasPValue, pValue);
			}

			if (sig != null)
				tr.setSingleSlotFiller(SCIOSlotTypes.hasSignificance, sig);

			if (tr != null)
				result.setSingleSlotFiller(SCIOSlotTypes.hasTrend, tr);

			if (group1 != null)
				result.setSingleSlotFiller(SCIOSlotTypes.hasTargetGroup, group1);

			if (group2 != null)
				result.setSingleSlotFiller(SCIOSlotTypes.hasReferenceGroup, group2);

			return result;
		}

		@Override
		public String toString() {
			return "ResultData [trend=" + trend + ", difference=" + difference + ", significance=" + significance
					+ ", pValue=" + pValue + ", invMethod=" + invMethod + "]";
		}

	}

	private List<ResultData> extractTrendInvMethodResultData(
			Map<Integer, Set<DocumentLinkedAnnotation>> annotationsPerSentence) {

		boolean add = false;
		int range = 5;

		List<ResultData> results = new ArrayList<>();

		for (Integer sentenceID : annotationsPerSentence.keySet()) {

			Set<DocumentLinkedAnnotation> annotations = annotationsPerSentence.get(sentenceID);

			List<DocumentLinkedAnnotation> trendRelated = null;
			List<DocumentLinkedAnnotation> invMRelated = new ArrayList<>();

			if ((trendRelated = getRelatedClassesOf(annotations, EntityType.get("Trend"))).isEmpty()) {
				continue;
			}

			for (int i = 0; i >= -range; i--) {

				annotations = annotationsPerSentence.get(sentenceID + i);
				if (annotations == null)
					continue;

				invMRelated.addAll(getSubClassOf(annotations, EntityType.get("InvestigationMethod")));
//				EntityType groupName = null;
//				containsRelatedClassOf(annotations, EntityType.get("GroupName"));
//				EntityType treatment = null;
//				containsRelatedClassOf(annotations, EntityType.get("Treatment"));

				if (!invMRelated.isEmpty()) {
//					&& groupName == null && treatment == null
					add = true;
					break;
				}

			}
			if (add) {

				ResultData resultData = new ResultData();
				for (DocumentLinkedAnnotation t : invMRelated) {
					if (SCIOEntityTypes.investigationMethod == t.getEntityType()
							|| SCIOEntityTypes.investigationMethod.isSuperEntityOf(t.getEntityType())) {
						resultData.invMethod = t;
						break;
					}
				}
				for (DocumentLinkedAnnotation t : trendRelated) {
					if (SCIOEntityTypes.trend == t.getEntityType()
							|| SCIOEntityTypes.trend.isSuperEntityOf(t.getEntityType())) {
						resultData.trend = t;
						break;
					}
				}
				for (DocumentLinkedAnnotation t : trendRelated) {
					if (SCIOEntityTypes.significance == t.getEntityType()
							|| SCIOEntityTypes.significance.isSuperEntityOf(t.getEntityType())) {
						resultData.significance = t;
						break;
					}
				}
				for (DocumentLinkedAnnotation t : trendRelated) {
					if (SCIOEntityTypes.observedDifference == t.getEntityType()
							|| SCIOEntityTypes.observedDifference.isSuperEntityOf(t.getEntityType())) {
						resultData.difference = t;
						break;
					}
				}
				for (DocumentLinkedAnnotation t : trendRelated) {
					if (SCIOEntityTypes.pValue == t.getEntityType()) {
						resultData.pValue = t;
						break;
					}
				}

				results.add(resultData);
			}
		}

		return results;

	}

	private Map<Integer, Set<DocumentLinkedAnnotation>> toSentencebasedAnnotations(
			Set<DocumentLinkedAnnotation> documentAnnotations) {
		Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = new HashMap<>();

		for (DocumentLinkedAnnotation ann : documentAnnotations) {

			entitiesPerSentence.putIfAbsent(ann.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex(),
					new HashSet<>());
			entitiesPerSentence.get(ann.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()).add(ann);
		}
		return entitiesPerSentence;
	}

	private List<DocumentLinkedAnnotation> getRelatedClassesOf(Set<DocumentLinkedAnnotation> annotations,
			EntityType entityType) {
		List<DocumentLinkedAnnotation> subList = new ArrayList<>();

		for (EntityType et : entityType.getRelatedEntityTypes()) {

			for (DocumentLinkedAnnotation documentLinkedAnnotation : annotations) {
				if (documentLinkedAnnotation.getEntityType() == et)
					subList.add(documentLinkedAnnotation);
			}
		}
		return subList;
	}

	private List<DocumentLinkedAnnotation> getSubClassOf(Set<DocumentLinkedAnnotation> annotations,
			EntityType entityType) {
		List<DocumentLinkedAnnotation> subList = new ArrayList<>();

		for (EntityType et : entityType.getTransitiveClosureSubEntityTypes()) {

			for (DocumentLinkedAnnotation documentLinkedAnnotation : annotations) {
				if (documentLinkedAnnotation.getEntityType() == et)
					subList.add(documentLinkedAnnotation);
			}
		}

		return subList;
	}

}
