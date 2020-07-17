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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.VertebralAreaWrapper;

/**
 * Class to predict the organism model in a binary classification style.
 * 
 * 
 * Create pairs of entities that are mentioned in the text and
 * 
 * @author hterhors
 *
 */
public class BinaryVertebralAreaExtraction extends BinaryExtraction {

	public static void main(String[] args) throws Exception {
		new BinaryVertebralAreaExtraction();

	}

	public BinaryVertebralAreaExtraction() throws Exception {
		super("VertebralArea");
	}

	@Override
	protected File getExternalNerlaFile() {
//		macroScore = Score [macroF1=0.384, macroPrecision=0.417, macroRecall=0.356]
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.vertebralArea);
//		return new File("data/additional_nerla/organism_model/LITERAL");
//		return new File("data/additional_nerla/organism_model/DOCUMENT_LINKED");
	}

	@Override
	protected EntityTemplate convertToEntityTemplate(DLAPredictions dlaPredictions) {
		EntityTypeAnnotation root = AnnotationBuilder.toAnnotation(SCIOEntityTypes.vertebralArea);

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
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.vertebralArea);
	}

	@Override
	protected List<DocumentLinkedAnnotation> getDLAnnotations(EntityTemplate et) {
		VertebralAreaWrapper organismModel = new VertebralAreaWrapper(et);
		return organismModel.getAnnotations();

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
