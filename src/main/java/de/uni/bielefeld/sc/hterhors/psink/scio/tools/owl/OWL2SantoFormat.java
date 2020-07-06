package de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl;

import java.io.File;
import java.io.IOException;

import de.uni.bielefeld.sc.hterhors.psink.scio.tools.owl.owlreader.SCIOEnvironment;

public class OWL2SantoFormat {
	public static void main(String[] args) throws IOException {

		File parentCSVDirectory = new File("test/santo_format/");

		int version = 65;
		OWLToSANTOConverter x = new OWLToSANTOConverter(
				SCIOEnvironment.getInstance(new File("src/main/resources/scio/SCIO_" + version + ".owl"), version));

		x.writeClassesFile(parentCSVDirectory, x.toClassesFile());

		x.writeSubClassesFile(parentCSVDirectory, x.toSubClassesFile());

		x.writeRelationsFile(parentCSVDirectory, x.toRelationsFile());

	}
}
