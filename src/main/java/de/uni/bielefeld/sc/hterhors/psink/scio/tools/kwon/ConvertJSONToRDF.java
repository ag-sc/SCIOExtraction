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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.experimental_group.wrapper.DefinedExperimentalGroup;

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
		String header = "Document\tInvestigationMethod\tSignificance\tPvalue\tTrend\tJudgement\tTargetTreatments\tReferenceTreatments\tBoth contain OEC\tAny contains OEC\tSwitch was applied\tAutomated Judgment\tPositiveFunctional\tPositiveNonFunctional";
		System.out.println(header);
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

//		if (!docName.startsWith("N075"))
//			return publication;

		publication.setSingleSlotFiller(SlotType.get("hasPubmedID"), getPubmedID(docName));
		EntityTemplate experiment = new EntityTemplate(EntityType.get("Experiment"));

		publication.addMultiSlotFiller(SlotType.get("describes"), experiment);

		for (AbstractAnnotation result : results) {
			setJudgement(docName, result);
			experiment.addMultiSlotFiller(SlotType.get("hasResult"), result);
		}

		return publication;
	}

	private static void setJudgement(String docName, AbstractAnnotation result) {

		try {
			DefinedExperimentalGroup def1 = new DefinedExperimentalGroup(
					result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasReferenceGroup"))
							.getSlotFiller().asInstanceOfEntityTemplate());
			DefinedExperimentalGroup def2 = new DefinedExperimentalGroup(result.asInstanceOfEntityTemplate()
					.getSingleFillerSlot(SlotType.get("hasTargetGroup")).getSlotFiller().asInstanceOfEntityTemplate());

			DefinedExperimentalGroup ref = null;
			DefinedExperimentalGroup target = null;

			boolean change = false;
			if (doNotSwitchPos(def1, def2)) {
				ref = def1;
				target = def2;
			} else {
				change = true;
				ref = def2;
				target = def1;
			}

			Set<EntityType> t = target.getRelevantTreatments().stream().map(e -> e.getEntityType())
					.collect(Collectors.toSet());

			Set<EntityType> r = ref.getRelevantTreatments().stream().map(e -> e.getEntityType())
					.collect(Collectors.toSet());
			boolean bothOEC = containsOEC(t) && containsOEC(r);
			boolean containsOEC = containsOEC(t) || containsOEC(r);

			if (t.equals(r))
				return;

			AbstractAnnotation invest = result.asInstanceOfEntityTemplate()
					.getSingleFillerSlot(SlotType.get("hasInvestigationMethod")).getSlotFiller();

			System.out.print(docName + "\t");
			try {
				System.out.print(invest.getEntityType().name);
			} catch (Exception e) {
				System.out.print("-");
			}

			AbstractAnnotation trend = null;
			AbstractAnnotation sig = null;
			try {
				trend = result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasTrend"))
						.getSlotFiller();

				System.out.print("\t");
				sig = trend.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasSignificance"))
						.getSlotFiller();

				if (sig.getEntityType() == EntityType.get("Significance"))
					System.out.print("-");
				else
					System.out.print(sig.getEntityType().name);
			} catch (Exception e) {
				System.out.print("-");
			}

			System.out.print("\t");
			AbstractAnnotation pvalue = null;
			try {
				pvalue = sig.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasPValue"))
						.getSlotFiller();
				System.out.print(pvalue.asInstanceOfLiteralAnnotation().getSurfaceForm());
			} catch (Exception e) {
				System.out.print("-");
			}

			System.out.print("\t");
			EntityType difference = null;
			try {
				trend = trend.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasDifference"))
						.getSlotFiller();

				difference = trend.getEntityType();
				if (change) {

					if (difference == EntityType.get("Higher"))
						difference = EntityType.get("Lower");
					else if (difference == EntityType.get("Lower"))
						difference = EntityType.get("Higher");
					else if (difference == EntityType.get("FasterIncrease"))
						difference = EntityType.get("SlowerIncrease");
					else if (difference == EntityType.get("SlowerIncrease"))
						difference = EntityType.get("FasterIncrease");

				}

				if (difference == EntityType.get("ObservedDifference"))
					System.out.print("-");
				else
					System.out.print(difference.name);

			} catch (Exception e) {
				System.out.print("-");
			}

			System.out.print("\t");
			AbstractAnnotation annJudgement = null;
			try {
				annJudgement = result.asInstanceOfEntityTemplate().getSingleFillerSlot(SlotType.get("hasJudgement"))
						.getSlotFiller();

				System.out.print(annJudgement.getEntityType().name);
			} catch (Exception e) {
				System.out.print("-");
			}

			String targetT = target.getRelevantTreatments().stream()
					.map(e -> e.getEntityType().name + " '"
							+ e.getRootAnnotation().asInstanceOfLiteralAnnotation().getSurfaceForm() + "'\n")
					.reduce("", String::concat);
			String refT = ref.getRelevantTreatments().stream()
					.map(e -> e.getEntityType().name + " '"
							+ e.getRootAnnotation().asInstanceOfLiteralAnnotation().getSurfaceForm() + "'\n")
					.reduce("", String::concat);

			System.out.print("\t\"");
			if (targetT.trim().isEmpty()) {
				System.out.print("-");
			} else {
				System.out.print(targetT.trim().replaceAll("\n", ","));
			}
			System.out.print("\"\t\"");
			if (refT.trim().isEmpty()) {
				System.out.print("-");
			} else {
				System.out.print(refT.trim().replaceAll("\n", ","));
			}
			System.out.print("\"");
			System.out.print("\t");
			System.out.print(bothOEC);
			System.out.print("\t");
			System.out.print(containsOEC);
			System.out.print("\t");
			System.out.print(change);

			EntityType _judgement = null;
			System.out.print("\t");
			try {
				_judgement = judgement.get(invest.getEntityType()).get(difference);

				System.out.print(_judgement.name);
			} catch (Exception e) {
				System.out.print("-");
			}

			boolean positiveFunctional = (sig != null && sig.getEntityType() == EntityType.get("PositiveSignificance")
					|| (sig != null && sig.getEntityType() == EntityType.get("Significance")
							&& pvalueToSignificance(pvalue)))
					&&
					//
					(_judgement != null && _judgement.equals(EntityType.get("Positive"))
							|| annJudgement != null && annJudgement.getEntityType().equals(EntityType.get("Positive")))
					//
					&& EntityType.get("FunctionalTest").isSuperEntityOf(invest.getEntityType());

			boolean positiveNonFunctional = (sig != null
					&& sig.getEntityType() == EntityType.get("PositiveSignificance")
					|| (sig != null && sig.getEntityType() == EntityType.get("Significance")
							&& pvalueToSignificance(pvalue)))
					&& (_judgement != null && _judgement.equals(EntityType.get("Positive"))
							|| annJudgement != null && annJudgement.getEntityType().equals(EntityType.get("Positive")))
					//
					&& EntityType.get("NonFunctionalTest").isSuperEntityOf(invest.getEntityType());

			System.out.print("\t");
			System.out.print(positiveFunctional);
			System.out.print("\t");
			System.out.print(positiveNonFunctional);
			/**
			 * TODO: change data accordingly.
			 */
//			if (annJudgement == null && _judgement != null)
//				result.asInstanceOfEntityTemplate().setSingleSlotFiller(SlotType.get("hasJudgement"),
//						AnnotationBuilder.toAnnotation(_judgement));

			System.out.println();
		} catch (IllegalArgumentException e) {
		}

	}

	final static Pattern pValuePattern = Pattern.compile(".*(0\\.\\d+)");

	private static boolean pvalueToSignificance(AbstractAnnotation pvalue2) {
		if (pvalue2 == null)
			return false;

		Matcher m = pValuePattern.matcher(pvalue2.asInstanceOfLiteralAnnotation().getSurfaceForm());
		if (!m.find()) {
			return false;
		}

		double pValue = Double.parseDouble(m.group(1));

		return (pvalue2.asInstanceOfLiteralAnnotation().getSurfaceForm().contains("<")
				|| pvalue2.asInstanceOfLiteralAnnotation().getSurfaceForm().contains("≤")) && pValue <= 0.05;
	}

	/**
	 * ToCheck is reference group, if it contains a vehicle and the other group
	 * contains OEC or no treatment
	 * 
	 * @param toCheck
	 * @param basedOn
	 * @return
	 */
	private static boolean doNotSwitchPos(DefinedExperimentalGroup toCheck, DefinedExperimentalGroup basedOn) {

		Set<EntityType> referenceTreats = toCheck.getRelevantTreatments().stream().map(e -> e.getEntityType())
				.collect(Collectors.toSet());

		Set<EntityType> targetTreats = basedOn.getRelevantTreatments().stream().map(e -> e.getEntityType())
				.collect(Collectors.toSet());

		boolean referenceContainsVehicle = containsVehicle(referenceTreats);
		boolean targetContainsVehicle = containsVehicle(targetTreats);

		boolean referenceContainsOEC = containsOEC(referenceTreats);
		boolean targetContainsOEC = containsOEC(targetTreats);

		if (targetTreats.containsAll(referenceTreats))
			return true;

		if (referenceContainsOEC && targetContainsOEC)
			return false;

		if (referenceTreats.isEmpty() && !targetTreats.isEmpty())
			return true;

		if (!referenceTreats.isEmpty() && targetTreats.isEmpty())
			return false;

		if (referenceContainsOEC && !targetContainsOEC) {
			return false;
		}

		if (!referenceContainsOEC && targetContainsOEC) {
			return true;
		}

		if (referenceContainsVehicle && !targetContainsVehicle) {
			return true;
		}
		if (!referenceContainsVehicle && targetContainsVehicle) {
			return false;
		}

		if (!referenceContainsVehicle && !referenceContainsOEC && targetContainsOEC) {
			return true;
		}

		if (!referenceContainsOEC && !targetContainsOEC)
			return true;

		throw new IllegalStateException();
	}

	private static boolean containsVehicle(Set<EntityType> toCheckTreats) {
		boolean toCheckContainsVehicle = false;
		for (EntityType entityType : EntityType.get("Vehicle").getRelatedEntityTypes()) {
			toCheckContainsVehicle |= toCheckTreats.contains(entityType);
			if (toCheckContainsVehicle)
				break;
		}
		return toCheckContainsVehicle;
	}

	private static boolean containsOEC(Set<EntityType> r) {
		return r.contains(EntityType.get("OlfactoryEnsheathingGliaCell"));
	}

	private static AbstractAnnotation getPubmedID(String docName) {
		/**
		 * TODO: how to get correct pubmed id???
		 */
		return AnnotationBuilder.toAnnotation("PubmedID", docName);
	}

}
