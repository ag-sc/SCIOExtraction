package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.semanticmr.corpus.InstanceProvider;
import de.hterhors.semanticmr.corpus.distributor.AbstractCorpusDistributor;
import de.hterhors.semanticmr.corpus.distributor.SpecifiedDistributor;
import de.hterhors.semanticmr.crf.exploration.SlotFillingExplorer;
import de.hterhors.semanticmr.crf.structure.IEvaluatable.Score;
import de.hterhors.semanticmr.crf.variables.Instance;
import de.hterhors.semanticmr.crf.variables.State;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.hterhors.semanticmr.projects.AbstractSemReadProject;
import de.uni.bielefeld.sc.hterhors.psink.scio.corpus.helper.SlotFillingCorpusBuilderBib;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.DataStructureLoader;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.SCIOEntityTypes;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.AgeNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.literal_normalization.WeightNormalization;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.slot_filling.orgmodel.OrganismModelRestrictionProvider.EOrgModelModifications;

/**
 * Slot filling for organism models.
 * 
 * @author hterhors
 * 
 *         Mean Score: Score [getF1()=0.880, getPrecision()=0.928,
 *         getRecall()=0.836, tp=168, fp=13, fn=33, tn=0]
 * 
 *         CRFStatistics [context=Train, getTotalDuration()=125115]
 * 
 *         CRFStatistics [context=Test, getTotalDuration()=3992]
 * 
 *         Compute coverage... Coverage Training: Score [getF1()=0.901,
 *         getPrecision()=1.000, getRecall()=0.820, tp=643, fp=0, fn=141, tn=0]
 * 
 *         Compute coverage... Coverage Development: Score [getF1()=0.931,
 *         getPrecision()=1.000, getRecall()=0.871, tp=175, fp=0, fn=26, tn=0]
 * 
 * 
 *         modelName: OrganismModel930148736
 *
 */
public class OrgModelSlotFilling {

	/**
	 * Start the slot filling procedure.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new OrgModelSlotFilling();
	}

	private static Logger log = LogManager.getFormatterLogger("SlotFilling");

	/**
	 * The directory of the corpus instances. In this example each instance is
	 * stored in its own json-file.
	 */
	private final File instanceDirectory;

//	private final EOrgModelModifications rule;

	public final String header = "Mode\tF1\tPrecision\tRecall";

	private final static DecimalFormat resultFormatter = new DecimalFormat("#.##");

	public OrgModelSlotFilling() throws IOException {

		/**
		 * Initialize the system.
		 * 
		 * The scope represents the specifications of the 4 defined specification files.
		 * The scope mainly affects the exploration.
		 */
		SystemScope scope = SystemScope.Builder.getScopeHandler()
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

		instanceDirectory = SlotFillingCorpusBuilderBib
				.getDefaultInstanceDirectoryForEntity(SCIOEntityTypes.organismModel);

		PrintStream resultsOut = new PrintStream(new File("results/organismModelResults.csv"));

		resultsOut.println(header);

		for (EOrgModelModifications rule : EOrgModelModifications.values()) {

			rule = EOrgModelModifications.SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE;

			OrganismModelRestrictionProvider.applySlotTypeRestrictions(rule);

			List<String> testInstanceNames2 = new ArrayList<>();
			List<String> trainingInstanceNames2 = Arrays.asList(
					"N009 Lee 2013 23804083",
					"N030 Massey 2006 16624960",
					"N158 Kang 2015",
					"N004 Bukhari, Torres et al. 2011",
					"N190 Pearse 2004",
					"N227 Yamamoto 2009",
					"N078 Aoki 2010",
					"N224 Wu 2015",
					"N022 Yick 2003 12821386",
					"N155 Jeffery 2005",
					"N023 Hunanyan 2013 23864374",
					"N167 Li 2012",
					"N231 Zhang 2011",
					"N157 Kalincik 2010",
					"N223 Wu 2008",
					"N067 Shinozaki, Iwanami et al. 2016",
					"N215 Verdu 2001",
					"N147 Garc%2B¡a-Al%2B¡as 2004",
					"N144 Ferrero-Gutierrez 2013",
					"N122 Yick, So et al. 2004");
			
//			Collections.shuffle(trainingInstanceNames2, new Random(1000L));
//			Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE	0.91	0.92	0.9
//			Score: SPECIES_GENDER_WEIGHT_AGE_CATEGORY_AGE	0.9	0.93	0.88

			List<String> developInstanceNames2 = Arrays.asList("N213 Torres-Espin 2013", "N244 Cloutier 2016",
					"N165 Li 2010", "N228 Yazdani 2012", "N173 L+¦pez-Vales 2006", "N166 Li 2011",
					"N113 Hyatt, Wang et al. 2010", "N058 Iaci, Vecchione et al. 2007", "N046 Imagama 2011 22114278",
					"N024 Chau 2004 14630702", "N060 Karismi-Abdolrezaee. 2010", "N098 Lee, Kim et al. 2016",
					"N089 Carter, McMahon et al. 2011", "N050 Alilain, Horn et al. 2011", "N187 Nash 2002",
					"N185 Munoz-Quiles 2009", "N226 Xiao 2007", "N040 Kim 2006 16705682", "N249 Khankan",
					"N211 Toft 2012", "N188 Negredo 2008", "N200 Sasaki 2004", "N139 Collazos-Castro 2005",
					"N196 Richter 2005", "N133 Bretzner 2008", "N049 Wang 2011 21299884", "N182 Moon 2006",
					"N013 CaffertyCafferty", "N209 Tharion 2011", "N219 Ramón-Cueto 1998", "N191 Pearse 2007",
					"N110 Cheng, C.-H.a  b  c, Lin et al. 2015", "N176 Lu 2006", "N077 Andrews 2007",
					"N230 Zhang 2011 2", "N189 Novikova 2011", "N034 Wilems 2015 26384702",
					"N114 Iseda, Okuda et al. 2008", "N127 Bowes, Massey et al. 2012", "N119 Wu, Klaw et al. 2015");

			AbstractCorpusDistributor corpusDistributor = new SpecifiedDistributor.Builder()
					.setTestInstanceNames(testInstanceNames2).setTrainingInstanceNames(trainingInstanceNames2)
					.setDevelopInstanceNames(developInstanceNames2).build();

			// AbstractCorpusDistributor corpusDistributor = new
			// ShuffleCorpusDistributor.Builder().setSeed(100L)
//					.setTrainingProportion(80).setDevelopmentProportion(20).setCorpusSizeFraction(1F).build();

			InstanceProvider instanceProvider = new InstanceProvider(instanceDirectory, corpusDistributor,
					OrganismModelRestrictionProvider.getByRule(rule));

			List<String> trainingInstanceNames = instanceProvider.getRedistributedTrainingInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> developInstanceNames = instanceProvider.getRedistributedDevelopmentInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			List<String> testInstanceNames = instanceProvider.getRedistributedTestInstances().stream()
					.map(t -> t.getName()).collect(Collectors.toList());

			String modelName = "OrganismModel" + new Random().nextInt();

			OrgModelSlotFillingPredictor predictor = new OrgModelSlotFillingPredictor(modelName, scope,
					trainingInstanceNames, developInstanceNames, testInstanceNames, rule);

			predictor.trainOrLoadModel();

			Map<Instance, State> finalStates = predictor.evaluateOnDevelopment();

			Score score = AbstractSemReadProject.evaluate(log, finalStates, predictor.predictionObjectiveFunction);

			resultsOut.println(toResults(rule, score));

			System.out.println(SlotFillingExplorer.averageNumberOfNewProposalStates);
			/**
			 * Finally, we evaluate the produced states and print some statistics.
			 */

//			final Score trainCoverage = predictor.computeCoverageOnTrainingInstances(false);
//			log.info("Coverage Training: " + trainCoverage);
//
//			final Score devCoverage = predictor.computeCoverageOnDevelopmentInstances(false);
//			log.info("Coverage Development: " + devCoverage);

			/**
			 * Computes the coverage of the given instances. The coverage is defined by the
			 * objective mean score that can be reached relying on greedy objective function
			 * sampling strategy. The coverage can be seen as the upper bound of the system.
			 * The upper bound depends only on the exploration strategy, e.g. the provided
			 * NER-annotations during slot-filling.
			 */
			log.info("Score: " + toResults(rule, score));
			log.info("modelName: " + predictor.modelName);
			/**
			 * TODO: Compare results with results when changing some parameter. Implement
			 * more sophisticated feature-templates.
			 */
			break;
		}

		resultsOut.flush();
		resultsOut.close();

//		Map<String, Set<AbstractAnnotation>> organismModelAnnotations = predictor
//				.predictInstances(new HashSet<>(testInstanceNames), 1);
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

	private String toResults(EOrgModelModifications rule, Score score) {
		return rule.name() + "\t" + resultFormatter.format(score.getF1()) + "\t"
				+ resultFormatter.format(score.getPrecision()) + "\t" + resultFormatter.format(score.getRecall());
	}

}
