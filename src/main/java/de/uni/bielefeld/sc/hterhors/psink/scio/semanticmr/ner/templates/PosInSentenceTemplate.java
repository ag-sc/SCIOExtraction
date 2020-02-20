package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * looks for one to a set number of words after a mention
 */

public class PosInSentenceTemplate extends AbstractFeatureTemplate<PosInSentenceTemplate.PosInSentenceScope> {

    public PosInSentenceTemplate() {
        super();
    }

    public PosInSentenceTemplate(boolean cache) {
        super(cache);
    }


    static class PosInSentenceScope
            extends AbstractFactorScope {

        DocumentLinkedAnnotation annotation;
        int senLength;

        public PosInSentenceScope(
                AbstractFeatureTemplate<PosInSentenceScope> template, DocumentLinkedAnnotation annotation, int senLength) {
            super(template);
            this.annotation = annotation;
            this.senLength = senLength;
        }

        @Override
        public int implementHashCode() {
            return 0;
        }

        @Override
        public boolean implementEquals(Object obj) {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            PosInSentenceScope that = (PosInSentenceScope) o;
            return senLength == that.senLength &&
                    Objects.equals(annotation, that.annotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), annotation, senLength);
        }
    }

    @Override
    public List<PosInSentenceScope> generateFactorScopes(State state) {
        List<PosInSentenceScope> factors = new ArrayList<>();

        Document document = state.getInstance().getDocument();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            DocumentToken token = annotation.relatedTokens.get(annotation.relatedTokens.size() - 1);
            factors.add(new PosInSentenceScope(this, annotation, annotation.getSentenceOfAnnotation().length())) ;
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<PosInSentenceScope> factor) {
        DocumentLinkedAnnotation annotation = factor.getFactorScope().annotation;
        int docLength = factor.getFactorScope().senLength;

        double x = annotation.documentPosition.docCharOffset / docLength;

        StringBuilder sb = new StringBuilder();
        annotation.relatedTokens.forEach(p -> sb.append(p.getText()).append(" "));


        if (x < 0.33) {
            factor.getFeatureVector().set("Im ersten Drittel: " + sb.toString().trim(), true);
        } else if (x > 0.66) {
            factor.getFeatureVector().set("Im letzten Drittel: " + sb.toString().trim(), true);
        } else {
            factor.getFeatureVector().set("im zweiten Drittel:" + sb.toString().trim(), true);
        }

    }
}
