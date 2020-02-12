package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ClusterTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class OrgModelSlotFillingPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger(OrgModelSlotFillingPredictor.class);
	private EOrgModelModifications rule;

	public OrgModelSlotFillingPredictor(String modelName, SystemScope scope, List<String> trainingInstances,
			List<String> developmentInstances, List<String> testInstances, EOrgModelModifications rule) {
		super(modelName, scope, trainingInstances, developmentInstances, testInstances, rule);
	}

	@Override
	protected File getExternalNerlaFile() {
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.organismModel);
//		 final File externalNerlaAnnotations = new File(
//				"src/main/resources/slotfilling/organism_model/corpus/Normal/");
//		 final File externalNerlaAnnotations = new File(
//				"src/main/resources/slotfilling/organism_model/corpus/HighRecall5/");
//		 final File externalNerlaAnnotations = new File(
//				"src/main/resources/slotfilling/organism_model/corpus/HighRecall10/");
//		 final File externalNerlaAnnotations = new File(
//				"src/main/resources/slotfilling/organism_model/corpus/HighRecall15/");
//		 final File externalNerlaAnnotations = new File(
//				"src/main/resources/slotfilling/organism_model/corpus/HighRecall20/");
//		final File externalNerlaAnnotations = new File(
//				"src/main/resources/slotfilling/organism_model/corpus/HighRecall30/");

	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.organismModel);
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.001, 0), new L2(0.0001F));
	}

	@Override
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();

		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
//		featureTemplates.add(new OlfactoryContextTemplate());
//		featureTemplates.add(new LocalityTemplate());
//		featureTemplates.add(new SlotIsFilledTemplate());
//		featureTemplates.add(new DocumentPartTemplate());

//		featureTemplates.add(new PriorNumericInterpretationOrgModelTemplate());

//		featureTemplates.add(new DocumentSectionTemplate());

//		featureTemplates.add(new LevenshteinTemplate());
//		featureTemplates.add(new NumericInterpretationTemplate());
		return featureTemplates;
	}

	@Override
	protected Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getFeatureTemplateParameters() {

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();
//		parameter.put(PriorNumericInterpretationOrgModelTemplate.class, new Object[] { trainingInstances });

		return parameter;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 10;
	}

	@Override
	protected IStateInitializer getStateInitializer() {
//		(instance -> {
		//
//						List<AbstractAnnotation> as = new ArrayList<>();
		//
//						for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
//							as.add(new EntityTemplate(AnnotationBuilder.toAnnotation("OrganismModel")));
//						}
//						return new State(instance, new Annotations(as));
//						//
//					});

		return ((instance) -> new State(instance,
				new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation(SCIOEntityTypes.organismModel)))));
	}

	@Override
	protected AbstractSampler getSampler() {
//		AbstractSampler sampler = SamplerCollection.topKModelDistributionSamplingStrategy(2);
//		AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//		AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
		AbstractSampler sampler = new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
		return sampler;
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/slotfilling/org_model/");
	}

	@Override
	protected Collection<GoldModificationRule> getGoldModificationRules(IModificationRule rule) {
		return OrganismModelRestrictionProvider.getByRule((EOrgModelModifications) rule);
	}

}
