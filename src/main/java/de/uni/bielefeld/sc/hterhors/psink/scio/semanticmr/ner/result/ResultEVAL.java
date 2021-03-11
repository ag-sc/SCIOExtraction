package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.result;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.OriginalCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score.EScoreType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.Instance.GoldModificationRule;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.eval.EEvaluationDetail;
import de.hterhors.semanticmr.eval.NerlaEvaluator;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JSONNerlaReader;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.hterhors.semanticmr.nerla.annotation.RegularExpressionNerlAnnotator;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation;
import de.hterhors.semanticmr.tools.AutomatedSectionifcation.ESection;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.anaesthetic.AnaestheticPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.delivery_method.nerla.DeliveryMethodPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.nerla.InjuryPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury_device.InjuryDevicePattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.investigation_method.nerla.InvestigationMethodPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.nerla.OrganismModelPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.result.nerla.ResultPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.nerla.TreatmentPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.trend.nerla.TrendPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.nerla.VertebralAreaPattern;

/**
 * Example of how to perform named entity recognition and linking.
 * 
 * @author hterhors Final Score: Score [ getF1()=0.170, getPrecision()=0.107,
 *         getRecall()=0.404, tp=72, fp=599, fn=106, tn=0] CRFStatistics
 *         [context=Train, getTotalDuration()=1699948] CRFStatistics
 *         [context=Test, getTotalDuration()=302525] modelName:
 *         GroupName_895041394
 */
public class ResultEVAL {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * Start the named entity recognition and linking procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new ResultEVAL();
	}

	public ResultEVAL() {
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadNERDataStructureReader("Result")).apply()
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				//
				.registerNormalizationFunction(new DosageNormalization())
				.registerNormalizationFunction(new DurationNormalization())
				.registerNormalizationFunction(new VolumeNormalization())
				.registerNormalizationFunction(new ForceNormalization())
				.registerNormalizationFunction(new ThicknessNormalization())
				.registerNormalizationFunction(new PressureNormalization())
				.registerNormalizationFunction(new LengthNormalization())
				.registerNormalizationFunction(new DistanceNormalization())

				/**
				 * Finally, we build the systems scope.
				 */
				.build();
		String modelName = "RESULT_" + new Random().nextInt();
		log.info("modelName: " + modelName);

		AbstractCorpusDistributor originalCorpusDistributor = new OriginalCorpusDistributor.Builder()
				.setCorpusSizeFraction(1F).build();

		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.maxNumberOfAnnotations = 300;

		Collection<GoldModificationRule> modfic = new ArrayList<Instance.GoldModificationRule>();

//		modfic.add(new GoldModificationRule() {
//
//			@Override
//			public AbstractAnnotation modify(AbstractAnnotation currentAnnotation) {
//
//				if (AutomatedSectionifcation
//						.getInstance(currentAnnotation.asInstanceOfDocumentLinkedAnnotation().document)
//						.getSection(currentAnnotation.asInstanceOfDocumentLinkedAnnotation()) != ESection.ABSTRACT)
//
//					return null;
//
//				return currentAnnotation;
//			}
//		});

		InstanceProvider instanceProvider = new InstanceProvider(
				NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result),
				originalCorpusDistributor, modfic);

		Map<Instance, Set<AbstractAnnotation>> goldAnnotations = new HashMap<>();
		Map<Instance, Set<AbstractAnnotation>> predictAnnotations = new HashMap<>();

		NerlaEvaluator evalDocLinked = new NerlaEvaluator(EEvaluationDetail.DOCUMENT_LINKED);
		NerlaEvaluator evalLiteral = new NerlaEvaluator(EEvaluationDetail.LITERAL);
		NerlaEvaluator evalEntityType = new NerlaEvaluator(EEvaluationDetail.ENTITY_TYPE);

		Score sTokenAll = new Score();
		Score sLiteralAll = new Score();
		Score sTypeAll = new Score();

//		JsonNerlaIO io = new JsonNerlaIO(true);

		File nerlaCRFDiractory = new File("data/nerla/crf");
		File nerlaHeuristicDiractory = new File("data/nerla/heuristics");
//		nerlaALLDiractory.mkdirs();
		JSONNerlaReader nerlaJSONReaderC = new JSONNerlaReader(nerlaCRFDiractory,
				new HashSet<>(instanceProvider.getInstances()));
		JSONNerlaReader nerlaJSONReaderH = new JSONNerlaReader(nerlaHeuristicDiractory,
				new HashSet<>(instanceProvider.getInstances()));
		for (Instance instance : instanceProvider.getInstances()) {
			predictAnnotations.put(instance, new HashSet<>());
			predictAnnotations.get(instance).addAll(nerlaJSONReaderC.getForInstance(instance).stream()
					
					.filter(a -> AutomatedSectionifcation.getInstance(a.asInstanceOfDocumentLinkedAnnotation().document)
							.getSection(a.asInstanceOfDocumentLinkedAnnotation()) == ESection.ABSTRACT)
					
					.filter(a -> a.getEntityType() != EntityType.get("Length"))
					.filter(a -> a.getEntityType() != EntityType.get("Pressure"))
					.filter(a -> a.getEntityType() != EntityType.get("StandardDeviation"))
					.filter(a -> a.getEntityType() != EntityType.get("StandardError"))
					.filter(a -> a.getEntityType() != EntityType.get("MeanValue"))
					.filter(a -> a.getEntityType() != EntityType.get("Temperature"))
					.filter(a -> a.getEntityType() != EntityType.get("Thickness"))
					.filter(a -> a.getEntityType() != EntityType.get("Volume"))
					.filter(a -> a.getEntityType() != EntityType.get("GroupName"))
					.filter(a -> a.getEntityType() != EntityType.get("GroupNumber"))
					.filter(a -> a.getEntityType() != EntityType.get("NNumber"))
//					.filter(a -> !a.getEntityType().isLiteral)
//					
					.collect(Collectors.toList()));
			predictAnnotations.get(instance).addAll(nerlaJSONReaderH.getForInstance(instance).stream()

					.filter(a -> AutomatedSectionifcation.getInstance(a.asInstanceOfDocumentLinkedAnnotation().document)
							.getSection(a.asInstanceOfDocumentLinkedAnnotation()) == ESection.ABSTRACT)

					.filter(a -> a.getEntityType() != EntityType.get("Length"))
					.filter(a -> a.getEntityType() != EntityType.get("Pressure"))
					.filter(a -> a.getEntityType() != EntityType.get("StandardDeviation"))
					.filter(a -> a.getEntityType() != EntityType.get("StandardError"))
					.filter(a -> a.getEntityType() != EntityType.get("MeanValue"))
					.filter(a -> a.getEntityType() != EntityType.get("Temperature"))
					.filter(a -> a.getEntityType() != EntityType.get("Thickness"))
					.filter(a -> a.getEntityType() != EntityType.get("Volume"))
					.filter(a -> a.getEntityType() != EntityType.get("GroupName"))
					.filter(a -> a.getEntityType() != EntityType.get("GroupNumber"))
					.filter(a -> a.getEntityType() != EntityType.get("NNumber"))
//					.filter(a -> !a.getEntityType().isLiteral)
//					
					.collect(Collectors.toList()));

			if (predictAnnotations.get(instance).isEmpty())
				continue;

//			log.info("Annotate doc: " + instance.getName());
//			for (BasicRegExPattern basicRegExPattern : pattern) {
//				RegularExpressionNerlAnnotator annotator = new RegularExpressionNerlAnnotator(basicRegExPattern);
//				for (Set<DocumentLinkedAnnotation> basicRegExPattern2 : annotator.annotate(instance.getDocument())
//						.values()) {
//					predictAnnotations.get(instance).addAll(basicRegExPattern2);
//				}
//			}
//			try {
//
//				List<JsonEntityAnnotationWrapper> wrappedAnnotation = predictAnnotations.get(instance).stream()
//						.map(d -> new JsonEntityAnnotationWrapper(d.asInstanceOfDocumentLinkedAnnotation()))
//						.collect(Collectors.toList());
//
//				io.writeNerlas(new File(nerlaALLDiractory, instance.getName() + ".nerla.json"), wrappedAnnotation);
//
//			} catch (IOException e) {
//				e.printStackTrace();
//			}

			goldAnnotations.put(instance, new HashSet<>());
			goldAnnotations.get(instance).addAll(instance.getGoldAnnotations().getAnnotations().stream()
					.filter(a -> AutomatedSectionifcation.getInstance(a.asInstanceOfDocumentLinkedAnnotation().document)
							.getSection(a.asInstanceOfDocumentLinkedAnnotation()) == ESection.ABSTRACT)
					.filter(a -> a.getEntityType() != EntityType.get("StandardDeviation"))
					.filter(a -> a.getEntityType() != EntityType.get("StandardError"))
					.filter(a -> a.getEntityType() != EntityType.get("MeanValue"))
					.filter(a -> a.getEntityType() != EntityType.get("Temperature"))
					.filter(a -> a.getEntityType() != EntityType.get("Length"))
					.filter(a -> a.getEntityType() != EntityType.get("Pressure"))
					.filter(a -> a.getEntityType() != EntityType.get("Thickness"))
					.filter(a -> a.getEntityType() != EntityType.get("Volume"))
					.filter(a -> a.getEntityType() != EntityType.get("GroupName"))
					.filter(a -> a.getEntityType() != EntityType.get("GroupNumber"))
					.filter(a -> a.getEntityType() != EntityType.get("NNumber"))
//					.filter(a -> !a.getEntityType().isLiteral)
//					
					.collect(Collectors.toList()));

			System.out.println(instance.getName());
//			System.out.println(goldAnnotations.get(instance));
//			System.out.println(predictAnnotations.get(instance));
			Score sToken = evalDocLinked.scoreMultiValues(goldAnnotations.get(instance),
					predictAnnotations.get(instance), EScoreType.MICRO);
			System.out.println(sToken);
			sTokenAll.add(sToken);

			Score sLiteral = evalLiteral.scoreMultiValues(goldAnnotations.get(instance),
					predictAnnotations.get(instance), EScoreType.MICRO);
			sLiteralAll.add(sLiteral);
			Score sType = evalEntityType.scoreMultiValues(goldAnnotations.get(instance),
					predictAnnotations.get(instance), EScoreType.MICRO);
			sTypeAll.add(sType);
		}
		System.out.println("-------------");
		System.out.println(sTokenAll.toTSVString());
		System.out.println(sLiteralAll.toTSVString());
		System.out.println(sTypeAll.toTSVString());

	}

}
