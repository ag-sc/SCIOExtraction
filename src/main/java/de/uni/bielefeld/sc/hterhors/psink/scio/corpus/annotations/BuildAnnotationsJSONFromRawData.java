package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.annotations;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReader;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.structure.annotations.filter.EntityTemplateAnnotationFilter;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.reader.csv.CSVDataStructureReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonInstanceIO;
import de.hterhors.semanticmr.json.converter.InstancesToJsonInstanceWrapper;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.santo.converter.reader.TextualAnnotationsReader;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.AnnotationsCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;

public class BuildAnnotationsJSONFromRawData {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
	public static final File SRC_MAIN_RESOURCES = new File("src/main/resources/");

	private static final File SANTO_RAW_DATA_DIR = new File(SRC_MAIN_RESOURCES,
			AnnotationsCorpusBuilderBib.RAW_DATA_DIR_NAME);

	private static final File ROOT_DATA_STRUCTURE_DIR = new File(SRC_MAIN_RESOURCES,
			AnnotationsCorpusBuilderBib.DATA_STRUCTURE_DIR_NAME);

	private static final File ROOT_DATA_STRUCTURE_ENTITIES = new File(ROOT_DATA_STRUCTURE_DIR,
			AnnotationsCorpusBuilderBib.ENTITIES_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_SLOTS = new File(ROOT_DATA_STRUCTURE_DIR,
			AnnotationsCorpusBuilderBib.SLOTS_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_STRUCTURES = new File(ROOT_DATA_STRUCTURE_DIR,
			AnnotationsCorpusBuilderBib.STRUCTURES_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_HIERARCHIES = new File(ROOT_DATA_STRUCTURE_DIR,
			AnnotationsCorpusBuilderBib.HIERARCHIES_FILE_NAME);

	private static final CSVDataStructureReader dataStructureReader = new CSVDataStructureReader(
			ROOT_DATA_STRUCTURE_ENTITIES, ROOT_DATA_STRUCTURE_HIERARCHIES, ROOT_DATA_STRUCTURE_SLOTS,
			ROOT_DATA_STRUCTURE_STRUCTURES);

	public static void main(String[] args) throws Exception {

		SystemScope.Builder.getScopeHandler().addScopeSpecification(dataStructureReader).build();

//		buildForOrganismModel();
//		buildForInjuryModel();
//		buildForAnaesthetic();
//		buildForTreatment();
//		buildForVertebralArea();
//		buildForDeliveryMethod();
		buildForInjuryDevice();
//		buildForInvestigationMethod();
//		buildForGroupName();
//		buildForTrend();
//		buildForResult();

	}

	private static void buildForResult() throws Exception {
		convertAnnotations(SCIOEntityTypes.result);
	}

	private static void buildForTrend() throws Exception {
		convertAnnotations(SCIOEntityTypes.trend);
	}

	private static void buildForGroupName() throws Exception {
		convertAnnotations(SCIOEntityTypes.definedExperimentalGroup);
	}

	private static void buildForInvestigationMethod() throws Exception {
		convertAnnotations(SCIOEntityTypes.investigationMethod);

	}

	private static void buildForVertebralArea() throws Exception {

		convertAnnotations(SCIOEntityTypes.vertebralArea);
	}

	private static void buildForDeliveryMethod() throws Exception {
		convertAnnotations(SCIOEntityTypes.deliveryMethod);

	}

	private static void buildForInjuryDevice() throws Exception {
		convertAnnotations(SCIOEntityTypes.injuryDevice);

	}

	private static void buildForTreatment() throws Exception {

		convertAnnotations(SCIOEntityTypes.treatment);
	}

	private static void buildForInjuryModel() throws Exception {

		convertAnnotations(SCIOEntityTypes.injury);
	}

	private static void buildForAnaesthetic() throws Exception {

		convertAnnotations(SCIOEntityTypes.anaesthetic);
	}

	private static void buildForOrganismModel() throws Exception {

		convertAnnotations(SCIOEntityTypes.organismModel);
	}

	private static void convertAnnotations(EntityType entityType) throws Exception {

		File annotationsDir = new File(AnnotationsCorpusBuilderBib.ANNOTATIONS_DIR,
				AnnotationsCorpusBuilderBib.toDirName(entityType));
		annotationsDir.mkdirs();

		Map<String, List<JsonEntityAnnotationWrapper>> annotationsPerDocumentName = readDocLinkedAnnotationsFromSanto(
				SANTO_RAW_DATA_DIR, entityType);

		JsonNerlaIO io = new JsonNerlaIO(true);

		for (Entry<String, List<JsonEntityAnnotationWrapper>> dp : annotationsPerDocumentName.entrySet()) {

			File outputFile = new File(annotationsDir, dp.getKey() + ".json");

			io.writeNerlas(outputFile, dp.getValue());
		}

	}

	/**
	 * Projects existing annotations into the whole document.
	 * 
	 * @param document
	 * @param annotations
	 */
	private static void projectAnnotationsIntoDocument(Document document, Set<AbstractAnnotation> annotations) {

		Set<AbstractAnnotation> additionalAnnotations = new HashSet<>();

		for (AbstractAnnotation abstractAnnotation : annotations) {

			Matcher m = Pattern
					.compile(Pattern.quote(abstractAnnotation.asInstanceOfDocumentLinkedAnnotation().getSurfaceForm()))
					.matcher(document.documentContent);

			while (m.find()) {
				try {

					additionalAnnotations.add(AnnotationBuilder.toAnnotation(document,
							abstractAnnotation.getEntityType().name, m.group(), m.start()));
				} catch (RuntimeException e) {
					System.out.println("Could not map annotation to tokens!");
				}
			}
		}
		System.out.println("Found additional annotation projections: " + additionalAnnotations.size());
		annotations.addAll(additionalAnnotations);

	}

	private static Map<String, List<JsonEntityAnnotationWrapper>> readDocLinkedAnnotationsFromSanto(File rawDataFile,
			EntityType rootET) throws Exception {
		File finalInstancesDir = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(rootET);
		finalInstancesDir.mkdirs();

		List<String> fileNames = Arrays.stream(rawDataFile.listFiles()).filter(f -> f.getName().endsWith(".csv"))
				.map(f -> f.getName().substring(0, f.getName().length() - 11)).collect(Collectors.toList());

		Collections.sort(fileNames);

		Map<String, List<JsonEntityAnnotationWrapper>> map = new HashMap<>();

		for (String dataFileName : fileNames) {
			map.putIfAbsent(dataFileName, new ArrayList<>());
			log.info("Read: " + dataFileName + "...");

			CSVReader reader = null;

			try {
				reader = new CSVReader(new FileReader(new File(rawDataFile, dataFileName + "_Jessica.annodb")));
				String[] line;
				while ((line = reader.readNext()) != null) {

					if (line.length == 1 && line[0].isEmpty() || line[0].startsWith("#"))
						continue;

					final EntityType entityType = EntityType
							.get(line[TextualAnnotationsReader.CLASS_TYPE_INDEX].trim());

					if (!rootET.getRelatedEntityTypes().contains(entityType))
						continue;

					final Integer onset = Integer.parseInt(line[TextualAnnotationsReader.ONSET_INDEX].trim());
					final String content = line[TextualAnnotationsReader.TEXT_MENTION_INDEX].trim();
					map.get(dataFileName)
							.add(new JsonEntityAnnotationWrapper(dataFileName, entityType.name, onset, content));
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null)
					reader.close();
			}

		}
		return map;
	}

}
