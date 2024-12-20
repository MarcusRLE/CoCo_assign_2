import java.util.HashMap;
import java.util.Map.Entry;
import java.util.List;
import java.util.ArrayList;

public abstract class AST{
    public void error(String msg){
        System.err.println(msg);
        System.exit(-1);
    }
};

/* Expressions are similar to arithmetic expressions in the impl
   language: the atomic expressions are just Signal (similar to
   variables in expressions) and they can be composed to larger
   expressions with And (Conjunction), Or (Disjunction), and Not
   (Negation). Moreover, an expression can be using any of the
   functions defined in the definitions. */

abstract class Expr extends AST{
    abstract public Boolean eval(Environment env);
}

class Conjunction extends Expr{
    // Example: Signal1 * Signal2 
    Expr e1,e2;
    Conjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}
    @Override
    public Boolean eval(Environment env) {
        Boolean conj = e1.eval(env) && e2.eval(env); 
        return conj;
    }
}

class Disjunction extends Expr{
    // Example: Signal1 + Signal2 
    Expr e1,e2;
    Disjunction(Expr e1,Expr e2){this.e1=e1; this.e2=e2;}
    @Override
    public Boolean eval(Environment env) {
        Boolean disj = e1.eval(env) || e2.eval(env); 
        return disj;
    }
}

class Negation extends Expr{
    // Example: /Signal
    Expr e;
    Negation(Expr e){this.e=e;}
    @Override
    public Boolean eval(Environment env) {
        Boolean neg = !e.eval(env); 
        return neg;
    }
}

class UseDef extends Expr{
    // Using any of the functions defined by "def"
    // e.g. xor(Signal1,/Signal2) 
    String f;  // the name of the function, e.g. "xor" 
    List<Expr> args;  // arguments, e.g. [Signal1, /Signal2]
    UseDef(String f, List<Expr> args){
	    this.f=f; this.args=args;
    }
    @Override
    public Boolean eval(Environment env) {
        Def def = env.getDef(f);
        List<String> argNames = def.args;

        /*
         * We create a new environment, which is a copy of the original
         * to evaluate the function so that the variables defined in the 
         * function are not saved in the original environment.
         */
        Environment newEnv = new Environment(env);
        for(int i = 0; i < argNames.size(); i++){
            newEnv.setVariable(argNames.get(i), args.get(i).eval(env));
        }
        return def.e.eval(newEnv);
    }
}

class Signal extends Expr{
    String varname; // a signal is just identified by a name 
    Signal(String varname){this.varname=varname;}
    @Override
    public Boolean eval(Environment env) {
        Boolean value = env.getVariable(varname);
        return value;
    }
}

class Def extends AST{
    // Definition of a function
    // Example: def xor(A,B) = A * /B + /A * B
    String f; // function name, e.g. "xor"
    List<String> args;  // formal arguments, e.g. [A,B]
    Expr e;  // body of the definition, e.g. A * /B + /A * B
    Def(String f, List<String> args, Expr e){
	    this.f=f; this.args=args; this.e=e;
    }
}

// An Update is any of the lines " signal = expression "
// in the update section

class Update extends AST{
    // Example Signal1 = /Signal2 
    String name;  // Signal being updated, e.g. "Signal1"
    Expr e;  // The value it receives, e.g., "/Signal2"
    Update(String name, Expr e){this.e=e; this.name=name;}

    public void eval(Environment env) {
        Boolean newValue = e.eval(env);
        env.setVariable(name, newValue);
    }
}

/* A Trace is a signal and an array of Booleans, for instance each
   line of the .simulate section that specifies the traces for the
   input signals of the circuit. It is suggested to use this class
   also for the output signals of the circuit in the second
   assignment.
*/

class Trace extends AST{
    // Example Signal = 0101010
    String signal;
    Boolean[] values;
    Trace(String signal, Boolean[] values){
	    this.signal=signal;
	    this.values=values;
    }

    public String toString(){
        String str = "";
        for(Boolean value : values){
            str += value ? "1" : "0";
        }
        str += " " + signal;
        return str;
    }
}

/* The main data structure of this simulator: the entire circuit with
   its inputs, outputs, latches, definitions and updates. Additionally
   for each input signal, it has a Trace as simulation input.
   
   There are two variables that are not part of the abstract syntax
   and thus not initialized by the constructor (so far): simoutputs
   and simlength. It is suggested to use these two variables for
   assignment 2 as follows: 
 
   1. all siminputs should have the same length (this is part of the
   checks that you should implement). set simlength to this length: it
   is the number of simulation cycles that the interpreter should run.

   2. use the simoutputs to store the value of the output signals in
   each simulation cycle, so they can be displayed at the end. These
   traces should also finally have the length simlength.
*/

class Circuit extends AST{
    String name;  
    List<String> inputs; 
    List<String> outputs;
    List<String>  latches;
    List<Def> definitions;
    List<Update> updates;
    List<Trace>  siminputs;
    List<Trace>  simoutputs = new ArrayList<Trace>();
    int simlength;
    Circuit(String name,
	    List<String> inputs,
	    List<String> outputs,
	    List<String>  latches,
	    List<Def> definitions,
	    List<Update> updates,
	    List<Trace>  siminputs){
	this.name=name;
	this.inputs=inputs;
	this.outputs=outputs;
	this.latches=latches;
	this.definitions=definitions;
	this.updates=updates;
	this.siminputs=siminputs;
    }

    private int calcSimLenght(){
        int length = siminputs.get(0).values.length;

        // Comparing length of all siminputs to verify if they are equal
        for(Trace siminput : siminputs){
            if(siminput.values.length != length){
                return -1;
            }
        }
        return length;
    }

    public void latchesInit(Environment env){
        for(String latch : latches){
            String latchOutput = latch + "'";
            env.setVariable(latchOutput, false);
        }
    }

    public void latchUpdate(Environment env){
        for(String latch : latches){
            Boolean currentValue = env.getVariable(latch);
            String latchOutput = latch + "'";
            env.setVariable(latchOutput, currentValue);
        }
    }

    public void initialize(Environment env){

        // Setting simlength + error handling
        if(0 > (simlength = calcSimLenght()))   throw new RuntimeException("Simulation inputs have different lengths");

        // Initializing outputs with empty traces of length simlength
        for(String output : outputs){
            simoutputs.add(new Trace(output, new Boolean[simlength]));
        }

        // Initializing inputs with values from cycle 0
        for(Trace siminput : siminputs){
            String variable = siminput.signal;

            // ====== ERROR HANDLING ======
            if(siminput.values == null){
                System.out.println("Values not defined: " + variable);
                throw new RuntimeException("Variable not defined: "+variable);
            } else if(siminput.values.length == 0){
                System.out.println("Length of values: " + siminput.values.length);
                throw new RuntimeException("Length of simputs values is 0");
            };

            Boolean initValue = siminput.values[0];
            env.setVariable(variable, initValue);
        }

        latchesInit(env);

        // Updating to initialize remaining signals
        for(Update update : updates){
            update.eval(env);
        }

        // Setting trace for simoutputs at cycle 0
        updateOutTrace(env, 0);
    }

    private void updateOutTrace(Environment env, int cycle){
        for(Trace simoutput : simoutputs){
            String variable = simoutput.signal;
            Boolean value = env.getVariable(variable);
            simoutput.values[cycle] = value;
        }
    }

    public void nextCycle(Environment env, int cycle){
        // Setting inputs of simnputs variables for the current cycle
        for(Trace siminput : siminputs){
            String variable = siminput.signal;

            // ====== ERROR HANDLING ======
            // Checking if value is defined for the current cycle
            if(siminput.values.length < cycle){
                throw new RuntimeException("Value [" + cycle + "] of variable '" + variable + "' not defined");
            };

            Boolean nextValue = siminput.values[cycle];
            env.setVariable(variable, nextValue);
        }

        // Updating latches
        latchUpdate(env);

        // Updating signals
        for(Update update : updates){
            update.eval(env);
        }

        // Setting trace for simoutputs at current cycle
        updateOutTrace(env, cycle);
    }

    public void runSimulator(Environment env){

        initialize(env);

        // Running simulation for all cycles starting at 1 (0 is already initialized)
        for(int cycle = 1; cycle < simlength; cycle++){
            nextCycle(env, cycle);
        }

        printTraces();
    }

    private void printTraces(){
        // Printing simpinputs and trace of simoutputs
        for(Trace siminput : siminputs){
            System.out.println(siminput.toString());
        }
        for(Trace simoutput : simoutputs){
            System.out.println(simoutput.toString());
        }
    }
}
