package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.candprov.sf.GeneralCandidateProvider;
import de.hterhors.semanticmr.candprov.sf.ICandidateProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ClusterTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
import de.hterhors.semanticmr.crf.templates.et.LocalityTemplate;
import de.hterhors.semanticmr.crf.templates.et.SlotIsFilledTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.vertebralarea.VertebralAreaPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DistinctMultiValueSlotsTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.MultiValueSlotSizeTemplate;

/**
 * Slot filling for injuries.
 * 
 * 
 * Mean Score: Score [getF1()=0.416, getPrecision()=0.521, getRecall()=0.347,
 * tp=76, fp=70, fn=143, tn=0] CRFStatistics [context=Train,
 * getTotalDuration()=200631] CRFStatistics [context=Test,
 * getTotalDuration()=6597] Compute coverage... Coverage Training: Score
 * [getF1()=0.950, getPrecision()=0.985, getRecall()=0.917, tp=719, fp=11,
 * fn=65, tn=0] Compute coverage... No states were generated for instance: N156
 * Kalincik 2010 2 Coverage Development: Score [getF1()=0.814,
 * getPrecision()=0.905, getRecall()=0.740, tp=162, fp=17, fn=57, tn=0]
 * Injury-520642072
 * 
 * 
 * @author hterhors
 *
 */
public class InjurySlotFillingPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger(InjurySlotFillingPredictor.class);

	private List<GeneralCandidateProvider> provider;

	public InjurySlotFillingPredictor(String modelName, SystemScope scope, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames) {

		super(modelName, scope, trainingInstanceNames, developInstanceNames, testInstanceNames);

	}

	@Override
	protected List<? extends ICandidateProvider> getAdditionalCandidateProvider() {
		provider = new ArrayList<>();
		String vertebralAreaModelName = "VertebralArea_" + modelName;
		VertebralAreaPredictor vertebralAreaPrediction = new VertebralAreaPredictor(vertebralAreaModelName, scope,
				trainingInstanceNames, developInstanceNames, testInstanceNames);

		vertebralAreaPrediction.trainOrLoadModel();

		vertebralAreaPrediction.predictAllInstances(2);

		for (Instance instance : instanceProvider.getInstances()) {
			GeneralCandidateProvider ap = new GeneralCandidateProvider(instance);
			ap.addBatchSlotFiller(vertebralAreaPrediction.predictHighRecallInstanceByName(instance.getName(), 2));
			provider.add(ap);
		}
		return provider;
	}

	@Override
	protected File getExternalNerlaAnnotations() {
//		final File externalNerlaAnnotations = new File("src/main/resources/slotfilling/injury/corpus/nerla/");
		 final File externalNerlaAnnotations = new File("src/main/resources/slotfilling/injury/corpus/Normal/");
//		 final File externalNerlaAnnotations = new File("src/main/resources/slotfilling/injury/corpus/HighRecall20/");
		return externalNerlaAnnotations;
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("src/main/resources/slotfilling/injury/corpus/instances/");
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.00001, 0), new L2(0.00));
	}

	@Override
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
//		featureTemplates.add(new LevenshteinTemplate());
		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new DistinctMultiValueSlotsTemplate());
		featureTemplates.add(new MultiValueSlotSizeTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
//		featureTemplates.add(new OlfactoryContextTemplate());
		featureTemplates.add(new DocumentPartTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());
//		featureTemplates.add(new PriorNumericInterpretationInjuryTemplate(trainingInstances));
//		featureTemplates.add(new NumericInterpretationTemplate());

		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 10;
	}

	@Override
	protected IStateInitializer getStateInitializer() {

//		IStateInitializer stateInitializer =
//		((instance) -> new State(instance, new Annotations(
//		//
//		new EntityTemplate(AnnotationBuilder.toAnnotation("Injury"))
////
//)));

		return (instance -> {

			List<AbstractAnnotation> as = new ArrayList<>();

			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("Injury")));
			}
			return new State(instance, new Annotations(as));
			//
		});
	}

	@Override
	protected AbstractSampler getSampler() {
//		AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//		AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
		AbstractSampler sampler = new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());

		return sampler;
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/slotfilling/injury/");
	}
}

//Mean Score: Score [getF1()=0.427, getPrecision()=0.467, getRecall()=0.393, tp=86, fp=98, fn=133, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=154358]
//CRFStatistics [context=Test, getTotalDuration()=4171]
//Compute coverage...
//Compute coverage...
//No states were generated for instance: N156 Kalincik 2010 2



//Mean Score: Score [getF1()=0.432, getPrecision()=0.537, getRecall()=0.361, tp=79, fp=68, fn=140, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=67438]
//CRFStatistics [context=Test, getTotalDuration()=151]
//Compute coverage...
//Compute coverage...
//No states were generated for instance: N092 Cote, Hanna et al. 2010
//No states were generated for instance: N141 Deng 2008
//No states were generated for instance: N144 Ferrero-Gutierrez 2013
//No states were generated for instance: N145 Fouad 2005
//No states were generated for instance: N156 Kalincik 2010 2
//No states were generated for instance: N158 Kang 2015
//No states were generated for instance: N189 Novikova 2011
//No states were generated for instance: N209 Tharion 2011


//Mean Score: Score [getF1()=0.432, getPrecision()=0.537, getRecall()=0.361, tp=79, fp=68, fn=140, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=70968]
//CRFStatistics [context=Test, getTotalDuration()=177]
//Compute coverage...
//Compute coverage...
//No states were generated for instance: N092 Cote, Hanna et al. 2010
//No states were generated for instance: N141 Deng 2008
//No states were generated for instance: N144 Ferrero-Gutierrez 2013
//No states were generated for instance: N145 Fouad 2005
//No states were generated for instance: N156 Kalincik 2010 2
//No states were generated for instance: N158 Kang 2015
//No states were generated for instance: N189 Novikova 2011
//No states were generated for instance: N209 Tharion 2011

//Mean Score: Score [getF1()=0.465, getPrecision()=0.536, getRecall()=0.411, tp=90, fp=78, fn=129, tn=0]
//CRFStatistics [context=Train, getTotalDuration()=71492]
//CRFStatistics [context=Test, getTotalDuration()=244]