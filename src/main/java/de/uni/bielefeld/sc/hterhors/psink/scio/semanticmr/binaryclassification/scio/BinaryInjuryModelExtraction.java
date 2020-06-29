package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.scio;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTypeAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.BinaryDataPoint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.BinaryExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.DLAPredictions;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.InjuryWrapper;

/**
 * Class to predict the organism model in a binary classification style.
 * 
 * 
 * Create pairs of entities that are mentioned in the text and
 * 
 * @author hterhors
 *
 */
public class BinaryInjuryModelExtraction extends BinaryExtraction {

	public static void main(String[] args) throws Exception {
		new BinaryInjuryModelExtraction();

	}
//	macroScore = Score [macroF1=0.207, macroPrecision=0.352, macroRecall=0.146]
//			binaryScore = Score [getF1()=0.363, getPrecision()=1.000, getRecall()=0.222, tp=71, fp=0, fn=249, tn=0]

//	macroScore = Score [macroF1=0.365, macroPrecision=0.614, macroRecall=0.259]
//			binaryScore = Score [getF1()=0.363, getPrecision()=1.000, getRecall()=0.222, tp=71, fp=0, fn=249, tn=0]

	public BinaryInjuryModelExtraction() throws Exception {
		super("Injury");
	}
//	with all properties
//	Score [getF1()=0.143, getPrecision()=1.000, getRecall()=0.077, tp=1, fp=0, fn=12, tn=0]
//			macroScore = Score [macroF1=0.091, macroPrecision=0.372, macroRecall=0.052]
//			binaryScore = Score [getAccuracy()=0.999, getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=665, fn=0, tn=604390]

	@Override
	protected File getExternalNerlaFile() {
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.injury);

//		macroScore = Score [macroF1=0.729, macroPrecision=0.862, macroRecall=0.631]
//				binaryScore = Score [getF1()=0.968, getPrecision()=0.938, getRecall()=1.000, tp=320, fp=21, fn=0, tn=0]

//		return new File("data/additional_nerla/injury/LITERAL");
//		return new File("data/additional_nerla/injury/DOCUMENT_LINKED");
	}

	@Override
	protected EntityTemplate convertToEntityTemplate(DLAPredictions dlaPredictions) {

		EntityTypeAnnotation root = AnnotationBuilder.toAnnotation(SCIOEntityTypes.injury);

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
					if (slotType.equals(SCIOSlotTypes.hasAnaesthesia))
						etPrediction.addMultiSlotFiller(slotType, dla);
				} catch (Exception e) {

				}
			}

		}

		return etPrediction;
	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.injury);
	}

	@Override
	protected List<DocumentLinkedAnnotation> getDLAnnotations(EntityTemplate et) {
		InjuryWrapper injury = new InjuryWrapper(et);
		return injury.getAnnotations();

	}

	@Override
	protected Map<String, Double> getFeatures(BinaryDataPoint dataPoint) {

		Map<String, Double> features = new HashMap<>();

		String name = "same sentence " + dataPoint.annotation1.getEntityType().name + "\t"
				+ dataPoint.annotation2.getEntityType().name;

		double value = dataPoint.annotation1.getSentenceIndex() == dataPoint.annotation2.getSentenceIndex() ? 1D : 0D;

		features.put(name, value);

//		features.put(dataPoint.annotation1.getEntityType() + " " + dataPoint.annotation1.getSurfaceForm(), 1D);
//		features.put(dataPoint.annotation2.getEntityType() + " " + dataPoint.annotation2.getSurfaceForm(), 1D);

		return features;
	}

}
