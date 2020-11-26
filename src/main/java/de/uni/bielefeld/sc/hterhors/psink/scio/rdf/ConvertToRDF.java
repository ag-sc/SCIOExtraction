package de.uni.bielefeld.sc.hterhors.psink.scio.rdf;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.vocabulary.RDF;

import de.hterhors.semanticmr.crf.structure.annotations.AbstractAnnotation;
import de.hterhors.semanticmr.crf.structure.annotations.EntityTemplate;
import de.hterhors.semanticmr.crf.structure.annotations.MultiFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SingleFillerSlot;
import de.hterhors.semanticmr.crf.structure.annotations.SlotType;

public class ConvertToRDF {

	private static final String SCIO_PREFIX = "http://psink.de/scio/";
	private static final String SCIO_DATA_PREFIX = "http://scio/data/";

	/*
	 * id map
	 */
	private Map<AbstractAnnotation, Integer> idMap = new HashMap<>();

	private Integer getID(AbstractAnnotation et) {
		if (idMap.get(et) == null) {
			idMap.put(et, idMap.size());
		}
		return idMap.get(et);
	}

	public int count = 0;

	public ConvertToRDF(File outPutFile, Map<String, List<EntityTemplate>> annotations) throws IOException {
		this(new HashMap<>(), outPutFile, annotations);

	}

	public ConvertToRDF(Map<AbstractAnnotation, Integer> idMap, File outPutFile,
			Map<String, List<EntityTemplate>> annotationsMap) throws IOException {
		Set<String> RDFData = new HashSet<>();
		this.idMap = idMap;

		for (Entry<String, List<EntityTemplate>> annMap : annotationsMap.entrySet()) {
			for (EntityTemplate et : annMap.getValue()) {
				try {
					RDFData.add(toRDFLabel(et, annMap.getKey()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				RDFData.addAll(convert(new HashSet<>(), et));
			}
		}

		List<String> RDFDataSorted = new ArrayList<>(RDFData);
		count = RDFDataSorted.size();
		Collections.sort(RDFDataSorted);

		PrintStream ps = new PrintStream(outPutFile);
		RDFDataSorted.forEach(ps::println);
		ps.flush();
		ps.close();

	}

	private Set<String> convert(Set<String> rdf, AbstractAnnotation et) {

		if (!et.isInstanceOfEntityTemplate())
			return rdf;

		try {
			rdf.add(toRDFTypeLine(et.asInstanceOfEntityTemplate()));
		} catch (Exception e) {

		}

		for (Entry<SlotType, SingleFillerSlot> singleSlots : et.asInstanceOfEntityTemplate().getSingleFillerSlots()
				.entrySet()) {
			AbstractAnnotation singleSlotFiller = singleSlots.getValue().getSlotFiller();
			try {
				rdf.add(toRDFLine(et, singleSlots.getKey(), singleSlotFiller));
			} catch (Exception e) {

			}

			convert(rdf, singleSlotFiller);
		}
		for (Entry<SlotType, MultiFillerSlot> multiSlots : et.asInstanceOfEntityTemplate().getMultiFillerSlots()
				.entrySet()) {
			for (AbstractAnnotation multiSlotFiller : multiSlots.getValue().getSlotFiller()) {
				try {
					rdf.add(toRDFLine(et, multiSlots.getKey(), multiSlotFiller));
				} catch (Exception e) {

				}
				convert(rdf, multiSlotFiller);
			}
		}
		return rdf;

	}

	private String toRDFLabel(EntityTemplate et, String label) throws Exception {
		return toResourceName(et) + " " + "<http://www.w3.org/2000/01/rdf-schema#label>" + " " + "\"" + label + "\" .";
	}

	private String toRDFTypeLine(EntityTemplate et) throws Exception {
		return toResourceName(et) + " " + "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" + " " + "<" + SCIO_PREFIX
				+ et.getEntityType().name + "> .";
	}

	private String toRDFLine(AbstractAnnotation et, SlotType slotType, AbstractAnnotation slotFiller) throws Exception {
		return toResourceName(et) + " " + toPropertyName(slotType) + " " + toResourceName(slotFiller) + " .";
	}

	private String toPropertyName(SlotType st) {
		return "<" + SCIO_PREFIX + st.name + ">";
	}

	private String toResourceName(AbstractAnnotation et) throws Exception {
		if (et.getEntityType().isLiteral)
			return "\"" + et.asInstanceOfLiteralAnnotation().getSurfaceForm() + "\"";
		if (et.isInstanceOfEntityTypeAnnotation())
			return "<" + SCIO_PREFIX + et.getEntityType().name + ">";

		return "<" + SCIO_DATA_PREFIX + et.getEntityType().name + "_" + getID(et) + ">";
	}

}
