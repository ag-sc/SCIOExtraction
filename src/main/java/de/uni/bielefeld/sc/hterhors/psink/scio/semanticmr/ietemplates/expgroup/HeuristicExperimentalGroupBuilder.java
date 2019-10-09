package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.examples.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.expgroup.specifications.ExperimentalGroupSpecifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.groupname.GroupNameNERLPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.injury.InjurySlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.orgmodel.OrgModelSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.treatment.TreatmentSlotFillingPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.normalizer.AgeNormalization;

public class HeuristicExperimentalGroupBuilder {

	public static void main(String[] args) {

		new HeuristicExperimentalGroupBuilder();

	}

	private final File instanceDirectory = new File(
			"src/main/resources/slotfilling/experimental_group/corpus/instances/");

	public HeuristicExperimentalGroupBuilder() {

		AbstractCorpusDistributor corpusDistributor = new OriginalCorpusDistributor.Builder()
				.setCorpusSizeFraction(0.25F).build();

		SystemScope scope = SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(ExperimentalGroupSpecifications.systemsScope).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization()).build();

		InstanceProvider.maxNumberOfAnnotations = 8;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
				.map(t -> t.getName()).collect(Collectors.toList());

		List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream().map(t -> t.getName())
				.collect(Collectors.toList());

		String rand = "392547881";// String.valueOf(new Random().nextInt());

		/**
		 * Predict GroupNames.
		 */

		GroupNameNERLPredictor groupNamePredictor = new GroupNameNERLPredictor("GroupName" + rand, scope,
				trainingInstanceNames, developInstanceNames, testInstanceNames);

		groupNamePredictor.trainOrLoadModel();

		Map<String, Set<AbstractAnnotation>> groupNameAnnotations = groupNamePredictor.predictBatchInstances();

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

		print(groupNameAnnotations);
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
