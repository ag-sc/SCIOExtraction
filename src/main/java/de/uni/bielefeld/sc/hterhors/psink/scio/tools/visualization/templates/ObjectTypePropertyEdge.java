package de.uni.bielefeld.sc.hterhors.psink.scio.tools.visualization.templates;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class ObjectTypePropertyEdge implements IGraphMLContent {

	final private int id;
	final private int sourceId;
	final private int targetId;
	final private String label;

	final public static String template = readTemplate();

	private static String readTemplate() {
		try {
			return Files.readAllLines(new File("graphml/templates/object_property_edge.tmplt").toPath(),
					Charset.defaultCharset()).stream().reduce("", String::concat);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private ObjectTypePropertyEdge(int id, int sourceId, int targetId, String label) {
		this.id = id;
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.label = label;
	}

	public static class Builder {
		final private int id;
		final private int sourceId;
		final private int targetId;
		private String label = "UnnamedLabel";

		public Builder(int id, int sourceId, int targetId) {
			this.id = id;
			this.sourceId = sourceId;
			this.targetId = targetId;
		}

		public String getLabel() {
			return label;
		}

		public Builder setLabel(String label) {
			this.label = label;
			return this;
		}

		public int getId() {
			return id;
		}

		public int getSourceId() {
			return sourceId;
		}

		public int getTargetId() {
			return targetId;
		}

		public ObjectTypePropertyEdge build() {
			return new ObjectTypePropertyEdge(id, sourceId, targetId, label);
		}

	}

	@Override
	public String toString() {
		return String.format(template, id, sourceId, targetId, label);
	}

}
