package de.uni.bielefeld.sc.hterhors.psink.scio.playground;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;

public class Heuristics {

	public static void main(String[] args) {

		new Heuristics();

	}

	private final File instanceDirectory = new File("src/main/resources/slotfilling/result/corpus/instances/");

	public Heuristics() {

		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder().setCorpusSizeFraction(1F)
				.build();

		SystemScope scope = SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("ExperimentalGroup")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build();

		for (EntityType et : EntityType.get("Treatment").getTransitiveClosureSubEntityTypes()) {

			System.out.println(et);
			et.getSlots().forEach(System.out::println);

			System.out.println();
			System.out.println();

		}

		System.exit(1);

		InstanceProvider.maxNumberOfAnnotations = 50;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		String rand = String.valueOf(new Random().nextInt());

		trainingInstanceNames.forEach(System.out::println);

		Map<String, Integer> countExpGroups = new HashMap<>();
		Map<String, Integer> countTreatments = new HashMap<>();
		Map<String, Integer> countInjuries = new HashMap<>();
		Map<String, Integer> countOrganismModels = new HashMap<>();

		instanceProvider.getRedistributedTrainingInstances().stream().forEach(i -> {
			countExpGroups.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
							.map(gresult -> Arrays.asList(
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasTargetGroup")).getSlotFiller(),
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasReferenceGroup")).getSlotFiller()))
							.filter(zzz -> zzz != null).flatMap(a -> a.stream()).distinct().count()));

			String s = i.getGoldAnnotations().getAnnotations().stream()
					.map(gresult -> Arrays.asList(
							gresult.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasTargetGroup"))
									.getSlotFiller(),
							gresult.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasReferenceGroup"))
									.getSlotFiller()))
					.filter(zzz -> zzz != null).flatMap(a -> a.stream()).filter(zzz -> zzz != null)
					.map(b -> b.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel)
							.getSlotFiller())
					.collect(Collectors.toList()).toString();

			countInjuries.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
							.map(gresult -> Arrays.asList(
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasTargetGroup")).getSlotFiller(),
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasReferenceGroup")).getSlotFiller()))
							.filter(zzz -> zzz != null).flatMap(a -> a.stream()).filter(zzz -> zzz != null)
							.map(b -> b.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasInjuryModel)
									.getSlotFiller())
							.filter(zzz -> zzz != null).distinct().count()));

			countOrganismModels.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
							.map(gresult -> Arrays.asList(
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasTargetGroup")).getSlotFiller(),
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasReferenceGroup")).getSlotFiller()))
							.filter(zzz -> zzz != null).flatMap(a -> a.stream()).filter(zzz -> zzz != null)
							.map(b -> b.asInstanceOfEntityTemplate().getSingleFillerSlot(SCIOSlotTypes.hasOrganismModel)
									.getSlotFiller())
							.filter(zzz -> zzz != null).distinct().count()));

			countTreatments.put(i.getName(),
					Integer.valueOf((int) i.getGoldAnnotations().getAnnotations().stream()
							.map(gresult -> Arrays.asList(
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasTargetGroup")).getSlotFiller(),
									gresult.asInstanceOfEntityTemplate()
											.getSingleFillerSlot(SlotType.get("hasReferenceGroup")).getSlotFiller()))
							.filter(zzz -> zzz != null).flatMap(a -> a.stream()).filter(zzz -> zzz != null)
							.flatMap(b -> b.asInstanceOfEntityTemplate()
									.getMultiFillerSlot(SCIOSlotTypes.hasTreatmentType).getSlotFiller().stream())
							.filter(zzz -> zzz != null).distinct().count()));

		});

//		countExpGroups.entrySet().forEach(System.out::println);
//		countInjuries.entrySet().forEach(System.out::println);
//		countOrganismModels.entrySet().forEach(System.out::println);
//		countTreatments.entrySet().forEach(System.out::println);

		int errors = 0;
		int overestimated = 0;
		int underestimated = 0;
		for (String docName : countExpGroups.keySet()) {

			int estimated = (countInjuries.get(docName) * countOrganismModels.get(docName)
					* countTreatments.get(docName));

			int error = estimated - countExpGroups.get(docName);

			if (error < 0) {
				underestimated += error;
			} else {
				overestimated += error;
			}

			errors += Math.abs(error);
			System.out.println(docName + "\testimated/real # of Exp. Groups\t" + countInjuries.get(docName) + "-"
					+ countOrganismModels.get(docName) + "-" + countTreatments.get(docName) + " = " + estimated + "/"
					+ countExpGroups.get(docName) + ", error = " + error);

		}
		System.out.println("Under estimations: " + underestimated);
		System.out.println("Over estimations: " + overestimated);

		System.out.println("Total number of errors made: " + errors);
		/**
		 * Predict GroupNames.
		 */

//		GroupNameNERLPredictor groupNamePredictor = new GroupNameNERLPredictor("GroupName" + rand, scope,
//				trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		groupNamePredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> groupNameAnnotations = groupNamePredictor.predictBatchInstances();

		/**
		 * Predict OrganismModels
		 */

//		OrgModelSlotFillingPredictor organismModelPredictor = new OrgModelSlotFillingPredictor("OrganismModel" + rand,
//				scope, trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		organismModelPredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = organismModelPredictor.predictAllInstances();
//
//		/**
//		 * Predict Injuries
//		 */
//
//		InjurySlotFillingPredictor injuryPredictor = new InjurySlotFillingPredictor("InjuryModel" + rand, scope,
//				trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		injuryPredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> injuryAnnotations = injuryPredictor.predictAllInstances();

		/**
		 * Predict Treatments
		 */

//		TreatmentSlotFillingPredictor treatmentPredictor = new TreatmentSlotFillingPredictor("Treatment" + rand, scope,
//				trainingInstanceNames, developInstanceNames, testInstanceNames);
//
//		treatmentPredictor.trainOrLoadModel();
//
//		Map<String, Set<AbstractAnnotation>> treatmentAnnotations = treatmentPredictor.predictAllInstances();

//		print(groupNameAnnotations);
//		print(organismModelAnnotations);
//		print(injuryAnnotations);
//		print(treatmentAnnotations);
	}

	public void print(Map<String, Set<AbstractAnnotation>> annotations) {
		for (Entry<String, Set<AbstractAnnotation>> e : annotations.entrySet()) {
			System.out.println(e.getKey());
			e.getValue().forEach(z -> System.out.println(z.toPrettyString()));
		}
		System.out.println("---------------------------------------");
	}

}
