package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.heuristics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;
import uk.ac.shef.wit.simmetrics.tokenisers.TokeniserQGram3;

public class LocalSentenceHeuristic {

	private static final double jaccardThreshold = 0.2D;
	final Map<Instance, Set<DocumentLinkedAnnotation>> annotations;
//	private final WEKAClustering gnc;
	private Map<Instance, Set<EntityTemplate>> groupAnnotations;
//	private GroupNameMapping groupNameMapping;

	public LocalSentenceHeuristic(Map<Instance, Set<DocumentLinkedAnnotation>> annotations,
			Map<Instance, Set<EntityTemplate>> groupAnnotations, List<Instance> trainingInstances) throws IOException {
		this.annotations = annotations;
		this.groupAnnotations = groupAnnotations;
//		gnc = new WEKAClustering(modelName);
//		this.groupNameMapping = new GroupNameMapping(100);
//		try {
//			gnc.trainOrLoad(trainingInstances);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}

	/*
	 * Best values investigationMethodRange = 5 && groupNameRange = 0-10
	 */
	final private int investigationMethodRange = 5;
	final int groupNameRange = 1;

	public Map<Instance, State> predictInstancesByHeuristic(List<Instance> instances) {

		Map<Instance, State> results = new HashMap<>();

		for (Instance instance : instances) {
			System.out.println(instance.getName());
			Set<DocumentLinkedAnnotation> documentAnnotations = this.annotations.get(instance);

			Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = toSentencebasedAnnotations(
					documentAnnotations);

			List<ResultData> trendInvResultData = extractTrendInvMethodResultData(entitiesPerSentence);
			System.out.println(trendInvResultData.size());

			if (SCIOSlotTypes.hasTargetGroup.isIncluded()) {
				Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup = getGroupMapping(instance);
				trendInvResultData = addExperimentalGroupData(namePerGroup, trendInvResultData, instance,
						entitiesPerSentence);
			}
			System.out.println(trendInvResultData.size());

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

	private List<ResultData> addExperimentalGroupData(Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			List<ResultData> trendInvResultData, Instance instance,
			Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence) {

//		System.out.println(instance.getName());

//		Map<Set<String>, Map<Set<String>, Boolean>> mappingData = groupNameMapping.getDataSet(instance);

//		System.out.println("---Local Group Names---");
//		for (Set<String> localGN : mappingData.keySet()) {
//			System.out.println(localGN);
//		}
//		System.out.println("------");
		List<ResultData> newResultData = new ArrayList<>();
		for (ResultData resultData : trendInvResultData) {

			int trendSentenceIndex = getSentenceIndexOfTrend(resultData);

			/**
			 * Look behind trend:
			 * 
			 * For the last "range"-number of sentences look for group names / experimental
			 * groups.
			 */

			Set<EntityTemplate> defExpGroups1 = new HashSet<>();
			Set<EntityTemplate> defExpGroups2 = new HashSet<>();

			if (new HashSet<>(namePerGroup.values()).size() == 2) {
				List<EntityTemplate> groups = new ArrayList<>(new HashSet<>(namePerGroup.values()));
				defExpGroups1.add(groups.get(0));
				defExpGroups2.add(groups.get(1));
			} else {
//			Set<String> usedDefExpGroupsNames = new HashSet<>();
//			System.out.println("Trend  Sentence index" + trendSentenceIndex);
				Set<DocumentLinkedAnnotation> names = new HashSet<>();
				for (int sentenceIndex = trendSentenceIndex; sentenceIndex >= trendSentenceIndex
						- groupNameRange; sentenceIndex--) {

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

					names.addAll(getRelatedClassesOf(annotationsInSentence, SCIOEntityTypes.groupName));
					names.addAll(getSubClassOf(annotationsInSentence, SCIOEntityTypes.experimentalGroup));

					List<DocumentLinkedAnnotation> sortedLocalNames = new ArrayList<>(names);
					/**
					 * Sort to compare first mention against all following ones if there are
					 * mentioned multiple. Sorting gains 4 points in macro F
					 */
					Collections.sort(sortedLocalNames, new Comparator<DocumentLinkedAnnotation>() {

						@Override
						public int compare(DocumentLinkedAnnotation o1, DocumentLinkedAnnotation o2) {

							/*
							 * TODO: HEURISTIC Sort by sentence index, sentences that appear later are
							 * chosen first.
							 */
							if (o1.getSentenceIndex() != o2.getSentenceIndex())
								return -Integer.compare(o1.getSentenceIndex(), o2.getSentenceIndex());
							/*
							 * If the sentence index is equal sort by char offset. names that appear in the
							 * sentence first are chosen first.
							 */
							return Integer.compare(o1.getStartDocCharOffset(), o2.getStartDocCharOffset());
						}
					});
					System.out.println((trendSentenceIndex - sentenceIndex) + " --> ### NUmber of Local Names = "
							+ sortedLocalNames.size());

//				System.out.println("namePerGroup: " + namePerGroup.keySet().stream()
//						.map(d -> d.getSurfaceForm() + " " + d.getEndDocCharOffset()).collect(Collectors.toSet()));

//					Set<String> localNames = sortedLocalNames.stream().map(s -> s.getSurfaceForm())
//							.collect(Collectors.toSet());
//					System.out.println("localNames: " + localNames);

//					if (mappingData.get(localNames) != null) {
//
//						Map<DocumentLinkedAnnotation, EntityTemplate> filterNamePerGroup = new HashMap<>();
//
//						/*
//						 * If the collected local group names have any match
//						 */
//						for (Entry<Set<String>, Boolean> mappings : mappingData.get(localNames).entrySet()) {
//
//							if (!mappings.getValue())
//								continue;
//
//							System.out.println("mappings: " + mappings);
//
//							/*
//							 * If the match was positive.
//							 */
//							for (String localGroupName : mappings.getKey()) {
//
//								for (DocumentLinkedAnnotation groupName : namePerGroup.keySet()) {
//
//									if (localGroupName.equals(groupName.getSurfaceForm())) {
//										filterNamePerGroup.put(groupName, namePerGroup.get(groupName));
//									}
//
//								}
//
//							}
//
//						}
//					}
					for (DocumentLinkedAnnotation groupNameAnnotation : sortedLocalNames) {

						Set<EntityTemplate> defExpGroups = defExpGroups1.isEmpty() ? defExpGroups1 : defExpGroups2;

						mapGroupNamesToGroups(namePerGroup, defExpGroups1, groupNameAnnotation, defExpGroups);
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
//					System.out.println(defExpGroups1.size());
//					System.out.println(defExpGroups2.size());

					if (defExpGroups1.size() >= 2) {
						break;
					}

					if (defExpGroups1.size() >= 1 && defExpGroups2.size() >= 1) {
						break;
					}

//					defExpGroups1.clear();
//					defExpGroups2.clear();

//				System.out.println(namePerGroup.keySet());
//				System.out.println(names.stream().map(d -> d.getSurfaceForm()).collect(Collectors.toSet()));
				}
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

	private void mapGroupNamesToGroups(Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			Set<EntityTemplate> defExpGroups1, DocumentLinkedAnnotation groupNameAnnotation,
			Set<EntityTemplate> defExpGroups) {
		if (referesToAExactTreatment(defExpGroups, namePerGroup, groupNameAnnotation)) {
//						System.out.println(" classified as EXACT!");
			defExpGroups.addAll(getExactTreatment(defExpGroups, defExpGroups1, namePerGroup, groupNameAnnotation));
		} else if (refersToMultipleGroups(defExpGroups, namePerGroup, groupNameAnnotation)) {
//			System.out.println(groupNameAnnotation.getSurfaceForm() + " classified as MULTIPLE!");
			defExpGroups
					.addAll(getMultipleTreatmentGroups(defExpGroups, defExpGroups1, namePerGroup, groupNameAnnotation));
		} else if (refersToAllControls(defExpGroups, namePerGroup, groupNameAnnotation)) {
//						System.out.println(groupNameAnnotation.getSurfaceForm() + " classified as CONTROLS!");
			defExpGroups.addAll(getAllControlGroups(defExpGroups, defExpGroups1, namePerGroup, groupNameAnnotation));
		} else if (refersToAllTreatmentsGroups(defExpGroups, namePerGroup, groupNameAnnotation)) {
//						System.out.println(groupNameAnnotation.getSurfaceForm() + " classified as TREATMENTS!");
			defExpGroups.addAll(getAllTreatmentGroups(defExpGroups, defExpGroups1, namePerGroup, groupNameAnnotation));
		} else if (refersToAllGroups(defExpGroups, defExpGroups1, namePerGroup, groupNameAnnotation)) {
//			System.out.println(groupNameAnnotation.getSurfaceForm() + " classified as ALL!");
			defExpGroups.addAll(namePerGroup.values());
		} else {
			Collection<? extends EntityTemplate> simpleOverlap = getSimpleOverlapGroups(defExpGroups, defExpGroups1,
					namePerGroup, groupNameAnnotation);
			/**
			 * Jaccard Simialrity
			 */

			defExpGroups.addAll(simpleOverlap);
//						if (simpleOverlap.isEmpty())
//							System.out.println(groupNameAnnotation.getSurfaceForm() + " can NOT be assiged");
//						else
//							System.out.println(groupNameAnnotation.getSurfaceForm() + " classified by OVERLAP");

		}
	}

	private final JaccardSimilarity jaccard = new JaccardSimilarity(new TokeniserQGram3());

	private Collection<? extends EntityTemplate> getSimpleOverlapGroups(Set<EntityTemplate> defExpGroups,
			Set<EntityTemplate> defExpGroups1, Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			DocumentLinkedAnnotation groupNameAnnotation) {

		List<EntityTemplate> overlapMatches = new ArrayList<>();

		String[] groupNameTokens = groupNameAnnotation.getSurfaceForm().split("\\W|\\s");

		for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

			String[] nameTokens = name.getSurfaceForm().split("\\W|\\s");
			int counter = 0;
			for (String gnt : groupNameTokens) {
				for (String nt : nameTokens) {

					/**
					 * TODO: remove stop words
					 */

					if (gnt.equals(nt)) {
						counter++;
					}
				}
			}
			if (counter >= 1)
				overlapMatches.add(namePerGroup.get(name));

//			double s = jaccard.getSimilarity(groupNameAnnotation.getSurfaceForm(), name.getSurfaceForm());
//			if (s >= jaccardThreshold) {
//				jaccardMatches.add(namePerGroup.get(name));
//			}
		}
		return overlapMatches;
	}

	private boolean refersToMultipleGroups(Set<EntityTemplate> defExpGroups,
			Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup, DocumentLinkedAnnotation groupNameAnnotation) {

		final String surfaceForm = groupNameAnnotation.getSurfaceForm();

		boolean multi = false;

		multi |= surfaceForm.contains(",");
		multi |= surfaceForm.contains("other");
		multi |= surfaceForm.contains("with");
		multi |= surfaceForm.contains("receiv");
		multi |= surfaceForm.contains("two");
		multi |= surfaceForm.contains("three");
		multi |= surfaceForm.contains("four");
		multi |= surfaceForm.contains("both");
		multi |= surfaceForm.contains("animals");
		multi |= surfaceForm.contains("rats");
		multi |= surfaceForm.contains("mice");
		multi |= surfaceForm.contains("individual");
		multi |= surfaceForm.contains("single");
		multi |= surfaceForm.contains("between");
		multi |= surfaceForm.matches(".*(and|or).*");
		multi |= surfaceForm.matches(".+(ed|ing)( |$).*"); // attempt to model modifier

		return multi;
	}

	private Collection<? extends EntityTemplate> getMultipleTreatmentGroups(Set<EntityTemplate> defExpGroups,
			Set<EntityTemplate> defExpGroups1, Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			DocumentLinkedAnnotation groupNameAnnotation) {
		final String surfaceForm = groupNameAnnotation.getSurfaceForm();
		Set<EntityTemplate> groups = new HashSet<>();
		if (surfaceForm.matches(".*(,|((and|or|with)( |$))).*")) {
			/**
			 * Split and add each separately by Exact/Fuzzy Match
			 */

			final String[] references = surfaceForm.split(",|((and|or|with)( |$))");
			for (String reference : references) {

				reference = reference.trim();

				for (DocumentLinkedAnnotation ndl : namePerGroup.keySet()) {
					if (ndl.getSurfaceForm().equals(reference)) {
						groups.add(namePerGroup.get(ndl));
					}
				}

				/**
				 * TODO: Remove stopwords
				 */
				reference = reference.replaceAll("groups?", "");
				reference = reference.replaceAll("treatments?", "");
				reference = reference.replaceAll("animals?", "");

				DocumentLinkedAnnotation bestFuzzyMatch = null;
				double sim = levenshteinThreshold;
				for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

					double s = LevenShteinSimilarities.levenshteinSimilarity(reference, name.getSurfaceForm(), 100);
					if (s >= sim) {
						sim = s;
						bestFuzzyMatch = name;
					}
				}
				if (bestFuzzyMatch != null)
					groups.add(namePerGroup.get(bestFuzzyMatch));

			}
		}
//
		else if (surfaceForm.contains("other") && surfaceForm.contains("all")) {
			/**
			 * Add all other groups. All groups which are not in the first set of groups.
			 */
			for (EntityTemplate group : namePerGroup.values()) {
				if (!defExpGroups1.contains(group))
					groups.add(group);
			}

		} else if (surfaceForm.contains("receiv") || surfaceForm.matches(".+(ed|ing)( |$).*")) {

			String treatment = surfaceForm;
			if (surfaceForm.contains("receiv"))
				treatment = surfaceForm.substring(surfaceForm.indexOf("receiv"));// attempt to extract
			// treatment(s)
			int threshold = (surfaceForm.contains("two") || surfaceForm.contains("both")
					|| surfaceForm.contains("combina")) ? 2 : 1;

			String[] groupNameTokens = treatment.split("\\W|\\s");

			for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

				String[] nameTokens = name.getSurfaceForm().split("\\W|\\s");
				boolean first = true;
				int counter = 0;
				for (String gnt : groupNameTokens) {
					/*
					 * Skip first token (receiv(ed|ing))
					 */
					if (first) {
						first = false;
						continue;
					}

					for (String nt : nameTokens) {

						/**
						 * TODO: remove stop words
						 */

						if (gnt.equals(nt)) {
							counter++;
						}
					}
				}
				if (counter != threshold)
					groups.add(namePerGroup.get(name));
				else
					break;
			}

		}

		else if (surfaceForm.contains("two") || surfaceForm.contains("both") || surfaceForm.contains("three")
				|| surfaceForm.contains("four")) {
			int threshold = surfaceForm.contains("two") || surfaceForm.contains("both") ? 2
					: (surfaceForm.contains("three") ? 3 : (surfaceForm.contains("four") ? 4 : 100));
			for (EntityTemplate group : namePerGroup.values()) {
				if (!defExpGroups1.contains(group)) {
					groups.add(group);
				}
				if (threshold == groups.size())
					break;
			}

		} else if (surfaceForm.contains("animals") || surfaceForm.contains("rats") || surfaceForm.contains("mice")) {

			String treatment = null;
			if (surfaceForm.contains("animals")) {

				treatment = groupNameAnnotation.getSurfaceForm().substring(0,
						groupNameAnnotation.getSurfaceForm().indexOf("animals"));// attempt to extract
			} else if (surfaceForm.contains("rats")) {

				treatment = groupNameAnnotation.getSurfaceForm().substring(0,
						groupNameAnnotation.getSurfaceForm().indexOf("rats"));// attempt to extract
			} else if (surfaceForm.contains("mice")) {

				treatment = groupNameAnnotation.getSurfaceForm().substring(0,
						groupNameAnnotation.getSurfaceForm().indexOf("mice"));// attempt to extract
			}
			/**
			 * TODO: remove stop words
			 */
			treatment = treatment.replaceAll("groups?", "");
			treatment = treatment.replaceAll("treatments?", "");
			treatment = treatment.replaceAll("animals?", "");

			// treatment(s)
			String[] groupNameTokens = treatment.split("\\W|\\s");

			for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

				String[] nameTokens = name.getSurfaceForm().split("\\W|\\s");
				int counter = 0;
				for (String gnt : groupNameTokens) {

					for (String nt : nameTokens) {

						if (gnt.equals(nt)) {
							counter++;
						}
					}
				}
				if (counter >= 1)
					groups.add(namePerGroup.get(name));
			}
//
		} else if (surfaceForm.contains("individual") || surfaceForm.contains("single")) {

			for (EntityTemplate group : namePerGroup.values()) {

				DefinedExperimentalGroup g = new DefinedExperimentalGroup(group);

				Set<EntityType> relTreatmentTypes = g.getRelevantTreatments().stream().map(a -> a.getEntityType())
						.collect(Collectors.toSet());

				/**
				 * TODO: Remove all treatment types which are no direct treatment. Usually
				 * vehicles etc.
				 * 
				 * IDEA: Remove all treatments types which are found in all treatments e.g.
				 * CyclosporineA
				 * 
				 */
				relTreatmentTypes.remove(SCIOEntityTypes.vehicle);
				relTreatmentTypes.removeAll(SCIOEntityTypes.vehicle.getTransitiveClosureSubEntityTypes());

				if (relTreatmentTypes.size() == 1) {
					groups.add(group);
				}
			}

//		} else if (surfaceForm.contains("between")) {
//

		}
		return groups;
	}

	private Collection<? extends EntityTemplate> getAllControlGroups(Set<EntityTemplate> defExpGroups,
			Set<EntityTemplate> defExpGroups1, Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			DocumentLinkedAnnotation groupNameAnnotation) {

		Set<EntityTemplate> controls = new HashSet<>();

		for (DocumentLinkedAnnotation groupName : namePerGroup.keySet()) {
			if (groupName.getSurfaceForm().contains("control")) {
				controls.add(namePerGroup.get(groupName));
			}
			if (groupName.getSurfaceForm().contains("sham")) {
				controls.add(namePerGroup.get(groupName));
			}
			if (groupName.getSurfaceForm().contains("untrained")) {
				controls.add(namePerGroup.get(groupName));
			}
			if (groupName.getSurfaceForm().contains("media")) {
				controls.add(namePerGroup.get(groupName));
			}
			if (groupName.getSurfaceForm().contains("vehicle")) {
				controls.add(namePerGroup.get(groupName));
			}
		}

		return controls;
	}

	private Collection<? extends EntityTemplate> getAllTreatmentGroups(Set<EntityTemplate> defExpGroups,
			Set<EntityTemplate> defExpGroups1, Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			DocumentLinkedAnnotation groupNameAnnotation) {

		Set<EntityTemplate> allTreatments = new HashSet<>();

		for (DocumentLinkedAnnotation groupName : namePerGroup.keySet()) {
			if (!groupName.getSurfaceForm().contains("control") && !groupName.getSurfaceForm().contains("sham")
					&& !groupName.getSurfaceForm().contains("media")) {
				allTreatments.add(namePerGroup.get(groupName));
			}
		}

		return allTreatments;

	}

	private Collection<EntityTemplate> getExactTreatment(Set<EntityTemplate> defExpGroups,
			Set<EntityTemplate> defExpGroups1, Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
			DocumentLinkedAnnotation groupNameAnnotation) {

		/**
		 * Search for direct match treatment.
		 */
		for (DocumentLinkedAnnotation ndl : namePerGroup.keySet()) {
			if (ndl.getSurfaceForm().equals(groupNameAnnotation.getSurfaceForm())) {
				return Arrays.asList(namePerGroup.get(ndl));
			}
		}

		DocumentLinkedAnnotation bestFuzzyMatch = null;
		double sim = levenshteinThreshold;
		for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

			double s = LevenShteinSimilarities.levenshteinSimilarity(groupNameAnnotation.getSurfaceForm(),
					name.getSurfaceForm(), 100);
			if (s >= sim) {
				sim = s;
				bestFuzzyMatch = name;
			}
		}
		if (bestFuzzyMatch != null)
			return Arrays.asList(namePerGroup.get(bestFuzzyMatch));

		return Collections.emptyList();
	}

	private boolean refersToAllGroups(Set<EntityTemplate> defExpGroups, Set<EntityTemplate> defExpGroups1,
			Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup, DocumentLinkedAnnotation groupNameAnnotation) {

		final String surfaceForm = groupNameAnnotation.getSurfaceForm();

		boolean allGroups = false;

		allGroups |= surfaceForm.contains("all");

		return allGroups;
	}

	private boolean refersToAllTreatmentsGroups(Set<EntityTemplate> defExpGroups,
			Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup, DocumentLinkedAnnotation groupNameAnnotation) {

		final String surfaceForm = groupNameAnnotation.getSurfaceForm();

		boolean allTreatments = false;

		allTreatments |= surfaceForm.contains("treatments");
		allTreatments |= surfaceForm.matches(".*all.*groups.*");

		return allTreatments;

	}

	private boolean refersToAllControls(Set<EntityTemplate> defExpGroups,
			Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup, DocumentLinkedAnnotation groupNameAnnotation) {
		final String surfaceForm = groupNameAnnotation.getSurfaceForm();

		boolean allControls = false;

		allControls |= surfaceForm.contains("untreated");
		allControls |= surfaceForm.contains("shams");
		allControls |= surfaceForm.contains("controls");

		return allControls;

	}

	public static final double levenshteinThreshold = 0.5;

	private boolean referesToAExactTreatment(Set<EntityTemplate> defExpGroups,
			Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup, DocumentLinkedAnnotation groupNameAnnotation) {

		/**
		 * Check for direct match first. If a direct match exists, it probably refers to
		 * this group only.
		 */
		for (DocumentLinkedAnnotation ndl : namePerGroup.keySet())
			if (ndl.getSurfaceForm().equals(groupNameAnnotation.getSurfaceForm()))
				return true;

		/**
		 * If there is no direct match, check for Levenshtein distance with fix
		 * threshold.
		 */

		boolean enableLevenshtein = true;
		if (enableLevenshtein) {

			for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {

				double s = LevenShteinSimilarities.levenshteinSimilarity(groupNameAnnotation.getSurfaceForm(),
						name.getSurfaceForm(), 100);

				if (s >= levenshteinThreshold) {
					return true;
				}
			}
		}

		return false;
	}

//
//	private DocumentLinkedAnnotation addClusterMatch(Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup,
//			Set<EntityTemplate> defExpGroups, DocumentLinkedAnnotation groupNameAnnotation) {
//
//		DocumentLinkedAnnotation bestFuzzyMatch = null;
//		double sim = 0.5;
//
//		for (DocumentLinkedAnnotation name : namePerGroup.keySet()) {
//
//			GroupNamePair gnp = new GroupNamePair(groupNameAnnotation, name, false, 0);
//			double s = gnc.classifyDocument(gnp);
//
//			if (s >= sim) {
//				sim = s;
//				bestFuzzyMatch = name;
////				System.out.println(
////						sim + "\tMatch: " + bestFuzzyMatch.getSurfaceForm() + "\t" + groupNameAnnotation.getSurfaceForm());
//			}
//		}
//
//		if (bestFuzzyMatch != null) {
////			System.out.println(
////					sim + "\tMatch: " + bestFuzzyMatch.getSurfaceForm() + "\t" + groupNameAnnotation.getSurfaceForm());
//			defExpGroups.add(namePerGroup.get(bestFuzzyMatch));
//		}
//		return bestFuzzyMatch;
//	}

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

	private Map<DocumentLinkedAnnotation, EntityTemplate> getGroupMapping(Instance instance) {

		Map<DocumentLinkedAnnotation, EntityTemplate> namePerGroup = new HashMap<>();
		for (EntityTemplate resultAnnotation : this.groupAnnotations.getOrDefault(instance, Collections.emptySet())) {

			DefinedExperimentalGroup group = new DefinedExperimentalGroup(resultAnnotation);

			DocumentLinkedAnnotation rootName = group.getGroupRootName();
			if (rootName != null)
				namePerGroup.put(rootName, group.get());

			for (DocumentLinkedAnnotation groupName : group.getGroupNames()) {
				if (groupName != null)
					namePerGroup.put(groupName, group.get());
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
					.setSingleSlotFiller(SCIOSlotTypes.hasInvestigationMethod, new EntityTemplate(invMethod));

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

		List<ResultData> results = new ArrayList<>();

		for (Integer sentenceID : annotationsPerSentence.keySet()) {

			boolean add = false;

			Set<DocumentLinkedAnnotation> annotations = annotationsPerSentence.get(sentenceID);

			List<DocumentLinkedAnnotation> trendRelated = null;
			List<DocumentLinkedAnnotation> invMRelated = new ArrayList<>();

			if ((trendRelated = getRelatedClassesOf(annotations, EntityType.get("Trend"))).isEmpty()) {
				continue;
			}

//			System.out.println("Trend sentence = " + sentenceID);

			for (int i = 0; i >= -investigationMethodRange; i--) {
//				System.out.println("look for investigationemthod: " + (sentenceID + i));
				annotations = annotationsPerSentence.get(sentenceID + i);
				if (annotations == null)
					continue;

				invMRelated.addAll(getSubClassOf(annotations, EntityType.get("InvestigationMethod")));

//				System.out.println(invMRelated);
//				System.out.println();
				if (!invMRelated.isEmpty()) {
					add = true;
					break;
				}

			}

//			System.out.println("add = " + add);
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
		System.out.println("results.size() = " + results.size());
		return results;

	}

	private Map<Integer, Set<DocumentLinkedAnnotation>> toSentencebasedAnnotations(
			Set<DocumentLinkedAnnotation> documentAnnotations) {
		Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = new HashMap<>();
		if (documentAnnotations != null)
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
