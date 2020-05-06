package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.heuristics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
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

			System.out.println("From:" + trendInvResultData.size());

			if (SCIOSlotTypes.hasTargetGroup.isIncluded())
				trendInvResultData = addGroupData(trendInvResultData, instance, entitiesPerSentence);

			System.out.println("To: " + trendInvResultData.size());
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

	private List<ResultData> addGroupData(List<ResultData> trendInvResultData, Instance instance,
			Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence) {

		Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup = extractGroupNameMapping(instance);
		System.out.println(instance.getName());
		int numberOfGroups = new HashSet<>(namePerGroup.values()).size();

//		System.out.println("Number of groups: " + numberOfGroups);
//		System.out.println("Number of results: " + trendInvResultData.size());
		List<ResultData> newResultData = new ArrayList<>();
		final int range = 10;
		for (ResultData resultData : trendInvResultData) {

			int trendSentenceIndex = getSentenceIndexOfTrend(resultData);

			/**
			 * Look behind trend:
			 * 
			 * For the last range number of sentences look for group names / experimental
			 * groups.
			 */

			Set<EntityTemplate> defExpGroups1 = new HashSet<>();
			Set<EntityTemplate> defExpGroups2 = new HashSet<>();

			Set<String> usedDefExpGroupsNames = new HashSet<>();
//			System.out.println("Trend  Sentence index" + trendSentenceIndex);
			for (int sentenceIndex = trendSentenceIndex; sentenceIndex >= trendSentenceIndex - range; sentenceIndex--) {

				Set<DocumentLinkedAnnotation> annotationsInSentence = entitiesPerSentence.get(sentenceIndex);

				if (annotationsInSentence == null)
					continue;

				/**
				 * Heuristic stop look behind if there is another trend mentioned.
				 */
//				if (trendSentenceIndex != sentenceIndex && defExpGroups.size() > 1
//						&& getRelatedClassesOf(annotationsInSentence, EntityType.get("Trend")).isEmpty()) {
//					break;
//
//				}
				Set<DocumentLinkedAnnotation> names = new HashSet<>();

				List<DocumentLinkedAnnotation> groupNames = getRelatedClassesOf(annotationsInSentence,
						EntityType.get("GroupName"));

				List<DocumentLinkedAnnotation> groups = getSubClassOf(annotationsInSentence,
						EntityType.get("ExperimentalGroup"));

				names.addAll(groupNames);
				names.addAll(groups);

				List<DocumentLinkedAnnotation> sortedNames = new ArrayList<>(names);

				/**
				 * Sort to compare first mention against all following ones if there are
				 * mentioned multiple. Sorting gains 4 points in macro F
				 */
				Collections.sort(sortedNames, new Comparator<DocumentLinkedAnnotation>() {

					@Override
					public int compare(DocumentLinkedAnnotation o1, DocumentLinkedAnnotation o2) {
						return Integer.compare(o1.getStartDocCharOffset(), o2.getStartDocCharOffset());
					}
				});

//				System.out.println("namePerGroup: " + namePerGroup.keySet().stream()
//						.map(d -> d.getSurfaceForm() + " " + d.getEndDocCharOffset()).collect(Collectors.toSet()));
//				System.out.println("sortedNames: " + sortedNames.stream()
//						.map(d -> d.getSurfaceForm() + " " + d.getEndDocCharOffset()).collect(Collectors.toSet()));

				for (DocumentLinkedAnnotation groupNameAnnotation : sortedNames) {

					Set<EntityTemplate> defExpGroups = defExpGroups1.isEmpty() ? defExpGroups1 : defExpGroups2;

//					if (groupNameAnnotation.getEntityType() == SCIOEntityTypes.analizedExperimentalGroup) {
////
//					} else {
//					 System.out.println(groupNameAnnotation.getSurfaceForm());
					if (!addDirectMatch(namePerGroup, defExpGroups, groupNameAnnotation, usedDefExpGroupsNames)) {
//						System.out.println("Direct Match");
//					else {
//						DocumentLinkedAnnotation bestClusterMatch = addClusterMatch(namePerGroup, defExpGroups,
//								groupNameAnnotation, usedDefExpGroupsNames);

//						System.out.println(
//								"Cluster: " + (bestClusterMatch == null ? "null" : bestClusterMatch.getSurfaceForm()));
//						bestClusterMatch =null;
//						if (bestClusterMatch == null) {
						DocumentLinkedAnnotation bestFuzzyMatch = addLevenshteinFuzzyMatch(namePerGroup, defExpGroups,
								groupNameAnnotation, usedDefExpGroupsNames);
//							System.out.println(
//									"Fuzzy: " + (bestFuzzyMatch == null ? "null" : bestFuzzyMatch.getSurfaceForm()));
////
						if (bestFuzzyMatch == null) {
							/**
							 * This is most prob. an AnalyzedExperimental group name. such as all other
							 * groups...
							 */

							if (groupNameAnnotation.getSurfaceForm().contains("groups")) {

								System.out.println("NEED FURTHER : " + groupNameAnnotation.toPrettyString());
							
								if (groupNameAnnotation.getSurfaceForm().contains("all")) {
									defExpGroups.addAll(namePerGroup.values());
								} else {

									DocumentLinkedAnnotation bfm = null;
									/**
									 * TODO: ADJUST TRESHOLD
									 */
									double sim = 0.8;

									for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

										double s = LevenShteinSimilarities.levenshteinSimilarity(
												groupNameAnnotation.getSurfaceForm(), name.getSurfaceForm(), 100);

										if (s >= sim) {
											sim = s;
											bfm = name;
											defExpGroups.add(namePerGroup.get(bfm));
											usedDefExpGroupsNames.add(bfm.getSurfaceForm());
										}
									}
								}

							} else {
								DocumentLinkedAnnotation bestClusterMatch = addClusterMatch(namePerGroup, defExpGroups,
										groupNameAnnotation, usedDefExpGroupsNames);

								if (bestClusterMatch == null) {

									System.out.println("COULD NOT MAP2: " + groupNameAnnotation.toPrettyString());
								}
							}

//							}
						}

						/**
						 * RESOLVE Unmappable names such as "all other three groups"
						 */

//
//						System.out.println();
//						}
					}

//					}
					/**
					 * Do not compare same groups in one result:
					 */

					defExpGroups2.removeAll(defExpGroups1);
				}
//				System.out.println("-------------------");
				/**
				 * Only one is requested
				 */
				// if (defExpGroups.size()!=0)
//					break;
				/**
				 * Heuristic. at least 2 are requested
				 */
//				System.out.println("Sentence Index look behind: " + sentenceIndex);
//				System.out.println(defExpGroups.size());

				if (defExpGroups1.size() >= 2) {
					break;
				}

				if (defExpGroups1.size() >= 1 && defExpGroups2.size() >= 1) {
					break;
				}

//				System.out.println(namePerGroup.keySet());
//				System.out.println(names.stream().map(d -> d.getSurfaceForm()).collect(Collectors.toSet()));
			}

//			if (defExpGroups1.size() > 2) {
//			System.out.println("SIZE1 : " + defExpGroups1.size());
//			System.out.println("SIZE2 : " + defExpGroups2.size());
//			System.out.println(usedDefExpGroupsNames);
//			System.out.println();
//			}
			for (EntityTemplate g1 : defExpGroups1) {

				for (EntityTemplate _g1 : defExpGroups1) {
					if (g1 == _g1)
						continue;
					ResultData clone = new ResultData(resultData);

					clone.group1 = g1;
					clone.group2 = _g1;

					newResultData.add(clone);

				}

				for (EntityTemplate g2 : defExpGroups2) {

					ResultData clone = new ResultData(resultData);

					clone.group1 = g1;
					clone.group2 = g2;

					newResultData.add(clone);
				}

			}

//			Iterator<EntityTemplate> it = defExpGroups1.iterator();
//
//			newResultData.add(resultData);
//
//			if (it.hasNext())
//				resultData.group1 = it.next();
//////			else
//////				System.out.println("Less than 1!");
////
//			if (it.hasNext())
//				resultData.group2 = it.next();
//
////				System.out.println("Less than 2!");
//
////			if (it.hasNext())
////				System.out.println("More than 2!");
		}
		return newResultData;
	}

//	Final Score:Score[getF1()=0.451, getPrecision()=0.891, getRecall()=0.302, tp=5889, fp=724, fn=13614, tn=0]
//			Final Score: Score [getF1()=0.448, getPrecision()=0.894, getRecall()=0.299, tp=5837, fp=689, fn=13666, tn=0]
//	Final Score: Score [getF1()=0.448, getPrecision()=0.894, getRecall()=0.299, tp=5837, fp=689, fn=13666, tn=0]

	private boolean addDirectMatch(Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			Set<EntityTemplate> defExpGroups, DocumentLinkedAnnotation groupNameAnnotation,
			Set<String> usedDefExpGroupsNames) {
		EntityTemplate g = null;
		for (DocumentLinkedAnnotation ndl : namePerGroup.keySet()) {
			if (ndl.getSurfaceForm().equals(groupNameAnnotation.getSurfaceForm())) {
				g = namePerGroup.get(ndl);
				usedDefExpGroupsNames.add(ndl.getSurfaceForm());
				break;
			}
		}

		if (g != null) {
			defExpGroups.add(g);
			return true;
		}
		return false;
	}

	private DocumentLinkedAnnotation addLevenshteinFuzzyMatch(
			Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup, Set<EntityTemplate> defExpGroups,
			DocumentLinkedAnnotation groupNameAnnotation, Set<String> usedDefExpGroupsNames) {

		DocumentLinkedAnnotation bestFuzzyMatch = null;
		/**
		 * TODO: ADJUST TRESHOLD
		 */
		double sim = 0.5;

		for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

			double s = LevenShteinSimilarities.levenshteinSimilarity(groupNameAnnotation.getSurfaceForm(),
					name.getSurfaceForm(), 100);

			if (s >= sim) {
				sim = s;
				bestFuzzyMatch = name;
			}
		}
		if (bestFuzzyMatch != null) {
//			System.out.println(sim);
//			System.out.println("Match: " + bestFuzzyMatch + "\t" + groupNameAnnotation.getSurfaceForm());
			defExpGroups.add(namePerGroup.get(bestFuzzyMatch));
			usedDefExpGroupsNames.add(bestFuzzyMatch.getSurfaceForm());
		}

		return bestFuzzyMatch;
	}

	private DocumentLinkedAnnotation addClusterMatch(Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			Set<EntityTemplate> defExpGroups, DocumentLinkedAnnotation groupNameAnnotation,
			Set<String> usedDefExpGroupsNames) {

		DocumentLinkedAnnotation bestFuzzyMatch = null;
		double sim = 0.5;

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
			usedDefExpGroupsNames.add(bestFuzzyMatch.getSurfaceForm());
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

				DocumentLinkedAnnotation rootName = group.getGroupRootName();

				namePerGroup.put(rootName, group.get());

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

		public ResultData() {
		}

		public ResultData(ResultData resultData) {

			this.trend = resultData.trend;
			this.difference = resultData.difference;
			this.significance = resultData.significance;
			this.pValue = resultData.pValue;
			this.invMethod = resultData.invMethod;
			this.group1 = resultData.group1;
			this.group2 = resultData.group2;

		}

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
		for (DocumentLinkedAnnotation documentLinkedAnnotation : annotations) {

			if (documentLinkedAnnotation.getEntityType() == entityType)
				subList.add(documentLinkedAnnotation);
			for (EntityType et : entityType.getTransitiveClosureSubEntityTypes()) {

				if (documentLinkedAnnotation.getEntityType() == et)
					subList.add(documentLinkedAnnotation);
			}
		}

		return subList;
	}

}
