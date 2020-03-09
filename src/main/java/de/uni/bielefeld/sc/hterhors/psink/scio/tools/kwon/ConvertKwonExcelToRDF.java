package de.uni.bielefeld.sc.hterhors.psink.scio.tools.kwon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

public class ConvertKwonExcelToRDF {

	
//	OEC
//	OEC+CELL
//	OEC+SUBSTANCE
	
//	Liste mit pubulcations + treatment(s) an Nicole
//	judgements 
	
	
	public static void main(String[] args) throws IOException {
		SystemScope.Builder.getScopeHandler().addScopeSpecification(BuildCorpusFromRawData.dataStructureReader).build();

		
		
		AbstractCorpusDistributor corpusDistributor = new ShuffleCorpusDistributor.Builder().setTrainingProportion(80)
				.setTestProportion(20).setSeed(1000L).build();

		InstanceProvider.maxNumberOfAnnotations = 1000;
		InstanceProvider.removeEmptyInstances = true;
		InstanceProvider.removeInstancesWithToManyAnnotations = true;

		File instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result);

		InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor);

		List<EntityTemplate> annotations = new ArrayList<>();

		instanceProvider.getInstances().stream().filter(a->a.toString().contains("N075"))
				.forEach(a -> annotations.add(toPublication(a.getGoldAnnotations().getAnnotations())));

		ConvertToRDF convertToRDF = new ConvertToRDF(new File("OEC.n-triples"), annotations);

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

		List<String> lines = Files.readAllLines(new File("AccessDB_Cleaned_v65.csv").toPath()).stream().skip(1)
				.collect(Collectors.toList());

		List<EntityTemplate> dataPoints = new ArrayList<>();

		Set<String> pubmedIDs = new HashSet<>();
		for (String line : lines) {
			String[] data = line.split("\t");

			EntityTemplate annotation = toDataPoint(data);

			if (pubmedIDs.add(data[0]))
				dataPoints.add(annotation);

		}
		new ConvertToRDF(new File("Excel2RDF.n-triples"), dataPoints);

		int count = 0;
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

		System.out.println(count);
	}

	private static EntityTemplate toPublication(List<AbstractAnnotation> results) {
		EntityTemplate publication = new EntityTemplate(EntityType.get("Publication"));
		publication.setSingleSlotFiller(SlotType.get("hasPubmedID"), getPubmedID(results));
		EntityTemplate experiment = new EntityTemplate(EntityType.get("Experiment"));

		publication.addMultiSlotFiller(SlotType.get("describes"), experiment);
		for (AbstractAnnotation result : results) {

			experiment.addMultiSlotFiller(SlotType.get("hasResult"), result);
		}

		return publication;
	}

	private static AbstractAnnotation getPubmedID(List<AbstractAnnotation> results) {
		/**
		 * TODO: how to get correct pubmed id???
		 */
		return AnnotationBuilder.toAnnotation("PubmedID", UUID.randomUUID().toString());
	}

	/*
	 * PubmedID, Experiment
	 */
	static Map<String, EntityTemplate> cache = new HashMap<>();

	private static EntityTemplate getPublicationForPubmedID(String pubmedID) {

		if (cache.get(pubmedID) == null) {
			EntityTemplate publication = new EntityTemplate(
					AnnotationBuilder.toAnnotation(EntityType.get("Publication")));

			publication.setSingleSlotFiller(SlotType.get("hasPubmedID"),
					AnnotationBuilder.toAnnotation("PubmedID", pubmedID));

			EntityTemplate experiment = new EntityTemplate(
					AnnotationBuilder.toAnnotation(EntityType.get("Experiment")));
			publication.addMultiSlotFiller(SlotType.get("describes"), experiment);
			cache.put(pubmedID, publication);
		}
		return cache.get(pubmedID);
	}

	private static EntityTemplate toDataPoint(String[] data) {
		EntityTemplate publication = getPublicationForPubmedID(data[0]);
		EntityTemplate experiment = (EntityTemplate) publication.getMultiFillerSlot(SlotType.get("describes"))
				.getSlotFiller().iterator().next();
		EntityTemplate result = new EntityTemplate(EntityType.get("Result"));
		experiment.addMultiSlotFiller(SlotType.get("hasResult"), result);
		EntityTemplate investigationMethod = new EntityTemplate(EntityType.get(data[1]));
		result.setSingleSlotFiller(SlotType.get("hasInvestigationMethod"), investigationMethod);
		EntityTemplate judgement = new EntityTemplate(EntityType.get(data[2]));
		result.setSingleSlotFiller(SlotType.get("hasJudgement"), judgement);
		EntityTemplate referenceGroup = new EntityTemplate(EntityType.get("DefinedExperimentalGroup"));

		addNNumber(data[3], referenceGroup);
		EntityTemplate targetGroup = new EntityTemplate(EntityType.get("DefinedExperimentalGroup"));

		result.setSingleSlotFiller(SlotType.get("hasReferenceGroup"), referenceGroup);
		result.setSingleSlotFiller(SlotType.get("hasTargetGroup"), targetGroup);

		EntityTemplate observationRef = addNumericalObservation(data[4], result, referenceGroup);

		EntityTemplate injuryModel = buildInjuryModel(data);

		referenceGroup.setSingleSlotFiller(SlotType.get("hasInjuryModel"), injuryModel);
		targetGroup.setSingleSlotFiller(SlotType.get("hasInjuryModel"), injuryModel);

		addNNumber(data[10], targetGroup);
		EntityTemplate observationTreat = addNumericalObservation(data[11], result, targetGroup);
		addTimepointObservation(data[12], result, observationRef, observationTreat);

		EntityTemplate organismModel = buildOrganismModel(data);

		referenceGroup.setSingleSlotFiller(SlotType.get("hasOrganismModel"), organismModel);
		targetGroup.setSingleSlotFiller(SlotType.get("hasOrganismModel"), organismModel);

		EntityTemplate treatment = buildTreatment(data);
		targetGroup.addMultiSlotFiller(SlotType.get("hasTreatmentType"), treatment);

		EntityTemplate trend = buildTrend(data[22]);
		result.setSingleSlotFiller(SlotType.get("hasTrend"), trend);
		return publication;
	}

	private static EntityTemplate buildTrend(String data) {
		EntityTemplate trend = new EntityTemplate(EntityType.get("Trend"));
		EntityTemplate observedDifference = new EntityTemplate(EntityType.get(data));
		trend.setSingleSlotFiller(SlotType.get("hasDifference"), observedDifference);
		return trend;
	}

	private static EntityTemplate buildTreatment(String[] data) {
		EntityTemplate treatment = new EntityTemplate(EntityType.get(data[18]));

		if (!data[19].trim().isEmpty()) {
			EntityTemplate compound = new EntityTemplate(AnnotationBuilder.toAnnotation(data[19]));
			treatment.setSingleSlotFiller(SlotType.get("hasCompound"), compound);

		}
		if (!data[20].trim().isEmpty()) {
			EntityTemplate deliverymethod = new EntityTemplate(AnnotationBuilder.toAnnotation(data[20]));
			treatment.setSingleSlotFiller(SlotType.get("hasDeliveryMethod"), deliverymethod);

		}
		if (!data[21].trim().isEmpty()) {
			treatment.setSingleSlotFiller(SlotType.get("hasDosage"),
					AnnotationBuilder.toAnnotation("Dosage", data[21]));

		}
		if (!data[24].trim().isEmpty()) {
			treatment.setSingleSlotFiller(SlotType.get("hasTemperture"),
					AnnotationBuilder.toAnnotation("Temperature", data[24]));

		}
		return treatment;
	}

	private static EntityTemplate buildOrganismModel(String[] data) {
		EntityTemplate organismModel = new EntityTemplate(EntityType.get("OrganismModel"));
		if (!data[14].trim().isEmpty())
			organismModel.setSingleSlotFiller(SlotType.get("hasAgeCategory"),
					AnnotationBuilder.toAnnotation(EntityType.get(data[14])));
		if (!data[15].trim().isEmpty())
			organismModel.setSingleSlotFiller(SlotType.get("hasGender"),
					AnnotationBuilder.toAnnotation(EntityType.get(data[15])));
		if (!data[16].trim().isEmpty())
			organismModel.setSingleSlotFiller(SlotType.get("hasOrganismSpecies"),
					AnnotationBuilder.toAnnotation(EntityType.get(data[16])));
		if (!data[17].trim().isEmpty())
			organismModel.setSingleSlotFiller(SlotType.get("hasWeight"),
					AnnotationBuilder.toAnnotation("Weight", data[17]));
		return organismModel;
	}

	private static EntityTemplate buildInjuryModel(String[] data) {
		EntityTemplate injuryModel = new EntityTemplate(EntityType.get(data[5]));
		if (!data[23].trim().isEmpty()) {
			injuryModel.setSingleSlotFiller(SlotType.get("hasInjuryLocation"),
					AnnotationBuilder.toAnnotation(EntityType.get(data[23])));
		}
		if (!data[6].trim().isEmpty()) {

			EntityTemplate injuryDevice = new EntityTemplate(EntityType.get(data[6]));
			if (!data[7].trim().isEmpty())
				injuryDevice.setSingleSlotFiller(SlotType.get("hasDistance"),
						AnnotationBuilder.toAnnotation("Distance", data[7]));
			if (!data[8].trim().isEmpty())
				injuryDevice.setSingleSlotFiller(SlotType.get("hasForce"),
						AnnotationBuilder.toAnnotation("Force", data[8]));
			if (!data[9].trim().isEmpty())
				injuryDevice.setSingleSlotFiller(SlotType.get("hasWeight"),
						AnnotationBuilder.toAnnotation("Weight", data[9]));

			injuryModel.setSingleSlotFiller(SlotType.get("hasInjuryDevice"), injuryDevice);
		}
		return injuryModel;
	}

	private static void addNNumber(String data, EntityTemplate referenceGroup) {
		if (!data.trim().isEmpty())
			referenceGroup.setSingleSlotFiller(SlotType.get("hasNNumber"),
					AnnotationBuilder.toAnnotation("NNumber", data));
	}

	private static EntityTemplate addNumericalObservation(String data, EntityTemplate result,
			EntityTemplate belongsToGroup) {
		if (!data.trim().isEmpty()) {
			EntityTemplate observation = new EntityTemplate(EntityType.get("Observation"));
			EntityTemplate numericValue = new EntityTemplate(EntityType.get("NumericValue"));
			numericValue.setSingleSlotFiller(SlotType.get("hasValue"), AnnotationBuilder.toAnnotation("Value", data));
			observation.setSingleSlotFiller(SlotType.get("hasNumericValue"), numericValue);
			result.addMultiSlotFiller(SlotType.get("hasObservation"), observation);
			observation.setSingleSlotFiller(SlotType.get("belongsTo"), belongsToGroup);
			return observation;
		}
		return null;
	}

	private static void addTimepointObservation(String data, EntityTemplate result, EntityTemplate observationRef,
			EntityTemplate observationTreat) {
		if (!data.trim().isEmpty()) {
			EntityTemplate observation = new EntityTemplate(EntityType.get("Observation"));
			result.addMultiSlotFiller(SlotType.get("hasObservation"), observation);
			if (observationRef != null)
				observation.setSingleSlotFiller(SlotType.get("hasEventBefore"), observationRef);
			if (observationTreat != null)
				observation.setSingleSlotFiller(SlotType.get("hasEventAfter"), observationTreat);
			observation.setSingleSlotFiller(SlotType.get("hasDuration"),
					AnnotationBuilder.toAnnotation("Duration", data));
		}
	}

}
