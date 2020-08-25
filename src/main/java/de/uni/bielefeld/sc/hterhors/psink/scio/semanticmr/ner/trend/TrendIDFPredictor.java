package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.trend;

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
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result_sentences.AbstractIDFPredictor;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Result;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.wrapper.Trend;

public class TrendIDFPredictor extends AbstractIDFPredictor {

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("Trend"))
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
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.trend), corpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {
			AutomatedSectionifcation.getInstance(instance);
		}

		TrendIDFPredictor trendPredictor = new TrendIDFPredictor();
		trendPredictor.setAnnotationStopwords(
				Arrays.asList("rats", "either", "number", "group", "groups", "numbers", "treatment", "respectively"));
		trendPredictor.setRemoveEntityTypes(
				Arrays.asList(EntityType.get("AlphaSignificanceNiveau"), EntityType.get("RepeatedMeasureTrend")));
		trendPredictor.setEnableUniGram(true);
		trendPredictor.setSentenceStopWords(Arrays.asList("arrow", "asterisk", "*", "bar"));
		trendPredictor.setEnableBiGram(false);
		trendPredictor.setRestrictToSections(Arrays.asList(ESection.RESULTS));
		trendPredictor.setLocalNormalizing(true);
		trendPredictor.setEnableStemming(false);
		trendPredictor.setIncludeNameContains(false);
		trendPredictor.setMinTokenLength(2);
		trendPredictor.setEnableLowerCasing(false);
		trendPredictor.setTrehsold(1);
		trendPredictor.setMaxAnnotationsPerSentence(3);
		trendPredictor.setMinAnnotationsPerSentence(2);
		trendPredictor.train(instanceProvider.getTrainingInstances());

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> predictions = trendPredictor
				.predictInstances(instanceProvider.getTestInstances());

		Map<Instance, Map<Integer, Set<DocumentLinkedAnnotation>>> groundTruth = trendPredictor
				.getGroundTruthAnnotations(instanceProvider.getTestInstances());
		Score s = trendPredictor.evaluate(groundTruth, predictions);

		new File("idf/trend/" + trendPredictor.toString().hashCode() + "/").mkdirs();

		trendPredictor.printErrors("idf/trend/" + trendPredictor.toString().hashCode() + "/"
				+ trendPredictor.toString().hashCode() + "_trend_errors.csv", groundTruth, predictions);
		trendPredictor.printIDFs("idf/trend/" + trendPredictor.toString().hashCode() + "/"
				+ trendPredictor.toString().hashCode() + "_trend_idfs.csv");
		String info = trendPredictor.printInfo("idf/trend/" + trendPredictor.toString().hashCode() + "/"
				+ trendPredictor.toString().hashCode() + "_trend_info.csv", s);

		System.out.println(info);
	}
//	score	Score [getF1()=0.219, getPrecision()=0.137, getRecall()=0.546, tp=231, fp=1456, fn=192, tn=0]
//	score	Score [getF1()=0.293, getPrecision()=0.210, getRecall()=0.489, tp=207, fp=781, fn=216, tn=0]

	protected List<DocumentLinkedAnnotation> extractData(Instance trainInstance) {

		List<DocumentLinkedAnnotation> ts = new ArrayList<>();

		for (AbstractAnnotation a : trainInstance.getGoldAnnotations().getAbstractAnnotations()) {
			if (a.isInstanceOfEntityTemplate()) {

				Result r = new Result(a.asInstanceOfEntityTemplate().asInstanceOfEntityTemplate());
				EntityTemplate trend = r.getTrend();

				if (trend == null)
					continue;

				Trend t = new Trend(trend);

				DocumentLinkedAnnotation root = t.getRootAnntoationAsDocumentLinkedAnnotation();

				if (root != null)
					ts.add(root);
				DocumentLinkedAnnotation diff = t.getDifferenceAsDocumentLinkedAnnotation();

				if (diff != null)
					ts.add(diff);

				SingleFillerSlot sigSlot = trend.getSingleFillerSlot(SCIOSlotTypes.hasSignificance);

				if (!sigSlot.containsSlotFiller()) {
					continue;
				}

				EntityTemplate significance = sigSlot.getSlotFiller().asInstanceOfEntityTemplate();

				DocumentLinkedAnnotation sig = significance.getRootAnnotation() != null
						&& significance.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()
								? significance.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation()
								: null;

				if (sig != null)
					ts.add(sig);

				SingleFillerSlot alphaSlot = significance.getSingleFillerSlot(SCIOSlotTypes.hasAlphaSignificanceNiveau);

				if (alphaSlot.containsSlotFiller()) {

					ts.add(alphaSlot.getSlotFiller().asInstanceOfDocumentLinkedAnnotation());

				}
				SingleFillerSlot pvalueSlot = significance.getSingleFillerSlot(SCIOSlotTypes.hasPValue);

				if (pvalueSlot.containsSlotFiller()
						&& pvalueSlot.getSlotFiller().isInstanceOfDocumentLinkedAnnotation()) {
					ts.add(pvalueSlot.getSlotFiller().asInstanceOfDocumentLinkedAnnotation());

				}

			} else {
				ts.add(a.asInstanceOfDocumentLinkedAnnotation());
			}

		}
		return ts;
	}
}
