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

public class PosInDocTemplate extends AbstractFeatureTemplate<PosInDocTemplate.PosInDocScope> {

    public PosInDocTemplate() {
        super();
    }

    public PosInDocTemplate(boolean cache) {
        super(cache);
    }


    static class PosInDocScope
            extends AbstractFactorScope {

        DocumentLinkedAnnotation annotation;
        int docLength;

        public PosInDocScope(
                AbstractFeatureTemplate<PosInDocScope> template, DocumentLinkedAnnotation annotation, int docLength) {
            super(template);
            this.annotation = annotation;
            this.docLength = docLength;
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
            PosInDocScope that = (PosInDocScope) o;
            return docLength == that.docLength &&
                    Objects.equals(annotation, that.annotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), annotation, docLength);
        }
    }

    @Override
    public List<PosInDocScope> generateFactorScopes(State state) {
        List<PosInDocScope> factors = new ArrayList<>();

        Document document = state.getInstance().getDocument();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            DocumentToken token = annotation.relatedTokens.get(annotation.relatedTokens.size() - 1);
            factors.add(new PosInDocScope(this, annotation, document.documentContent.length()));
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<PosInDocScope> factor) {
        DocumentLinkedAnnotation annotation = factor.getFactorScope().annotation;
        int docLength = factor.getFactorScope().docLength;

        double x = annotation.documentPosition.docCharOffset / docLength;

        StringBuilder sb = new StringBuilder();
        annotation.relatedTokens.forEach(p -> sb.append(p.getText()).append(" "));

//        if(x < 0.33) {
//            factor.getFeatureVector().set("Im ersten Drittel: "+annotation.getEntityType().name, true);
//        } else if (x > 0.66) {
//            factor.getFeatureVector().set("Im zweiten Drittel: "+annotation.getEntityType().name, true);
//        } else{
//            factor.getFeatureVector().set("im letzten Drittel:"+annotation.getEntityType().name, true);
//        }

        if (x < 0.33) {
            factor.getFeatureVector().set("Im ersten Drittel des Docs: " + sb.toString().trim(), true);
        } else if (x > 0.66) {
            factor.getFeatureVector().set("Im letzten Drittel des Docs: " + sb.toString().trim(), true);
        } else {
            factor.getFeatureVector().set("im zweiten Drittel des Docs:" + sb.toString().trim(), true);
        }
    }
}
