package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.plaf.basic.BasicToolBarUI.DockingListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.crf.exploration.IExplorationStrategy;
import de.hterhors.semanticmr.crf.exploration.constraints.AbstractHardConstraint;
import de.hterhors.semanticmr.crf.exploration.constraints.HardConstraintsProvider;
import de.hterhors.semanticmr.crf.learner.AdvancedLearner;
import de.hterhors.semanticmr.crf.learner.optimizer.SGD;
import de.hterhors.semanticmr.crf.learner.regularizer.L2;
import de.hterhors.semanticmr.crf.sampling.AbstractBeamSampler;
import de.hterhors.semanticmr.crf.sampling.AbstractSampler;
import de.hterhors.semanticmr.crf.sampling.impl.EpochSwitchSampler;
import de.hterhors.semanticmr.crf.sampling.impl.beam.EpochSwitchBeamSampler;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
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
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.DeliveryMethodRestrictionProvider.EDeliveryMethodModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.hardconstraints.DistinctEntityTemplateConstraint;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.templates.EntityTypeContextTemplate;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.OrganismModelWrapper;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.wrapper.SCIOWrapper;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 *
 */
public class DeliveryMethodPredictor extends AbstractSlotFillingPredictor {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	public DeliveryMethodPredictor(String modelName, List<String> trainingInstanceNames,
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule) {
		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule);
	}

	private Map<Instance, Set<AbstractAnnotation>> organismModel;

	public void setOrganismModel(Map<Instance, Set<AbstractAnnotation>> organismModel) {
		this.organismModel = organismModel;
	}

	@Override
	protected File getInstanceDirectory() {
		return SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.deliveryMethod);
	}

	@Override
	protected File getModelBaseDir() {
		return new File("models/slotfilling/delivery_method/");
	}

	@Override
	protected AdvancedLearner getLearner() {
		return new AdvancedLearner(new SGD(0.00001, 0), new L2(0.00));
	}

	@Override
	protected File getExternalNerlaFile() {
		return SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(SCIOEntityTypes.deliveryMethod);
	}

	@Override
	protected IStateInitializer getStateInitializer() {
//		return new GenericMultiCardinalityInitializer(SCIOEntityTypes.deliveryMethod,
//				instanceProvider.getRedistributedTrainingInstances());

//		mit 2,2
//		results: ROOT_LOCATION_DURATION	0.48	0.47	0.49
//		CRFStatistics [context=Train, getTotalDuration()=68497]
//		CRFStatistics [context=Test, getTotalDuration()=1829]
//		modelName: DeliveryMethod1684797654
//		return new GenericMultiCardinalityInitializer(SCIOEntityTypes.deliveryMethod, 2, 2);

//		results: ROOT_LOCATION_DURATION	0.57	0.52	0.62
		return (instance -> {
			return new State(instance, new Annotations(new EntityTemplate(SCIOEntityTypes.deliveryMethod),
					new EntityTemplate(SCIOEntityTypes.deliveryMethod)));
		});
//		results: ROOT_LOCATION_DURATION	0.43	0.67	0.32
//		return (instance -> {
//			return new State(instance, new Annotations(new EntityTemplate(SCIOEntityTypes.deliveryMethod)));
//		});

//		results: ROOT_LOCATION_DURATION	0.52	0.53	0.52
//		return (instance -> {
//
//			List<AbstractAnnotation> as = new ArrayList<>();
//
//			for (int i = 0; i < instance.getGoldAnnotations().getAnnotations().size(); i++) {
//				as.add(new EntityTemplate(AnnotationBuilder.toAnnotation(SCIOEntityTypes.deliveryMethod)));
//			}
//			return new State(instance, new Annotations(as));
//			//
//		});
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
	protected List<AbstractFeatureTemplate<?>> getFeatureTemplates() {
		List<AbstractFeatureTemplate<?>> featureTemplates = new ArrayList<>();
//		featureTemplates.add(new LevenshteinTemplate());
		featureTemplates.add(new IntraTokenTemplate());
		featureTemplates.add(new NGramTokenContextTemplate());
		featureTemplates.add(new SingleTokenContextTemplate());
		featureTemplates.add(new ContextBetweenSlotFillerTemplate());
		featureTemplates.add(new ClusterTemplate());
		featureTemplates.add(new EntityTypeContextTemplate());
		featureTemplates.add(new LocalityTemplate());
		featureTemplates.add(new SlotIsFilledTemplate());

		return featureTemplates;
	}

	@Override
	protected int getNumberOfEpochs() {
		return 10;
	}

	@Override
	protected Collection<GoldModificationRule> getGoldModificationRules(IModificationRule rule) {
		return DeliveryMethodRestrictionProvider.getByRule((EDeliveryMethodModifications) rule);
	}

	@Override
	public List<IExplorationStrategy> getAdditionalExplorer() {
		return Collections.emptyList();
//		return Arrays.asList(new RootTemplateCardinalityExplorer(trainingObjectiveFunction.getEvaluator(),
//				EExplorationMode.ANNOTATION_BASED, AnnotationBuilder.toAnnotation(EntityType.get("DeliveryMethod"))));
	}

	@Override
	protected AbstractBeamSampler getBeamSampler() {
		return new EpochSwitchBeamSampler(epoch -> epoch % 2 == 0);
	}

	@Override
	public HardConstraintsProvider getHardConstraints() {
		HardConstraintsProvider p = new HardConstraintsProvider();
		p.addHardConstraints(new DistinctEntityTemplateConstraint(predictionObjectiveFunction.getEvaluator()));
		p.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate) {

				Set<AbstractAnnotation> orgModels = organismModel.get(currentState.getInstance());
				Set<Integer> sentences = new HashSet<>();

				for (AbstractAnnotation orgModel : orgModels) {

					OrganismModelWrapper w = new OrganismModelWrapper(orgModel.asInstanceOfEntityTemplate());

					sentences.addAll(
							w.getAnnotations().stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));

				}
				int max = 0;

				for (Integer integer : sentences) {
					max = Math.max(max, integer);
				}

				List<DocumentLinkedAnnotation> as = new ArrayList<>();
				Set<Integer> sentences2 = new HashSet<>();

				SCIOWrapper.collectDLA(as, entityTemplate.asInstanceOfEntityTemplate());

//				for (AbstractAnnotation ab : currentState.getGoldAnnotations().getAnnotations()) {

//				SCIOWrapper.collectDLA(as, ab.asInstanceOfEntityTemplate());
				sentences2.addAll(as.stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));
//				}

				for (Integer integer : sentences2) {

					if (Math.abs(max - integer) > 30)
						return true;
				}
//				System.out.println(Math.abs(max - min));

				return false;
			}
		});
		return p;

	}

	// Mit organism model constraint
	// results: ROOT_LOCATION_DURATION 0.49 0.44 0.55
	// results scoreRoot: ROOT_LOCATION_DURATION 0.65 0.58 0.74
	// Compute coverage...
	//
	// Coverage Training: Score [getF1()=0.763, getPrecision()=0.794,
	// getRecall()=0.735, tp=355, fp=92, fn=128, tn=0]
	// Compute coverage...
	// Coverage Development: Score [getF1()=0.815, getPrecision()=0.830,
	// getRecall()=0.800, tp=88, fp=18, fn=22, tn=0]
	// results: ROOT_LOCATION_DURATION 0.49 0.44 0.55
	// CRFStatistics [context=Train, getTotalDuration()=90413]
	// CRFStatistics [context=Test, getTotalDuration()=1107]
	// modelName: DeliveryMethod-670648592

	// Ohne organism model constraint
	// results: ROOT_LOCATION_DURATION 0.5 0.44 0.58
	// results scoreRoot: ROOT_LOCATION_DURATION 0.6 0.53 0.68
	// Compute coverage...
	//
	// Coverage Training: Score [getF1()=0.844, getPrecision()=0.843,
	// getRecall()=0.845, tp=408, fp=76, fn=75, tn=0]
	// Compute coverage...
	// Coverage Development: Score [getF1()=0.826, getPrecision()=0.833,
	// getRecall()=0.818, tp=90, fp=18, fn=20, tn=0]
	// results: ROOT_LOCATION_DURATION 0.5 0.44 0.58
	// CRFStatistics [context=Train, getTotalDuration()=171058]
	// CRFStatistics [context=Test, getTotalDuration()=2966]
	// modelName: DeliveryMethod1883270709
}
