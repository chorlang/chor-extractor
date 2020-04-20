package parsing;

import antlrgen.ChoreographyBaseVisitor;
import extraction.choreography.ChoreographyASTNode;
import extraction.choreography.ChoreographyBody;
import extraction.choreography.Program;
import org.antlr.v4.runtime.tree.ParseTree;


public class ChoreographyASTToProgram extends ChoreographyBaseVisitor<ChoreographyASTNode> {
    static Program toProgram(ParseTree tree){
        return (Program)(new ChoreographyASTToProgram().getProgram(tree));
    }

    public ChoreographyASTNode getProgram(ParseTree tree){
        return this.visit(tree);
    }


}
