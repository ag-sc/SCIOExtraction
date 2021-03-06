package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.scio;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.structure.annotations.normalization.AbstractNormalizationFunction;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.BinaryDataPoint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.BinaryExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.DLAPredictions;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.IGetNormalizationFunction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.AnaestheticWrapper;

/**
 * Class to predict the organism model in a binary classification style.
 * 
 * 
 * Create pairs of entities that are mentioned in the text and
 * 
 * @author hterhors
 *
 */
public class BinaryAnaestheticExtraction extends BinaryExtraction {

	public static void main(String[] args) throws Exception {
		new BinaryAnaestheticExtraction();

	}
//	macroScore = Score [macroF1=0.518, macroPrecision=0.792, macroRecall=0.385]
//			binaryScore = Score [getAccuracy()=1.000, getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=16, fn=0, tn=1076138]

	public BinaryAnaestheticExtraction() throws Exception {
		super("Anaesthetic", new IGetNormalizationFunction() {
			@Override
			public List<AbstractNormalizationFunction> get() {
				return Arrays.asList(new WeightNormalization(), new DistanceNormalization(),
						new DurationNormalization(), new VolumeNormalization(), new ForceNormalization(),
						new ThicknessNormalization(), new LengthNormalization(), new PressureNormalization(),
						new DosageNormalization());
			}
		});
	}

	@Override
	protected File getExternalNerlaFile() {
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.anaesthetic);
	}

	@Override
	protected EntityTemplate convertToEntityTemplate(DLAPredictions dlaPredictions) {
		EntityTypeAnnotation root = AnnotationBuilder.toAnnotation(SCIOEntityTypes.anaesthetic);

		for (DocumentLinkedAnnotation dla : dlaPredictions.collection) {

			if (root.getEntityType().getTransitiveClosureSubEntityTypes().contains(dla.entityType)) {
				root = dla;
			}

		}
		EntityTemplate etPrediction = new EntityTemplate(root);
		for (DocumentLinkedAnnotation dla : dlaPredictions.collection) {

			for (SlotType slotType : etPrediction.getSingleFillerSlotTypes()) {
				try {
					etPrediction.setSingleSlotFiller(slotType, dla);
				} catch (Exception e) {

				}
			}
			for (SlotType slotType : etPrediction.getMultiFillerSlotTypes()) {
				try {
					etPrediction.addMultiSlotFiller(slotType, dla);
				} catch (Exception e) {

				}
			}

		}

		return etPrediction;
	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.anaesthetic);
	}

	@Override
	protected List<DocumentLinkedAnnotation> getDLAnnotations(EntityTemplate et) {
		AnaestheticWrapper anaesthetic = new AnaestheticWrapper(et);
		return anaesthetic.getAnnotations();

	}

	@Override
	protected Map<String, Double> getFeatures(BinaryDataPoint dataPoint) {

		Map<String, Double> features = new HashMap<>();

		String name = "same sentence " + dataPoint.annotation1.getEntityType().name + "\t"
				+ dataPoint.annotation2.getEntityType().name;

		double value = dataPoint.annotation1.getSentenceIndex() == dataPoint.annotation2.getSentenceIndex() ? 1D : 0D;

		features.put(name, value);

		return features;
	}
}
