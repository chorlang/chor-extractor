package extraction;

public abstract class Label {

    enum LabelType{
        THEN, ELSE, COMMUNICATION, SELECTION
    }

    public abstract Label copy();

    public LabelType labelType;

    public boolean flipped = false;

    static abstract class ConditionLabel extends Label{
        public String process, expression;

        public ConditionLabel(String process, String expression, LabelType type){
            this.process = process;
            this.expression = expression;
            labelType = type;
        }

        static class ThenLabel extends ConditionLabel{
            public ThenLabel(String process, String expression){
                super(process, expression, LabelType.THEN);
            }

            public Label copy(){
                return new ThenLabel(process, expression);
            }

            public String toString() {
                return String.format("if %s,%s then ", process, expression);
            }
        }

        static class ElseLabel extends ConditionLabel{
            public ElseLabel(String process, String expression){
                super(process, expression, LabelType.ELSE);
            }

            public Label copy(){
                return new ThenLabel(process, expression);
            }

            public String toString() {
                return String.format("if %s,%s else ", process, expression);
            }
        }
    }

    static abstract class InteractionLabel extends Label{
        public String sender, receiver, expression;
        public InteractionLabel(String sender, String receiver, String expression, LabelType type){
            this.sender = sender;
            this.receiver = receiver;
            this.expression = expression;
            labelType = type;
        }

        static class CommunicationLabel extends InteractionLabel{
            public CommunicationLabel(String sender, String receiver, String expression){
                super(sender, receiver, expression, LabelType.COMMUNICATION);
            }

            public Label copy(){
                return new CommunicationLabel(sender, receiver, expression);
            }

            public String toString(){
                return String.format("%s,%s->%s", sender, receiver, expression);
            }

            static class SelectionLabel extends InteractionLabel{
                public SelectionLabel(String sender, String receiver, String label){
                    super(sender, receiver, label, LabelType.SELECTION);
                }

                public Label copy(){
                    return new SelectionLabel(sender, receiver, expression);
                }

                public String toString(){
                    return String.format("%s->%s[%s]", sender, receiver, expression);
                }
            }
        }
    }
}
