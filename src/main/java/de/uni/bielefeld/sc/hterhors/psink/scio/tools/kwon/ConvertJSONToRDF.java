package de.uni.bielefeld.sc.hterhors.psink.scio.tools.kwon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.set.SynchronizedSet;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.slot_filling.BuildCorpusFromRawData;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import weka.gui.ETable;

public class ConvertJSONToRDF {

//	OEC
//	OEC+CELL
//	OEC+SUBSTANCE

//	Liste mit pubulcations + treatment(s) an Nicole
//	judgements 

	/*
	 * Inbvestigationmethod, Trend ,Judgement ;
	 */
	public static Map<EntityType, Map<EntityType, EntityType>> judgement = new HashMap<>();

	public static void main(String[] args) throws IOException {
		SystemScope.Builder.getScopeHandler().addScopeSpecification(BuildCorpusFromRawData.dataStructureReader).build();

		Files.readAllLines(new File("judgements.csv").toPath()).stream().skip(1).forEach(a -> addToJudgement(a));

		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 1000;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		File instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		List<EntityTemplate> dataPoints = new ArrayList<>();

		instanceProvider.getInstances().stream()
				.forEach(a -> dataPoints.add(toPublication(a.getName(), a.getGoldAnnotations().getAnnotations())));

		new ConvertToRDF(new File("OEC.n-triples"), dataPoints);

		System.exit(1);

//		EntityType.get("FunctionalTest").getTransitiveClosureSubEntityTypes().stream()
//				.filter(a -> !EntityType.get("MotorTest").getTransitiveClosureSubEntityTypes().contains(a))
//				.filter(a -> !EntityType.get("GaitTest").getTransitiveClosureSubEntityTypes().contains(a))
//				.filter(a -> !EntityType.get("LocomotorTest").getTransitiveClosureSubEntityTypes().contains(a))
//				.filter(a -> !EntityType.get("NeurologicScalesTest").getTransitiveClosureSubEntityTypes().contains(a))
//				.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + "> "));
//System.out.println();
//		EntityType.get("NeurologicScalesTest").getTransitiveClosureSubEntityTypes()
//				.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + "> "));
//		EntityType.get("MotorTest").getTransitiveClosureSubEntityTypes()
//				.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + "> "));
//		EntityType.get("GaitTest").getTransitiveClosureSubEntityTypes()
//				.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + "> "));
//		EntityType.get("LocomotorTest").getTransitiveClosureSubEntityTypes()
//				.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + "> "));
//		EntityType.get("PartialTransection").getTransitiveClosureSubEntityTypes()
//				.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + ">"));
//		EntityType.get("Clip").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+">"));
//		EntityType.get("FunctionalTest").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+"> "));
//		EntityType.get("NonFunctionalTest").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+">\"));
//		EntityType.get("MouseSpecies").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+"> "));
//		EntityType.get("RatSpecies").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+"> "));
//		EntityType.get("MonkeySpecies").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+"> "));
//		EntityType.get("AnimalSpecies").getTransitiveClosureSubEntityTypes().stream()
//				.filter(a -> !EntityType.get("MouseSpecies").getTransitiveClosureSubEntityTypes().contains(a))
//				.filter(a -> !EntityType.get("RatSpecies").getTransitiveClosureSubEntityTypes().contains(a))
//				.filter(a -> !EntityType.get("MonkeySpecies").getTransitiveClosureSubEntityTypes().contains(a))
//				.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + "> "));

//		System.exit(1);

		for (EntityTemplate publication : dataPoints) {

			boolean containsNonFuc = false;
			boolean containsFuc = false;
			Set<String> nonfuncTests = new HashSet<>();
			Set<String> funcTests = new HashSet<>();
			Set<String> trends = new HashSet<>();
			for (AbstractAnnotation experiment : publication.getMultiFillerSlot(SlotType.get("describes"))
					.getSlotFiller()) {

				Set<AbstractAnnotation> results = experiment.asInstanceOfEntityTemplate()
						.getMultiFillerSlot(SlotType.get("hasResult")).getSlotFiller();

				for (AbstractAnnotation result : results) {
					if (result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasInvestigationMethod"))
							.containsSlotFiller()) {
						EntityType investMethod = result.asInstanceOfEntityTemplate()
								.getSingleFillerSlot(SlotType.get("hasInvestigationMethod")).getSlotFiller()
								.getEntityType();
						containsFuc |= EntityType.get("FunctionalTest").isSuperEntityOf(investMethod);
						containsNonFuc |= EntityType.get("NonFunctionalTest").isSuperEntityOf(investMethod);

						if (EntityType.get("NonFunctionalTest").isSuperEntityOf(investMethod))
							nonfuncTests.add(investMethod.name);
						if (EntityType.get("FunctionalTest").isSuperEntityOf(investMethod))
							funcTests.add(investMethod.name);
					}
				}
			}

			if (containsFuc && containsNonFuc)
				System.out.println(publication.getSingleFillerSlot(SlotType.get("hasPubmedID")).getSlotFiller()
						.asInstanceOfLiteralAnnotation().getSurfaceForm());
//				System.out.println(publication.getSingleFillerSlot(SlotType.get("hasPubmedID")).getSlotFiller()
//						.asInstanceOfLiteralAnnotation().getSurfaceForm() + "\t" + (pos) + "\t" + trends + "\t"
//						+ nonfuncTests + "\t" + funcTests);

		}

	}

	private static void addToJudgement(String line) {
		if (line.split("\t").length < 3 || !line.split("\t")[2].trim().equals("100"))
			return;

		judgement.putIfAbsent(EntityType.get(line.split("\t")[0]), new HashMap<>());
		judgement.get(EntityType.get(line.split("\t")[0])).put(EntityType.get("Higher"),
				EntityType.get(line.split("\t")[1]));
		judgement.get(EntityType.get(line.split("\t")[0])).put(EntityType.get("FasterIncrease"),
				EntityType.get(line.split("\t")[1]));
	}

	private static EntityTemplate toPublication(String docName, List<AbstractAnnotation> results) {
		EntityTemplate publication = new EntityTemplate(EntityType.get("Publication"));
		publication.setSingleSlotFiller(SlotType.get("hasPubmedID"), getPubmedID(docName));
		EntityTemplate experiment = new EntityTemplate(EntityType.get("Experiment"));

		publication.addMultiSlotFiller(SlotType.get("describes"), experiment);
		for (AbstractAnnotation result : results) {
			System.out.print(docName + "\t");
			setJudgement(result);
			System.out.println();
			experiment.addMultiSlotFiller(SlotType.get("hasResult"), result);
		}

		return publication;
	}

	private static void setJudgement(AbstractAnnotation result) {

		AbstractAnnotation invest = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SlotType.get("hasInvestigationMethod")).getSlotFiller();

		if (invest == null)
			return;

		System.out.print(invest.getEntityType().name);

		AbstractAnnotation trend = result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasTrend"))
				.getSlotFiller();
		if (trend == null)
			return;

		trend = trend.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasDifference")).getSlotFiller();

		if (trend == null)
			return;

		System.out.print("\t");
		System.out.print(trend.getEntityType().name);

		if (judgement.get(invest.getEntityType()) == null)
			return;

		EntityType _judgement = judgement.get(invest.getEntityType()).get(trend.getEntityType());

		if (_judgement == null)
			return;

		System.out.print("\t");
		System.out.print(_judgement.name);

		AbstractAnnotation annJudgement = result.asInstanceOfEntityTemplate()
				.getSingleFillerSlot(SlotType.get("hasJudgement")).getSlotFiller();

		if (annJudgement == null)
			result.asInstanceOfEntityTemplate().setSingleSlotFiller(SlotType.get("hasJudgement"),
					AnnotationBuilder.toAnnotation(_judgement));
		else {
			System.out.print("\t");
			System.out.print(annJudgement.getEntityType().name);
		}

	}

	private static AbstractAnnotation getPubmedID(String docName) {
		/**
		 * TODO: how to get correct pubmed id???
		 */
		return AnnotationBuilder.toAnnotation("PubmedID", docName);
	}

}
