package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.investigationMethod;

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
		for (Instance instance : instanceProvider.getInstances()) {
			AutomatedSectionifcation.getInstance(instance);
		}

		InvestigationMethodIDFPredictor investigationPredictor = new InvestigationMethodIDFPredictor();
		investigationPredictor.setAnnotationStopwords(Arrays.asList("either", "number", "group", "groups", "numbers"));
		investigationPredictor.setSentenceStopWords(Arrays.asList("arrow", "asterisk", "*", "bar"));
		investigationPredictor.setRemoveEntityTypes(Arrays.asList(EntityType.get("InvestigationMethod")));
		investigationPredictor.setEnableUniGram(true);
		investigationPredictor.setEnableBiGram(false);
		investigationPredictor.setRestrictToSections(Arrays.asList(ESection.RESULTS));
		investigationPredictor.setLocalNormalizing(true);
		investigationPredictor.setEnableStemming(true);
		investigationPredictor.setEnableLowerCasing(true);
		investigationPredictor.train(instanceProvider.getTrainingInstances());
		investigationPredictor.printIDFs("idf/investigationMethod_idf.csv");

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictions = investigationPredictor
				.predictInstances(instanceProvider.getTestInstances());

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> groundTruth = investigationPredictor
				.getGroundTruthAnnotations(instanceProvider.getTestInstances());
		Score s = investigationPredictor.evaluate(groundTruth, predictions);
		System.out.println(s);
	}

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
