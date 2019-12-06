package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
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
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates.SlotIsFilledTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates.VertebralAreaConditionTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates.VertebralAreaRootMatchTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.vertebralarea.templates.VertebralAreaRootOverlapTemplate;
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
public class VertebralAreaPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger("VertebralAreaPrediction");

	public VertebralAreaPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames) {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		super(modelName, scope, trainingInstanceNames, developInstanceNames, testInstanceNames);

	}

	@Override
	protected File getExternalNerlaAnnotations() {
		return new File("src/main/resources/slotfilling/vertebral_area/corpus/nerla/");
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("src/main/resources/slotfilling/vertebral_area/corpus/instances/");
	}

	@Override
	protected AdvancedLearner getLearner() {
//		return new AdvancedLearner(new SGD(0.001, 0), new L2(0.00));
		return new AdvancedLearner(new SGD(0.00001, 0), new L2(0.00));
	}

	@Override
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
//		featureTemplates.add(new LevenshteinTemplate());

		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
//		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());

		featureTemplates.add(new VertebralAreaRootOverlapTemplate());
		featureTemplates.add(new VertebralAreaConditionTemplate());
		featureTemplates.add(new VertebralAreaRootMatchTemplate());
		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 35;
	}

	@Override
	protected IStateInitializer getStateInitializer() {
//		return (instance -> {
//			
//			List<AbstractAnnotation> as = new ArrayList<>();
//			
//			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
//				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("VertebralArea")));
//			}
//			return new State(instance, new Annotations(as));
//			//
//		});
		return (instance -> {
			return new State(instance,
					new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation("VertebralArea"))));
			//
		});
	}

	@Override
	protected AbstractSampler getSampler() {
//		return SamplerCollection.greedyModelStrategy();
//		return SamplerCollection.greedyObjectiveStrategy();
		return new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		return new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		return new EpochSwitchSampler(e -> new Random(e).nextBoolean());
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/slotfilling/vertebral_area/");
	}

	@Override
	protected Collection<GoldModificationRule> getGoldModificationRules() {
		Collection<GoldModificationRule> goldModificationRules = new ArrayList<>();

		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getRootAnnotation().entityType == EntityType.get("VertebralArea"))
				return a;
			return null;
		});

		goldModificationRules.add(a -> {
			if (a.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasUpperVertebrae"))
					.containsSlotFiller()
					&& a.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasLowerVertebrae"))
							.containsSlotFiller())

				return a;
			return null; // remove from annotation if upper or lower vertebrae is missing.
		});
		return goldModificationRules;
	}
}
