val inconsistentBeliefs: MutableSet<Belief> = mutableSetOf()
const val RELEVANT_ORS = 5

class Disjunction(disjunctionString: String, parentCNF: CNF?) {
    val parent = parentCNF
    val variables: MutableList<Literal> = mutableListOf()

    init {
        for (literalString in disjunctionString.split('|')) {
            variables.add(Literal(literalString, this))
        }
    }

    fun evaluate(map: Map<String, Boolean?>): Boolean? {
        var falseVariableCounter = 0
        for (variable in variables) {
            if (variable.evaluate(map) == true) {
                return true
            } else if (variable.evaluate(map) == false) {
                falseVariableCounter++
            }
        }
        if (falseVariableCounter == variables.size) {
            return false
        }
        return null
    }
}

/**
 * This is a conjunction
 */
public class CNF(CNFString: String, parentBelief: Belief) {
    val parent = parentBelief
    val disjunctions: MutableList<Disjunction> = mutableListOf()

    init {
        try {
            val stringList = CNFString.split('&')
            for (disjunctionString in stringList) {
                disjunctions.add(Disjunction(disjunctionString, this))
            }
        } catch (e: KotlinNullPointerException) {
            println("Invalid input")
        }
    }


    fun evaluate(map: Map<String, Boolean?>): Boolean? {
        for (disjunction in disjunctions) {
            disjunction.evaluate(map) ?: return null //Return null if null
            if (!disjunction.evaluate(map)!!) {
                return false
            }
        }
        return true
    }
}

class Literal(val literalString: String, parentDisjunction: Disjunction) {
    val parent = parentDisjunction
    var varName: String = ""
    var isNot: Boolean = false

    init {
        if (literalString.contains('~')) {
            isNot = true
        }
        val regex = "[a-zA-Z]+".toRegex()
        varName = regex.find(literalString, 0)!!.value
    }

    fun evaluate(map: Map<String, Boolean?>): Boolean? {
        if (map[varName] == null) {
            return null
        }
        if (isNot) {
            return !map[varName]!!
        }
        return map[varName]!!
    }
}

/**
 * This function is basically different forms confirmation basis, algorithmically
 */
fun getWorth(belief: Belief): Int {
    return belief.addedNumber
}


class Belief(originalExpression: String) {
    val CNFString = originalExpression
    var CNF: CNF = CNF(originalExpression, this)

    //addedNumber Is used to order the beliefs. Maybe we should just use a sorted list instead.
    //This is mainly for if we just remove the oldest belief first, which is dubious, but at the same time
    //we automatically assume that newer beliefs are true, so it follows that older beliefs are less true
    var addedNumber: Int = 0

    //All beliefs that directly follow from this belief. This is the "Children"
    val entailments: MutableList<Belief> = mutableListOf()

    val parents: MutableList<Belief> = mutableListOf() //The corresponding parents. Not sure if we want this

}

/**
 * The big boy
 */
class BeliefBase {
    private var numberOfBeliefs: Int = 0 //Keeps track of total number of beliefs that have been added. Works as a "timestamp"

    private val beliefs: MutableSet<Belief> = mutableSetOf() //Only holds base beliefs. None of these have parents
    private val allEntailments: MutableSet<Belief> = mutableSetOf() //Temporary store of entailments. They are also children of the base beliefs
    private val allEntailmentStrings: MutableSet<String> = mutableSetOf()
    private fun selectAndRemoveBelief(contradictingBeliefs: Set<Belief>, holyBelief: Belief) {

        if (contradictingBeliefs.size == 1 && contradictingBeliefs.first() == holyBelief) {
            println("Your belief is an oxymoron (Contradiction). It will not be added to the belief base")
            beliefs.remove(holyBelief)
            return
        }
        val beliefsToNumbers: MutableMap<Belief, Int> = mutableMapOf() //Rename this?
        for (belief in contradictingBeliefs) {
            beliefsToNumbers[belief] = getWorth(belief)
        }
        beliefsToNumbers.remove(holyBelief) //We remove the holy belief, because we don't want it removed from the set of all beliefs
        beliefs.remove(beliefsToNumbers.minBy { it.value }.key) //Lowest val
    }

    private fun printBeliefs() {

        val printedBeliefs: MutableMap<String, Boolean> = mutableMapOf()

        println("Current base beliefs are:")
        for (belief in beliefs) {
            if (printedBeliefs[belief.CNFString] != true) println("\t" + belief.CNFString)
            printedBeliefs[belief.CNFString] = true
        }
        println("Entailments of these are:")
        for (belief in beliefs) {
            for (child in belief.entailments) {
                if (printedBeliefs[child.CNFString] != true) println("\t" + child.CNFString)
                printedBeliefs[child.CNFString] = true
            }
        }
        println()
        println()
        println()
    }

    private fun addBelief(beliefToAdd: Belief) {
        beliefToAdd.addedNumber = numberOfBeliefs
        numberOfBeliefs++
        beliefs.add(beliefToAdd)
    }

    private fun clearAllEntailments() {
        //Hugely inefficient, but I don't see the issue. Simpler than going through every child and determining if it's still true
        allEntailmentStrings.clear()
        for (belief in beliefs) {
            belief.entailments.clear()
            if (belief.parents.isNotEmpty()) {
                throw Exception("Base belief had parent")
            }
        }
    }

    private fun redoEntailments() {
        clearAllEntailments()
        revise()
    }

    /**
     * The "main" method for adding a belief
     */
    fun giveBeliefString(newBeliefString: String) {
        giveBelief(Belief(newBeliefString))
    }

    private fun giveBelief(newBelief: Belief) {
        addBelief(newBelief)

        while (!DPLL_satisfiable()) {
            println("Model is not satisfiable. Removing belief to fix")
            selectAndRemoveBelief(inconsistentBeliefs, newBelief)
        }
        redoEntailments()
        printBeliefs()
    }

    private fun allClausesTrue(clauses: Set<Disjunction>, model: Map<String, Boolean?>): Boolean {
        var truthCounter = 0
        for (clause in clauses) {
            if (clause.evaluate(model) == true) {
                truthCounter++
            }
        }
        return truthCounter == clauses.size
    }

    private fun someClauseFalse(clauses: Set<Disjunction>, model: Map<String, Boolean?>): Boolean {
        for (clause in clauses) {
            if (clause.evaluate(model) == false) {
                inconsistentBeliefs.add(clause.parent!!.parent)
                return true
            }
        }
        return false
    }

    private fun DPLL_satisfiable(): Boolean {
        val clauses: MutableSet<Disjunction> = mutableSetOf()
        val literals: MutableSet<Literal> = mutableSetOf()
        val model: MutableMap<String, Boolean?> = mutableMapOf()

        for (belief in beliefs) {
            clauses.addAll(belief.CNF.disjunctions)
        }

        for (belief in beliefs) {
            for (disjunction in belief.CNF.disjunctions) {
                for (literal in disjunction.variables) {
                    literals.add(literal)
                }
            }
        }
        inconsistentBeliefs.clear()
        return DPLL(clauses, literals, model)
    }

    private fun DPLL(
        clauses: Set<Disjunction>,
        symbols: MutableSet<Literal>,
        model: MutableMap<String, Boolean?>
    ): Boolean {

        //If every clause in clauses is true in model then return true
        if (allClausesTrue(clauses, model)) {
            return true
        }

        //If some clause in clauses is false in model then return false
        if (someClauseFalse(clauses, model)) {
            return false
        }

        // iterate over strings in model. If any string is only presented one time, safely set it to true and check the model
        //P, value = FINDPURESYMBOL(symbol, clauses, model)
        var pureLiteral: Literal? = null
        for (literal in symbols) {
            var pure: Boolean = true
            val symbolsOfLiteral: List<Literal> =
                symbols.filter { sym -> sym.varName == literal.varName }
            for (innerLiteral in symbolsOfLiteral) {
                if (innerLiteral.isNot != literal.isNot) {
                    pure = false
                    break
                }
            }
            if (pure) {
                pureLiteral = literal
            }
        }
        //If P != null return DPLL(clauses, symbols - P, model where P = value)
        if (pureLiteral != null && model[pureLiteral.varName] == null) {

            model[pureLiteral.varName] = !pureLiteral.isNot
            symbols.remove(pureLiteral)
            return DPLL(clauses, symbols, model)
        }

        /*  Iterates over the clauses. If a clause contains exactly one undecided literal
            and all other literals are false (as by the model),
            then it should set that literal to true in the model
            and remove it from the symbols set.*/
        var secondP: Literal? = null
        for (clause in clauses) {
            var undecidedCount: Int = 0
            var allLiteralsFalse = true
            for (variable in clause.variables) {
                val status: Boolean? = model[variable.varName]
                if (status == null) {
                    undecidedCount += 1
                    secondP = variable
                } else if (status == true) {
                    allLiteralsFalse = false
                    break
                }
            }
            if (undecidedCount == 1 && allLiteralsFalse) {
                if (secondP != null) {
                    model[secondP.varName] = !secondP.isNot //Fixed it here?
                }
                symbols.remove(secondP)
            }
        }

        //We set the first unassigned literal to either true or false and run the function recursively
        val thirdP: Literal = symbols.first()
        symbols.remove(thirdP)

        val modelWherePTrue = model.toMutableMap()
        val modelWherePFalse = model.toMutableMap()
        modelWherePTrue[thirdP.varName] = true
        modelWherePFalse[thirdP.varName] = false
        return DPLL(clauses, symbols, modelWherePTrue) || DPLL(clauses, symbols, modelWherePFalse)
    }

    //The function that revises the beliefs and adds entailments
    private fun revise(): Boolean {
        val allLiterals: MutableSet<Literal> = mutableSetOf()
        allLiterals.clear()
        //Add all literals to set
        for (belief in beliefs) {
            for (disjunction in belief.CNF.disjunctions) {
                allLiterals.addAll(disjunction.variables)
            }
        }

        //add all entailments' literals to the set as well
        val visitedBeliefs: MutableMap<Belief, Boolean> = mutableMapOf()
        for (belief in beliefs) {
            for (child in belief.entailments) {
                if (visitedBeliefs[child] != true) {
                    for (disjunction in child.CNF.disjunctions) {
                        allLiterals.addAll(disjunction.variables)
                    }
                    visitedBeliefs[child] = true
                }
            }
        }


        for (i in 0 until allLiterals.size - 1) {
            for (j in i + 1 until allLiterals.size) {
                if (allLiterals.elementAt(i).isNot != allLiterals.elementAt(j).isNot &&
                    allLiterals.elementAt(i).varName == allLiterals.elementAt(j).varName
                ) {

                    //Some wild string concatenation that CNF allows us to use
                    //We basically make new beliefs as entailments based on the current beliefs
                    var stringPartOne = ""
                    for (literal in allLiterals.elementAt(i).parent.variables){
                        if(literal != allLiterals.elementAt(i)){
                            stringPartOne = cropString(stringPartOne + "|" +  literal.literalString)
                        }
                    }

                    var stringPartTwo = ""
                    for (literal in allLiterals.elementAt(j).parent.variables){
                        if(literal != allLiterals.elementAt(j)){
                            stringPartTwo = cropString(stringPartTwo + "|" + literal.literalString)
                        }
                    }

                    stringPartOne.replace("(", "").replace(")", "")
                    stringPartTwo.replace("(", "").replace(")", "")
                    var newString = cropString(
                        "$stringPartOne|$stringPartTwo"
                    )
                    newString = newString.replace(" ", "") //removes spaces

                    if(allEntailmentStrings.contains(newString)) break
                    if(newString.count { it == '|' } > RELEVANT_ORS){
                        break
                    }
                    allEntailmentStrings.add(newString)


                    val newBelief = Belief(newString)
                    val allRelevant: MutableSet<String> = mutableSetOf()
                    for (belief in beliefs){
                        for (entailment in belief.entailments)
                        allRelevant.add(entailment.CNFString)
                    }

                    //if a new belief is found it is added to the entailment
                    if (!allRelevant.contains(newBelief.CNFString)){
                        var new = true
                        for (belief in allEntailments){
                            if (belief.CNFString == newBelief.CNFString){
                                new = false
                                break
                            }
                        }

                        allEntailments.add(newBelief)
                        allLiterals.elementAt(i).parent.parent!!.parent.entailments.add(newBelief)
                        allLiterals.elementAt(j).parent.parent!!.parent.entailments.add(newBelief)
                        if(new) revise()
                    }


                }
            }
        }
        return false
    }

    //Takes a string that needs to be removed and redos the entailments
    fun contractBelief(stringToRemove: String): Boolean {
        var beliefToRemove : Belief? = null
        for (belief in beliefs){
            if (belief.CNFString == stringToRemove){
                //We avoid a ConcurrentModificationException
                beliefToRemove = belief
            }
        }
        if (beliefToRemove != null){
            beliefs.remove(beliefToRemove)
            redoEntailments()
            printBeliefs()
            return true
        }
        return false
    }
}

private fun cropString(input: String): String {
    var newString = " $input"
    // Regex to find the index of the first and last letter.
    val firstLetterIndex = newString.indexOfFirst { it.isLetter() }
    val lastLetterIndex = newString.indexOfLast { it.isLetter() }

    if (firstLetterIndex == -1 || lastLetterIndex == -1) {
        // No letters found, return an empty string or the original string based on requirement.
        return ""
    }

    newString = newString.substring(firstLetterIndex-1,lastLetterIndex+1)
    if (newString[0] == '~'){
        return newString
    }
    return newString.substring(1)
}
