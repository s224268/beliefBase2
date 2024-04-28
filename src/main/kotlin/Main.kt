import org.logicng.formulas.FormulaFactory
import org.logicng.io.parsers.PropositionalParser


fun main() {
    takeInput()
}

fun takeInput(){
    val beliefBase: BeliefBase = BeliefBase()
    //TODO: Print grammar rules
    while(true){
        println("State your belief:")

        try{
            val input = readLine()!!
            giveBelief(input,beliefBase)
        } catch (e: Exception ){
            when(e){
                is java.lang.StringIndexOutOfBoundsException -> println("An error was found in your input. Please check it and try again")
                is java.lang.NullPointerException -> println("An error was found in your input. Please check it and try again")
                else -> println("A fatal error occured: " + e)
            }

        }

    }
}

fun testMain(){
    val beliefBase: BeliefBase = BeliefBase()
    giveBelief("AND(IFF('k','b'),NOT('k'))", beliefBase)
    /*
    giveBelief("NOT('c')", beliefBase)
    giveBelief("OR('a','b','c')", beliefBase)
    giveBelief("NOT('a')", beliefBase)

     */
}

fun giveBelief(input: String, beliefBase: BeliefBase){
    val f = FormulaFactory()
    val p = PropositionalParser(f)
    val formula = p.parse(input)
    println(formula.cnf())
    /*
    val cnfList: List<CNFimported> = toClass(stringTo(input))
    val b = cnfList[0].convert().toSAT()
    println(b)
    beliefBase.giveBeliefString(cnfList[0].convert().simplify().toSAT())

     */
}

fun toCNF2(){

}