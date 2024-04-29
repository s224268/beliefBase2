import org.logicng.formulas.FormulaFactory
import org.logicng.io.parsers.PropositionalParser


fun main() {
    //print(removeDuplicatePatterns("~abdg~aaa~bvv|d~ssds"))
    takeInput()
}

fun takeInput() {
    val beliefBase: BeliefBase = BeliefBase()
    //TODO: Print grammar rules
    while (true) {
        println("To contract an belief, write -r <belief>, where belief is the CNF exactly as printed.")
        println("Otherwise, state your belief:")
        val input = readLine()!!
        if (input[0] == '-') {
            if (input[1] == 'r') beliefBase.contractBelief(input.substring(3))
        } else
            giveBelief(input, beliefBase)
    }
}

fun giveBelief(input: String, beliefBase: BeliefBase) {
    val f = FormulaFactory()
    val p = PropositionalParser(f)
    val formula = p.parse(input).cnf()
    println("Your input in CNF is: " + formula)
    if (formula.toString().contains("true")) {
        println("Your input is a tautology, and is therefore ignored")
        return
    }
    if (formula.toString().contains("false")) {
        println("Your input is a contradiction, and is therefore ignored")
        return
    }

    beliefBase.giveBeliefString(formula.toString())
}
