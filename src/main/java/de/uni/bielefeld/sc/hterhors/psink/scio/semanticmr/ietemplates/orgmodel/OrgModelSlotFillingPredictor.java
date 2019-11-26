package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.SamplerCollection;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.slots.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.slots.SlotType;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.templates.et.ClusterTemplate;
import de.hterhors.semanticmr.crf.templates.et.ContextBetweenSlotFillerTemplate;
import de.hterhors.semanticmr.crf.templates.et.LocalityTemplate;
import de.hterhors.semanticmr.crf.templates.et.SlotIsFilledTemplate;
import de.hterhors.semanticmr.crf.templates.shared.IntraTokenTemplate;
import de.hterhors.semanticmr.crf.templates.shared.NGramTokenContextTemplate;
import de.hterhors.semanticmr.crf.templates.shared.SingleTokenContextTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.IStateInitializer;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.templates.PriorNumericInterpretationOrgModelTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.DocumentPartTemplate;
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

	public OrgModelSlotFillingPredictor(String modelName, SystemScope scope, List<String> trainingInstances,
			List<String> developmentInstances, List<String> testInstances) {
		super(modelName, scope, trainingInstances, developmentInstances, testInstances);
	}

	@Override
	protected File getExternalNerlaAnnotations() {
//		final File externalNerlaAnnotations = new File("src/main/resources/slotfilling/organism_model/corpus/nerla/");
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
		 final File externalNerlaAnnotations = new File(
				"src/main/resources/slotfilling/organism_model/corpus/HighRecall30/");

		return externalNerlaAnnotations;
	}

	@Override
	protected File getInstanceDirectory() {
		return new File("src/main/resources/slotfilling/organism_model/corpus/instances/");
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.001, 0), new L2(0.0001));
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
		featureTemplates.add(new OlfactoryContextTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());
		featureTemplates.add(new DocumentPartTemplate());
		featureTemplates.add(new PriorNumericInterpretationOrgModelTemplate());

//		featureTemplates.add(new LevenshteinTemplate());
//		featureTemplates.add(new NumericInterpretationTemplate());
		return featureTemplates;
	}

	@Override
	protected Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> getFeatureTemplateParameters() {

		Map<Class<? extends AbstractFeatureTemplate<?>>, Object[]> parameter = new HashMap<>();
		parameter.put(PriorNumericInterpretationOrgModelTemplate.class, new Object[] { trainingInstances });

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
				new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation("OrganismModel")))));
	}

	@Override
	protected AbstractSampler getSampler() {
//		AbstractSampler sampler = SamplerCollection.topKModelDistributionSamplingStrategy(2);
		AbstractSampler sampler = SamplerCollection.greedyModelStrategy();
//		AbstractSampler sampler = SamplerCollection.greedyObjectiveStrategy();
//		AbstractSampler sampler = new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		AbstractSampler sampler = new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		AbstractSampler sampler = new EpochSwitchSampler(e -> new Random(e).nextBoolean());
		return sampler;
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/slotfilling/org_model/");
	}

	@Override
	public HardConstraintsProvider getHardConstraints() {
		HardConstraintsProvider hardConstraintsProvider = new HardConstraintsProvider();

		hardConstraintsProvider.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(EntityTemplate entityTemplate) {
				SingleFillerSlot sfs = entityTemplate.getSingleFillerSlot(SlotType.get("hasAge"));

				if (sfs.containsSlotFiller()) {

					DocumentLinkedAnnotation a = sfs.getSlotFiller().asInstanceOfDocumentLinkedAnnotation();
					List<DocumentToken> sentence = a.document
							.getSentenceByIndex(a.relatedTokens.get(0).getSentenceIndex());

					for (DocumentToken token : sentence) {
						if (token.getText().equals("OECs")) {

							return true;
						}

					}
				}

				return false;
			}
		});

		hardConstraintsProvider.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(EntityTemplate entityTemplate) {
				SingleFillerSlot sfs = entityTemplate.getSingleFillerSlot(SlotType.get("hasAge"));

				if (sfs.containsSlotFiller()) {

					DocumentLinkedAnnotation a = sfs.getSlotFiller().asInstanceOfDocumentLinkedAnnotation();

					List<DocumentToken> l = a.document.tokenList.subList(a.relatedTokens.get(0).getDocTokenIndex() - 4,
							a.relatedTokens.get(a.relatedTokens.size() - 1).getDocTokenIndex() + 4);

					for (DocumentToken token : l) {
						if (token.getText().equals("treated")) {
							return true;
						}
					}

				}

				return false;
			}
		});
		hardConstraintsProvider.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(EntityTemplate entityTemplate) {
				SingleFillerSlot sfs = entityTemplate.getSingleFillerSlot(SlotType.get("hasGender"));

				if (sfs.containsSlotFiller()) {

					DocumentLinkedAnnotation a = sfs.getSlotFiller().asInstanceOfDocumentLinkedAnnotation();

					if (a.getEntityType() == EntityType.get("Male") || a.getEntityType() == EntityType.get("Female")) {

						List<DocumentToken> l = a.document.tokenList.subList(
								a.relatedTokens.get(0).getDocTokenIndex() - 2,
								a.relatedTokens.get(a.relatedTokens.size() - 1).getDocTokenIndex() + 2);

						for (DocumentToken token : l) {
							if (token.getText().equals("and")) {
								return true;
							}
						}

					} else {
						return false;
					}

				}

				return false;
			}
		});
		return hardConstraintsProvider;
	}
}
