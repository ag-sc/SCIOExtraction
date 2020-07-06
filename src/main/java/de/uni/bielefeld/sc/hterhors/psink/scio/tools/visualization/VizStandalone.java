package de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization;

import java.io.File;

import de.hterhors.semanticmr.init.reader.csv.CSVDataStructureReader;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.tools.specifications.OWL2SpecsFormat;

public class VizStandalone {

	private static File specTmpFiles = new File(".tmp/specs_format/");

	private static OWL2SpecsFormat formatter;

	public static void main(String[] args) throws Exception {

		String owlFile = args.length == 0 ? "src/main/resources/scio/SCIO_65.owl" : args[0];
		String vizOutputFileName = args.length == 0 ? "test/viz/SCIO_65.graphml" : args[1];

		File inputOWL = new File(owlFile);
		File vizOutputFile = new File(vizOutputFileName);
		specTmpFiles.mkdirs();

		formatter = new OWL2SpecsFormat(specTmpFiles, inputOWL);

		SystemScope.Builder.getScopeHandler().addScopeSpecification(new CSVDataStructureReader(formatter.entitiesFile,
				formatter.hierarchiesFile, formatter.slotsFile, formatter.structuresFile)).build();

		new OWL2GraphmlConverter(vizOutputFile,formatter.hierarchiesFile,
				formatter.structuresFile);
	}

}
