
package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.iaa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.eval.STDEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.iaa.ComputeBagOfAnnotations.KappasSubject;

public class ComputeBagOfSentenceAnnotations {

	private final Set<EntityType> mainTypes;

	public ComputeBagOfSentenceAnnotations(Set<EntityType> mainTypes) {
		this.mainTypes = mainTypes;
	}

	public Map<EntityType, Double> compute(Set<DocumentLinkedAnnotation> boe1, Set<DocumentLinkedAnnotation> boe2) {

		Map<EntityType, Double> similarityMap = new HashMap<>();

		for (EntityType entityType : mainTypes) {

			Set<Annotation> reducedBOE1 = boe1.stream()
					.filter(et -> entityType.getRelatedEntityTypes().contains(et.getEntityType()))
					.map(e -> new Annotation(e.getEntityType(), e.getSentenceIndex())).collect(Collectors.toSet());
			Set<Annotation> reducedBOE2 = boe2.stream()
					.filter(et -> entityType.getRelatedEntityTypes().contains(et.getEntityType()))
					.map(e -> new Annotation(e.getEntityType(), e.getSentenceIndex())).collect(Collectors.toSet());

			double similarity = 0;

			similarity = STDEvaluator.f1(reducedBOE1, reducedBOE2);

			similarityMap.put(entityType, similarity);
		}

		return similarityMap;
	}

	static class Annotation {
		public final EntityType type;
		public final int sentenceID;

		public Annotation(EntityType type, int sentenceID) {
			super();
			this.type = type;
			this.sentenceID = sentenceID;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + sentenceID;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			Annotation other = (Annotation) obj;
			if (sentenceID != other.sentenceID)
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Annotation [type=" + type + ", sentenceID=" + sentenceID + "]";
		}

	}

	public Map<EntityType, Double> computeKappa(Set<DocumentLinkedAnnotation> boe1,
			Set<DocumentLinkedAnnotation> boe2) {

		Map<EntityType, Double> similarityMap = new HashMap<>();

		for (EntityType entityType : mainTypes) {

			double similarity = 0;

			int count = 0;
			// Make for each type separate and average over all.
			for (EntityType rootEt : entityType.getRelatedEntityTypes()) {

				Set<DocumentLinkedAnnotation> reducedBOE1 = boe1.stream().filter(et -> rootEt == et.getEntityType())
						.collect(Collectors.toSet());
				Set<DocumentLinkedAnnotation> reducedBOE2 = boe2.stream().filter(et -> rootEt == et.getEntityType())
						.collect(Collectors.toSet());

				if (reducedBOE1.isEmpty() && reducedBOE2.isEmpty())
					continue;

				count++;

				similarity += FleissKappa
						.computeKappa(toMatrix(entityType.getRelatedEntityTypes(), reducedBOE1, reducedBOE2));
			}

			similarityMap.put(entityType, count == 0 ? 0 : similarity / count);
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
					e.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()),
					e.asInstanceOfDocumentLinkedAnnotation().getEntityType());
		}

		for (AbstractAnnotation e : reducedBOE2) {
			k2.put(new KappasSubject(e.asInstanceOfDocumentLinkedAnnotation().document.documentID,
					e.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()),
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

		public KappasSubject(String docID, int sentenceID) {
			super();
			this.docID = docID;
			this.sentenceID = sentenceID;
		}

		final String docID;
		final int sentenceID;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((docID == null) ? 0 : docID.hashCode());
			result = prime * result + sentenceID;
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
			if (sentenceID != other.sentenceID)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "KappasSubject [docID=" + docID + ", tokenID=" + sentenceID + "]";
		}

	}

}
