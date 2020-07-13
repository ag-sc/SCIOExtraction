package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel;

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
import de.hterhors.semanticmr.crf.templates.et.LocalityTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.AnnotationsCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.OlfactoryContextTemplate;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class OrgModelSlotFillingPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger(OrgModelSlotFillingPredictor.class);
	private EOrgModelModifications rule;

	public OrgModelSlotFillingPredictor(String modelName, List<String> trainingInstances,
			List<String> developmentInstances, List<String> testInstances, EOrgModelModifications rule, ENERModus modus) {
		super(modelName, trainingInstances, developmentInstances, testInstances, rule, modus);
	}

	// MIT GOLD ANNOATIONEN
//	MACRO	Root = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasAge = 0.820	0.857	0.786	0.820	0.857	0.786	1.000	1.000	1.000
//			MACRO	hasWeight = 0.900	0.900	0.900	0.900	0.900	0.900	1.000	1.000	1.000
//			MACRO	hasOrganismSpecies = 0.919	0.925	0.912	0.993	1.000	0.986	0.925	0.925	0.925
//			MACRO	hasAgeCategory = 0.965	0.972	0.958	0.965	0.972	0.958	1.000	1.000	1.000
//			MACRO	hasGender = 0.993	1.000	0.986	0.993	1.000	0.986	1.000	1.000	1.000
//			MACRO	Cardinality = 0.987	1.000	0.975	0.987	1.000	0.975	1.000	1.000	1.000
//			MACRO	Overall = 0.953	0.972	0.934	0.966	0.972	0.960	0.986	1.000	0.973
//			Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE	0.94	0.97	0.91
//			modelName: OrganismModel-1616477023
//			CRFStatistics [context=Train, getTotalDuration()=14004]
//			CRFStatistics [context=Test, getTotalDuration()=388]

	@Override
	protected File getExternalNerlaFile() {
		// Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE 0.95 0.96 0.94
		// modelName: OrganismModel-1205615375
		if (modus == ENERModus.GOLD)
			return new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR,
					AnnotationsCorpusBuilderBib.toDirName(SCIOEntityTypes.organismModel));
		else
			return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.organismModel);

		// Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE 0.85 0.88 0.83
		// modelName: OrganismModel-673015650
		// return new
		// File("src/main/resources/additional_nerla/organism_model/LITERAL");
//		Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE	0.87	0.91	0.83
//		modelName: OrganismModel1469327283
//		return new File("src/main/resources/additional_nerla/organism_model/DOCUMENT_LINKED");
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
//		featureTemplates.add(new EntityTypeContextTemplate());
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
