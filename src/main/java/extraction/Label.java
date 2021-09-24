package extraction;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class Label {

    public enum LabelType{
        THEN, ELSE, COMMUNICATION, SELECTION, MULTICOM, INTRODUCTION
    }

    public abstract Label copy();

    public LabelType labelType;

    public boolean flipped = false;

    public static final List<LabelType> conditionTypes = Arrays.asList(LabelType.THEN, LabelType.ELSE);
    public static final List<LabelType> interactionTypes = Arrays.asList(LabelType.COMMUNICATION, LabelType.SELECTION);

    public static class MulticomLabel extends Label{
        public final List<InteractionLabel> communications;
        public MulticomLabel(List<InteractionLabel> communications){
            this.communications = communications;
            labelType = LabelType.MULTICOM;
        }

        /**
         * Creates a new Label object identical to this one, but its content is copied by reference.
         * @return A shallow copy of this label.
         */
        public Label copy(){ return new MulticomLabel(communications); }

        @Override
        public String toString(){
            var out = new StringBuilder("(");
            for (InteractionLabel com : communications){
                out.append(com.toString()).append(", ");
            }
            out.delete(out.length()-2, out.length()); //Delete trailing " | "
            out.append(")");
            return out.toString();
        }

    }

    public static abstract class ConditionLabel extends Label{
        public String process, expression;

        public ConditionLabel(String process, String expression, LabelType type){
            this.process = process;
            this.expression = expression;
            labelType = type;
        }

        public static class ThenLabel extends ConditionLabel{
            public ThenLabel(String process, String expression){
                super(process, expression, LabelType.THEN);
            }

            public Label copy(){
                return new ThenLabel(process, expression);
            }

            public String toString() {
                return String.format("if %s.%s then ", process, expression);
            }
        }

        public static class ElseLabel extends ConditionLabel{
            public ElseLabel(String process, String expression){
                super(process, expression, LabelType.ELSE);
            }

            public Label copy(){
                return new ThenLabel(process, expression);
            }

            public String toString() {
                return String.format("if %s.%s else ", process, expression);
            }
        }
    }

    // == InteractionLabels ==
    public static abstract class InteractionLabel extends Label{
        public String sender, receiver, expression;
        public InteractionLabel(String sender, String receiver, String expression, LabelType type){
            this.sender = sender;
            this.receiver = receiver;
            this.expression = expression;
            labelType = type;
        }
    }

    public static class CommunicationLabel extends InteractionLabel {
        public CommunicationLabel(String sender, String receiver, String expression) {
            super(sender, receiver, expression, LabelType.COMMUNICATION);
        }

        public Label copy() {
            return new Label.CommunicationLabel(sender, receiver, expression);
        }

        public String toString() {
            return String.format("%s.%s->%s", sender, expression, receiver);
        }
    }

    public static class SelectionLabel extends InteractionLabel{
        public SelectionLabel(String sender, String receiver, String label){
            super(sender, receiver, label, LabelType.SELECTION);
        }

        public Label copy(){
            return new Label.SelectionLabel(sender, receiver, expression);
        }

        public String toString(){
            return String.format("%s->%s[%s]", sender, receiver, expression);
        }
    }

    public static class IntroductionLabel extends InteractionLabel{
        public String introducer, process1, process2;   //These are duplicates of the fields in its superclass
        public IntroductionLabel(String introducer, String process1, String process2){
            super(introducer, process1, process2, LabelType.INTRODUCTION); //Process2 is the expression field of the parent
            this.introducer = introducer;
            this.process1 = process1;
            this.process2 = process2;
        }
        @Override
        public Label copy() {
            return new IntroductionLabel(introducer, process1, process2);
        }
        public String toString() {
            return String.format("%s.%s<->%s", sender, expression, receiver);
        }
    }
}
