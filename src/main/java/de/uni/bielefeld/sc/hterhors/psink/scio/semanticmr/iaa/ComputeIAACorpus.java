package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.iaa;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DistanceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DosageNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.DurationNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ForceNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.LengthNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.PressureNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.ThicknessNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.VolumeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.wrapper.Injury;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.ResultSlotFillingHeuristic.EvalPair;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.wrapper.Treatment;

public class ComputeIAACorpus {

	static enum EAnnotator {
		Julia, Jessica;
	}

	public static void main(String[] args) {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization()).build();

		InstanceProvider prov = new InstanceProvider(new File("iaa/data/slot_filling/result/instances/"),
				new OriginalCorpusDistributor.Builder().build());

		System.out.println("Read corpus:");
		for (Instance instance : prov.getInstances()) {
			System.out.println(instance.getName());
		}
		System.out.println("done!");

		Map<EAnnotator, Map<String, Instance>> data = new HashMap<>();

		data.put(EAnnotator.Julia, new HashMap<>());
		data.put(EAnnotator.Jessica, new HashMap<>());

		Set<String> names = new HashSet<>();

		for (Instance instance : prov.getInstances()) {
			if (instance.getName().startsWith(EAnnotator.Jessica.name())) {
				data.get(EAnnotator.Jessica).put(instance.getName().replaceFirst(EAnnotator.Jessica.name(), ""),
						instance);
				names.add(instance.getName().replaceFirst(EAnnotator.Jessica.name(), ""));
			}
			if (instance.getName().startsWith(EAnnotator.Julia.name())) {
				data.get(EAnnotator.Julia).put(instance.getName().replaceFirst(EAnnotator.Julia.name(), ""), instance);
				names.add(instance.getName().replaceFirst(EAnnotator.Julia.name(), ""));
			}
		}
//		SlotFillingObjectiveFunction of = new SlotFillingObjectiveFunction(EScoreType.MACRO,

		CartesianEvaluator eval = new CartesianEvaluator(EEvaluationDetail.ENTITY_TYPE,
				EEvaluationDetail.DOCUMENT_LINKED);

		Map<EntityType, List<EvalPair>> pairsMap = new HashMap<>();
		pairsMap.put(EntityType.get("Result"), new ArrayList<>());
		pairsMap.put(EntityType.get("OrganismModel"), new ArrayList<>());
		pairsMap.put(EntityType.get("Injury"), new ArrayList<>());
		pairsMap.put(EntityType.get("InjuryDevice"), new ArrayList<>());
		pairsMap.put(EntityType.get("DeliveryMethod"), new ArrayList<>());
		pairsMap.put(EntityType.get("VertebralLocation"), new ArrayList<>());
		pairsMap.put(EntityType.get("Treatment"), new ArrayList<>());
		pairsMap.put(EntityType.get("ExperimentalGroup"), new ArrayList<>());
		pairsMap.put(EntityType.get("Anaesthetic"), new ArrayList<>());
		pairsMap.put(EntityType.get("InvestigationMethod"), new ArrayList<>());
		pairsMap.put(EntityType.get("Trend"), new ArrayList<>());
		for (String name : names) {

			Instance juliaI = data.get(EAnnotator.Julia).get(name);
			Instance jessicaI = data.get(EAnnotator.Jessica).get(name);

			pairsMap.get(EntityType.get("Result")).add(new EvalPair(juliaI,
					jessicaI.getGoldAnnotations().getAnnotations(), juliaI.getGoldAnnotations().getAnnotations()));

			pairsMap.get(EntityType.get("Trend"))
					.add(new EvalPair(juliaI, getTrend(jessicaI.getGoldAnnotations().getAnnotations()),
							getTrend(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("InvestigationMethod"))
					.add(new EvalPair(juliaI, getInvestigationMethod(jessicaI.getGoldAnnotations().getAnnotations()),
							getInvestigationMethod(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("OrganismModel"))
					.add(new EvalPair(juliaI, getOrgModel(jessicaI.getGoldAnnotations().getAnnotations()),
							getOrgModel(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("Injury"))
					.add(new EvalPair(juliaI, getInjuryModel(jessicaI.getGoldAnnotations().getAnnotations()),
							getInjuryModel(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("VertebralLocation"))
					.add(new EvalPair(juliaI, getInjuryLocation(jessicaI.getGoldAnnotations().getAnnotations()),
							getInjuryLocation(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("DeliveryMethod"))
					.add(new EvalPair(juliaI, getDeliveryMethod(jessicaI.getGoldAnnotations().getAnnotations()),
							getDeliveryMethod(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("Anaesthetic"))
					.add(new EvalPair(juliaI, getAnaesthetic(jessicaI.getGoldAnnotations().getAnnotations()),
							getAnaesthetic(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("InjuryDevice"))
					.add(new EvalPair(juliaI, getInjuryDevice(jessicaI.getGoldAnnotations().getAnnotations()),
							getInjuryDevice(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("Treatment"))
					.add(new EvalPair(juliaI, getTreatment(jessicaI.getGoldAnnotations().getAnnotations()),
							getTreatment(juliaI.getGoldAnnotations().getAnnotations())));

			pairsMap.get(EntityType.get("ExperimentalGroup"))
					.add(new EvalPair(juliaI, getExperimentalGroup(jessicaI.getGoldAnnotations().getAnnotations()),
							getExperimentalGroup(juliaI.getGoldAnnotations().getAnnotations())));

//			System.out.println(name);
//			System.out.println("julia");
//			System.out.println(juliaI.getGoldAnnotations());
//			System.out.println("jessica");
//			System.out.println(jessicaI.getGoldAnnotations());

		}

		for (Entry<EntityType, List<EvalPair>> e : pairsMap.entrySet()) {
			Score score = new Score(EScoreType.MICRO);
			for (EvalPair string : e.getValue()) {
//				System.out.println(string.gold.size());
//				System.out.println(string.pred.size());
				score.add(eval.scoreMultiValues(string.gold, string.pred, EScoreType.MICRO));
			}
			System.out.println(e.getKey().name + " " + score.getF1(new DecimalFormat("#.##")));
		}
//		Set<EntityType> mainTypes = new HashSet<>(Arrays.asList(SCIOEntityTypes.organismModel, SCIOEntityTypes.injury,
//				SCIOEntityTypes.injuryDevice, SCIOEntityTypes.deliveryMethod, SCIOEntityTypes.anaesthetic,
//				SCIOEntityTypes.vertebralLocation, SCIOEntityTypes.treatment, SCIOEntityTypes.result,
//				SCIOEntityTypes.experimentalGroup, SCIOEntityTypes.trend, SCIOEntityTypes.investigationMethod));

		Score score = new Score(EScoreType.MICRO);

		System.out.println("==========");
		for (EntityType et : pairsMap.keySet()) {
			Set<EntityType> mainTypes = new HashSet<>(Arrays.asList(et));

			Set<AbstractAnnotation> boe1 = new HashSet<>();
			Set<AbstractAnnotation> boe2 = new HashSet<>();
			for (EvalPair string : pairsMap.get(et)) {
				for (AbstractAnnotation string2 : string.gold) {
					boe1.addAll(getAnnotations(string2));
				}
				for (AbstractAnnotation string2 : string.pred) {
					boe2.addAll(getAnnotations(string2));
				}
			}
			String x = "";
	
			System.out.println("=====STRICT KAPPA=====");
			ComputeBagOfAnnotations computeStrict = new ComputeBagOfAnnotations(mainTypes,
					EEvaluationDetail.DOCUMENT_LINKED);
			for (Entry<EntityType, Double> entityType : computeStrict
					.computeKappa(
							boe1.stream().filter(a -> a.isInstanceOfDocumentLinkedAnnotation())
									.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet()),
							boe2.stream().filter(a -> a.isInstanceOfDocumentLinkedAnnotation())
									.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet()))
					.entrySet()) {
				System.out.println(
						entityType.getKey().name + "\t" + new DecimalFormat("#.##").format(entityType.getValue()));
				x +=  new DecimalFormat("#.##").format(entityType.getValue());
			}
			System.out.println("=====BO SENTENCE=====");

			ComputeBagOfSentenceAnnotations computeSentence = new ComputeBagOfSentenceAnnotations(mainTypes);
			for (Entry<EntityType, Double> entityType : computeSentence
					.compute(
							boe1.stream().filter(a -> a.isInstanceOfDocumentLinkedAnnotation())
									.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet()),
							boe2.stream().filter(a -> a.isInstanceOfDocumentLinkedAnnotation())
									.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet()))
					.entrySet()) {
				System.out.println(
						entityType.getKey().name + "\t" + new DecimalFormat("#.##").format(entityType.getValue()));
				x += " & " + new DecimalFormat("#.##").format(entityType.getValue());
			}
			ComputeBagOfAnnotations computeBOE = new ComputeBagOfAnnotations(mainTypes, EEvaluationDetail.ENTITY_TYPE);
			System.out.println("=====BOE=====");
			for (Entry<EntityType, Double> entityType : computeBOE
					.compute(
							boe1.stream().filter(a -> a.isInstanceOfDocumentLinkedAnnotation())
									.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet()),
							boe2.stream().filter(a -> a.isInstanceOfDocumentLinkedAnnotation())
									.map(a -> a.asInstanceOfDocumentLinkedAnnotation()).collect(Collectors.toSet()))
					.entrySet()) {
//				System.out.println(
//						entityType.getKey().name + "\t" + entityType.getValue().getF1(new DecimalFormat("#.##")));
				System.out.println(
						entityType.getKey().name + "\t" + new DecimalFormat("#.##").format(entityType.getValue()));
				x += " && " +new DecimalFormat("#.##").format(entityType.getValue());
			}

			System.out.println(x);
		}
//		Set<DocumentLinkedAnnotation> boe1dl = new HashSet<>();
//
//		Set<DocumentLinkedAnnotation> boe2dl = new HashSet<>();

//		Map<EntityType, Double> kappaMap = computeBOE.computeKappa(boe1dl, boe2dl);
//
//		for (Entry<EntityType, Double> entityType : kappaMap.entrySet()) {
//			System.out.println("computeKappa:" + entityType);
//		}

//		Map<EntityType, Double> skappaMap = computeSentence.computeKappa(boe1dl, boe2dl);
//
//		for (Entry<EntityType, Double> entityType : skappaMap.entrySet()) {
//			System.out.println("sentence computeKappa:" + entityType);
//		}

//		Map<EntityType, Score> similarityMap = computeBOE.computeScore(boe1, boe2);
//
//		for (Entry<EntityType, Score> entityType : similarityMap.entrySet()) {
//			System.out.println("computeBOE:" + entityType);
//		}
//
//		Map<EntityType, Score> similarityStrictMap = computeStrict.computeScore(boe1, boe2);
//
//		for (Entry<EntityType, Score> entityType : similarityStrictMap.entrySet()) {
//			System.out.println("computeStrict:" + entityType);
//		}
	}

	private static Set<? extends AbstractAnnotation> getAnnotations(AbstractAnnotation string2) {
		Set<AbstractAnnotation> aanns = new HashSet<>();

		if (!string2.isInstanceOfEntityTemplate()) {
			aanns.add(string2);
			return aanns;
		}

		aanns.add(string2.asInstanceOfEntityTemplate().getRootAnnotation());

		for (Entry<SlotType, MultiFillerSlot> abstractAnnotation : string2.asInstanceOfEntityTemplate()
				.getMultiFillerSlots().entrySet()) {

			for (AbstractAnnotation abstractAnnotation2 : abstractAnnotation.getValue().getSlotFiller()) {
				aanns.addAll(getAnnotations(abstractAnnotation2));
			}

		}

		for (Entry<SlotType, SingleFillerSlot> abstractAnnotation : string2.asInstanceOfEntityTemplate()
				.getSingleFillerSlots().entrySet()) {

			AbstractAnnotation a = abstractAnnotation.getValue().getSlotFiller();
			if (a != null)
				aanns.addAll(getAnnotations(a));

		}

		return aanns;
	}

	private static List<AbstractAnnotation> getDeliveryMethod(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.addAll(new Injury(abstractAnnotation2.getInjury()).getDeliveryMethods());

				for (AbstractAnnotation abstractAnnotation3 : abstractAnnotation2.getTreatments()) {

					l.add(new Treatment(abstractAnnotation3.asInstanceOfEntityTemplate()).getDeliveryMethod());
				}
			}
		}

		return new ArrayList<>(l.stream().filter(a -> a != null).collect(Collectors.toList()));
	}

	private static List<AbstractAnnotation> getAnaesthetic(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.addAll(new Injury(abstractAnnotation2.getInjury()).getAnaesthetics());
			}
		}

		return new ArrayList<>(l.stream().filter(a -> a != null).collect(Collectors.toList()));
	}

	private static List<AbstractAnnotation> getInjuryLocation(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.add(new Injury(abstractAnnotation2.getInjury()).getInjuryLocation());
			}
		}

		return new ArrayList<>(l.stream().filter(a -> a != null).collect(Collectors.toList()));
	}

	private static List<AbstractAnnotation> getInjuryDevice(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.add(new Injury(abstractAnnotation2.getInjury()).getInjuryDevice());
			}
		}

		return new ArrayList<>(l.stream().filter(a -> a != null).collect(Collectors.toList()));
	}

	private static List<AbstractAnnotation> getOrgModel(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.add(abstractAnnotation2.getOrganismModel());
			}
		}

		return new ArrayList<>(l);
	}

	private static List<AbstractAnnotation> getInvestigationMethod(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());
			l.add(r.getInvestigationMethod());
		}

		return new ArrayList<>(l);
	}

	private static List<AbstractAnnotation> getTrend(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());
			l.add(r.getTrend());
		}

		return new ArrayList<>(l);
	}

	private static List<AbstractAnnotation> getExperimentalGroup(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.add(abstractAnnotation2.get());
			}
		}

		return new ArrayList<>(l);
	}

	private static List<AbstractAnnotation> getTreatment(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.addAll(abstractAnnotation2.getTreatments());
			}
		}

		return new ArrayList<>(l);
	}

	private static List<AbstractAnnotation> getInjuryModel(List<AbstractAnnotation> annotations) {
		Set<AbstractAnnotation> l = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Result r = new Result(abstractAnnotation.asInstanceOfEntityTemplate());

			for (DefinedExperimentalGroup abstractAnnotation2 : r.getDefinedExperimentalGroups()) {
				l.add(abstractAnnotation2.getInjury());
			}
		}

		return new ArrayList<>(l);
	}

	private static Set<AbstractAnnotation> exampleRandomBOEData() {
		return exampleRandomBOEData(new Random().nextLong());
	}

	private static Set<AbstractAnnotation> exampleRandomBOEData(long seed) {
		Set<AbstractAnnotation> boe1 = new HashSet<>();
		Random random = new Random(seed);
		for (EntityType entityType : EntityType.getEntityTypes()) {
			if (random.nextBoolean())
				boe1.add(AnnotationBuilder.toAnnotation(entityType));
		}
		return boe1;
	}

	public ComputeIAACorpus() {

	}

	public void computeIAA() {

	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

}
