package de.uni.bielefeld.sc.hterhors.psink.scio.corpus.ner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.ShuffleCorpusDistributor;
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
import de.hterhors.semanticmr.tools.specifications.DataStructureWriter;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.NERCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOSlotTypes;

public class BuildNERCorpusFromSlotFillingData {

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");
	public static final File SRC_MAIN_RESOURCES = new File("src/main/resources/");

	private static final File ROOT_DATA_STRUCTURE_DIR = new File(SRC_MAIN_RESOURCES,
			NERCorpusBuilderBib.DATA_STRUCTURE_DIR_NAME);

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

		buildForOrganismModel();
		buildForInjuryModel();
		buildForTreatment();
		buildForVertebralArea();
		buildForDeliveryMethod();
		buildForInvestigationMethod();
		buildForGroupName();
		buildForTrend();

	}

	private static void buildForTrend() throws Exception {
		SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasTrend.includeRec();
		buildSubDataStructureFiles(SCIOEntityTypes.trend);
		buildInstances(SCIOEntityTypes.trend);
		SlotType.restoreExcludance();
	}

	private static void buildForGroupName() throws Exception {

		SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasGroupName.include();
		buildSubDataStructureFiles(SCIOEntityTypes.groupName);
		buildInstances(SCIOEntityTypes.definedExperimentalGroup, SCIOEntityTypes.groupName);
		SlotType.restoreExcludance();
	}

	private static void buildForInvestigationMethod() throws Exception {

		SlotType.storeExcludance();
		SlotType.excludeAll();
//		SCIOSlotTypes.hasLocation.include();
		buildSubDataStructureFiles(SCIOEntityTypes.investigationMethod);
		buildInstances(SCIOEntityTypes.investigationMethod);
		SlotType.restoreExcludance();

	}

	private static void buildForVertebralArea() throws Exception {

		SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasLowerVertebrae.include();
		SCIOSlotTypes.hasUpperVertebrae.include();
		buildSubDataStructureFiles(SCIOEntityTypes.vertebralArea);
		buildInstances(SCIOEntityTypes.vertebralArea);
		SlotType.restoreExcludance();
	}

	private static void buildForDeliveryMethod() throws Exception {

		SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasDirection.include();
		SCIOSlotTypes.hasLocation.include();
		buildSubDataStructureFiles(SCIOEntityTypes.deliveryMethod);
		buildInstances(SCIOEntityTypes.deliveryMethod);
		SlotType.restoreExcludance();

		buildSubDataStructureFiles(SCIOEntityTypes.deliveryMethod);

	}

	private static void buildForTreatment() throws Exception {

		SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasDeliveryMethod.include();
		SCIOSlotTypes.hasCompound.include();
		SCIOSlotTypes.hasDirection.include();
		SCIOSlotTypes.hasLocation.include();
		SCIOSlotTypes.hasDosage.include();
		buildSubDataStructureFiles(SCIOEntityTypes.treatment);
		buildInstances(SCIOEntityTypes.treatment);
		SlotType.restoreExcludance();
	}

	private static void buildForInjuryModel() throws Exception {

		SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasInjuryDevice.include();
		SCIOSlotTypes.hasInjuryLocation.include();
		SCIOSlotTypes.hasInjuryAnaesthesia.include();
		SCIOSlotTypes.hasLowerVertebrae.include();
		SCIOSlotTypes.hasUpperVertebrae.include();
		buildSubDataStructureFiles(SCIOEntityTypes.injury);
		buildInstances(SCIOEntityTypes.injury);
		SlotType.restoreExcludance();
	}

	private static void buildForOrganismModel() throws Exception {

		SlotType.storeExcludance();
		SlotType.excludeAll();
		SCIOSlotTypes.hasAge.include();
		SCIOSlotTypes.hasGender.include();
		SCIOSlotTypes.hasAgeCategory.include();
		SCIOSlotTypes.hasOrganismSpecies.include();
		SCIOSlotTypes.hasWeight.include();
		buildSubDataStructureFiles(SCIOEntityTypes.organismModel);
		buildInstances(SCIOEntityTypes.organismModel);
		SlotType.restoreExcludance();
	}

	private static void buildSubDataStructureFiles(EntityType rootEntityType) throws Exception {

		final String dataStructureDirName = NERCorpusBuilderBib.toDirName(rootEntityType)
				+ NERCorpusBuilderBib.DATA_STRUCTURE_DIR_NAME;

		File finalDataStructureDir = new File(NERCorpusBuilderBib.NER_DIR, dataStructureDirName);
		finalDataStructureDir.mkdirs();

//		DataStructureWriter.writeEntityDataStructureFile(NERCorpusBuilderBib.buildEntitiesFile(rootEntityType),
//				rootEntityType);
//		NERCorpusBuilderBib.buildHierarchiesFile(rootEntityType).createNewFile();
//		NERCorpusBuilderBib.buildSlotsFile(rootEntityType).createNewFile();
//		NERCorpusBuilderBib.buildStructuresFile(rootEntityType).createNewFile();

		DataStructureWriter.writeEntityDataStructureFile(NERCorpusBuilderBib.buildEntitiesFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeHierarchiesDataStructureFile(NERCorpusBuilderBib.buildHierarchiesFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeSlotsDataStructureFile(NERCorpusBuilderBib.buildSlotsFile(rootEntityType),
				rootEntityType);
		DataStructureWriter.writeStructuresDataStructureFile(ROOT_DATA_STRUCTURE_STRUCTURES,
				NERCorpusBuilderBib.buildStructuresFile(rootEntityType), rootEntityType);

	}

	private static void buildInstances(EntityType entityType) throws Exception {
		buildInstances(entityType, entityType);
	}

	private static void buildInstances(EntityType rootEntityType, EntityType entityTypeOfInterest) throws Exception {

		NERCorpusBuilderBib.NER_DIR.mkdir();

		AbstractCorpusDistributor shuffleCorpusDistributor = new ShuffleCorpusDistributor.Builder()
				.setCorpusSizeFraction(1F).setTrainingProportion(80).setTestProportion(20).setSeed(RANDOM_SEED).build();
		InstanceProvider.maxNumberOfAnnotations = 120;
		InstanceProvider instanceProvider = new InstanceProvider(
				SlotFillingCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(rootEntityType),
				shuffleCorpusDistributor);

		for (Instance instance : instanceProvider.getInstances()) {
			
			List<Instance> newInstances = new ArrayList<>();
			Set<DocumentLinkedAnnotation> annotations = new HashSet<>();

			for (EntityTemplate annotation : instance.getGoldAnnotations().<EntityTemplate>getAnnotations()) {
				extractAnnotations(annotations, annotation);
			}

			if (entityTypeOfInterest != rootEntityType) {
				Set<DocumentLinkedAnnotation> unifyET = new HashSet<>();

				for (DocumentLinkedAnnotation abstractAnnotation : annotations) {
					unifyET.add(new DocumentLinkedAnnotation(abstractAnnotation.document, entityTypeOfInterest,
							abstractAnnotation.textualContent, abstractAnnotation.documentPosition));
				}

				annotations = unifyET;
			}
			
//			projectAnnotationsIntoDocument(instance.getDocument(), annotations);

			newInstances.add(new Instance(instance.getOriginalContext(), instance.getDocument(),
					new Annotations(new ArrayList<>(annotations))));

			InstancesToJsonInstanceWrapper conv = new InstancesToJsonInstanceWrapper(newInstances);

			File dir = NERCorpusBuilderBib.getDefaultInstanceDirectoryForEntity(entityTypeOfInterest);
			dir.mkdirs();

			JsonInstanceIO io = new JsonInstanceIO(true);
			io.writeInstances(new File(dir, instance.getName() + ".json"), conv.convertToWrapperInstances());

		}
	}

	private static void extractAnnotations(Set<DocumentLinkedAnnotation> annotations, EntityTemplate annotation) {

		if (annotation.getRootAnnotation().isInstanceOfDocumentLinkedAnnotation()) {
			DocumentLinkedAnnotation a = annotation.getRootAnnotation().asInstanceOfDocumentLinkedAnnotation();
			annotations.add(a);
		}

		EntityTemplateAnnotationFilter filter = annotation.filter().docLinkedAnnoation().nonEmpty().merge().multiSlots()
				.singleSlots().build();
		for (Entry<SlotType, Set<AbstractAnnotation>> a : filter.getMergedAnnotations().entrySet()) {
			for (AbstractAnnotation documentLinkedAnnotation : a.getValue()) {
				annotations.add(documentLinkedAnnotation.asInstanceOfDocumentLinkedAnnotation());
			}
		}

		EntityTemplateAnnotationFilter filter2 = annotation.filter().merge().entityTemplateAnnoation().multiSlots()
				.nonEmpty().singleSlots().build();

		for (Entry<SlotType, Set<AbstractAnnotation>> a : filter2.getMergedAnnotations().entrySet()) {
			for (AbstractAnnotation abstractAnnotation : a.getValue()) {
				extractAnnotations(annotations, abstractAnnotation.asInstanceOfEntityTemplate());
			}
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

}
