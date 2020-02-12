package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.slot_filling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.EInstanceContext;
import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.reader.csv.CSVDataStructureReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.nerla.JsonNerlaIO;
import de.hterhors.semanticmr.json.nerla.wrapper.JsonEntityAnnotationWrapper;
import de.hterhors.semanticmr.nerla.annotation.BasicRegExPattern;
import de.hterhors.semanticmr.nerla.annotation.RegularExpressionNerlAnnotator;
import de.hterhors.semanticmr.santo.converter.Santo2JsonConverter;
import de.hterhors.semanticmr.tools.specifications.DataStructureWriter;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.deliverymethod.nerla.DeliveryMethodPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.injury.nerla.InjuryPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.investigation_method.nerla.InvestigationMethodPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.nerla.OrganismModelPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.treatment.nerla.TreatmentPattern;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.vertebralarea.nerla.VertebralAreaPattern;

/**
 * This class builds the complete corpus from the raw data.
 * 
 * @author hterhors
 *
 */
public class BuildCorpusFromRawData {
	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
	public static final File SRC_MAIN_RESOURCES = new File("src/main/resources/");

	private static final File SANTO_RAW_DATA_DIR = new File(SRC_MAIN_RESOURCES, SlotFillingCorpusBuilderBib.RAW_DATA_DIR_NAME);
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

	private static final CSVDataStructureReader dataStructureReader = new CSVDataStructureReader(
			ROOT_DATA_STRUCTURE_ENTITIES, ROOT_DATA_STRUCTURE_HIERARCHIES, ROOT_DATA_STRUCTURE_SLOTS,
			ROOT_DATA_STRUCTURE_STRUCTURES);

	private static final long RANDOM_SEED = 1000L;

	private static final String RDF_SCHEMA_LABEL = "<http://www.w3.org/2000/01/rdf-schema#label>";

	private static final String RDF_SCHEMA_COMMENT = "<http://www.w3.org/2000/01/rdf-schema#comment>";

	private static final String SCIO_NAME_SPACE = "http://psink.de/scio";
	private static final String RESOURCE_NAME_SPACE = "http://scio/data";

	public static void main(String[] args) throws Exception {
		SystemScope.Builder.getScopeHandler().addScopeSpecification(dataStructureReader).build();
		SlotFillingCorpusBuilderBib.SLOT_FILLING_DIR.mkdir();

		buildCorpusForOrganismModel();
		buildCorpusForInjuryModel();
		buildCorpusForTreatmentType();
		buildCorpusForVertebralArea();
		buildCorpusForDeliveryMethod();
		buildCorpusForExperimentalGroup();
		buildCorpusForInvestigationMethod();
		buildCorpusForObservation();
		buildCorpusForResult();
		System.exit(1);
	}

	private static void buildCorpusForResult() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.result);
		Set<String> annotatedDocuments = new HashSet<>(
				Files.readAllLines(new File(SRC_MAIN_RESOURCES, "corpus_docs.csv").toPath()));
		convertFromSanto2JsonCorpus(annotatedDocuments, Collections.emptySet(), SCIOEntityTypes.result, true, true,
				true);

		File tmpUnrolledRawDataDir = new File(SlotFillingCorpusBuilderBib.DATA_DIRECTORY,
				SlotFillingCorpusBuilderBib.toDirName(SCIOEntityTypes.result) + "_tmp/");
		tmpUnrolledRawDataDir.mkdirs();
		tmpUnrolledRawDataDir.deleteOnExit();

		new ResolvePairwiseComparedGroups(SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.result),
				SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.observation),
				tmpUnrolledRawDataDir);

		convertFromSanto2JsonCorpus(tmpUnrolledRawDataDir, annotatedDocuments, Collections.emptySet(),
				SCIOEntityTypes.result, true, true, true);

	}

	private static void buildCorpusForObservation() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.observation);
		Set<String> annotatedDocuments = new HashSet<>(
				Files.readAllLines(new File(SRC_MAIN_RESOURCES, "corpus_docs.csv").toPath()));
		convertFromSanto2JsonCorpus(annotatedDocuments, Collections.emptySet(), SCIOEntityTypes.observation, true, true,
				true);

	}

	private static void buildCorpusForExperimentalGroup() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.experimentalGroup);
		Set<String> annotatedDocuments = new HashSet<>(
				Files.readAllLines(new File(SRC_MAIN_RESOURCES, "corpus_docs.csv").toPath()));
		convertFromSanto2JsonCorpus(annotatedDocuments, Collections.emptySet(), SCIOEntityTypes.experimentalGroup, true,
				true, true);

	}

	private static void buildCorpusForVertebralArea() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.vertebralArea);
		convertFromSanto2JsonCorpus(Collections.emptySet(), Collections.emptySet(), SCIOEntityTypes.vertebralArea, true,
				true, false);
		annotateWithRegularExpressions(new VertebralAreaPattern(SCIOEntityTypes.vertebralArea));
	}

	private static void buildCorpusForDeliveryMethod() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.deliveryMethod);

		Set<String> annotatedDocuments = new HashSet<>(
				Files.readAllLines(new File(SRC_MAIN_RESOURCES, "corpus_docs.csv").toPath()));
		convertFromSanto2JsonCorpus(annotatedDocuments, Collections.emptySet(), SCIOEntityTypes.deliveryMethod, true,
				true, false);
		annotateWithRegularExpressions(new DeliveryMethodPattern(SCIOEntityTypes.deliveryMethod));
	}

	private static void buildCorpusForInvestigationMethod() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.investigationMethod);

		Set<String> annotatedDocuments = new HashSet<>(
				Files.readAllLines(new File(SRC_MAIN_RESOURCES, "corpus_docs.csv").toPath()));

		Set<SlotType> investigationMethodSlotTypes = new HashSet<>();
		investigationMethodSlotTypes.add(SCIOSlotTypes.hasLocation);
		convertFromSanto2JsonCorpus(annotatedDocuments, investigationMethodSlotTypes,
				SCIOEntityTypes.investigationMethod, true, true, false);
		annotateWithRegularExpressions(new InvestigationMethodPattern(SCIOEntityTypes.investigationMethod));

	}

	private static void buildCorpusForTreatmentType() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.treatment);

		Set<String> annotatedDocuments = new HashSet<>(
				Files.readAllLines(new File(SRC_MAIN_RESOURCES, "corpus_docs.csv").toPath()));
		Set<SlotType> treatmentSlotTypes = new HashSet<>();
		treatmentSlotTypes.add(SCIOSlotTypes.hasDeliveryMethod);
		treatmentSlotTypes.add(SCIOSlotTypes.hasCompound);
		treatmentSlotTypes.add(SCIOSlotTypes.hasDirection);
		treatmentSlotTypes.add(SCIOSlotTypes.hasLocation);
		treatmentSlotTypes.add(SCIOSlotTypes.hasDosage);
		convertFromSanto2JsonCorpus(annotatedDocuments, treatmentSlotTypes, SCIOEntityTypes.treatment, true, true,
				false);
		annotateWithRegularExpressions(new TreatmentPattern(SCIOEntityTypes.treatment));

	}

	private static void buildCorpusForOrganismModel() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.organismModel);

		convertFromSanto2JsonCorpus(Collections.emptySet(), Collections.emptySet(), SCIOEntityTypes.organismModel, true,
				true, false);

		annotateWithRegularExpressions(new OrganismModelPattern(SCIOEntityTypes.organismModel));
	}

	private static void buildCorpusForInjuryModel() throws Exception {
		buildSubDataStructureFiles(SCIOEntityTypes.injury);
		Set<SlotType> injurySlotTypes = new HashSet<>();
		injurySlotTypes.add(SCIOSlotTypes.hasInjuryDevice);
		injurySlotTypes.add(SCIOSlotTypes.hasInjuryLocation);
		injurySlotTypes.add(SCIOSlotTypes.hasInjuryAnaesthesia);
		injurySlotTypes.add(SCIOSlotTypes.hasLowerVertebrae);
		injurySlotTypes.add(SCIOSlotTypes.hasUpperVertebrae);
		convertFromSanto2JsonCorpus(Collections.emptySet(), injurySlotTypes, SCIOEntityTypes.injury, true, true, false);
		annotateWithRegularExpressions(new InjuryPattern(SCIOEntityTypes.injury));

	}

	private static void buildSubDataStructureFiles(EntityType rootEntityType) throws Exception {

		final String dataStructureDirName = SlotFillingCorpusBuilderBib.toDirName(rootEntityType)
				+ SlotFillingCorpusBuilderBib.DATA_STRUCTURE_DIR_NAME;

		File finalDataStructureDir = new File(SlotFillingCorpusBuilderBib.SLOT_FILLING_DIR, dataStructureDirName);
		finalDataStructureDir.mkdirs();

		DataStructureWriter.writeEntityDataStructureFile(SlotFillingCorpusBuilderBib.buildEntitiesFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeHierarchiesDataStructureFile(SlotFillingCorpusBuilderBib.buildHierarchiesFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeSlotsDataStructureFile(SlotFillingCorpusBuilderBib.buildSlotsFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeStructuresDataStructureFile(ROOT_DATA_STRUCTURE_STRUCTURES,
				SlotFillingCorpusBuilderBib.buildStructuresFile(rootEntityType), rootEntityType);

	}

	private static void convertFromSanto2JsonCorpus(Set<String> filterNames, Set<SlotType> filterSlotTypes,
			EntityType entityType, boolean includeSubEntities, boolean jsonPrettyString, boolean deepRec)
			throws Exception {

		convertFromSanto2JsonCorpus(SANTO_RAW_DATA_DIR, filterNames, filterSlotTypes, entityType, includeSubEntities,
				jsonPrettyString, deepRec);
	}

	private static void annotateWithRegularExpressions(BasicRegExPattern patternCollection) {

		File finalInstancesDir = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(patternCollection.getRootEntityType());
		finalInstancesDir.mkdirs();

		final File nerlaDiractory = SlotFillingCorpusBuilderBib.getDefaultRegExNerlaDir(patternCollection.getRootEntityType());
		nerlaDiractory.mkdirs();

		RegularExpressionNerlAnnotator annotator = new RegularExpressionNerlAnnotator(patternCollection);

		InstanceProvider instanceProvider = new InstanceProvider(finalInstancesDir);

		JsonNerlaIO io = new JsonNerlaIO(true);
		for (Instance instance : instanceProvider.getInstances()) {

			try {

				Map<EntityType, Set<DocumentLinkedAnnotation>> annotations = annotator.annotate(instance.getDocument());

				List<JsonEntityAnnotationWrapper> wrappedAnnotation = annotations.values().stream()
						.flatMap(v -> v.stream()).map(d -> new JsonEntityAnnotationWrapper(d))
						.collect(Collectors.toList());

				io.writeNerlas(new File(nerlaDiractory, instance.getName() + ".nerla.json"), wrappedAnnotation);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void convertFromSanto2JsonCorpus(File rawDataFile, Set<String> filterNames,
			Set<SlotType> filterSlotTypes, EntityType entityType, boolean includeSubEntities, boolean jsonPrettyString,
			boolean deepRec) throws Exception {
		File finalInstancesDir = SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(entityType);
		finalInstancesDir.mkdirs();

		final Random random = new Random(RANDOM_SEED);

		List<String> fileNames = Arrays.stream(rawDataFile.listFiles()).filter(f -> f.getName().endsWith(".csv"))
				.map(f -> f.getName().substring(0, f.getName().length() - 11)).collect(Collectors.toList());

		Collections.sort(fileNames);

		for (String dataFileName : fileNames) {

			if (!filterNames.isEmpty() && !filterNames.contains(dataFileName)) {
				log.info("Skip: " + dataFileName + "...");
				continue;
			}

			log.info("Convert: " + dataFileName + "...");

			final String fileName = dataFileName + "_" + entityType.name + ".json";

			Santo2JsonConverter converter = new Santo2JsonConverter(filterSlotTypes, dataFileName,
					new File(rawDataFile, dataFileName + "_export.csv"),
					new File(rawDataFile, dataFileName + "_Jessica.annodb"),
					new File(rawDataFile, dataFileName + "_Jessica.n-triples"), SCIO_NAME_SPACE, RESOURCE_NAME_SPACE);

			converter.addIgnoreProperty(RDF_SCHEMA_COMMENT);
			converter.addIgnoreProperty(RDF_SCHEMA_LABEL);

			final double rand = random.nextDouble();

			final EInstanceContext context = rand < 0.6 ? EInstanceContext.TRAIN
					: rand < 0.8 ? EInstanceContext.DEVELOPMENT : EInstanceContext.TEST;

			converter.convert(context, new File(finalInstancesDir, fileName), entityType, includeSubEntities,
					jsonPrettyString, deepRec);

		}
	}

}
