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
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.AnnotationsCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.AbstractSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
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
			List<String> developInstanceNames, List<String> testInstanceNames, IModificationRule rule,
			ENERModus modus) {
		super(modelName, trainingInstanceNames, developInstanceNames, testInstanceNames, rule, modus);
	}

	private Map<String, Set<AbstractAnnotation>> organismModel;

	public void setOrganismModel(Map<String, Set<AbstractAnnotation>> organismModel) {
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

	// GOLD
//	MACRO	Root = 0.561	0.688	0.474	0.599	0.733	0.506	0.938	0.938	0.938
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	1.000	1.000	1.000
//			MACRO	hasLocations = 0.405	0.500	0.341	0.651	0.762	0.575	0.623	0.656	0.593
//			MACRO	Cardinality = 0.861	1.000	0.755	0.918	1.067	0.806	0.938	0.938	0.938
//			MACRO	Overall = 0.475	0.594	0.396	0.612	0.745	0.523	0.777	0.797	0.758
//			CRFStatistics [context=Train, getTotalDuration()=78416]
//			CRFStatistics [context=Test, getTotalDuration()=324]
//			modelName: DeliveryMethod-1029832602

//	MIT REG EX
//	MACRO	Root = 0.604	0.750	0.505	0.644	0.800	0.539	0.938	0.938	0.938
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.437	0.500	0.388	0.761	0.828	0.710	0.574	0.604	0.546
//			MACRO	Cardinality = 0.861	1.000	0.755	0.918	1.067	0.806	0.938	0.938	0.938
//			MACRO	Overall = 0.510	0.625	0.431	0.685	0.800	0.605	0.745	0.781	0.712
//			CRFStatistics [context=Train, getTotalDuration()=84268]
//			CRFStatistics [context=Test, getTotalDuration()=372]
//			modelName: DeliveryMethod492293592

	@Override
	protected File getExternalNerlaFile() {

		if (modus == ENERModus.GOLD)
			return new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR,
					AnnotationsCorpusBuilderBib.toDirName(SCIOEntityTypes.deliveryMethod));
		else
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
//		return new EpochSwitchSampler(epoch -> epoch <5);
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
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {

				return entityTemplate.getEntityType().equals(SCIOEntityTypes.deliveryMethod);
			}
		});

		p.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {

				if (entityTemplate.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasLocations)
						.containsSlotFiller())
					return entityTemplate.asInstanceOfEntityTemplate().getMultiFillerSlot(SCIOSlotTypes.hasLocations)
							.size() > 1;
				return false;
			}
		});
		p.addHardConstraints(new AbstractHardConstraint() {

			@Override
			public boolean violatesConstraint(State currentState, EntityTemplate entityTemplate, int annotationIndex) {

				if (organismModel == null)
					return false;

				Set<AbstractAnnotation> orgModels = organismModel.get(currentState.getInstance().getName());
				Set<Integer> sentences = new HashSet<>();

				for (AbstractAnnotation orgModel : orgModels) {

					OrganismModelWrapper w = new OrganismModelWrapper(orgModel.asInstanceOfEntityTemplate());

					sentences.addAll(
							w.getAnnotations().stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));

				}
				int maxOrganismIndex = 0;

				for (Integer integer : sentences) {
					maxOrganismIndex = Math.max(maxOrganismIndex, integer);
				}

				List<DocumentLinkedAnnotation> as = new ArrayList<>();
				Set<Integer> sentences2 = new HashSet<>();

				SCIOWrapper.collectDLA(as, entityTemplate.asInstanceOfEntityTemplate());

//				for (AbstractAnnotation ab : currentState.getGoldAnnotations().getAnnotations()) {

//				SCIOWrapper.collectDLA(as, ab.asInstanceOfEntityTemplate());
				sentences2.addAll(as.stream().map(a -> a.getSentenceIndex()).collect(Collectors.toSet()));
//				}

				for (Integer integer : sentences2) {

					if (maxOrganismIndex < integer && Math.abs(maxOrganismIndex - integer) > 30)
						return true;
				}
//				System.out.println(Math.abs(max - min));

				return false;
			}
		});
		return p;

	}

//	Only 1 root entity
//	MACRO	Root = 0.797	0.938	0.693	0.850	1.000	0.739	0.938	0.938	0.938
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.329	0.375	0.294	0.574	0.621	0.538	0.574	0.604	0.546
//			MACRO	Cardinality = 0.861	1.000	0.755	0.918	1.067	0.806	0.938	0.938	0.938
//			MACRO	Overall = 0.553	0.656	0.478	0.742	0.840	0.671	0.745	0.781	0.712
//			CRFStatistics [context=Train, getTotalDuration()=31176]
//			CRFStatistics [context=Test, getTotalDuration()=267]
//			modelName: DeliveryMethod271072862
//	genericMultiValueSampling
//	MACRO	Root = 0.774	0.688	0.885	0.826	0.733	0.944	0.938	0.938	0.938
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.346	0.281	0.450	0.604	0.466	0.824	0.574	0.604	0.546
//			MACRO	Cardinality = 0.818	0.719	0.948	0.872	0.767	1.011	0.938	0.938	0.938
//			MACRO	Overall = 0.550	0.484	0.637	0.738	0.620	0.894	0.745	0.781	0.712
//			CRFStatistics [context=Train, getTotalDuration()=145438]
//			CRFStatistics [context=Test, getTotalDuration()=629]
//			modelName: DeliveryMethod895188161

//	
//	MACRO	Root = 0.604	0.750	0.505	0.644	0.800	0.539	0.938	0.938	0.938
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.470	0.562	0.403	0.819	0.931	0.739	0.574	0.604	0.546
//			MACRO	Cardinality = 0.861	1.000	0.755	0.918	1.067	0.806	0.938	0.938	0.938
//			MACRO	Overall = 0.527	0.656	0.440	0.707	0.840	0.618	0.745	0.781	0.712
//			CRFStatistics [context=Train, getTotalDuration()=59421]
//			CRFStatistics [context=Test, getTotalDuration()=263]
//			modelName: DeliveryMethod-1123276396

//	----------------------------------

//	locations
//	MACRO	Root = 0.659	0.594	0.740	0.659	0.594	0.740	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.256	0.219	0.309	0.447	0.362	0.567	0.574	0.604	0.546
//			MACRO	Cardinality = 0.818	0.719	0.948	0.818	0.719	0.948	1.000	1.000	1.000
//			MACRO	Overall = 0.451	0.406	0.506	0.698	0.595	0.826	0.646	0.682	0.613
//			CRFStatistics [context=Train, getTotalDuration()=97070]
//			CRFStatistics [context=Test, getTotalDuration()=715]
//			modelName: DeliveryMethod1920323739

//	distinct et + location
//	MACRO	Root = 0.539	0.688	0.443	0.539	0.688	0.443	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.470	0.562	0.403	0.819	0.931	0.739	0.574	0.604	0.546
//			MACRO	Cardinality = 0.797	0.938	0.693	0.797	0.938	0.693	1.000	1.000	1.000
//			MACRO	Overall = 0.494	0.625	0.409	0.663	0.800	0.574	0.745	0.781	0.712
//			CRFStatistics [context=Train, getTotalDuration()=64076]
//			CRFStatistics [context=Test, getTotalDuration()=282]
//			modelName: DeliveryMethod-827265531

//	mit distinct  et + 
//	MACRO	Root = 0.784	0.781	0.786	0.784	0.781	0.786	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.436	0.396	0.486	0.747	0.650	0.866	0.584	0.609	0.561
//			MACRO	Cardinality = 0.893	0.875	0.911	0.893	0.875	0.911	1.000	1.000	1.000
//			MACRO	Overall = 0.583	0.550	0.621	0.777	0.703	0.861	0.751	0.783	0.721
//			CRFStatistics [context=Train, getTotalDuration()=108335]
//			CRFStatistics [context=Test, getTotalDuration()=926]
//			modelName: DeliveryMethod1647710236

//	Mit distinct et + orgmodel
//	MACRO	Root = 0.726	0.643	0.833	0.726	0.643	0.833	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.322	0.281	0.377	0.569	0.450	0.728	0.566	0.625	0.518
//			MACRO	Cardinality = 0.850	0.781	0.932	0.850	0.781	0.932	1.000	1.000	1.000
//			MACRO	Overall = 0.503	0.448	0.574	0.784	0.647	0.960	0.642	0.693	0.598
//			CRFStatistics [context=Train, getTotalDuration()=64304]
//			CRFStatistics [context=Test, getTotalDuration()=716]
//			modelName: DeliveryMethod-42372606

//	Mit distinct et + locations

//	MACRO	Root = 0.722	0.656	0.802	0.722	0.656	0.802	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.341	0.281	0.434	0.595	0.466	0.796	0.574	0.604	0.546
//			MACRO	Cardinality = 0.818	0.719	0.948	0.818	0.719	0.948	1.000	1.000	1.000
//			MACRO	Overall = 0.500	0.422	0.615	0.774	0.618	1.000	0.646	0.682	0.615
//			CRFStatistics [context=Train, getTotalDuration()=92375]
//			CRFStatistics [context=Test, getTotalDuration()=737]
//			modelName: DeliveryMethod-838974458

//	Mit distinct et
//	MACRO	Root = 0.686	0.625	0.760	0.686	0.625	0.760	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.420	0.349	0.528	0.719	0.573	0.941	0.584	0.609	0.561
//			MACRO	Cardinality = 0.818	0.719	0.948	0.818	0.719	0.948	1.000	1.000	1.000
//			MACRO	Overall = 0.548	0.440	0.729	0.844	0.644	1.000	0.650	0.682	0.729
//			CRFStatistics [context=Train, getTotalDuration()=128621]
//			CRFStatistics [context=Test, getTotalDuration()=1294]
//			modelName: DeliveryMethod-1933699436

//	Ohne constraints
//	MACRO	Root = 0.591	0.531	0.667	0.591	0.531	0.667	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.261	0.219	0.325	0.447	0.359	0.579	0.584	0.609	0.561
//			MACRO	Cardinality = 0.818	0.719	0.948	0.818	0.719	0.948	1.000	1.000	1.000
//			MACRO	Overall = 0.436	0.380	0.510	0.670	0.557	0.822	0.650	0.682	0.621
//			CRFStatistics [context=Train, getTotalDuration()=129489]
//			CRFStatistics [context=Test, getTotalDuration()=1303]
//			modelName: DeliveryMethod1222760859

//	Mit org model und location = 1  und distinct
//	MACRO	Root = 0.794	0.733	0.867	0.794	0.733	0.867	1.000	1.000	1.000
//			MACRO	hasDuration = 0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000	0.000
//			MACRO	hasLocations = 0.277	0.250	0.309	0.488	0.400	0.598	0.566	0.625	0.518
//			MACRO	Cardinality = 0.857	0.781	0.948	0.857	0.781	0.948	1.000	1.000	1.000
//			MACRO	Overall = 0.500	0.453	0.558	0.780	0.654	0.934	0.642	0.693	0.598
//			CRFStatistics [context=Train, getTotalDuration()=52415]
//			CRFStatistics [context=Test, getTotalDuration()=452]
//			modelName: DeliveryMethod954161288
}
