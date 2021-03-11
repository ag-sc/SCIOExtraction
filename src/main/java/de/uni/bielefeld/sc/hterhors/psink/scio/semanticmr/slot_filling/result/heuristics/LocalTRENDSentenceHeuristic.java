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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.ResultData;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.TrendData;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaccardSimilarity;
import uk.ac.shef.wit.simmetrics.tokenisers.TokeniserQGram3;

public class LocalTRENDSentenceHeuristic {

	private static final double jaccardThreshold = 0.2D;
	final Map<Instance, Set<DocumentLinkedAnnotation>> annotations;
//	private final WEKAClustering gnc;
//	private GroupNameMapping groupNameMapping;

	public LocalTRENDSentenceHeuristic(Map<Instance, Set<DocumentLinkedAnnotation>> annotations) throws IOException {
		this.annotations = annotations;

	}

	/*
	 * Best values investigationMethodRange = 5 && groupNameRange = 0-10
	 */

	public Map<Instance, State> predictInstancesByHeuristic(List<Instance> instances) {

		Map<Instance, State> results = new HashMap<>();

		for (Instance instance : instances) {
			System.out.println(instance.getName());
			Set<DocumentLinkedAnnotation> documentAnnotations = this.annotations.get(instance);

			Map<Integer, Set<DocumentLinkedAnnotation>> entitiesPerSentence = toSentencebasedAnnotations(
					documentAnnotations);

			List<TrendData> trendInvResultData = extractTrendData(entitiesPerSentence);
			System.out.println(trendInvResultData.size());

			List<AbstractAnnotation> predictions = new ArrayList<>();

			for (TrendData resultData : trendInvResultData) {

				EntityTemplate result = resultData.toTrend();

				if (result != null)
					predictions.add(result);

			}

			results.put(instance, new State(instance, new Annotations(predictions)));
		}

		return results;

	}

	private List<TrendData> extractTrendData(Map<Integer, Set<DocumentLinkedAnnotation>> annotationsPerSentence) {

		List<TrendData> trends = new ArrayList<>();

		for (Integer sentenceID : annotationsPerSentence.keySet()) {

			Set<DocumentLinkedAnnotation> annotations = annotationsPerSentence.get(sentenceID);

			List<DocumentLinkedAnnotation> trendRelated = null;

			if ((trendRelated = getRelatedClassesOf(annotations, EntityType.get("Trend"))).isEmpty()) {
				continue;
			}

//			System.out.println("Trend sentence = " + sentenceID);

			TrendData resultData = new TrendData();
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

			trends.add(resultData);
		}
		System.out.println("trend.size() = " + trends.size());
		return trends;

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

}
