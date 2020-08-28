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
public class VertebralLocationPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public VertebralLocationPredictor(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule,
			ENERModus modus) {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule, modus);

	}

	private Map<String, Set<AbstractAnnotation>> organismModel;

	public void setOrganismModel(Map<String, Set<AbstractAnnotation>> organismModel) {
		this.organismModel = organismModel;
	}

	@Override
	protected File getExternalNerlaFile() {
		if (modus == ENERModus.GOLD)
			return new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR,
					AnnotationsCorpusBuilderBib.toDirName(SCIOEntityTypes.vertebralLocation));
		else
			return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.vertebralLocation);
	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.vertebralLocation);
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

		featureTemplates.add(new AnnotationExistsInAbstractTemplate());

		featureTemplates.add(new VertebralAreaRootOverlapTemplate());
		featureTemplates.add(new VertebralAreaConditionTemplate());
		featureTemplates.add(new VertebralAreaRootMatchTemplate());
		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
//		return 10;
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
					new Annotations(new EntityTemplate(AnnotationBuilder.toAnnotation("VertebralLocation"))));
			//
		});
	}

	@Override
	protected AbstractSampler getSampler() {
//		return SamplerCollection.greedyModelStrategy();
//		return SamplerCollection.greedyObjectiveStrategy();
		return new EpochSwitchSampler(epoch -> epoch % 2 == 0);
//		return new EpochSwitchSampler(new RandomSwitchSamplingStrategy());
//		return new EpochSwitchSampler(e -> new Random(e).nextBoolean());.
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/slotfilling/vertebral_location/");
	}

	@Override
	protected Collection<GoldModificationRule> getGoldModificationRules(IModificationRule rule) {
		Collection<GoldModificationRule> goldModificationRules = new ArrayList<>(
				VertebralAreaRestrictionProvider.getByRule((EVertebralAreaModifications) rule));

//		goldModificationRules.add(a -> {
//			if (a.asInstanceOfEntityTemplate().getRootAnnotation().entityType == EntityType.get("VertebralLocation"))
//				return a;
//			return null;
//		});

		goldModificationRules.add(a -> {
			if (a.getEntityType() == SCIOEntityTypes.vertebralArea
					&& a.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasUpperVertebrae"))
							.containsSlotFiller()
					&& a.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasLowerVertebrae"))
							.containsSlotFiller()|| a.getEntityType()!= SCIOEntityTypes.vertebralArea)

				return a;
			return null; // remove from annotation if upper or lower vertebrae is missing.
		});
		return goldModificationRules;
	}

	@Override
	public HardConstraintsProvider getHardConstraints() {
		HardConstraintsProvider prov = new HardConstraintsProvider();
//		prov.addHardConstraints(new DistinctVertebreaTemplateConstraint());
//		prov.addHardConstraints(new AbstractHardConstraint() {
//
//			@Override
//			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {
//
//				if (organismModel == null)
//					return false;
//
//				Set<AbstractAnnotation> orgModels = organismModel.get(currentState.getInstance().getName());
//				Set<Integer> sentences = new HashSet<>();
//
//				for (AbstractAnnotation orgModel : orgModels) {
//
//					OrganismModelWrapper w = new OrganismModelWrapper(orgModel.asInstanceOfEntityTemplate());
//
//					sentences.addAll(
//							w.getAnnotations().stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));
//
//				}
//				int max = 0;
//
//				for (Integer integer : sentences) {
//					max = Math.max(max, integer);
//				}
//
//				List<DocumentLinkedAnnotation> as = new ArrayList<>();
//				Set<Integer> sentences2 = new HashSet<>();
//
//				SCIOWrapper.collectDLA(as, entityTemplate.asInstanceOfEntityTemplate());
//
////				for (AbstractAnnotation ab : currentState.getGoldAnnotations().getAnnotations()) {
//
////				SCIOWrapper.collectDLA(as, ab.asInstanceOfEntityTemplate());
//				sentences2.addAll(as.stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));
////				}
//
//				for (Integer integer : sentences2) {
//
//					if (max < integer && Math.abs(max - integer) > 30)
//						return true;
//				}
////				System.out.println(Math.abs(max - min));
//
//				return false;
//			}
//		});
//	
		return prov;
	}

//	ORGANISMMODEL
//	MACRO	Root = 1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000
//			MACRO	hasLowerVertebrae = 0.533	0.533	0.533	0.667	0.667	0.667	0.800	0.800	0.800
//			MACRO	hasUpperVertebrae = 0.600	0.600	0.600	0.750	0.750	0.750	0.800	0.800	0.800
//			MACRO	Cardinality = 1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000
//			MACRO	Overall = 0.711	0.711	0.711	0.766	0.711	0.821	0.929	1.000	0.867
//			modelName: VertebralArea-523026520

//	DISTINCT Vertebrea
//	MACRO	Root = 1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000
//			MACRO	hasLowerVertebrae = 0.400	0.400	0.400	0.462	0.462	0.462	0.867	0.867	0.867
//			MACRO	hasUpperVertebrae = 0.467	0.467	0.467	0.500	0.500	0.500	0.933	0.933	0.933
//			MACRO	Cardinality = 1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000
//			MACRO	Overall = 0.664	0.711	0.622	0.687	0.711	0.667	0.966	1.000	0.933
//			modelName: VertebralArea-429362399

//	NO HArd constraints
//	MACRO	Root = 1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000
//			MACRO	hasLowerVertebrae = 0.667	0.667	0.667	0.769	0.769	0.769	0.867	0.867	0.867
//			MACRO	hasUpperVertebrae = 0.733	0.733	0.733	0.786	0.786	0.786	0.933	0.933	0.933
//			MACRO	Cardinality = 1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000	1.000
//			MACRO	Overall = 0.800	0.800	0.800	0.829	0.800	0.857	0.966	1.000	0.933
//			modelName: VertebralArea504234718
}
