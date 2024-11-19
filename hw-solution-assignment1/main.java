import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.CharStreams;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class main {
    public static void main(String[] args) throws IOException{
	// we expect exactly one argument: the name of the input file
	if (args.length!=1) {
	    System.err.println("\n");
	    System.err.println("Hardware Simulator\n");
	    System.err.println("==================\n\n");
	    System.err.println("Please give as input argument a filename\n");
	    System.exit(-1);
	}
	String filename=args[0];

	// open the input file
	CharStream input = CharStreams.fromFileName(filename);
	    //new ANTLRFileStream (filename); // depricated
	
	// create a lexer/scanner
	hwLexer lex = new hwLexer(input);
	
	// get the stream of tokens from the scanner
	CommonTokenStream tokens = new CommonTokenStream(lex);
	
	// create a parser
	hwParser parser = new hwParser(tokens);
	
	// and parse anything from the grammar for "start"
	ParseTree parseTree = parser.start();

	/* The AstMaker generates the abstract syntax to be used for
	   the second assignment, where for the start symbol of the
	   ANTLR grammar, it generates an object of class Circuit (see
	   AST.java). */
	
	Circuit p = (Circuit) new AstMaker().visit(parseTree);
	System.out.println("\n\n====================================");

	System.out.println("\nSimulation trace for " + args[0] + "\n");

	/*
	 * Running the simulator, adding the simulation trace to the output
	 * 
	 * We use the definitions from Circuit p to create a new environment, which is used in runSimulator.
	 */
	p.runSimulator(new Environment(p.definitions));
	System.out.println("\n====================================\n\n");
    }

	
}

// The visitor for producing the Abstract Syntax (see AST.java).

class AstMaker extends AbstractParseTreeVisitor<AST> implements hwVisitor<AST> {

    public AST visitStart(hwParser.StartContext ctx){
	List<String> ins=new ArrayList<String>();
	for(Token t:ctx.ins){
	    ins.add(t.getText());
	}
	List<String> outs=new ArrayList<String>();
	for(Token t:ctx.outs){
	    outs.add(t.getText());
	}
	List<String> latches=new ArrayList<String>();
	for(Token t:ctx.ls){
	    latches.add(t.getText());
	}
	List<Def> defs=new ArrayList<Def>();
	for(hwParser.DefdeclContext t:ctx.defs){
	    defs.add((Def) visit(t));
	}
	List<Update> updates=new ArrayList<Update>();
	for(hwParser.UpdatedeclContext t:ctx.up){
	    updates.add((Update) visit(t));
	}
	List<Trace> siminp=new ArrayList<Trace>();
	for(hwParser.SimInpContext t:ctx.simin)
	    siminp.add((Trace) visit(t));
	return new Circuit(ctx.name.getText(),ins,outs,latches,defs,updates,siminp);
    };

    public AST visitSimInp(hwParser.SimInpContext ctx){
	String s=ctx.str.getText();
	// s is a string consisting of characters '0' and '1' (not numbers!)
	Boolean[] tr=new Boolean[s.length()];
	// for the simulation it is more convenient to work with
	// Booleans, so converting the string s to an array of
	// Booleans here:	
	for(int i=0; i<s.length();i++)
	    tr[i]=(s.charAt(i)=='1'); 
	return new Trace(ctx.in.getText(),tr);
    }
    
    public AST visitDefdecl(hwParser.DefdeclContext ctx){
	List<String> args=new ArrayList<String>();
	for(Token t:ctx.xs)
	    args.add(t.getText());
	return new Def(ctx.f.getText(),args,(Expr) visit(ctx.e));
    }

    public AST visitUpdatedecl(hwParser.UpdatedeclContext ctx){
	return new Update(ctx.write.getText(),
			  (Expr) visit(ctx.e));
    }
    
    
    public AST visitSignal(hwParser.SignalContext ctx){
	return new Signal(ctx.x.getText());
    };

    public AST visitConjunction(hwParser.ConjunctionContext ctx){
	return new Conjunction((Expr) visit(ctx.e1),
			       (Expr)visit(ctx.e2));
    };

    public AST visitDisjunction(hwParser.DisjunctionContext ctx){
	return new Disjunction((Expr) visit(ctx.e1),
			       (Expr)visit(ctx.e2));
    };

    public AST visitNegation(hwParser.NegationContext ctx){
	return new Negation((Expr) visit(ctx.e)); 
    };

    public AST visitParenthesis(hwParser.ParenthesisContext ctx){
	return (Expr) visit(ctx.e);
    }

    public AST visitUseDef(hwParser.UseDefContext ctx){
	List<Expr> args=new ArrayList<Expr>();
	for(hwParser.ExprContext e:ctx.es)
	    args.add((Expr) visit(e));
	return new UseDef(ctx.f.getText(),args);
    }
	
}


