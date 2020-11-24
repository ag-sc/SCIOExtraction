package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
import de.hterhors.semanticmr.crf.templates.et.LocalityTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.AnnotationsCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.VertebralAreaRestrictionProvider.EVertebralAreaModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates.SlotIsFilledTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates.VertebralAreaConditionTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates.VertebralAreaRootMatchTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.templates.VertebralAreaRootOverlapTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.AnnotationExistsInAbstractTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;

/**
 * Slot filling for organism models. Mean Score: Score [getF1()=0.771,
 * getPrecision()=0.871, getRecall()=0.692, tp=27, fp=4, fn=12, tn=0]
 * CRFStatistics [context=Train, getTotalDuration()=44913] CRFStatistics
 * [context=Test, getTotalDuration()=198] Compute coverage... Coverage Training:
 * Score [getF1()=0.985, getPrecision()=1.000, getRecall()=0.970, tp=128, fp=0,
 * fn=4, tn=0] Compute coverage... Coverage Development: Score [getF1()=0.853,
 * getPrecision()=1.000, getRecall()=0.744, tp=29, fp=0, fn=10, tn=0] modelName:
 * VertebralArea-475240935
 * 
 * @author hterhors
 *
 */
public class VertebralLocationPredictorPrediction extends VertebralLocationPredictor {

	public VertebralLocationPredictorPrediction(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule,
			ENERModus modus) {

		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule, modus);

	}

	@Override
	protected File getExternalNerlaFile() {
		return new File("prediction/nerla");
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("prediction/instances");
	}

}
