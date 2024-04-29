public val inconsistentBeliefs: MutableSet<Belief> = mutableSetOf()
public val RELEVANT_ORS = 2

public class Disjunction(val disjunctionString: String, parentCNF: CNF?) {
    val parent = parentCNF
    val variables: MutableList<Literal> = mutableListOf()

    init {
        for (literalString in disjunctionString.split('|')) {
            //println(literalString)
            //println("or")
            variables.add(Literal(literalString, this))
        }
    }

    fun evaluate(map: Map<String, Boolean?>): Boolean? {
        var falseVariableCounter = 0
        for (variable in variables) {
            //return variable.evaluate(map)
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
public class CNF(var CNFString: String, parentBelief: Belief) {
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
            val result = disjunction.evaluate(map)
            if (result == null) return null
            if (!disjunction.evaluate(map)!!) {
                return false
            }
        }
        return true
    }
}

public class Literal(val literalString: String, parentDisjunction: Disjunction) {
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
 * This function is basically confirmation basis, algorithmically
 */
public fun getWorth(belief: Belief): Int {
    //TODO: We should ideally determine this based on number of entailments, but this works for now
    return belief.addedNumber
}

/**
 * Decides on which belief to remove.
 */


public class Belief(originalExpression: String) {
    val CNFString = originalExpression
    var CNF: CNF = CNF(originalExpression, this)

    //addedNumber Is used to order the beliefs. Maybe we should just use a sorted list instead.
    //This is mainly for if we just remove the oldest belief first, which is dubious, but at the same time
    //we automatically assume that newer beliefs are true so it follows that older beliefs are less true
    var addedNumber: Int = 0

    //All beliefs that directly follow from this belief. This is the "Children"
    val entailments: MutableList<Belief> = mutableListOf()

    val parents: MutableList<Belief> = mutableListOf() //The corresponding parents. Not sure if we want this

    fun giveEntailment(newBelief: Belief){

    }
}

/**
 * The big boy
 */
class BeliefBase {
    private var numberOfBeliefs: Int = 0 //Keeps track of total number of beliefs that have been added. Works as a "timestamp"

    private val beliefs: MutableSet<Belief> = mutableSetOf() //Only holds base beliefs. None of these have parents
    private val allEntailments: MutableSet<Belief> = mutableSetOf() //

    private fun selectAndRemoveBelief(contradictingBeliefs: Set<Belief>, holyBelief: Belief) {

        //TODO The following is based on number of entailments/children.
        // We could, alternatively, just order them based on addedNumber if this is impractical

        if (contradictingBeliefs.size == 1 && contradictingBeliefs.first() == holyBelief) {
            println("Your belief is an oxymoron (Contradiction). It will not be added to the belief base")
            beliefs.remove(holyBelief)
            return
        }

        val beliefsToNumbers: MutableMap<Belief, Int> = mutableMapOf() //Rename this?
        for (belief in contradictingBeliefs) {
            beliefsToNumbers.put(key = belief, value = getWorth(belief))
        }
        beliefsToNumbers.remove(holyBelief) //We remove the holy belief, because we don't want it removed from the set of all beliefs
        beliefs.remove(beliefsToNumbers.minBy { it.value }.key) //Lowest val
    }

    private fun printBeliefs() {

        val printedBeliefs: MutableMap<String, Boolean> = mutableMapOf()

        println("Current base beliefs are:")
        for (belief in beliefs) {
            if (printedBeliefs[belief.CNFString] != true) println(belief.CNFString)
            printedBeliefs[belief.CNFString] = true
        }
        println("Entailments of these are:")
        for (belief in beliefs) {
            for (child in belief.entailments) {
                if (printedBeliefs[child.CNFString] != true) println("\t" + child.CNFString)
                printedBeliefs[child.CNFString] = true
            }
        }
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
    public fun giveBeliefString(newBeliefString: String) {
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
        val clauses: MutableSet<Disjunction> = mutableSetOf<Disjunction>()
        val literals: MutableSet<Literal> = mutableSetOf<Literal>()
        val model: MutableMap<String, Boolean?> = mutableMapOf()

        for (belief in beliefs) {
            clauses.addAll(belief.CNF.disjunctions)
        }

        for (belief in beliefs) {
            for (disjunc in belief.CNF.disjunctions) {
                for (literal in disjunc.variables) {
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
        //println("Testing with model " + model)
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
            var symbolsOfLiteral: List<Literal> =
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

        if (pureLiteral != null && model[pureLiteral.varName] == null) { //If P != null return DPLL(clauses, symbols - P, model where P = value)

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


        //P = FIRST(Symbols) [pick any?]
        //Rest = REST(symbols)
        var thirdP: Literal
        thirdP = symbols.first()
        symbols.remove(thirdP)

        val modelWherePTrue = model.toMutableMap()
        val modelWherePFalse = model.toMutableMap()
        modelWherePTrue.set(thirdP.varName, true)
        modelWherePFalse.set(thirdP.varName, false)
        return DPLL(clauses, symbols, modelWherePTrue) || DPLL(clauses, symbols, modelWherePFalse)
    }

    val allEntailmentStrings: MutableSet<String> = mutableSetOf()


    fun revise(): Boolean {
        val allLiterals: MutableSet<Literal> = mutableSetOf()
        allLiterals.clear()
        for (belief in beliefs) {
            for (disjunction in belief.CNF.disjunctions) {
                allLiterals.addAll(disjunction.variables)
            }
        }


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

                    //Some wild string concanation that CNF allows us to use
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
                        stringPartOne + "|" + stringPartTwo
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

                    if (!allRelevant.contains(newBelief.CNFString)){
                        var New = true
                        for (belief in allEntailments){
                            if (belief.CNFString == newBelief.CNFString){
                                New = false
                                break
                            }
                        }
                        //We still add here, because it might be new parents. Who cares, its just time

                        allEntailments.add(newBelief)
                        allLiterals.elementAt(i).parent.parent!!.parent.entailments.add(newBelief)
                        allLiterals.elementAt(j).parent.parent!!.parent.entailments.add(newBelief)
                        if(New) revise()
                    }


                }
            }
        }
        return false
    }



    // negate belief
    // loop through all disjunctions in the new belief
    // then do an inner loop for all disjunctions in current belief base

    // if a variable name in the disjunction (from the new belief) is
    // present in a disjunction (from the current belief base), then smack it down
    // to one clause, by removing the variable from the clause, and exchange it
    // for the other variable in the disjunction (from the new belief)

    // aka.
    // current negated disjunction in the outer loop is: (P V Q)
    // current disjunction in the inner loop is: (-P V R)
    // they together hold -P and P
    // Remove (-P V R) from the belief base, and replace it with one clause
    // that just contains the remains of the two clauses, aka. (Q V R)
    // keep doing this.
    // If any clause is at any time empty, there is a contradiction.
    // // If we can not get an empty clause, then the new belief is not entailed
    // by the belief base. although, it might still be true,
    // but we are uncertain if it is due to the belief base.

    fun justAnotherCoolFunction(newBelief: Belief): Boolean {
        var anythingChanged = false
        for (outerDisjunction in newBelief.CNF.disjunctions) {
            outerDisjunction.variables
            for (belief in beliefs) {
                for (innerDisjunction in belief.CNF.disjunctions) {
                    val variablesToRemove = innerDisjunction.variables.filter { innerVariable ->
                        outerDisjunction.variables.any { outerVariable ->
                            innerVariable.varName == outerVariable.varName && innerVariable.isNot != outerVariable.isNot
                        }
                    }
                    if (variablesToRemove.isNotEmpty()) {
                        innerDisjunction.variables.removeAll(variablesToRemove)
                        if (innerDisjunction.variables.isEmpty()) {
                            belief.CNF.disjunctions.remove(innerDisjunction)
                        }
                        anythingChanged = true
                    }
                }
            }
        }
        return anythingChanged
    }

    fun justAnotherAnotherOtherCoolFunction(newBelief: Belief): Boolean {
        var anythingChanged = false
        for (outerDisjunction in newBelief.CNF.disjunctions) {
            for (belief in beliefs) {
                for (innerDisjunction in belief.CNF.disjunctions) {
                    val variablesToRemove = innerDisjunction.variables.filter { innerVariable ->
                        outerDisjunction.variables.any { outerVariable ->
                            innerVariable.varName == outerVariable.varName && innerVariable.isNot != outerVariable.isNot
                        }
                    }

                    if (variablesToRemove.isNotEmpty()) {
                        val newVariables = innerDisjunction.variables.filterNot { it in variablesToRemove }
                        if (newVariables.isNotEmpty()) {
                            val newDisjunctionString =
                                "(${newVariables.map { literal -> literal.literalString }.joinToString("|") { it }})"
                            println("newstring is:" + newDisjunctionString)
                            var newBelief = Belief(newDisjunctionString)
                            allEntailments.add(newBelief)
                            //outerDisjunction.parent!!.parent.entailments.add(newBelief)
                            //innerDisjunction.parent!!.parent.entailments.add(Belief(newString))
                            anythingChanged = true
                        }
                    }
                }
            }
        }
        /*for (belief in beliefs) {
            justAnotherAnotherOtherOtherOtherCoolFunction(belief)
        }*/
        return anythingChanged
    }

    private fun justAnotherAnotherOtherOtherOtherCoolFunction(newEntailment: Belief): Boolean {
        var anythingChanged = false
        for (outerBelief in beliefs.flatMap { it.entailments }) {
            for (outerDisjunction in outerBelief.entailments.flatMap { entailment -> entailment.CNF.disjunctions }) {
                for (belief in beliefs.flatMap { it.entailments }) {
                    for (innerDisjunction in belief.CNF.disjunctions) {
                        val variablesToRemove = innerDisjunction.variables.filter { innerVariable ->
                            outerDisjunction.variables.any { outerVariable ->
                                innerVariable.varName == outerVariable.varName && innerVariable.isNot != outerVariable.isNot
                            }
                        }
                        if (variablesToRemove.isNotEmpty()) {
                            innerDisjunction.variables.removeAll(variablesToRemove)
                            if (innerDisjunction.variables.isEmpty()) {
                                belief.CNF.disjunctions.remove(innerDisjunction)
                            }
                            anythingChanged = true
                        }
                    }
                }
            }
        }
        return anythingChanged
    }

    fun justAnotherOtherWorkingPleaseMamaCoolIKilledAManFunction(): Boolean {
        var anythingChanged = false
        for (outerBelieve in allEntailments) {
            for (outerDisjunction in outerBelieve.CNF.disjunctions) {
                for (belief in allEntailments) {
                    for (innerDisjunction in belief.CNF.disjunctions) {
                        val variablesToRemove = innerDisjunction.variables.filter { innerVariable ->
                            outerDisjunction.variables.any { outerVariable ->
                                innerVariable.varName == outerVariable.varName && innerVariable.isNot != outerVariable.isNot
                            }
                        }
                        if (variablesToRemove.isNotEmpty()) {
                            innerDisjunction.variables.removeAll(variablesToRemove)
                            if (innerDisjunction.variables.isEmpty()) {
                                belief.CNF.disjunctions.remove(innerDisjunction)
                            }
                            anythingChanged = true
                        }
                    }
                }
            }
        }
        return anythingChanged
    }

    fun contractBelief(stringToRemove: String): Boolean {
        var beliefToRemove : Belief? = null
        for (belief in beliefs){
            if (belief.CNFString == stringToRemove){
                beliefToRemove = belief //We avoid a ConcurrentModificationException
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

private fun cropString(inputt: String): String {
    var input = " " + inputt
    // Regex to find the index of the first and last letter.
    val firstLetterIndex = input.indexOfFirst { it.isLetter() }
    val lastLetterIndex = input.indexOfLast { it.isLetter() }

    if (firstLetterIndex == -1 || lastLetterIndex == -1) {
        // No letters found, return an empty string or the original string based on requirement.
        return ""
    }

    input = input.substring(firstLetterIndex-1,lastLetterIndex+1)
    if (input[0] == '~'){
        return input
    }
    return input.substring(1)
}
