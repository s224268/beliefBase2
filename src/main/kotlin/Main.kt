import org.logicng.formulas.FormulaFactory
import org.logicng.io.parsers.PropositionalParser


fun main() {
    //print(removeDuplicatePatterns("~abdg~aaa~bvv|d~ssds"))
    takeInput()
}

fun takeInput(){
    val beliefBase: BeliefBase = BeliefBase()
    //TODO: Print grammar rules
    while(true){
        println("State your belief:")
        val input = readLine()!!
        giveBelief(input,beliefBase)
    }
}

fun giveBelief(input: String, beliefBase: BeliefBase){
    val f = FormulaFactory()
    val p = PropositionalParser(f)
    val formula = p.parse(input).cnf()
    println("Your input in CNF is: " + formula)
    if(formula.toString().contains("true")){
        println("Your input is a tautology, and is therefore ignored")
        return
    }
    if(formula.toString().contains("false")){
        println("Your input is a contradiction, and is therefore ignored")
        return
    }

    beliefBase.giveBeliefString(formula.toString())
}
