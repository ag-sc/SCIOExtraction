
package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.iaa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.eval.AbstractEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;

public class ComputeBagOfAnnotations {

	private final AbstractEvaluator evaluator;
	private final Set<EntityType> mainTypes;

	public ComputeBagOfAnnotations(Set<EntityType> mainTypes, EEvaluationDetail evaluationDetail) {
		this.mainTypes = mainTypes;
		this.evaluator = new NerlaEvaluator(evaluationDetail);
	}

	public Map<EntityType, Score> computeScore(Set<AbstractAnnotation> boe1, Set<AbstractAnnotation> boe2) {

		Map<EntityType, Score> similarityMap = new HashMap<>();

		for (EntityType entityType : mainTypes) {
			Set<AbstractAnnotation> reducedBOE1 = boe1.stream()
					.filter(et -> entityType.getRelatedEntityTypes().contains(et.getEntityType()))
					.collect(Collectors.toSet());
			Set<AbstractAnnotation> reducedBOE2 = boe2.stream()
					.filter(et -> entityType.getRelatedEntityTypes().contains(et.getEntityType()))
					.collect(Collectors.toSet());

			Score similarity = new Score();

			similarity = evaluator.scoreMultiValues(reducedBOE1, reducedBOE2, EScoreType.MICRO);
			similarityMap.put(entityType, similarity);
		}

		return similarityMap;
	}

	public Map<EntityType, Double> computeKappa(Set<DocumentLinkedAnnotation> boe1,
			Set<DocumentLinkedAnnotation> boe2) {

		Map<EntityType, Double> similarityMap = new HashMap<>();

		for (EntityType entityType : mainTypes) {
			Set<DocumentLinkedAnnotation> reducedBOE1 = boe1.stream()
					.filter(et -> entityType.getRelatedEntityTypes().contains(et.getEntityType()))
					.collect(Collectors.toSet());
			Set<DocumentLinkedAnnotation> reducedBOE2 = boe2.stream()
					.filter(et -> entityType.getRelatedEntityTypes().contains(et.getEntityType()))
					.collect(Collectors.toSet());

			double similarity = 0;

			similarity = FleissKappa
					.computeKappa(toMatrix(entityType.getRelatedEntityTypes(), reducedBOE1, reducedBOE2));

			similarityMap.put(entityType, similarity);
		}

		return similarityMap;
	}

	private int[][] toMatrix(Set<EntityType> enitityTypes, Set<DocumentLinkedAnnotation> reducedBOE1,
			Set<DocumentLinkedAnnotation> reducedBOE2) {

		Map<String, Integer> etindex = new HashMap<>();
		for (EntityType et : enitityTypes) {
			etindex.put(et.name, etindex.size());
		}
		int lastindex = etindex.size();
		etindex.put("MISSING", lastindex);

		int numberOfEntityLabels = etindex.size();
		int numberOfTokens = 0;

		Map<KappasSubject, EntityType> k1 = new HashMap<>();
		Map<KappasSubject, EntityType> k2 = new HashMap<>();

		for (AbstractAnnotation e : reducedBOE1) {
			k1.put(new KappasSubject(e.asInstanceOfDocumentLinkedAnnotation().document.documentID,
					e.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getDocTokenIndex()),
					e.asInstanceOfDocumentLinkedAnnotation().getEntityType());
		}

		for (AbstractAnnotation e : reducedBOE2) {
			k2.put(new KappasSubject(e.asInstanceOfDocumentLinkedAnnotation().document.documentID,
					e.asInstanceOfDocumentLinkedAnnotation().relatedTokens.get(0).getDocTokenIndex()),
					e.asInstanceOfDocumentLinkedAnnotation().getEntityType());
		}
		Map<KappasSubject, Integer> ksindex = new HashMap<>();

		for (KappasSubject kappasSubject : k1.keySet()) {
			ksindex.put(kappasSubject, ksindex.size());
		}
		for (KappasSubject kappasSubject : k2.keySet()) {
		
			if (ksindex.containsKey(kappasSubject))
				continue;
			ksindex.put(kappasSubject, ksindex.size());
	
		}

		numberOfTokens += ksindex.size();

		int[][] matrix = new int[numberOfTokens][numberOfEntityLabels];

		for (Entry<KappasSubject, Integer> ksi : ksindex.entrySet()) {
			if (k1.containsKey(ksi.getKey())) {
				matrix[ksi.getValue()][etindex.get(k1.get(ksi.getKey()).name)]++;
			} else {
				matrix[ksi.getValue()][lastindex]++;
			}
			if (k2.containsKey(ksi.getKey())) {
				matrix[ksi.getValue()][etindex.get(k2.get(ksi.getKey()).name)]++;
			} else {
				matrix[ksi.getValue()][lastindex]++;
			}
		}
		
		return matrix;
	}

	static class KappasSubject {

		public KappasSubject(String docID, int tokenID) {
			super();
			this.docID = docID;
			this.tokenID = tokenID;
		}

		final String docID;
		final int tokenID;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((docID == null) ? 0 : docID.hashCode());
			result = prime * result + tokenID;
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
			KappasSubject other = (KappasSubject) obj;
			if (docID == null) {
				if (other.docID != null)
					return false;
			} else if (!docID.equals(other.docID))
				return false;
			if (tokenID != other.tokenID)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KappasSubject [docID=" + docID + ", tokenID=" + tokenID + "]";
		}

	}

}
