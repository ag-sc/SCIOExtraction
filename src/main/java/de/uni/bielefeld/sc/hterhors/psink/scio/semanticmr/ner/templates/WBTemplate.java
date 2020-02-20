package de.uni.bielefeld.sc.hterhors.psink.scio.semanticmr.ner.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hterhors.semanticmr.crf.model.AbstractFactorScope;
import de.hterhors.semanticmr.crf.model.Factor;
import de.hterhors.semanticmr.crf.structure.EntityType;
import de.hterhors.semanticmr.crf.structure.annotations.DocumentLinkedAnnotation;
import de.hterhors.semanticmr.crf.templates.AbstractFeatureTemplate;
import de.hterhors.semanticmr.crf.variables.Document;
import de.hterhors.semanticmr.crf.variables.DocumentToken;
import de.hterhors.semanticmr.crf.variables.State;

/**
 * WBF: first word in between if more than two words in between
 * WBL: last word in between if more than two words in between
 */
public class WBTemplate extends AbstractFeatureTemplate<WBTemplate.WBScope> {

    private static boolean WBFACTIVE = true;
    private static boolean WBLACTIVE = true;

    static class WBScope
            extends AbstractFactorScope {

        DocumentToken tokenOne;
        DocumentToken tokenTwo;

        EntityType typeOne;
        EntityType typeTwo;

        Document document;

        public WBScope(AbstractFeatureTemplate template, DocumentToken tokenOne, DocumentToken tokenTwo, EntityType typeOne, EntityType typeTwo, Document document) {
            super(template);
            this.tokenOne = tokenOne;
            this.tokenTwo = tokenTwo;
            this.typeOne = typeOne;
            this.typeTwo = typeTwo;
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
            WBScope that = (WBScope) o;
            return Objects.equals(tokenOne, that.tokenOne) &&
                    Objects.equals(tokenTwo, that.tokenTwo) &&
                    Objects.equals(typeOne, that.typeOne) &&
                    Objects.equals(typeTwo, that.typeTwo) &&
                    Objects.equals(document, that.document);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), tokenOne, tokenTwo, typeOne, typeTwo, document);
        }
    }

    @Override
    public List<WBScope> generateFactorScopes(State state) {
        List<WBScope> factors = new ArrayList<>();
        Document document = state.getInstance().getDocument();

        for (DocumentLinkedAnnotation annotation : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
            for (DocumentLinkedAnnotation annotation2 : super.<DocumentLinkedAnnotation>getPredictedAnnotations(state)) {
                if (!annotation.equals(annotation2)) {

                    factors.add(new WBScope(this,
                            annotation.relatedTokens.get(annotation.relatedTokens.size() - 1),
                            annotation2.relatedTokens.get(0),
                            annotation.entityType, annotation2.entityType,
                            document));
                }
            }
        }
        return factors;
    }

    @Override
    public void generateFeatureVector(Factor<WBScope> factor) {

        if (factor.getFactorScope().tokenOne.getDocTokenIndex() < factor.getFactorScope().tokenTwo.getDocTokenIndex() &&
                factor.getFactorScope().tokenTwo.getDocTokenIndex() - factor.getFactorScope().tokenOne.getDocTokenIndex() > 3) {

            if (WBFACTIVE) {
                String firstWordBetween = factor.getFactorScope().document.tokenList.get(factor.getFactorScope().tokenOne.getDocTokenIndex()+1).getText();
                factor.getFeatureVector().set("WBF: <" + factor.getFactorScope().typeOne.name + ", "
                        + factor.getFactorScope().typeTwo.name + "> " + firstWordBetween, true);
            }
            if (WBLACTIVE) {
                String lastWordBetween = factor.getFactorScope().document.tokenList.get(factor.getFactorScope().tokenTwo.getDocTokenIndex()-1).getText();
                factor.getFeatureVector().set("WBL: <" + factor.getFactorScope().typeOne.name + " "
                        + factor.getFactorScope().typeTwo.name + "> " + lastWordBetween, true);
            }


        }
    }

    private String[] tokenizeString(String text) {
        return text.split("\\s+");
    }
}
