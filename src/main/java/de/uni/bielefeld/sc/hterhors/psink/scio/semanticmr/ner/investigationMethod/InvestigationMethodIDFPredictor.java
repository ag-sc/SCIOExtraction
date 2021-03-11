package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.investigationMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result_sentences.AbstractIDFPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.Stats;

public class InvestigationMethodIDFPredictor extends AbstractIDFPredictor {

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("InvestigationMethod"))
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setSeed(1000L).setTestProportion(20).setCorpusSizeFraction(1F).build();
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;
		InstanceProvider.removeInstancesWithToManyAnnotations = false;

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.investigationMethod),
				corpusDistributor);
		
		int count=0;
		for (Instance instance : instanceProvider.getInstances()) {
			AutomatedSectionifcation.getInstance(instance);
			count+=instance.getGoldAnnotations().getAnnotations().size();
		}
		System.out.println(count);
		System.out.println((double)count / instanceProvider.getInstances().size()
				);
		Stats.computeNormedVar(instanceProvider.getInstances(), SCIOEntityTypes.investigationMethod);

		
		InvestigationMethodIDFPredictor investigationPredictor = new InvestigationMethodIDFPredictor();
//		investigationPredictor.setAnnotationStopwords(
//				Arrays.asList("either", "number", "group", "groups", "numbers", "not", "did", "spinal", "cord"));
//		investigationPredictor.setSentenceStopWords(Arrays.asList("arrow", "asterisk", "*", "bar"));
//		investigationPredictor.setRemoveEntityTypes(
//				Arrays.asList(EntityType.get("InvestigationMethod"), EntityType.get("FunctionalTest")));
		investigationPredictor.setEnableUniGram(true);
		investigationPredictor.setEnableBiGram(false);
		investigationPredictor.setRestrictToSections(Arrays.asList(ESection.RESULTS));
		investigationPredictor.setLocalNormalizing(true);
		investigationPredictor.setEnableStemming(false);
		investigationPredictor.setIncludeNameContains(false);
		investigationPredictor.setMinTokenLength(2);
		investigationPredictor.setEnableLowerCasing(true);
		investigationPredictor.setTrehsold(0);
		investigationPredictor.setMinAnnotationsPerSentence(0);
		investigationPredictor.setMaxAnnotationsPerSentence(2);

		investigationPredictor.train(instanceProvider.getTrainingInstances());

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictions = investigationPredictor
				.predictInstances(instanceProvider.getTestInstances());

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> groundTruth = investigationPredictor
				.getGroundTruthAnnotations(instanceProvider.getTestInstances());
		Score s = investigationPredictor.evaluate(groundTruth, predictions);

		new File("idf/invm/" + investigationPredictor.toString().hashCode() + "/").mkdirs();

		investigationPredictor.printErrors(
				"idf/invm/" + investigationPredictor.toString().hashCode() + "/"
						+ investigationPredictor.toString().hashCode() + "_investigationMethod_errors.csv",
				groundTruth, predictions);
		investigationPredictor.printIDFs("idf/invm/" + investigationPredictor.toString().hashCode() + "/"
				+ investigationPredictor.toString().hashCode() + "_investigationMethod_idfs.csv");
		String info = investigationPredictor.printInfo("idf/invm/" + investigationPredictor.toString().hashCode() + "/"
				+ investigationPredictor.toString().hashCode() + "_investigationMethod_info.csv", s);

		System.out.println(info);

	}
//	score	Score [getF1()=0.092, getPrecision()=0.050, getRecall()=0.582, tp=99, fp=1893, fn=71, tn=0]

	protected List<DocumentLinkedAnnotation> extractData(Instance trainInstance) {

		List<DocumentLinkedAnnotation> ims = new ArrayList<>();
		for (AbstractAnnotation a : trainInstance.getGoldAnnotations().getAbstractAnnotations()) {

			if (a.isInstanceOfDocumentLinkedAnnotation()) {
				ims.add(a.asInstanceOfDocumentLinkedAnnotation());
			} else {

				for (EntityTemplate result : trainInstance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
					Result r = new Result(result);
					EntityTemplate invM = r.getInvestigationMethod();
					if (invM != null)
						if (invM.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation())
							ims.add(invM.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation());

				}
			}
		}
		return ims;

	}

}
