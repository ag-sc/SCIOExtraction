package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.hterhors.semanticmr.corpus.EInstanceContext;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.variables.Annotations;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.nerla.annotation.RegularExpressionNerlAnnotator;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.slot_filling.helper.ResolvePairwiseComparedGroups.SantoAnnotations;
import de.uni.bielefeld.sc.hterhors.psink.scio.rdf.ConvertToRDF;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.ENERModus;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.nerla.OrganismModelPattern;

public class ApplyOrganismModelExtractionToText {

	public static void main(String[] args) throws IOException {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		SystemScope.Builder.getScopeHandler()
				/**
				 * We add a scope reader that reads and interprets the 4 specification files.
				 */
				.addScopeSpecification(DataStructureLoader.loadSlotFillingDataStructureReader("OrganismModel"))
				/**
				 * We apply the scope, so that we can add normalization functions for various
				 * literal entity types, if necessary.
				 */
				.apply()
				/**
				 * Now normalization functions can be added. A normalization function is
				 * especially used for literal-based annotations. In case a normalization
				 * function is provided for a specific entity type, the normalized value is
				 * compared during evaluation instead of the actual surface form. A
				 * normalization function normalizes different surface forms so that e.g. the
				 * weights "500 g", "0.5kg", "500g" are all equal. Each normalization function
				 * is bound to exactly one entity type.
				 */
				.registerNormalizationFunction(new WeightNormalization())
				.registerNormalizationFunction(new AgeNormalization())
				/**
				 * Finally, we build the systems scope.
				 */
				.build();

		List<Instance> instances = new ArrayList<>();
		OrganismModelRestrictionProvider
				.applySlotTypeRestrictions(EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE);

		List<String> testInstanceNames = new ArrayList<>();

//		int a = 0;
//		final int max = new File("Upload/XML1/").listFiles().length;

		instances = Arrays.asList(new File("Upload/XML1/").listFiles()).parallelStream().limit(10).map(file -> {

			try {

				String content = Files.readAllLines(file.toPath()).stream().reduce("", String::concat);
				System.out.println(file.getName());
				Document d = new Document(file.getName(), content);
				testInstanceNames.add(file.getName());
				Instance i = new Instance(EInstanceContext.UNSPECIFIED, d, new Annotations());

				RegularExpressionNerlAnnotator annotator = new RegularExpressionNerlAnnotator(
						new OrganismModelPattern(SCIOEntityTypes.organismModel));

				Map<EntityType, Set<DocumentLinkedAnnotation>> annotations = annotator.annotate(i.getDocument());

				for (EntityType string : annotations.keySet()) {
					i.addCandidateAnnotations(annotations.get(string));
				}

				return i;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			return null;

		}).filter(f -> f != null).collect(Collectors.toList());

//		for (File file : ) {
//
//			a++;
//			if (a == 1000)
//				break;
//
//			System.out.println(a + " / " + max);
//		}
//
		String modelName = "OrganismModel_PREDICT";

		OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(modelName, new ArrayList<>(),
				new ArrayList<>(), testInstanceNames, EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE,
				ENERModus.PREDICT);

		predictor.trainOrLoadModel();

		List<EntityTemplate> annotations = new ArrayList<>();

		for (Entry<Instance, State> instance : predictor.predictInstance(instances).entrySet()) {

			System.out.println(instance.getKey().getName() + "\t"
					+ instance.getValue().getCurrentPredictions().getAnnotations().get(0).toPrettyString());
			annotations.addAll(instance.getValue().getCurrentPredictions().getAnnotations());
		}

		new ConvertToRDF(new File("organismModel.n-triples"), annotations);
		//
//
//
//		int docID = 0;
//		for (Entry<String, Set<AbstractAnnotation>> annotations : organismModelAnnotations.entrySet()) {
//
//			SantoAnnotations collectRDF = new SantoAnnotations(new HashSet<>(), new HashMap<>());
//			for (AbstractAnnotation annotation : annotations.getValue()) {
//
//				AnnotationsToSantoAnnotations.collectRDF(annotation, collectRDF, "http://scio/data/",
//						"http://psink.de/scio/");
//
//			}
//			PrintStream psRDF = new PrintStream(
//					"autoextraction/organismmodel/" + annotations.getKey() + "_AUTO.n-triples");
//			PrintStream psAnnotation = new PrintStream(
//					"autoextraction/organismmodel/" + annotations.getKey() + "_AUTO.annodb");
////			PrintStream psDocument = new PrintStream("unroll/organismmodel/" + annotations.getKey() + "_export.csv");
//
//			List<String> c = new ArrayList<>(collectRDF.getRdf().stream().collect(Collectors.toList()));
//			List<String> c2 = new ArrayList<>(collectRDF.getAnnodb().stream().collect(Collectors.toList()));
//			Collections.sort(c);
//			Collections.sort(c2);
//			c.forEach(psRDF::println);
//			c2.forEach(psAnnotation::println);
//			psAnnotation.close();
//			psRDF.close();
//
////			psDocument.print(toCSV(docID, instance.getDocument().tokenList));
////			psDocument.close();
//			docID++;
//		}

	}

}
