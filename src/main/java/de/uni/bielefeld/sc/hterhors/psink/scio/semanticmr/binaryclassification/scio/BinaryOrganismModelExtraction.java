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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.BinaryDataPoint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.BinaryExtraction;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.binaryclassification.DLAPredictions;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.OrganismModelWrapper;

/**
 * Class to predict the organism model in a binary classification style.
 * 
 * 
 * Create pairs of entities that are mentioned in the text and
 * 
 * @author hterhors
 *
 */
public class BinaryOrganismModelExtraction extends BinaryExtraction {

	public static void main(String[] args) throws Exception {
		new BinaryOrganismModelExtraction();

	}

	public BinaryOrganismModelExtraction() throws Exception {
		super("OrganismModel");
	}

	@Override
	protected File getExternalNerlaFile() {
//		macroScore = Score [macroF1=0.863, macroPrecision=0.958, macroRecall=0.785]
//				binaryScore = Score [getAccuracy()=0.979, getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=177, fn=0, tn=8196]
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.organismModel);
//		macroScore = Score [macroF1=0.845, macroPrecision=0.993, macroRecall=0.736]
//				binaryScore = Score [getAccuracy()=0.219, getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=89, fn=0, tn=25]
//		return new File("data/additional_nerla/organism_model/LITERAL");
//		macroScore = Score [macroF1=0.854, macroPrecision=0.993, macroRecall=0.749]
//				binaryScore = Score [getAccuracy()=0.192, getF1()=0.000, getPrecision()=0.000, getRecall()=0.000, tp=0, fp=97, fn=0, tn=23]
//		return new File("data/additional_nerla/organism_model/DOCUMENT_LINKED");
	}

	@Override
	protected EntityTemplate convertToEntityTemplate(DLAPredictions dlaPredictions) {

		EntityTypeAnnotation root = AnnotationBuilder.toAnnotation(SCIOEntityTypes.organismModel);

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
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.organismModel);
	}

	@Override
	protected List<DocumentLinkedAnnotation> getDLAnnotations(EntityTemplate et) {
		OrganismModelWrapper organismModel = new OrganismModelWrapper(et);
		return organismModel.getAnnotations();

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
