package de.uni.bielefeld.sc.hterhors.psink.scio.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.opencsv.CSVReader;

import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.init.specifications.SystemScope;
import de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ietemplates.DataStructureLoader;

/**
 * This class reads all annotations from the extracted .annodb files and checks
 * if the annotations are leaf classes of the ontology.
 * 
 * @author hterhors
 *
 */
public class AnnotationCheck {

	public static void main(String[] args) throws IOException {

		SystemScope.Builder.getScopeHandler()
				.addScopeSpecification(DataStructureLoader.loadDataStructureReader("Result")).build();

		File rootDir = new File("rawData/export_29052019/");
		List<File> files = new ArrayList<>(Arrays.asList(rootDir.listFiles()));
		Collections.sort(files);

		List<String> organismModelDocs = Files
				.readAllLines(new File("src/main/resources/slotfilling/corpus_docs.csv").toPath());

		for (File file : files) {
			boolean use = false;
			for (String usedFile : organismModelDocs) {
				if (usedFile.startsWith(file.getName().split("_")[0]))
					use = true;
			}

			if (!use)
				continue;

			if (!file.getName().endsWith(".annodb"))
				continue;

			CSVReader reader = new CSVReader(new BufferedReader(new FileReader(file)));

			for (String[] annotation : reader.readAll()) {

				if (annotation[0].trim().startsWith("#") || annotation[0].trim().isEmpty())
					continue;

//				if (annotation[6].trim().isEmpty())
//					continue;

				final EntityType entityType = EntityType.get(annotation[1].trim());

				if (entityType.isLeafEntityType() || entityType.isLiteral || !entityType.hasNoSlots())
					continue;

				String x = file.getName() + "\t" + annotation[1].trim() + "\t" + annotation[2].trim() + "\t"
						+ annotation[3].trim() + "\t" + annotation[4].trim();
				System.out.println(x);
			}
			reader.close();
		}
	}
}
