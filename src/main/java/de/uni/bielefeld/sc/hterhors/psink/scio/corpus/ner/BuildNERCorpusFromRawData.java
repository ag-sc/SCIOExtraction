package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.ner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReader;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.AnnotationBuilder;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.init.reader.csv.CSVDataStructureReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.json.JsonInstanceIO;
import de.hterhors.semanticmr.json.converter.InstancesToJsonInstanceWrapper;
import de.hterhors.semanticmr.tools.specifications.DataStructureWriter;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class BuildNERCorpusFromRawData {

	public static final int CLASS_TYPE_INDEX = 1;
	public static final int ONSET_INDEX = 2;
	public static final int TEXT_MENTION_INDEX = 4;

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
	public static final File SRC_MAIN_RESOURCES = new File("src/main/resources/");

	private static final File ROOT_DATA_STRUCTURE_DIR = new File(SRC_MAIN_RESOURCES,
			NERCorpusBuilderBib.DATA_STRUCTURE_DIR_NAME);
	private static final File SANTO_RAW_DATA_DIR = new File(SRC_MAIN_RESOURCES, NERCorpusBuilderBib.RAW_DATA_DIR_NAME);
	private static final File ROOT_DATA_STRUCTURE_ENTITIES = new File(ROOT_DATA_STRUCTURE_DIR,
			NERCorpusBuilderBib.ENTITIES_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_SLOTS = new File(ROOT_DATA_STRUCTURE_DIR,
			NERCorpusBuilderBib.SLOTS_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_STRUCTURES = new File(ROOT_DATA_STRUCTURE_DIR,
			NERCorpusBuilderBib.STRUCTURES_FILE_NAME);
	private static final File ROOT_DATA_STRUCTURE_HIERARCHIES = new File(ROOT_DATA_STRUCTURE_DIR,
			NERCorpusBuilderBib.HIERARCHIES_FILE_NAME);

	private static final CSVDataStructureReader dataStructureReader = new CSVDataStructureReader(
			ROOT_DATA_STRUCTURE_ENTITIES, ROOT_DATA_STRUCTURE_HIERARCHIES, ROOT_DATA_STRUCTURE_SLOTS,
			ROOT_DATA_STRUCTURE_STRUCTURES);

	private static final long RANDOM_SEED = 1000L;

	public static void main(String[] args) throws Exception {
		SystemScope.Builder.getScopeHandler().addScopeSpecification(dataStructureReader).build();

		/**
		 * TODO: needs specific adjustments for individuals entity types. Decide which
		 * annotations should be considered.
		 */

//		buildForOrganismModel();
//		buildForInjuryModel();
//		buildForTreatment();
//		buildForVertebralArea();
//		buildForDeliveryMethod();
//		buildForInvestigationMethod();
//		buildForGroupName();
		buildForCompound();
//		buildForTrend();

	}

	private static void buildForTrend() throws Exception {
		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasTrend.includeRec();
		buildSubDataStructureFiles(SCIOEntityTypes.trend);
		buildInstances(SCIOEntityTypes.trend, SCIOEntityTypes.result);
		SlotType.restoreExcludance(x);
	}

	private static void buildForGroupName() throws Exception {

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasGroupName.include();
		buildSubDataStructureFiles(SCIOEntityTypes.groupName);
		buildInstances(SCIOEntityTypes.groupName, SCIOEntityTypes.experimentalGroup);
		SlotType.restoreExcludance(x);
	}

	private static void buildForCompound() throws Exception {
		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		buildSubDataStructureFiles(SCIOEntityTypes.compound);
		buildInstances(SCIOEntityTypes.compound, SCIOEntityTypes.treatment);
		SlotType.restoreExcludance(x);
	}

	private static void buildForInvestigationMethod() throws Exception {

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
//		SCIOSlotTypes.hasLocation.include();
		buildSubDataStructureFiles(SCIOEntityTypes.investigationMethod);
		buildInstances(SCIOEntityTypes.investigationMethod, SCIOEntityTypes.result);
		SlotType.restoreExcludance(x);

	}

	private static void buildForVertebralArea() throws Exception {

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasLowerVertebrae.include();
		SCIOSlotTypes.hasUpperVertebrae.include();
		buildSubDataStructureFiles(SCIOEntityTypes.vertebralArea);
		buildInstances(SCIOEntityTypes.vertebralArea, SCIOEntityTypes.injury);
		SlotType.restoreExcludance(x);
	}

	private static void buildForDeliveryMethod() throws Exception {

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasDirection.include();
		SCIOSlotTypes.hasLocation.include();
		buildSubDataStructureFiles(SCIOEntityTypes.deliveryMethod);
		buildInstances(SCIOEntityTypes.deliveryMethod, SCIOEntityTypes.experimentalGroup);
		SlotType.restoreExcludance(x);

		buildSubDataStructureFiles(SCIOEntityTypes.deliveryMethod);

	}

	private static void buildForTreatment() throws Exception {

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasDeliveryMethod.include();
		SCIOSlotTypes.hasCompound.include();
		SCIOSlotTypes.hasDirection.include();
		SCIOSlotTypes.hasLocation.include();
		SCIOSlotTypes.hasDosage.include();
		buildSubDataStructureFiles(SCIOEntityTypes.treatment);
		buildInstances(SCIOEntityTypes.treatment, SCIOEntityTypes.treatment);
		SlotType.restoreExcludance(x);
	}

	private static void buildForInjuryModel() throws Exception {

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasInjuryDevice.include();
		SCIOSlotTypes.hasInjuryLocation.include();
		SCIOSlotTypes.hasInjuryAnaesthesia.include();
		SCIOSlotTypes.hasLowerVertebrae.include();
		SCIOSlotTypes.hasUpperVertebrae.include();
		buildSubDataStructureFiles(SCIOEntityTypes.injury);
		buildInstances(SCIOEntityTypes.injury, SCIOEntityTypes.injury);
		SlotType.restoreExcludance(x);
	}

	private static void buildForOrganismModel() throws Exception {

		Map<SlotType, Boolean> x = SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasAge.include();
		SCIOSlotTypes.hasGender.include();
		SCIOSlotTypes.hasAgeCategory.include();
		SCIOSlotTypes.hasOrganismSpecies.include();
		SCIOSlotTypes.hasWeight.include();
		buildSubDataStructureFiles(SCIOEntityTypes.organismModel);
		buildInstances(SCIOEntityTypes.organismModel, SCIOEntityTypes.organismModel);
		SlotType.restoreExcludance(x);
	}

	private static void buildSubDataStructureFiles(EntityType rootEntityType) throws Exception {

		final String dataStructureDirName = NERCorpusBuilderBib.toDirName(rootEntityType)
				+ NERCorpusBuilderBib.DATA_STRUCTURE_DIR_NAME;

		File finalDataStructureDir = new File(NERCorpusBuilderBib.NER_DIR, dataStructureDirName);
		finalDataStructureDir.mkdirs();

		DataStructureWriter.writeEntityDataStructureFile(NERCorpusBuilderBib.buildEntitiesFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeHierarchiesDataStructureFile(NERCorpusBuilderBib.buildHierarchiesFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeSlotsDataStructureFile(NERCorpusBuilderBib.buildSlotsFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeStructuresDataStructureFile(ROOT_DATA_STRUCTURE_STRUCTURES,
				NERCorpusBuilderBib.buildStructuresFile(rootEntityType), rootEntityType);

	}

	private static void buildInstances(EntityType rootEntityType, EntityType mainEntity) throws Exception {

		NERCorpusBuilderBib.NER_DIR.mkdir();

		AbstractCorpusDistributor shuffleCorpusDistributor = new ShuffleCorpusDistributor.Builder()
				.setCorpusSizeFraction(1F).setTrainingProportion(80).setTestProportion(20).setSeed(RANDOM_SEED).build();

		InstanceProvider.maxNumberOfAnnotations = 120;
		InstanceProvider instanceProvider = new InstanceProvider(
				SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(mainEntity), shuffleCorpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {

			List<Instance> newInstances = new ArrayList<>();

			Set<DocumentLinkedAnnotation> annotations = readAnnotations(instance.getDocument(), instance.getName(),
					rootEntityType);

//			projectAnnotationsIntoDocument(instance.getDocument(), annotations);

			newInstances.add(new Instance(instance.getOriginalContext(), instance.getDocument(),
					new Annotations(new ArrayList<>(annotations))));

			InstancesToJsonInstanceWrapper conv = new InstancesToJsonInstanceWrapper(newInstances);

			File dir = NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(rootEntityType);
			dir.mkdirs();

			JsonInstanceIO io = new JsonInstanceIO(true);
			io.writeInstances(new File(dir, instance.getName() + ".json"), conv.convertToWrapperInstances());

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

	private static Set<DocumentLinkedAnnotation> readAnnotations(Document document, String fileName,
			EntityType rootEntityType) throws IOException {
		CSVReader reader = null;

		final Set<DocumentLinkedAnnotation> annotations = new HashSet<>();

		reader = new CSVReader(new FileReader(new File(SANTO_RAW_DATA_DIR, fileName + "_Jessica.annodb")));
		String[] line;
		while ((line = reader.readNext()) != null) {

			if (line.length == 1 && line[0].isEmpty() || line[0].startsWith("#"))
				continue;
			try {
				DocumentLinkedAnnotation annotation = AnnotationBuilder.toAnnotation(document,
						line[CLASS_TYPE_INDEX].trim(), line[TEXT_MENTION_INDEX].trim(),
						Integer.parseInt(line[ONSET_INDEX].trim()));

				if (rootEntityType.getHierarchicalEntityTypes().contains(annotation.getEntityType()))
					annotations.add(annotation);

				/**
				 * For group names
				 */
				// if (EntityType.get("ExperimentalGroup").getTransitiveClosureSubEntityTypes()
//						.contains(annotation.getEntityType())
//						|| rootEntityType.getRelatedEntityTypes().contains(annotation.getEntityType()))
//					annotations.add(AnnotationBuilder.toAnnotation(annotation.document, rootEntityType,
//							annotation.getSurfaceForm(), annotation.getStartDocCharOffset()));

			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		reader.close();

		return annotations;
	}
}
