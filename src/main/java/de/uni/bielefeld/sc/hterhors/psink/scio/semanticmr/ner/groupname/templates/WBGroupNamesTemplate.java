package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.groupname.templates;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * WBF: first word in between if more than two words in between
 * WBL: last word in between if more than two words in between
 */
public class WBGroupNamesTemplate extends AbstractFeatureTemplate<WBGroupNamesTemplate.WBScope> {

    private static boolean WBFACTIVE = true;
    private static boolean WBLACTIVE = true;

    static class WBScope
            extends AbstractFactorScope {

        DocumentLinkedAnnotation annoOne;
        DocumentLinkedAnnotation annoTwo;

        Document document;

        public WBScope(AbstractFeatureTemplate template, DocumentLinkedAnnotation annoOne,
                       DocumentLinkedAnnotation annoTwo, Document document) {
            super(template);

            this.annoOne = annoOne;
            this.annoTwo = annoTwo;
            this.document = document;
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
            WBScope wbScope = (WBScope) o;
            return Objects.equals(annoOne, wbScope.annoOne) &&
                    Objects.equals(annoTwo, wbScope.annoTwo) &&
                    Objects.equals(document, wbScope.document);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), annoOne, annoTwo, document);
        }
    }

    @Override
    public List<WBScope> generateFactorScopes(State state) {
        List<WBScope> factors = new ArrayList<>();
        Document document = state.getInstance().getDocument();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
                if (!annotation.equals(annotation2)) {

                    factors.add(new WBScope(this, annotation, annotation2, document));
                }
            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<WBScope> factor) {

        int indexOne = factor.getFactorScope().annoOne.relatedTokens.get(factor.getFactorScope().annoOne.relatedTokens.size() - 1).getDocTokenIndex();
        int indexTwo = factor.getFactorScope().annoTwo.relatedTokens.get(0).getDocTokenIndex();


        StringBuilder sbOne = new StringBuilder();
        factor.getFactorScope().annoOne.relatedTokens.forEach(p -> sbOne.append(p.getText()).append(" "));

        StringBuilder sbTwo = new StringBuilder();
        factor.getFactorScope().annoTwo.relatedTokens.forEach(p -> sbTwo.append(p.getText()).append(" "));


        if (indexOne < indexTwo && indexTwo - indexOne > 3) {

            if (WBFACTIVE) {
                String firstWordBetween = factor.getFactorScope().document.tokenList.get(indexOne + 1).getText();

                factor.getFeatureVector().set("First word inbetween: <\"" + sbOne.toString() + "\", \""
                        + sbTwo.toString() + "\"> " + firstWordBetween, true);
            }

            if (WBLACTIVE) {
                String lastWordBetween = factor.getFactorScope().document.tokenList.get(indexTwo - 1).getText();

                factor.getFeatureVector().set("Last word inbetween: <\"" + sbOne.toString() + "\", \""
                        + sbTwo.toString() + "\"> " + lastWordBetween, true);
            }


        }
    }
}
