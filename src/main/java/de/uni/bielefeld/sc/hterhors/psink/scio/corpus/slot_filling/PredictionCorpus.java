package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.slot_filling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.EInstanceContext;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.reader.csv.CSVDataStructureReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonInstanceIO;
import de.hterhors.semanticmr.json.converter.InstancesToJsonInstanceWrapper;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.hterhors.semanticmr.nerla.annotation.RegularExpressionNerlAnnotator;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
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

public class PredictionCorpus {
	public static final File SRC_MAIN_RESOURCES = new File("src/main/resources/");

	private static final File ROOT_DATA_STRUCTURE_DIR = new File(SRC_MAIN_RESOURCES,
			SlotFillingCorpusBuilderBib.DATA_STRUCTURE_DIR_NAME);
	private static final File ROOT_DATA_STRUCTURE_ENTITIES = new File(ROOT_DATA_STRUCTURE_DIR,
			SlotFillingCorpusBuilderBib.ENTITIES_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_SLOTS = new File(ROOT_DATA_STRUCTURE_DIR,
			SlotFillingCorpusBuilderBib.SLOTS_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_STRUCTURES = new File(ROOT_DATA_STRUCTURE_DIR,
			SlotFillingCorpusBuilderBib.STRUCTURES_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_HIERARCHIES = new File(ROOT_DATA_STRUCTURE_DIR,
			SlotFillingCorpusBuilderBib.HIERARCHIES_FILE_NAME);
	public static final CSVDataStructureReader dataStructureReader = new CSVDataStructureReader(
			ROOT_DATA_STRUCTURE_ENTITIES, ROOT_DATA_STRUCTURE_HIERARCHIES, ROOT_DATA_STRUCTURE_SLOTS,
			ROOT_DATA_STRUCTURE_STRUCTURES);

	public static void main(String[] args) {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(dataStructureReader).build();

		final File nerlaDirectory = new File("prediction/nerla/");
		nerlaDirectory.mkdirs();

		File writeToFile = new File("prediction/instances/");
		writeToFile.mkdirs();

		AtomicInteger c = new AtomicInteger(0);

		final int max = new File("Upload/XML2/").listFiles().length;
//		for (File file : new File("Upload/XML1/").listFiles()) {

		List<File> l = new ArrayList<File>(Arrays.asList(new File("Upload/XML2/").listFiles()));

		Collections.sort(l);

		l.stream().parallel().forEach(file -> {
			String name = "";
			try {

				String content = Files.readAllLines(file.toPath()).stream().reduce("", String::concat).trim();
				Document d = new Document(file.getName(), content);
				name = d.documentID.replaceAll("\\.txt", "") + ".json";
				System.out.println(name + " content.length() = " + content.length());
				File outputFile = new File(writeToFile, name);
				File nerlaOutFile = new File(nerlaDirectory, d.documentID.replaceAll("\\.txt", "") + ".nerla.json");

				if (!(outputFile.exists() && nerlaOutFile.exists())) {

					JsonNerlaIO io = new JsonNerlaIO(true);

					Instance i = new Instance(EInstanceContext.PREDICT, d, new Annotations());

					List<JsonEntityAnnotationWrapper> wrappedAnnotation = new ArrayList<>();

					wrappedAnnotation.addAll(
							annotateWithRegularExpressions(d, new OrganismModelPattern(SCIOEntityTypes.organismModel)));
					wrappedAnnotation
							.addAll(annotateWithRegularExpressions(d, new TrendPattern(SCIOEntityTypes.trend)));
					wrappedAnnotation
							.addAll(annotateWithRegularExpressions(d, new ResultPattern(SCIOEntityTypes.result)));
					wrappedAnnotation.addAll(annotateWithRegularExpressions(d,
							new VertebralAreaPattern(SCIOEntityTypes.vertebralLocation)));
					wrappedAnnotation.addAll(
							annotateWithRegularExpressions(d, new AnaestheticPattern(SCIOEntityTypes.anaesthetic)));
					wrappedAnnotation.addAll(
							annotateWithRegularExpressions(d, new InjuryDevicePattern(SCIOEntityTypes.injuryDevice)));
					wrappedAnnotation.addAll(annotateWithRegularExpressions(d,
							new DeliveryMethodPattern(SCIOEntityTypes.deliveryMethod)));
					wrappedAnnotation.addAll(annotateWithRegularExpressions(d,
							new InvestigationMethodPattern(SCIOEntityTypes.investigationMethod)));
					wrappedAnnotation
							.addAll(annotateWithRegularExpressions(d, new TreatmentPattern(SCIOEntityTypes.treatment)));
					wrappedAnnotation
							.addAll(annotateWithRegularExpressions(d, new InjuryPattern(SCIOEntityTypes.injury)));

					List<Instance> instances = new ArrayList<>();
					instances.add(i);

					print(outputFile, nerlaOutFile, io, wrappedAnnotation, instances);
				}

			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();

			}
			c.incrementAndGet();
			System.out.println(c + " / " + max + "Doc:\t" + name);
		});

	}

	private synchronized static void print(File outputFile, File nerlaOutFile, JsonNerlaIO io,
			List<JsonEntityAnnotationWrapper> wrappedAnnotation, List<Instance> instances) throws IOException {
		InstancesToJsonInstanceWrapper conv = new InstancesToJsonInstanceWrapper(instances);
		JsonInstanceIO writer = new JsonInstanceIO(true);
		writer.writeInstances(outputFile, conv.convertToWrapperInstances());
		io.writeNerlas(nerlaOutFile, wrappedAnnotation);
	}

	private static List<JsonEntityAnnotationWrapper> annotateWithRegularExpressions(Document document,
			BasicRegExPattern patternCollection) {

		RegularExpressionNerlAnnotator annotator = new RegularExpressionNerlAnnotator(patternCollection);

		Map<EntityType, Set<DocumentLinkedAnnotation>> annotations = annotator.annotate(document);

		return annotations.values().stream().flatMap(v -> v.stream()).map(d -> new JsonEntityAnnotationWrapper(d))
				.collect(Collectors.toList());

	}

}
