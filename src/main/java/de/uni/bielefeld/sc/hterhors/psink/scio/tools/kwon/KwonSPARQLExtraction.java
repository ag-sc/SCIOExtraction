package de.uni.bielefeld.sc.hterhors.psink.scio.tools.kwon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class KwonSPARQLExtraction {

	public static void main(String[] args) throws IOException {

		new KwonSPARQLExtraction();

	}

	Model model = ModelFactory.createDefaultModel();

	private PrintStream dataPrinter;

	enum EScoreTypes {
		PRIMATE, LARGER_ANIMAL, RAT, MOUSE, CONTUSION, CLIP_COMPRESSION, PARTIAL_TRANSECTION, TIME_MIN_MAX, TIME_PRIOR,
		CLINICALLY_MF_BBB, CLINICALLY_MF_CERVICAL_MOTOR, CLINICALLY_MF_NON_BBB, CLINICALLY_MF_CERVICAL_NON_MOTOR,
		CLINICALLY_MF_DOSAGE_RESPONSE;

	}

	public KwonSPARQLExtraction() throws IOException {

		model.read(new FileInputStream("Excel2RDF.n-triples"), null, "N-TRIPLES");
		dataPrinter = new PrintStream(new File("kwon_data.csv"));
//		model.read(new FileInputStream("OEC.n-triples"), null, "N-TRIPLES");
//		dataPrinter = new PrintStream(new File("oec_data.csv"));
//
		String pubmedIds = getPubmedIDsOfEffacyPublications();
////
//		getAnimalScore(EScoreTypes.PRIMATE, pubmedIds, getPrimateSpecies());
//		getAnimalScore(EScoreTypes.LARGER_ANIMAL, pubmedIds, getLargerAnimalSpecies());
//		getAnimalScore(EScoreTypes.RAT, pubmedIds, getRatSpecies());
//		getAnimalScore(EScoreTypes.MOUSE, pubmedIds, getMouseSpecies());
////
//		getContusionScore(pubmedIds);
//		getClipCompressionScore(pubmedIds);
//		getPartialTransectionSharpScore(pubmedIds);
////
		timeWindowScore(pubmedIds);
////
//		clinicallyMeaningFullBBB();
//		clinicallyMeaningFullOtherMotor();
//		clinicallyMeaningFullCervicalMotor();
//		clinicallyMeaningFullCervicalNonMotor();
//		clinicallyMeaningFullDosageResponseResult();
//		publicationReproduceability();
	}

	private int hourToMin(int hour) {
		return hour * 60;
	}

//	Interpretation Thoracic | Cervical + Ref TreatmentDosage != Treat TreatmentDosage  + Positive Judgement 

//		2 Fall (input 1 Result)
//		Ctrl. Dosage != Treat Dosage & judgement = Positive 
//

//			1 Fall (Input zwei Results) 
//		1 Treat. Treatment = 2 Treat Treatment  &
//	1Ctrl. Dosage = 2 Ctrl. Dosage  &
//			1Treat Dosage != 2treat Dosage &
//			1 judgement != 2 Judgement (Neutral+negative vs. positive )
	public void clinicallyMeaningFullDosageResponseResult() {
		final String queryString = "SELECT DISTINCT ?pubmedID "
				+ "(GROUP_CONCAT (DISTINCT ?posTreat) AS ?posTreats) ?injuryLocation (GROUP_CONCAT (DISTINCT ?negTreat) AS ?negTreats)"
				+ "WHERE {" + " ?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
				+ " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."

				+ " ?experiment <http://psink.de/scio/hasResult> ?resultPos ."
				+ " ?resultPos <http://psink.de/scio/hasJudgement> ?judgementPos ."
				+ " ?judgementPos a <http://psink.de/scio/Positive> ."

				//
				+ " {" + "?resultPos <http://psink.de/scio/hasTargetGroup> ?posGroup ."
				+ " ?posGroup <http://psink.de/scio/hasTreatmentType> ?posTreatment ."

				+ " ?experiment <http://psink.de/scio/hasResult> ?resultNeg ."
				+ " ?resultNeg <http://psink.de/scio/hasJudgement> ?judgementNeg ."
				+ " ?judgementNeg a ?judgementNegV ." + " FILTER(?judgementNegV != <http://psink.de/scio/Positive>) "

				+ " ?resultNeg <http://psink.de/scio/hasTargetGroup> ?negGroup ."
//				compare two result
				+ "}UNION{"
//				within single result
				+ " ?resultPos <http://psink.de/scio/hasTargetGroup> ?posGroup ."
				+ " ?posGroup <http://psink.de/scio/hasTreatmentType> ?posTreatment ."
				+ " ?resultPos <http://psink.de/scio/hasReferenceGroup> ?negGroup ." + "}"
				//

				+ "{?posTreatment a ?compoundType . "
				+ "?posTreatment <http://psink.de/scio/hasTemperature> ?posDosage ." + "}UNION{"
				+ " ?posTreatment <http://psink.de/scio/hasDosage> ?posDosage ."
				+ " ?posTreatment <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType ." + "}"

				+ " BIND (CONCAT(STR(?compoundType),\" \", STR(?posDosage)) AS ?posTreat)"

				+ " ?negGroup <http://psink.de/scio/hasTreatmentType> ?negTreatment ."

				+ "{?negTreatment a ?compoundType . "
				+ "?negTreatment <http://psink.de/scio/hasTemperature> ?negDosage ." + "}UNION{"
				+ " ?negTreatment <http://psink.de/scio/hasDosage> ?negDosage ."
				+ " ?negTreatment <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"

				+ " BIND (CONCAT(STR(?compoundType),\" \", STR(?negDosage)) AS ?negTreat)"

				+ " ?posGroup <http://psink.de/scio/hasInjuryModel> ?posInjuryModel ."
				+ " ?posInjuryModel <http://psink.de/scio/hasInjuryLocation> ?injuryLocation."

				+ " ?negGroup <http://psink.de/scio/hasInjuryModel> ?negInjuryModel ."
				+ " ?negInjuryModel <http://psink.de/scio/hasInjuryLocation> ?injuryLocation."

				+ " VALUES ?injuryLocation { <http://psink.de/scio/Cervical> <http://psink.de/scio/Thoracic> }."

				+ "} GROUP BY ?pubmedID ?injuryLocation ?posTreat ?negTreat ";

		Query query = QueryFactory.create(queryString);
		System.out.println(query);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				String dosage1 = querySolution.getLiteral("posTreats").getString();
				String dosage2 = querySolution.getLiteral("negTreats").getString();
				if (!dosage1.equals(dosage2)) {
					String line = EScoreTypes.CLINICALLY_MF_DOSAGE_RESPONSE + "\t"
							+ querySolution.getLiteral("pubmedID") + "\t" + dosage1 + "\t" + dosage2 + "\t"
							+ querySolution.getResource("injuryLocation") + "\t"
							+ querySolution.getResource("judgementPos") + "\t"
							+ querySolution.getResource("judgementNeg");
					dataPrinter.println(line);
					System.out.println(line);
				}
			}
		}
	}

//	Interpretation Cervical  + FuncTest (ohne MotorTest +LocomotorTest + GaitTest ) + Positive Judgement  & Positive Judgement in NonFuncTest
	public void clinicallyMeaningFullCervicalNonMotor() {
		final String queryString = "SELECT DISTINCT (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
				+ " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?nonFunctionalResult ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasJudgement> ?nonFunctionJudgement ."
				+ " ?nonFunctionJudgement a <http://psink.de/scio/Positive> ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasInvestigationMethod> ?nonFunctionalMethod ."
				+ " ?nonFunctionalMethod a ?nonFunctionalTest ." + " VALUES ?nonFunctionalTest { "
				+ getNonFunctionalTests() + " } ." + " ?experiment <http://psink.de/scio/hasResult> ?functionalResult ."
				+ " ?functionalResult <http://psink.de/scio/hasInvestigationMethod> ?functionalMethod ."
				+ " ?functionalMethod a ?functionalTest." + " VALUES ?functionalTest { " + getNonMotorTests() + " } ."
				+ " ?functionalResult <http://psink.de/scio/hasJudgement> ?functionJudgement ."
				+ " ?functionJudgement a <http://psink.de/scio/Positive> . "

				+ " ?funcTreatmentGroup <http://psink.de/scio/hasInjuryModel> ?injuryModel ."
				+ " ?injuryModel <http://psink.de/scio/hasInjuryLocation> <http://psink.de/scio/Cervical>."
				+ " ?functionalResult <http://psink.de/scio/hasTargetGroup> ?funcTreatmentGroup ."
				+ "{?funcTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?funcTreatment ."
				+ " ?funcTreatment a ?compoundType. " + " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. "
				+ "}" + " UNION{" + " ?funcTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?funcTreatmentType."
				+ " ?funcTreatmentType <http://psink.de/scio/hasCompound> ?funcCompound ."
				+ " ?funcCompound a ?compoundType .}"

				+ " ?nonFunctionalResult <http://psink.de/scio/hasTargetGroup> ?nonFuncTreatmentGroup ."
				+ "{?nonFuncTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?nonFuncTreatment ."
				+ " ?nonFuncTreatment a ?compoundType. " + " VALUES ?compoundType { " + getNonCompoundTreatments()
				+ " }. " + "}UNION{"
				+ " ?nonFuncTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?nonFuncTreatmentType."
				+ " ?nonFuncTreatmentType <http://psink.de/scio/hasCompound> ?compound ."
				+ " ?compound a ?compoundType .}" + "} GROUP BY ?pubmedID ?treatmentGroup";

		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String line = EScoreTypes.CLINICALLY_MF_CERVICAL_NON_MOTOR + "\t" + querySolution.getLiteral("pubmedID")
						+ "\t" + querySolution.getLiteral("compoundTypes");
				dataPrinter.println(line);
				System.out.println(line);
			}
		}
	}

	private String getNonMotorTests() {
		return "<http://psink.de/scio/MechanicalAllodyniaTest> " + "<http://psink.de/scio/NormoxicBreathingTest>"
				+ "<http://psink.de/scio/SurfaceRightingReflexTest> "
				+ "<http://psink.de/scio/MotorEvokedPotentialsTest> " + "<http://psink.de/scio/HReflexTest> "
				+ "<http://psink.de/scio/TailFlickReflexTest> " + "<http://psink.de/scio/ElectromyographyTest>"
				+ " <http://psink.de/scio/PhysiologyTest>" + " <http://psink.de/scio/BladderFunctionTest>"
				+ " <http://psink.de/scio/PainTest> <http://psink.de/scio/BloodCirculationExamination>"
				+ " <http://psink.de/scio/BodyWeightExamination> <http://psink.de/scio/AvoidanceResponseTest> "
				+ " <http://psink.de/scio/GastricUlcerogenesisTest> "
				+ "<http://psink.de/scio/SomatosensoryEvokedPotentialsTest> <http://psink.de/scio/MotorReflexTest> "
				+ "<http://psink.de/scio/ContactPlacingResponseTest> <http://psink.de/scio/BreathingTest> "
				+ "<http://psink.de/scio/HypoxicBreathingTest> <http://psink.de/scio/InfectionExamination>"
				+ " <http://psink.de/scio/CardioVascularFunctionTest> <http://psink.de/scio/AdhesiveRemovalTest> "
				+ "<http://psink.de/scio/AutonomicDysreflexiaTest> <http://psink.de/scio/SensoryTest> "
				+ " <http://psink.de/scio/BloodBrainBarrierTest> " + "<http://psink.de/scio/ThermalHyperalgesiaTest>  "
				+ "<http://psink.de/scio/HindpawSensationTest> <http://psink.de/scio/ElectrophysiologyTest> "
				+ "<http://psink.de/scio/SpinalCordEvokedPotentialsTest>";
	}

//	Interpretation Cervical  + MotorTest +LocomotorTest + GaitTest + Positive Judgement  & Positive Judgement in NonFuncTest
	public void clinicallyMeaningFullCervicalMotor() {
		final String queryString = "SELECT DISTINCT (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
				+ " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?nonFunctionalResult ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasJudgement> ?nonFunctionJudgement ."
				+ " ?nonFunctionJudgement a <http://psink.de/scio/Positive> ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasInvestigationMethod> ?nonFunctionalMethod ."
				+ " ?nonFunctionalMethod a ?nonFunctionalTest ." + " VALUES ?nonFunctionalTest { "
				+ getNonFunctionalTests() + " } ." + " ?experiment <http://psink.de/scio/hasResult> ?functionalResult ."
				+ " ?functionalResult <http://psink.de/scio/hasInvestigationMethod> ?functionalMethod ."
				+ " ?functionalMethod a ?functionalTest." + " VALUES ?functionalTest { " + getMotorfunctionTests()
				+ " } ." + " ?functionalResult <http://psink.de/scio/hasJudgement> ?functionJudgement ."
				+ " ?functionJudgement a <http://psink.de/scio/Positive> . "

				+ " ?funcTreatmentGroup <http://psink.de/scio/hasInjuryModel> ?injuryModel ."
				+ " ?injuryModel <http://psink.de/scio/hasInjuryLocation> <http://psink.de/scio/Cervical>."

				+ " ?functionalResult <http://psink.de/scio/hasTargetGroup> ?funcTreatmentGroup ."
				+ "{?funcTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?funcTreatment ."
				+ " ?funcTreatment a ?compoundType. " + " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. "
				+ "}" + " UNION{" + " ?funcTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?funcTreatmentType."
				+ " ?funcTreatmentType <http://psink.de/scio/hasCompound> ?funcCompound ."
				+ " ?funcCompound a ?compoundType .}"

				+ " ?nonFunctionalResult <http://psink.de/scio/hasTargetGroup> ?nonFuncTreatmentGroup ."
				+ "{?nonFuncTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?nonFuncTreatment ."
				+ " ?nonFuncTreatment a ?compoundType. " + " VALUES ?compoundType { " + getNonCompoundTreatments()
				+ " }. " + "}" + " UNION{"
				+ " ?nonFuncTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?nonFuncTreatmentType."
				+ " ?nonFuncTreatmentType <http://psink.de/scio/hasCompound> ?compound ."
				+ " ?compound a ?compoundType .}" + "} GROUP BY ?pubmedID ?treatmentGroup";

		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String line = EScoreTypes.CLINICALLY_MF_CERVICAL_MOTOR + "\t" + querySolution.getLiteral("pubmedID")
						+ "\t" + querySolution.getLiteral("compoundTypes");
				dataPrinter.println(line);
				System.out.println(line);
			}
		}
	}

	private String getMotorfunctionTests() {
		return "<http://psink.de/scio/NeurologicScalesTest> <http://psink.de/scio/BMSTest> <http://psink.de/scio/GaleRatingScoreTest> <http://psink.de/scio/BBBTest> <http://psink.de/scio/OpenFieldTest> <http://psink.de/scio/BBBSubscoreTest>  <http://psink.de/scio/MotorTest>  <http://psink.de/scio/GaitTest>  <http://psink.de/scio/LocomotorTest>  <http://psink.de/scio/ForelimbStrengthTest> <http://psink.de/scio/ForelimbAsymmetryTest> <http://psink.de/scio/StaircaseTest> <http://psink.de/scio/SinglePelletReachingTest> <http://psink.de/scio/ManualDexterityTest> <http://psink.de/scio/LimbMuscleStrengthTest> <http://psink.de/scio/RearingTest> <http://psink.de/scio/InclinedPlaneTest>  <http://psink.de/scio/NarrowBeamTest> <http://psink.de/scio/AutomatedGaitAnalysis> <http://psink.de/scio/LadderRungTest> <http://psink.de/scio/LouisvilleSwimScale> <http://psink.de/scio/FootprintAnalysisTest> <http://psink.de/scio/WalkingAnalysisTest> <http://psink.de/scio/SwimmingTest> ";
	}

//	Interpretation Thoracic Functest (ohne BBBTest )+ Positive Judgement  & Positive Judgement in NonFuncTest
	public void clinicallyMeaningFullOtherMotor() {
		final String queryString = "SELECT DISTINCT (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
				+ " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?nonFunctionalResult ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasJudgement> ?nonFunctionJudgement ."
				+ " ?nonFunctionJudgement a <http://psink.de/scio/Positive> ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasInvestigationMethod> ?nonFunctionalMethod ."
				+ " ?nonFunctionalMethod a ?nonFunctionalTest ." + " VALUES ?nonFunctionalTest { "
				+ getNonFunctionalTests() + " } ." + " ?experiment <http://psink.de/scio/hasResult> ?functionalResult ."
				+ " ?functionalResult <http://psink.de/scio/hasInvestigationMethod> ?functionalMethod ."
				+ " ?functionalMethod a ?functionalTest." + " VALUES ?functionalTest { " + getFunctionalTests() + " } ."
				+ " FILTER(?functionalTest != <http://psink.de/scio/BBBTest> ) "
				+ " ?functionalResult <http://psink.de/scio/hasJudgement> ?functionJudgement ."
				+ " ?functionJudgement a <http://psink.de/scio/Positive> . "

				+ " ?injuryModel <http://psink.de/scio/hasInjuryLocation> <http://psink.de/scio/Thoracic>."
				+ " ?funcTreatmentGroup <http://psink.de/scio/hasInjuryModel> ?injuryModel ."

				+ " ?functionalResult <http://psink.de/scio/hasTargetGroup> ?funcTreatmentGroup ."
				+ "{?funcTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?funcTreatment ."
				+ " ?funcTreatment a ?compoundType. " + " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. "
				+ "}" + " UNION{" + " ?funcTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?funcTreatmentType."
				+ " ?funcTreatmentType <http://psink.de/scio/hasCompound> ?nonFuncCompound ."
				+ " ?nonFuncCompound a ?compoundType .}"

				+ " ?nonFunctionalResult <http://psink.de/scio/hasTargetGroup> ?nonFuncTreatmentGroup ."
				+ "{?nonFuncTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?nonFuncTreatment ."
				+ " ?nonFuncTreatment a ?compoundType. " + " VALUES ?compoundType { " + getNonCompoundTreatments()
				+ " }. " + "}" + " UNION{"
				+ " ?nonFuncTreatmentGroup <http://psink.de/scio/hasTreatmentType> ?nonFuncTreatmentType."
				+ " ?nonFuncTreatmentType <http://psink.de/scio/hasCompound> ?compound ."
				+ " ?compound a ?compoundType .}" + "}  GROUP BY ?pubmedID ?treatmentGroup";

		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String line = EScoreTypes.CLINICALLY_MF_NON_BBB + "\t" + querySolution.getLiteral("pubmedID") + "\t"
						+ querySolution.getLiteral("compoundTypes");
				dataPrinter.println(line);
				System.out.println(line);
			}
		}

	}

//	Interpretation Thoracic + Positive Judgement in NonFuncTest + BBBTest >= 9 & < 9 | >= 14 & < 14 + BBBTest Positive Judgement
	public void clinicallyMeaningFullBBB() {
		final String queryString = "SELECT DISTINCT (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID ?BBBTreatValue ?BBBRefValue WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
				+ " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?nonFunctionalResult ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasJudgement> ?nonFunctionJudgement ."
				+ " ?nonFunctionJudgement a <http://psink.de/scio/Positive> ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasInvestigationMethod> ?nonFunctionalMethod ."
				+ " ?nonFunctionalMethod a ?nonFunctionalTest ." + " VALUES ?nonFunctionalTest { "
				+ getNonFunctionalTests() + " } ." + " ?experiment <http://psink.de/scio/hasResult> ?functionalResult ."
				+ " ?functionalResult <http://psink.de/scio/hasInvestigationMethod> ?functionalMethod ."
				+ " ?functionalMethod a <http://psink.de/scio/BBBTest> ."
				+ " ?functionalResult <http://psink.de/scio/hasJudgement> ?functionJudgement ."
				+ " ?functionJudgement a <http://psink.de/scio/Positive> . "

				+ " ?functionalResult <http://psink.de/scio/hasReferenceGroup> ?referenceGroup ."
				+ " ?functionalResult <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."

				+ " ?functionalResult <http://psink.de/scio/hasObservation> ?BBBTreatObservation ."
				+ " ?functionalResult <http://psink.de/scio/hasObservation> ?BBBRefObservation ."

				+ " ?BBBTreatObservation <http://psink.de/scio/belongsTo> ?treatmentGroup ."
				+ " ?BBBRefObservation <http://psink.de/scio/belongsTo> ?referenceGroup ."

				+ " ?BBBTreatObservation <http://psink.de/scio/hasNumericValue> ?BBBTreat ."
				+ " ?BBBRefObservation <http://psink.de/scio/hasNumericValue> ?BBBRef ."
				+ " ?BBBTreat <http://psink.de/scio/hasValue> ?BBBTreatValue ."
				+ " ?BBBRef <http://psink.de/scio/hasValue> ?BBBRefValue ."
				//
				+ " ?functionalResult <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."
				+ " ?treatmentGroup <http://psink.de/scio/hasInjuryModel> ?injuryModel ."
				+ " ?injuryModel <http://psink.de/scio/hasInjuryLocation> <http://psink.de/scio/Thoracic>."
				//

				+ "{?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundType. "
				+ " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. " + "}" + " UNION{"
				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"
				+ "} GROUP BY ?pubmedID ?BBBTreatValue ?BBBRefValue ?treatmentGroup"

		;
		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String BBBTreat = querySolution.getLiteral("BBBTreatValue").getString();
				String BBBRef = querySolution.getLiteral("BBBRefValue").getString();

				if (!BBBTreat.trim().isEmpty() && !BBBRef.trim().isEmpty()) {
					double BBBTreatValue = Double.parseDouble(BBBTreat);
					double BBBRefValue = Double.parseDouble(BBBRef);
					String line = EScoreTypes.CLINICALLY_MF_BBB + "\t" + querySolution.getLiteral("pubmedID") + "\t"
							+ querySolution.getLiteral("compoundTypes") + "\t" + BBBTreatValue + "\t" + BBBRefValue
							+ "\t"
							+ ((BBBRefValue < 9 && BBBTreatValue >= 9) || (BBBRefValue < 14 && BBBTreatValue >= 14));
					dataPrinter.println(line);
					System.out.println(line);
				}

			}
		}

	}

	public void timeWindowScore(String listOfPublications) {
		final String queryString = "SELECT DISTINCT  (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID ?timepoint  WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ." + " VALUES ?pubmedID { "
				+ listOfPublications + " } ." + " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?result ."
				+ " ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."
				+ " ?result <http://psink.de/scio/hasReferenceGroup> ?referenceGroup ."
				+ " ?result <http://psink.de/scio/hasObservation> ?timePointObservation ."
				+ " ?result <http://psink.de/scio/hasObservation> ?treatObservation ."
				+ " ?result <http://psink.de/scio/hasObservation> ?refObservation ."
				+ " ?timePointObservation <http://psink.de/scio/hasEventBefore> ?refObservation ."
				+ " ?timePointObservation <http://psink.de/scio/hasEventAfter> ?treatObservation ."
				+ " ?timePointObservation <http://psink.de/scio/hasDuration> ?timepoint ."

				+ "{?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundType. "
				+ " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. " + "}" + " UNION{"
				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"
				+ "} GROUP BY ?pubmedID ?timepoint ?treatmentGroup";

		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();

				timeWindowScoreMM(querySolution, hourToMin(0), hourToMin(1));
				timeWindowScoreMM(querySolution, hourToMin(1), hourToMin(4));
				timeWindowScoreMM(querySolution, hourToMin(4), hourToMin(12));
				timeWindowScoreMM(querySolution, hourToMin(12), hourToMin(100000));
				timeWindowScoreMM(querySolution, -1, -1);

			}
		}
	}

	private void timeWindowScoreMM(QuerySolution querySolution, int minMin, int maxMin) {
		String timepointString = querySolution.getLiteral("timepoint").getString();
		String line = null;
		if (timepointString.matches("\\d+ ?min")) {
			int timepoint = Integer.parseInt(timepointString.substring(0, timepointString.length() - 3).trim());
			if (timepoint >= minMin && timepoint < maxMin) {
				line = EScoreTypes.TIME_MIN_MAX + "\t" + querySolution.getLiteral("pubmedID") + "\t"
						+ querySolution.getLiteral("compoundTypes") + "\t" + querySolution.getLiteral("timepoint")
						+ "\t" + minMin + "\t" + maxMin;
			}
		} else if (timepointString.trim().equals("prior")) {
			line = EScoreTypes.TIME_PRIOR + "\t" + querySolution.getLiteral("pubmedID") + "\t"
					+ querySolution.getLiteral("compoundTypes") + "\t" + querySolution.getLiteral("timepoint");
		} else {
			throw new IllegalArgumentException("Unkown timepointString string: " + timepointString);
		}
		if (line != null) {
			dataPrinter.println(line);
			System.out.println(line);
		}
	}

	public void getContusionScore(String listOfPublications) {
		final String queryString = "SELECT DISTINCT ?injuryType (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ." + " VALUES ?pubmedID { "
				+ listOfPublications + " } ." + " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?result ."
				+ " ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."
				+ " ?treatmentGroup <http://psink.de/scio/hasInjuryModel> ?injuryModel ."
				+ " ?injuryModel a  <http://psink.de/scio/Contusion> ."
				+ " ?injuryModel <http://psink.de/scio/hasInjuryLocation> ?injuryType ."
				+ " VALUES ?injuryType { <http://psink.de/scio/Thoracic> <http://psink.de/scio/Cervical> }."
				+ "{?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundType. "
				+ " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. " + "}" + " UNION{"
				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"
				+ "} GROUP BY ?pubmedID ?injuryType ?treatmentGroup";
		Query query = QueryFactory.create(queryString);
		System.out.println(query);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String line = EScoreTypes.CONTUSION + "\t" + querySolution.getLiteral("pubmedID") + "\t"
						+ querySolution.getLiteral("compoundTypes") + "\t" + querySolution.getResource("injuryType");
				dataPrinter.println(line);
				System.out.println(line);
			}
		}
	}

	public void getClipCompressionScore(String listOfPublications) {
		final String queryString = "SELECT DISTINCT (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID ?injuryType  WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ." + " VALUES ?pubmedID { "
				+ listOfPublications + " } ." + " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?result ."
				+ " ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."
				+ " ?treatmentGroup <http://psink.de/scio/hasInjuryModel> ?injuryModel ."
				+ " ?injuryModel a  <http://psink.de/scio/Compression> ."
				+ " ?injuryModel <http://psink.de/scio/hasInjuryLocation> ?injuryType ."
				+ " VALUES ?injuryType { <http://psink.de/scio/Thoracic> <http://psink.de/scio/Cervical> }."
				+ " ?injuryModel <http://psink.de/scio/hasInjuryDevice> ?injuryDevice ."
				+ " ?injuryDevice a ?deviceType ." + " VALUES ?deviceType { " + getClips() + " }"
				+ "{?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundType. "
				+ " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. " + "}" + " UNION{"
				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"
				+ "}" + "GROUP BY ?pubmedID ?injuryType ?treatmentGroup";
		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String line = EScoreTypes.CLIP_COMPRESSION + "\t" + querySolution.getLiteral("pubmedID") + "\t"
						+ querySolution.getLiteral("compoundTypes") + "\t" + querySolution.getResource("injuryType");
				dataPrinter.println(line);
				System.out.println(line);
			}
		}
	}

	public void getPartialTransectionSharpScore(String listOfPublications) {
		final String queryString = "SELECT DISTINCT (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) ?pubmedID ?injuryLocation WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ." + " VALUES ?pubmedID { "
				+ listOfPublications + " } ." + " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?result ."
				+ " ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."
				+ " ?treatmentGroup <http://psink.de/scio/hasInjuryModel> ?injuryModel ." +

				"{?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundType. "
				+ " VALUES ?compoundType { " + getNonCompoundTreatments() + " }. " + "}" + " UNION{"
				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"

				+ " ?injuryModel <http://psink.de/scio/hasInjuryLocation> ?injuryLocation ."
				+ " VALUES ?injuryLocation { <http://psink.de/scio/Thoracic> <http://psink.de/scio/Cervical> }."
				+ " ?injuryModel a  ?injuryType ." + " VALUES ?injuryType { " + getPartialTransection() + " }" + "}"
				+ "GROUP BY ?pubmedID ?injuryLocation ?treatmentGroup";
		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String line = EScoreTypes.PARTIAL_TRANSECTION + "\t" + querySolution.getLiteral("pubmedID") + "\t"
						+ querySolution.getLiteral("compoundTypes") + "\t"
						+ querySolution.getResource("injuryLocation");
				dataPrinter.println(line);
				System.out.println(line);
			}
		}
	}

	private String getNonCompoundTreatments() {
		return "<http://psink.de/scio/HypothermicTreatment>";
	}

	private String getPartialTransection() {
		return "<http://psink.de/scio/PartialTransection> <http://psink.de/scio/DorsalHemisection> <http://psink.de/scio/VentralHemisection> <http://psink.de/scio/LateralHemisection>";
	}

	private String getClips() {
		return "<http://psink.de/scio/Clip> <http://psink.de/scio/AneurysmClip> <http://psink.de/scio/IrisClip> <http://psink.de/scio/EpiduralClip>";
	}

	public void getAnimalScore(EScoreTypes scoreType, String listOfPublications, String listOfValues) {
		final String queryString = "SELECT DISTINCT  ?organismSpecies ?pubmedID (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes) WHERE {"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ." + " VALUES ?pubmedID { "
				+ listOfPublications + " } ." + " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?result ."
				+ " ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."

				+ " {?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundType. "
				+ " VALUES ?compoundType { " + getNonCompoundTreatments() + "}. " + "}" + " UNION{"
				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"

				+ " ?treatmentGroup <http://psink.de/scio/hasOrganismModel> ?organismModel ."
				+ " ?organismModel <http://psink.de/scio/hasOrganismSpecies> ?organismSpecies ."
				+ " VALUES ?organismSpecies { " + listOfValues + " } ." +

				"} GROUP BY ?pubmedID ?organismSpecies ?treatmentGroup";

		Query query = QueryFactory.create(queryString);
		System.out.println(query);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String line = scoreType + "\t" + querySolution.getLiteral("pubmedID") + "\t"
						+ querySolution.getLiteral("compoundTypes") + "\t"
						+ querySolution.getResource("organismSpecies");
				dataPrinter.println(line);
				System.out.println(line);

			}
		}
	}

	private void publicationReproduceability() {
		final String queryString = "SELECT DISTINCT "
				+ "?pubmedID ?judgement ?investigationMethod (GROUP_CONCAT (DISTINCT ?compoundTypes) as ?compoundType)" 
				+ "WHERE{"
				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
				+ " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?result ."

				+ " ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."
				+ " ?result <http://psink.de/scio/hasInvestigationMethod> ?invM."
				+ " ?invM a ?investigationMethod"

				+ " {?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundTypes. "
				+ " VALUES ?compoundTypes { " + getNonCompoundTreatments() + "}. " + "}" + " UNION{"
				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundTypes .}"

				+ " ?result <http://psink.de/scio/hasJudgement> ?judgementNode ." 
				+ " ?judgementNode a ?judgement ." // shouldn't this point to the type judgement instead?
				+ " } GROUP BY ?pubmedID ?judgement ?treatmentGroup ?investigationMethod";
		
//		final String queryString = "SELECT DISTINCT "
//				+ "?pubmedID ?judgement (GROUP_CONCAT (DISTINCT ?compoundType) as ?compoundTypes)" + "WHERE{"
//				+ "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
//				+ " ?publication a <http://psink.de/scio/Publication> ."
//				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
//				+ " ?experiment <http://psink.de/scio/hasResult> ?result ."
//
//				+ " ?result <http://psink.de/scio/hasTargetGroup> ?treatmentGroup ."
//
//				+ " {?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treat ." + " ?treat a ?compoundType. "
//				+ " VALUES ?compoundType { " + getNonCompoundTreatments() + "}. " + "}" + " UNION{"
//				+ " ?treatmentGroup <http://psink.de/scio/hasTreatmentType> ?treatmentType."
//				+ " ?treatmentType <http://psink.de/scio/hasCompound> ?compound ." + " ?compound a ?compoundType .}"
//
//				+ " ?result <http://psink.de/scio/hasJudgement> ?judgementNode ." + " ?judgementNode a ?judgement ."
//				+ " } GROUP BY ?pubmedID ?judgement ?treatmentGroup";
		Query query = QueryFactory.create(queryString);
		System.out.println(query);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
				String pubmedID = querySolution.getLiteral("pubmedID").getString();
				String judgement = querySolution.getResource("judgement").toString();
				String treatmens = querySolution.getLiteral("compoundType").getString();
				String investigationMethod = querySolution.getResource("investigationMethod").toString();
				System.out.println(pubmedID + "\t"+investigationMethod +"\t" + judgement + "\t" + treatmens);
			}
		}

	}

	public String getPubmedIDsOfEffacyPublications() {

		Set<String> pubmedIDs = new HashSet<>();

		final String queryString = "SELECT DISTINCT "
//				+ "* "
				+ "?pubmedID  " + "WHERE{" + "?publication <http://psink.de/scio/hasPubmedID> ?pubmedID ."
				+ " ?publication a <http://psink.de/scio/Publication> ."
				+ " ?publication <http://psink.de/scio/describes> ?experiment ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?functionalResult ."
				+ " ?experiment <http://psink.de/scio/hasResult> ?nonFunctionalResult ."
				+ " ?functionalResult <http://psink.de/scio/hasInvestigationMethod> ?functionalMethod ."
				+ " ?functionalResult <http://psink.de/scio/hasJudgement> ?functionJudgement ."
				+ " ?functionJudgement a <http://psink.de/scio/Positive> ."
				+ " ?nonFunctionalResult <http://psink.de/scio/hasJudgement> ?nonFunctionJudgement ."
				+ " ?nonFunctionJudgement a <http://psink.de/scio/Positive> ."
				+ " ?functionalMethod a ?functionalTest ." + " VALUES ?functionalTest { " + getFunctionalTests()
				+ " } ." + " ?nonFunctionalResult <http://psink.de/scio/hasInvestigationMethod> ?nonFunctionalMethod ."
				+ " ?nonFunctionalMethod a ?nonFunctionalTest ." + " VALUES ?nonFunctionalTest { "
				+ getNonFunctionalTests() + " } ." + "}";
		Query query = QueryFactory.create(queryString);
		System.out.println(query);

		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();

			for (; results.hasNext();) {
				QuerySolution querySolution = results.nextSolution();
//				System.out.println(querySolution);
				pubmedIDs.add(querySolution.getLiteral("pubmedID").getString());
			}
		}

		StringBuffer bf = new StringBuffer();
		pubmedIDs.forEach(a -> bf.append("\"" + a + "\" "));
		System.out.println(bf);

		return bf.toString();
	}

	private String getPrimateSpecies() {
//		EntityType.get("AnimalSpecies").getTransitiveClosureSubEntityTypes().stream()
//		.filter(a -> !EntityType.get("MouseSpecies").getTransitiveClosureSubEntityTypes().contains(a))
//		.filter(a -> !EntityType.get("RatSpecies").getTransitiveClosureSubEntityTypes().contains(a))
//		.filter(a -> !EntityType.get("MonkeySpecies").getTransitiveClosureSubEntityTypes().contains(a))
//		.forEach(s -> System.out.print("<http://psink.de/scio/" + s.name + "> "));
		return "<http://psink.de/scio/MacaqueMonkey> <http://psink.de/scio/MonkeySpecies> ";
	}

	private String getLargerAnimalSpecies() {
//		EntityType.get("MonkeySpecies").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+"> "));
		return "<http://psink.de/scio/GuineaPigSpecies> <http://psink.de/scio/MinipigSpecies>  <http://psink.de/scio/HartleyGuineaPig> <http://psink.de/scio/RabbitSpecies> <http://psink.de/scio/NewZealandRabbit> <http://psink.de/scio/CatSpecies> <http://psink.de/scio/DogSpecies>";
	}

	private String getRatSpecies() {
//		EntityType.get("RatSpecies").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+"> "));
		return "<http://psink.de/scio/RatSpecies> <http://psink.de/scio/SpragueDawleyRat> <http://psink.de/scio/LewisRat> <http://psink.de/scio/FischerRat> <http://psink.de/scio/ListerHoodedRat> <http://psink.de/scio/WistarRat> <http://psink.de/scio/LongEvansRat> <http://psink.de/scio/AlbinoSwissRat> ";
	}

	private String getMouseSpecies() {
//		EntityType.get("MouseSpecies").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("<http://psink.de/scio/"+s.name+"> "));
		return "<http://psink.de/scio/MouseSpecies> <http://psink.de/scio/BALB_C_Mouse> <http://psink.de/scio/CD1_Mouse> <http://psink.de/scio/C57_BL6_Mouse> <http://psink.de/scio/CD2_Mouse> ";
	}

	private String getNonFunctionalTests() {
//		EntityType.get("NonFunctionalTest").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("\"<http://psink.de/scio/"+s.name+">\" "));
		return "<http://psink.de/scio/OxidativeStressTest> <http://psink.de/scio/AxonalDiebackTest> <http://psink.de/scio/AstrogliosisTest> <http://psink.de/scio/NonNeuronalCellChangesTest> <http://psink.de/scio/LesionVolumeTest> <http://psink.de/scio/TissueSparingTest> <http://psink.de/scio/NeurogenesisTest> <http://psink.de/scio/InflammationTest> <http://psink.de/scio/SchwannCellChangesTest> <http://psink.de/scio/HemorrhageTest> <http://psink.de/scio/NeuronalChangesTest> <http://psink.de/scio/MolecularChangesTest> <http://psink.de/scio/OligodendrogliaChangesTest> <http://psink.de/scio/HistologicalInvestigationTest> <http://psink.de/scio/ApoptosisTest> <http://psink.de/scio/GeneExpressionAnalysis> <http://psink.de/scio/AxonalRegenerationTest> <http://psink.de/scio/NeuroprotectionTest> <http://psink.de/scio/SecondaryDegenerationTest> <http://psink.de/scio/AxonalChangesTest> <http://psink.de/scio/CystVolumeTest> <http://psink.de/scio/AxonalDamageTest> <http://psink.de/scio/NeuronalActivityTest> <http://psink.de/scio/AxonalSproutingTest> <http://psink.de/scio/ScarringTest> <http://psink.de/scio/ProteinLevelAnalysis> <http://psink.de/scio/MyelinationTest> <http://psink.de/scio/AngiogenesisTest> <http://psink.de/scio/NeuronalCellLossTest> <http://psink.de/scio/ToxicityTest> ";
	}

	private String getFunctionalTests() {
//		EntityType.get("FunctionalTest").getTransitiveClosureSubEntityTypes().forEach(s->System.out.print("\"<http://psink.de/scio/"+s.name+">\" "));
		return "<http://psink.de/scio/ForelimbStrengthTest> <http://psink.de/scio/MechanicalAllodyniaTest> <http://psink.de/scio/NormoxicBreathingTest> <http://psink.de/scio/RearingTest> <http://psink.de/scio/GaitTest> <http://psink.de/scio/BBBTest> <http://psink.de/scio/SurfaceRightingReflexTest> <http://psink.de/scio/MotorEvokedPotentialsTest> <http://psink.de/scio/AutomatedGaitAnalysis> <http://psink.de/scio/GaleRatingScoreTest> <http://psink.de/scio/HReflexTest> <http://psink.de/scio/LouisvilleSwimScale> <http://psink.de/scio/FootprintAnalysisTest> <http://psink.de/scio/TailFlickReflexTest> <http://psink.de/scio/ElectromyographyTest> <http://psink.de/scio/PhysiologyTest> <http://psink.de/scio/ForelimbAsymmetryTest> <http://psink.de/scio/BladderFunctionTest> <http://psink.de/scio/PainTest> <http://psink.de/scio/BloodCirculationExamination> <http://psink.de/scio/BodyWeightExamination> <http://psink.de/scio/BMSTest> <http://psink.de/scio/AvoidanceResponseTest> <http://psink.de/scio/MotorTest> <http://psink.de/scio/GastricUlcerogenesisTest> <http://psink.de/scio/OpenFieldTest> <http://psink.de/scio/InclinedPlaneTest> <http://psink.de/scio/WalkingAnalysisTest> <http://psink.de/scio/SomatosensoryEvokedPotentialsTest> <http://psink.de/scio/SwimmingTest> <http://psink.de/scio/MotorReflexTest> <http://psink.de/scio/ContactPlacingResponseTest> <http://psink.de/scio/StaircaseTest> <http://psink.de/scio/BreathingTest> <http://psink.de/scio/HypoxicBreathingTest> <http://psink.de/scio/InfectionExamination> <http://psink.de/scio/LadderRungTest> <http://psink.de/scio/CardioVascularFunctionTest> <http://psink.de/scio/SinglePelletReachingTest> <http://psink.de/scio/LimbMuscleStrengthTest> <http://psink.de/scio/AdhesiveRemovalTest> <http://psink.de/scio/AutonomicDysreflexiaTest> <http://psink.de/scio/SensoryTest> <http://psink.de/scio/BBBSubscoreTest> <http://psink.de/scio/LocomotorTest> <http://psink.de/scio/BloodBrainBarrierTest> <http://psink.de/scio/ThermalHyperalgesiaTest> <http://psink.de/scio/NeurologicScalesTest> <http://psink.de/scio/HindpawSensationTest> <http://psink.de/scio/ElectrophysiologyTest> <http://psink.de/scio/ManualDexterityTest> <http://psink.de/scio/SpinalCordEvokedPotentialsTest> <http://psink.de/scio/NarrowBeamTest> ";
	}

}
