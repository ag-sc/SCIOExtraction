package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.results;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.eval.CartesianEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.tools.AutomatedSectionifcation;

public class ResultSentenceDetection {

	private final File instanceDirectory;

	private InstanceProvider instanceProvider;
	private List<Instance> trainingInstances;
	private List<Instance> devInstances;
	private List<Instance> testInstances;

	private String modelName;
	private File outputFile;

	private final String rand;

	public static void main(String[] args) {
		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("Result")).build();
		new ResultSentenceDetection();
	}

	public ResultSentenceDetection() {
		this.instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);


		CartesianEvaluator.MAXIMUM_PERMUTATION_SIZE = 4;

		SlotFillingExplorer.MAX_NUMBER_OF_ANNOTATIONS = 8;

		rand = String.valueOf(new Random().nextInt(100000));

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 80;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		trainingInstances = instanceProvider.getRedistributedTrainingInstances();
		devInstances = instanceProvider.getRedistributedDevelopmentInstances();
		testInstances = instanceProvider.getRedistributedTestInstances();

		for (Instance instance : trainingInstances) {
			AutomatedSectionifcation sectionification = AutomatedSectionifcation.getInstance(instance);
			for (AbstractAnnotation result : instance.getGoldAnnotations().getAnnotations()) {
				if (!result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasTrend"))
						.containsSlotFiller())
					continue;

				printRec(sectionification, result.asInstanceOfEntityTemplate()
						.getSingleFillerSlot(SlotType.get("hasTrend")).getSlotFiller());

//				System.out.println(result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasTrend"))
//						.getSlotFiller().toPrettyString());

//				System.out.println(result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasTrend"))
//						.getSlotFiller().asInstanceOfDocumentLinkedAnnotation().getSentenceOfAnnotation());

//				System.out.println(result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasInvestigationMethod")));

			}

		}

	}

	public void printRec(AutomatedSectionifcation sectionification, AbstractAnnotation et) {
		if (et.isInstanceOfDocumentLinkedAnnotation()) {
			System.out
					.println(sectionification.getSection(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex()));
			System.out.println(et.asInstanceOfDocumentLinkedAnnotation().getSentenceIndex());
			return;
		}

		for (Entry<SlotType, SingleFillerSlot> instance2 : et.asInstanceOfEntityTemplate().getSingleFillerSlots()
				.entrySet()) {
			printRec(sectionification,instance2.getValue().getSlotFiller());

		}
	}

}
