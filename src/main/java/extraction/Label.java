package extraction;

import java.util.*;

public abstract class Label {

    public enum LabelType{
        THEN, ELSE, COMMUNICATION, SELECTION, MULTICOM, INTRODUCTION
    }

    public LabelType labelType;

    public boolean flipped = false;
    public Map<String, String> becomes = Map.of();

    public static class SpawnLabel extends Label{
        public final String parent, child;
        public SpawnLabel(String parent, String child){
            this.parent = parent;
            this.child = child;
        }
        @Override public String toString(){
            return String.format("%s spawns %s ", parent, child);
        }
    }

    public static class MulticomLabel extends Label{
        public final List<InteractionLabel> communications;
        public MulticomLabel(List<InteractionLabel> communications){
            this.communications = communications;
            labelType = LabelType.MULTICOM;
        }

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

            public String toString() {
                return String.format("if %s.%s then ", process, expression);
            }
        }

        public static class ElseLabel extends ConditionLabel{
            public ElseLabel(String process, String expression){
                super(process, expression, LabelType.ELSE);
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

        public String toString() {
            return String.format("%s.%s->%s", sender, expression, receiver);
        }
    }

    public static class SelectionLabel extends InteractionLabel{
        public SelectionLabel(String sender, String receiver, String label){
            super(sender, receiver, label, LabelType.SELECTION);
        }

        public String toString(){
            return String.format("%s->%s[%s]", sender, receiver, expression);
        }
    }

    public static class IntroductionLabel extends InteractionLabel{
        public String introducer, leftProcess, rightProcess;   //These are duplicates of the fields in its superclass
        public IntroductionLabel(String introducer, String leftProcess, String rightProcess){
            super(introducer, rightProcess, leftProcess, LabelType.INTRODUCTION); //Process2 is the expression field of the parent
            this.introducer = introducer;
            this.leftProcess = leftProcess;
            this.rightProcess = rightProcess;
        }
        public String toString() {
            return String.format("%s.%s<->%s", sender, expression, receiver);
        }
    }
}
