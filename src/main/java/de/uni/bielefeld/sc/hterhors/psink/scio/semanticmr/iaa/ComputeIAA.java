package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.iaa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;

public class ComputeIAA {

	public static void main(String[] args) {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization()).build();

		Set<EntityType> mainTypes = new HashSet<>(Arrays.asList(SCIOEntityTypes.organismModel));
//		Set<EntityType> mainTypes = new HashSet<>(Arrays.asList(SCIOEntityTypes.organismModel, SCIOEntityTypes.injury,
//				SCIOEntityTypes.treatment, SCIOEntityTypes.result, SCIOEntityTypes.experimentalGroup,
//				SCIOEntityTypes.trend, SCIOEntityTypes.investigationMethod));

		ComputeBagOfAnnotations computeBOE = new ComputeBagOfAnnotations(mainTypes, EEvaluationDetail.ENTITY_TYPE);

		ComputeBagOfAnnotations computeStrict = new ComputeBagOfAnnotations(mainTypes,
				EEvaluationDetail.DOCUMENT_LINKED);

		ComputeBagOfSentenceAnnotations computeSentence = new ComputeBagOfSentenceAnnotations(mainTypes);

		Set<AbstractAnnotation> boe1 = exampleRandomBOEData();
		Set<AbstractAnnotation> boe2 = exampleRandomBOEData();

		Document d = new Document("blabl", "This is a test. This is a second text.");

		Set<DocumentLinkedAnnotation> boe1dl = new HashSet<>();

		boe1dl.add(AnnotationBuilder.toAnnotation(d, EntityType.get("Male"), "This", 0));
		boe1dl.add(AnnotationBuilder.toAnnotation(d, EntityType.get("Female"), "is", 5));
//		boe1dl.add(AnnotationBuilder.toAnnotation(d, EntityType.get("Age"), "is", 21));

		Set<DocumentLinkedAnnotation> boe2dl = new HashSet<>();
		boe2dl.add(AnnotationBuilder.toAnnotation(d, EntityType.get("Male"), "This", 0));
		boe2dl.add(AnnotationBuilder.toAnnotation(d, EntityType.get("Male"), "This", 16));
//		boe2dl.add(AnnotationBuilder.toAnnotation(d, EntityType.get("Female"), "is", 5));
//		boe2dl.add(AnnotationBuilder.toAnnotation(d, EntityType.get("Compound"), "is", 21));

//		Map<EntityType, Double> kappaMap = computeBOE.computeKappa(boe1dl, boe2dl);
//
//		for (Entry<EntityType, Double> entityType : kappaMap.entrySet()) {
//			System.out.println("computeKappa:" + entityType);
//		}

		Map<EntityType, Double> skappaMap = computeSentence.computeKappa(boe1dl, boe2dl);
		
		for (Entry<EntityType, Double> entityType : skappaMap.entrySet()) {
			System.out.println("sentence computeKappa:" + entityType);
		}

//		Map<EntityType, Score> similarityMap = computeBOE.computeScore(boe1, boe2);
//
//		for (Entry<EntityType, Score> entityType : similarityMap.entrySet()) {
//			System.out.println("computeBOE:" + entityType);
//		}
//
//		Map<EntityType, Score> similarityStrictMap = computeStrict.computeScore(boe1, boe2);
//
//		for (Entry<EntityType, Score> entityType : similarityStrictMap.entrySet()) {
//			System.out.println("computeStrict:" + entityType);
//		}
	}

	private static Set<AbstractAnnotation> exampleRandomBOEData() {
		return exampleRandomBOEData(new Random().nextLong());
	}

	private static Set<AbstractAnnotation> exampleRandomBOEData(long seed) {
		Set<AbstractAnnotation> boe1 = new HashSet<>();
		Random random = new Random(seed);
		for (EntityType entityType : EntityType.getEntityTypes()) {
			if (random.nextBoolean())
				boe1.add(AnnotationBuilder.toAnnotation(entityType));
		}
		return boe1;
	}

	public ComputeIAA() {

	}

	public void computeIAA() {

	}

}
