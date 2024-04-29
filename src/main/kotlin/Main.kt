import org.logicng.formulas.FormulaFactory
import org.logicng.io.parsers.PropositionalParser


fun main() {
    println("Rules for beliefs: " +
            "\n - Use single letter variables \n" +
            "Use the following symbols:\n" +
            " - NOT: ~\n" +
            " - AND: &\n" +
            " - OR: |\n" +
            " - IFF: <=> \n" +
            " - imp: => \n" +
            " - Brackets: (), as you'd expect\n" +
            "Press enter to continue")
    readln()
    clearTerminal()
    takeInput()
}


fun takeInput() {
    val beliefBase: BeliefBase = BeliefBase()
    while (true) {
        println("To contract a belief, write -r <belief>, where belief is the CNF exactly as printed.")
        println("Otherwise, state your belief:")
        val input = readLine()!!
        if (input[0] == '-') {
            if (input[1] == 'r') beliefBase.contractBelief(input.substring(3))
        } else
            giveBelief(input, beliefBase)
    }
}

//Takes the input as a string and converts it to CNF before adding it to the BeliefBase
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

fun clearTerminal(){
    for (i in 0..35){
        println()
    }
}