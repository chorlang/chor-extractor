package executable.tests

import endpointprojection.EndPointProjection
import extraction.Extraction
import extraction.Strategy
import extraction.network.*
import extraction.choreography.*
import parsing.Parser
import utility.Bisimulation
import utility.choreographyStatistics.ChoreographyStatistics
import utility.choreographyStatistics.LengthOfProcedures
import utility.choreographyStatistics.NumberOfActions
import utility.fuzzing.NetworkFuzzer
import utility.networkRefactor.NetworkShifter
import utility.networkRefactor.NetworkUnfolder
import utility.networkStatistics.NetworkStatistics

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.ParseException
import java.util.*

object Benchmarks {
    //data class StatHeader(val length: String, val numOfProcesses: String, val numOfCondition: String, val numOfProcedures: String)

    data class ScrewedExecutionStatistics(val choreographyId: String,
                                          val minExecutionTime: Long, val maxExecutionTime: Long, val avgExecutionTime: Double,
                                          val minNodes: Int, val maxNodes: Int, val avgNodes: Int,
                                          val minBadLoops: Int, val maxBadLoops: Int, val avgBadLoops: Int)

//    companion object {
        private const val TEST_DIR = "tests"
        private const val CHOREOGRAPHY_PREFIX = "choreography-"
        private const val PROJECTION_PREFIX = "projection-"
        private const val FUZZ_PREFIX = "projection-fuzzed-"
        private const val UNROLLED_PREFIX = "projection-unrolled-"
        private const val UNROLLED_STATISTICS_PREFIX = "stats-unrolled-"
        private const val EXTRACTION_FUZZED_PREFIX = "extraction-fuzzed-"
        private const val EXTRACTION_PREFIX = "extraction-"
        private const val PROJECTION_STATISTICS_PREFIX = "stats-projection-"
        private const val EXTRACTION_STATISTICS_PREFIX = "stats-extraction-"
        private const val FUZZ_STATISTICS_PREFIX = "stats-fuzzing-"
        private const val FUZZED_EXTRACTION_STATISTICS_PREFIX = "stats-extraction-fuzzed-"
        private const val COMBINED_STATISTICS_PREFIX = "stats-"
        private const val SEP = "\t"

        private val PROJECTION_STATISTICS_HEADER =
                arrayOf("testId","numberOfActions","numberOfProcesses","numberOfProcedures","numberOfConditionals",
                "minLengthOfProcesses","maxLengthOfProcesses","avgLengthOfProcesses",
                "minNumberOfProceduresInProcesses","maxNumberOfProceduresInProcesses","avgNumberOfProceduresInProcesses",
                "minNumberOfConditionalsInProcesses","maxNumberOfConditionalsInProcesses","avgNumberOfConditionalsInProcesses","numberOfProcessesWithConditionals",
                "minProcedureLengthInProcesses","maxProcedureLengthInProcesses","avgProcedureLengthInProcesses").joinToString(SEP)
        private val EXTRACTION_STATISTICS_HEADER = arrayOf("testId","strategy","time(msec)","nodes","badLoops","mainLength","numOfProcedures","minProcedureLength","maxProcedureLength","avgProcedureLength").joinToString(SEP)
        private val SCREWED_PROJECTION_STATISTICS_HEADER = arrayOf("testId",
                "screwedId",
                "screwedNetwork",
                "addProcessPosition",
                "removeProcessPosition",
                "swapProcessPosition",
                "time(msecs)",
                "badLoops",
                "nodes").joinToString(SEP)
        private val SCREWED_EXTRACTION_STATISTICS_HEADER = arrayOf("testId",
                "minExecutionTime","maxExecutionTime","avgExecutionTime",
                "minNodes","maxNodes","avgNodes",
                "minBadLoops","maxBadLoops","avgBadLoops").joinToString(SEP)

        private val FUZZ_STATISTICS_HEADER = arrayOf("testId",
                "minLengthOfProcesses","maxLengthOfProcesses","avgLengthOfProcesses",
                "minNumberOfProceduresInProcesses","maxNumberOfProceduresInProcesses","avgNumberOfProceduresInProcesses",
                "minNumberOfConditionalsInProcesses","maxNumberOfConditionalsInProcesses","avgNumberOfConditionalsInProcesses","numberOfProcessesWithConditionals",
                "minProcedureLengthInProcesses","maxProcedureLengthInProcesses","avgProcedureLengthInProcesses").joinToString(SEP)
//    }

    /**
     * for each file with choreographies
     * 1. generate networks and write them to the file %original_file_name% networks
     * 2. get statistics and write to the file %original_file_name% statistics
     */

//    @Test
    fun epp() {
        checkOutputFolder()
        val choreographyFiles = parseChoreographyFiles(TEST_DIR, CHOREOGRAPHY_PREFIX) //HashMap<filename, HashMap<choreography_id, choreography_body>>
        choreographyFiles.forEach { (fileId, choreographyMap) ->
            if ( Files.notExists( Paths.get( "$OUTPUT_DIR/$PROJECTION_PREFIX$fileId" ) ) ) {
                val projectionMap = HashMap<String, Pair<Program, Network>>()
                choreographyMap.forEach { (choreographyId, choreography) ->
                    println("Projecting $choreographyId in $CHOREOGRAPHY_PREFIX$fileId")
                    val program = Parser.stringToProgram(choreography)
                    projectionMap[choreographyId] = Pair(program, EndPointProjection.project(program))
                }
                writeNetworksToFile(projectionMap, "$PROJECTION_PREFIX$fileId")
                writeNetworkStatisticsToFile(projectionMap, "$PROJECTION_STATISTICS_PREFIX$fileId")
            }
        }
    }

    private fun writeNetworksToFile(projectionMap: Map<String, Pair<Program, Network>>, filename: String) {
        File(OUTPUT_DIR, filename).printWriter().use { out ->
            out.println("testId${SEP}network")
            projectionMap.forEach { (id, pair) -> out.println("$id$SEP${pair.second}") }
        }
    }

    private fun writeNetworkStatisticsToFile(projectionMap: Map<String, Pair<Program, Network>>, filename: String) {
        File(OUTPUT_DIR, filename).printWriter().use { out ->
            out.println(PROJECTION_STATISTICS_HEADER)
            projectionMap.forEach { (id, pair) ->
                val networkStatistics = NetworkStatistics.compute(pair.second)
                val choreographyStatistics = ChoreographyStatistics.compute(pair.first)
                out.println("$id$SEP" +
                        "${choreographyStatistics.numberOfActions}$SEP" +
                        "${choreographyStatistics.numberOfProcesses}$SEP" +
                        "${choreographyStatistics.numberOfProcedures}$SEP" +
                        "${choreographyStatistics.numberOfConditionals}$SEP" +
                        "${networkStatistics.minLengthOfProcesses}$SEP" +
                        "${networkStatistics.maxLengthOfProcesses}$SEP" +
                        "${networkStatistics.avgLengthOfProcesses}$SEP" +
                        "${networkStatistics.minNumberOfProceduresInProcesses}$SEP" +
                        "${networkStatistics.maxNumberOfProceduresInProcesses}$SEP" +
                        "${networkStatistics.avgNumberOfProceduresInProcesses}$SEP" +
                        "${networkStatistics.minNumberOfConditionalsInProcesses}$SEP" +
                        "${networkStatistics.maxNumberOfConditionalsInProcesses}$SEP" +
                        "${networkStatistics.avgNumberOfConditionalsInProcesses}$SEP" +
                        "${networkStatistics.numberOfProcessesWithConditionals}$SEP" +
                        "${networkStatistics.minProcedureLengthInProcesses}$SEP" +
                        "${networkStatistics.maxProcedureLengthInProcesses}$SEP" +
                        "${networkStatistics.avgProcedureLengthInProcesses}"
                )
            }
        }
    }

    /* private fun cleanTestFolder(dirPath: String, prefix: String) {
        val dir = File(dirPath)
        require(dir.exists() && dir.isDirectory)

        for (fileName in dir.list()) {
            if (!fileName.startsWith(prefix)) {
                val file = File("$dirPath/$fileName")
                file.delete()
            }
        }
    } */


//    @Test
    fun extractionSoundness() {
        val originalChoreographies = parseChoreographyFiles(TEST_DIR, CHOREOGRAPHY_PREFIX)
        var ok = 0
        var maybe = 0
        var fail = 0
        Strategy.values().filter { it != Strategy.Default }.forEach { strategy ->
            val extractedChoreographies = parseExtractionFiles(TEST_DIR, "$EXTRACTION_PREFIX${strategy}-")
            originalChoreographies.forEach { (fileId, choreographyData) ->
                choreographyData.forEach { (id, choreography) ->
//                    println((extractedChoreographies[fileId]!!)[id]!!)
                    print("Checking $id in $fileId extracted with strategy $strategy. ")
                    try {
                        when( Bisimulation.bisimilar(choreography, (extractedChoreographies[fileId]!!)[id]!!)!! ) { //!!!!!!
                            Bisimulation.Throolean.OK -> { println( "Done" ); ok++ }
                            Bisimulation.Throolean.MAYBE -> { println( "Timeout" ); maybe++ }
                            Bisimulation.Throolean.FAIL -> { println( "Fail" ); fail++ }
                        }
                    } catch (ex:KotlinNullPointerException) {
                        println("This choreography has not been extracted yet.")
                    }
                }
            }
        }
        println("And the grand totals are:")
        println("\tOk: $ok")
        println("\tTimeout: $maybe")
        println("\tFail: $fail")
    }


    /*@Test
fun extractionSoundnessC41() {
    //NOTE that "10-6-0-0" doesn't contain "C41"
    val originalChoreographies = parseChoreographyFiles(TEST_DIR, CHOREOGRAPHY_PREFIX)
    val extractedChoreographies = parseExtractionFiles(TEST_DIR, EXTRACTION_PREFIX)

    assert(bisimilar((originalChoreographies["10-6-0-0"]!!)["C41"]!!, (extractedChoreographies["10-6-0-0"]!!)["C41"]!!))
}*/

    private fun parseExtractionFiles(dirPath: String, prefix: String): HashMap<String, HashMap<String, String>> {
        val dir = File(dirPath)
        require(dir.exists() && dir.isDirectory)

        val fileToChoreographyMap = HashMap<String, HashMap<String, String>>()

        for (fileName in dir.list()) {
            if (fileName.startsWith(prefix)) {
                val file = File("$dirPath/$fileName")
                fileToChoreographyMap[fileName.substringAfter(prefix)] = parseExtractionFile(file)
            }
        }

        return fileToChoreographyMap
    }

    private fun writeExtractionsToFile(extractionMap: Map<String, Pair<Program, Long>>, filename: String, strategyName: String) {
        File(OUTPUT_DIR, filename).printWriter().use { out ->
            out.println("testId${SEP}strategy${SEP}choreography")
            extractionMap.forEach { (id, pair) -> out.println("${id}${SEP}$strategyName${SEP}${pair.first}") }
        }
    }

    private fun writeExtractionStatisticsToFile(extractionMap: Map<String, Pair<Program, Long>>, filename: String, strategyName: String) {
        File(OUTPUT_DIR, filename).printWriter().use { out ->
            out.println(EXTRACTION_STATISTICS_HEADER)
            extractionMap.forEach { (id, pair) ->
                val program = pair.first
                val statistics = program.statistics.fold(Program.GraphStatistics(0, 0), { one, two -> Program.GraphStatistics(one.nodeCount + two.nodeCount, one.badLoopCount + two.badLoopCount) } )
                val choreographyProcedures = program.choreographies.map { it!!.procedures.size }.fold( 0, Int::plus )
                val lengthOfProcedures = program.choreographies.flatMap { LengthOfProcedures().getLength(it!!) }
                val numberOfActions = program.choreographies.map { NumberOfActions.compute(it!!) }.fold( 0, Int::plus )

                out.println("$id$SEP" +
                        "$strategyName$SEP" +
                        "${pair.second}$SEP" +
                        "${statistics.nodeCount}$SEP" +
                        "${statistics.badLoopCount}$SEP" +
                        "${numberOfActions}$SEP" +
                        "${choreographyProcedures}$SEP" +
                        "${lengthOfProcedures.min() ?: 0}$SEP" +
                        "${lengthOfProcedures.max() ?: 0}$SEP" +
                        "${if (lengthOfProcedures.isNotEmpty()) lengthOfProcedures.average() else 0}"
                )
            }
        }
    }

//    @Test
    fun extractionTest() = Strategy.values().forEach { if ( it != Strategy.Default ) extraction(it) }


    private fun fuzzUntilItWorks(network:String, dels:Int, swaps:Int):Network {
        do {
          try {
              return NetworkFuzzer.fuzz(network, dels, swaps)
          } catch( e:IllegalStateException ) {
              println("Refuzzing...")
          }
        } while( true )
    }

    private fun fuzz(dels:Int, swaps:Int) {
        checkOutputFolder()

        val networkFiles = parseNetworkFiles(TEST_DIR, PROJECTION_PREFIX) // HashMap<filename, HashMap<id, network_body>>
        networkFiles.forEach { (fileId, networkMap) ->
            val filePrefix = "$dels-$swaps-$fileId"
            if ( Files.notExists( Paths.get( "$OUTPUT_DIR/$FUZZ_PREFIX$filePrefix" ) ) && !fileId.contains("fuzzed") && !fileId.contains("unrolled")) {
                val fuzzMap = HashMap<String, Pair<Network, Long>>()
                networkMap
//                        .filter { (id, network) -> id == "C129" }
                        .forEach { id, network ->
                            println("Fuzzing $id from $PROJECTION_PREFIX$fileId")
                            val start = System.currentTimeMillis()
                            val fuzzedNetwork = fuzzUntilItWorks( network, dels, swaps )
                            val executionTime = System.currentTimeMillis() - start

                            fuzzMap[id] = Pair(fuzzedNetwork, executionTime)
                        }
                writeFuzzedNetworksToFile(fuzzMap, "$FUZZ_PREFIX$filePrefix")
                writeFuzzingStatisticsToFile(fuzzMap, "$FUZZ_STATISTICS_PREFIX$filePrefix")
//            }
            }
        }
    }

//    @Test
    fun fuzzThemAll() {
        fuzz(0, 1)
        fuzz(1, 0)
        fuzz(2, 2)
    }

//    @Test
    fun unrollAndShift() {
        checkOutputFolder()

        val networkFiles = parseNetworkFiles(TEST_DIR, PROJECTION_PREFIX) // HashMap<filename, HashMap<id, network_body>>
        networkFiles.forEach { (fileId, networkMap) ->
            if ( Files.notExists( Paths.get( "$OUTPUT_DIR/$UNROLLED_PREFIX$fileId" ) ) && !fileId.contains("fuzzed") && !fileId.contains("unrolled") && fileId.matches("\\d+-\\d+-\\d+-[1-9]+".toRegex()) ) {
                val resultMap = HashMap<String, Pair<Network, Long>>()
                networkMap
                        .forEach { (id, network) ->
                            println("Doing stuff with $id from $PROJECTION_PREFIX$fileId")
                            val start = System.currentTimeMillis()
                            val pShift = 0.6
                            val pUnfold = 0.2
                            val iterations = 0
                            val newNetworks = NetworkShifter.compute(NetworkUnfolder.compute( network, pUnfold, iterations ).toString(), pShift)
                            val executionTime = System.currentTimeMillis() - start

                            resultMap[id] = Pair(newNetworks, executionTime)
                        }
                writeFuzzedNetworksToFile(resultMap, "$UNROLLED_PREFIX$fileId")
                writeFuzzingStatisticsToFile(resultMap, "$UNROLLED_STATISTICS_PREFIX$fileId")
            }
        }
    }

    private fun writeFuzzedNetworksToFile(projectionMap: Map<String, Pair<Network, Long>>, filename: String) {
        File(OUTPUT_DIR, filename).printWriter().use { out ->
            out.println("testId${SEP}network")
            projectionMap.forEach { (id, pair) -> out.println("$id$SEP${pair.first}") }
        }
    }

    private fun writeFuzzingStatisticsToFile(projectionMap: Map<String, Pair<Network, Long>>, filename: String) {
        File(OUTPUT_DIR, filename).printWriter().use { out ->
            out.println(FUZZ_STATISTICS_HEADER)
            projectionMap.forEach { (id, pair) ->
                val networkStatistics = NetworkStatistics.compute(pair.first)
                out.println("$id$SEP" +
                        "${networkStatistics.minLengthOfProcesses}$SEP" +
                        "${networkStatistics.maxLengthOfProcesses}$SEP" +
                        "${networkStatistics.avgLengthOfProcesses}$SEP" +
                        "${networkStatistics.minNumberOfProceduresInProcesses}$SEP" +
                        "${networkStatistics.maxNumberOfProceduresInProcesses}$SEP" +
                        "${networkStatistics.avgNumberOfProceduresInProcesses}$SEP" +
                        "${networkStatistics.minNumberOfConditionalsInProcesses}$SEP" +
                        "${networkStatistics.maxNumberOfConditionalsInProcesses}$SEP" +
                        "${networkStatistics.avgNumberOfConditionalsInProcesses}$SEP" +
                        "${networkStatistics.numberOfProcessesWithConditionals}$SEP" +
                        "${networkStatistics.minProcedureLengthInProcesses}$SEP" +
                        "${networkStatistics.maxProcedureLengthInProcesses}$SEP" +
                        "${networkStatistics.avgProcedureLengthInProcesses}"
                )
            }
        }
    }

    fun extraction(strategy: Strategy) {
        checkOutputFolder()

        val networkFiles = parseNetworkFiles(TEST_DIR, PROJECTION_PREFIX) // HashMap<filename, HashMap<id, network_body>>

        networkFiles.forEach { (fileId, networkMap) ->
            if ( Files.notExists( Paths.get( "$OUTPUT_DIR/$EXTRACTION_PREFIX${strategy.name}-$fileId" ) ) ) {
                val extractionMap = HashMap<String, Pair<Program, Long>>()
                networkMap
//                        .filter { (id, network) -> id == "C129" }
                        .forEach { (id, network) ->
                            println("Extracting $id from $PROJECTION_PREFIX$fileId with strategy ${strategy.name}")
                            val start = System.currentTimeMillis()
                            var program = Extraction.extractChoreography(network, strategy, setOf())
                            if ( program.choreographies.contains(null) ) {
                                println("There's an unextractable network in $id from $PROJECTION_PREFIX$fileId with strategy ${strategy.name}")
                                program = Program(emptyList(), program.statistics)
                            }
                            val executionTime = System.currentTimeMillis() - start

                            extractionMap[id] = Pair(program, executionTime)
                        }
                writeExtractionsToFile(extractionMap, "$EXTRACTION_PREFIX${strategy.name}-$fileId", strategy.name)
                writeExtractionStatisticsToFile(extractionMap, "$EXTRACTION_STATISTICS_PREFIX${strategy.name}-$fileId", strategy.name)
//            }
//            }
            }
        }
    }

    @Suppress("PrivatePropertyName")
    private val OUTPUT_DIR = TEST_DIR

    private fun checkOutputFolder() {
        val dir = File(OUTPUT_DIR)
        if (!dir.exists())
            dir.mkdirs()
        else if (!dir.isDirectory)
            throw InternalError("$OUTPUT_DIR already exists and is not a directory")
    }

    private fun parseNetworkFiles(dirPath: String, prefix: String): HashMap<String, HashMap<String, String>> {
        val dir = File(dirPath)
        require(dir.exists() && dir.isDirectory)

        val fileToNetworksMap = HashMap<String, HashMap<String, String>>()

        for (fileName in dir.list()) {
            if (fileName.startsWith(prefix)) {
                val file = File("$dirPath/$fileName")
                fileToNetworksMap[fileName.substringAfter(prefix)] = parseNetworkFile(file)
            }
        }

        return fileToNetworksMap
    }

    private fun parseNetworkFile(file: File): HashMap<String, String> {
        val networks = HashMap<String, String>()

        file.forEachLine { line ->
            val separator = line.indexOf(SEP)
            if (separator != -1) {
                networks[line.substring(0, separator)] = line.substring(separator + 1)
            }
        }

        networks.remove("testId") //remove csv title

        return networks
    }

    /**
     * @input: dir_path
     * @output: HashMap<file_id, HashMap<choreography_id, choreography_body>>
     */
    private fun parseChoreographyFiles(dirPath: String, prefix: String): HashMap<String, HashMap<String, String>> {
        val dir = File(dirPath)
        require(dir.exists() && dir.isDirectory)

        val fileToChoreographyMap = HashMap<String, HashMap<String, String>>()

        for (fileName in dir.list()) {
            if (fileName.startsWith(prefix)) {
                val file = File("$dirPath/$fileName")
                fileToChoreographyMap[fileName.substringAfter(prefix)] = parseChoreographyFile(file)
            }
        }

        return fileToChoreographyMap
    }

    /**
     * @output: HashMap<choreography_id, choreography_body>>
     */
    private fun parseChoreographyFile(file: File): HashMap<String, String> {
        val choreography = ArrayList<String>()
        var name = ""

        val choreographyMap = HashMap<String, String>()

        file.forEachLine { line ->
            when {
                line.startsWith("***") -> name = line.substring(4).substringBefore("***").trim()
                line.startsWith("def") -> choreography.add(line)
                line.startsWith("main") -> choreography.add(line)
                line.isEmpty() -> {
                    choreographyMap[name] = choreography.joinToString(separator = " ")
                    choreography.clear()
                    name = ""
                }
                else -> throw ParseException("line $line was unexpected in $file", 0)
            }
        }

        if (choreography.isNotEmpty() && name != "") {
            choreographyMap[name] = choreography.joinToString(separator = " ")
        }

        return choreographyMap
    }

    private fun parseExtractionFile(file: File): HashMap<String, String> {
        val choreographyMap = HashMap<String, String>()

        file.forEachLine { line ->
            when {
                line.startsWith("id") -> {
                }
                else -> {
                    val (id, _, choreography) = line.split(SEP, limit = 3)
                    choreographyMap[id] = choreography
                }
            }
        }

        return choreographyMap
    }

    /*private fun parseStatName(name: String): StatHeader {
        val stat = name.split("-".toRegex())
        return StatHeader(stat[stat.size - 4], stat[stat.size - 3], stat[stat.size - 2], stat[stat.size - 1])
    }*/

//    @Test
    fun runAllBenchmarks() {
        epp()
        extractionTest()
        //screwDataStatistics()
        makeCombinedStatistics()
    }

//    @Test
    fun myTest() {
        val group = "fuzzed-0-1"
//        val group = ""
//        val group = "unrolled"
        val pair = Pair("all", ".*^(?!.*(fuzzed|unrolled))\\d.*\$")
//        val pair = Pair("comms-only", "(\\d+)-6-0-0")
//        val pair = Pair("increasing-ifs-procedures", "200-5-(\\d+)-(\\d+)")
        val strategy = Strategy.LongestFirst
//        combineStatistics("${group}-${pair.first}", "${group}${pair.second}", strategy) right for ""
        combineStatistics(pair.first, pair.second, strategy, group)
    }

//    @Test
    fun makeCombinedStatistics() {
        val tests = arrayOf(
                Pair("comms-only", "(\\d+)-6-0-0"),
                Pair("increasing-ifs-no-recursion", "50-6-(\\d+)-0"),
                Pair("increasing-ifs-procedures", "200-5-(\\d+)-(\\d+)"),
                Pair("increasing-processes", "500-(\\d+)-0-0"),
                Pair("increasing-ifs-with-recursion", "100-10-(\\d+)-5"),
                Pair("increasing-procedures-no-ifs", "1000-5-0-(\\d+)"),
                Pair("increasing-procedures-fixed-ifs", "200-10-20-(\\d+)"),
//                Pair("all", ".*")
                Pair("all", ".*^(?!.*(fuzzed|unrolled))\\d.*\$")
        )

        val groups = arrayOf( "", "fuzzed-0-1", "fuzzed-1-0", "fuzzed-2-2", "unrolled" )

        groups.forEach { group ->
            tests.forEach { pair ->
                Strategy.values().filter { it != Strategy.Default }.forEach { strategy ->
                    combineStatistics(pair.first, pair.second, strategy, group)
                }
                doTheRightThing(group, pair.first)
            }
        }
    }

    private fun retrieveTestFileData(prefix: String, regexStr: String): HashMap<String, String> {
        val dir = File(TEST_DIR)
        val regex = regexStr.toRegex()
//        println("Prefix $prefix and regex $regexStr")
        val data = HashMap<String, String>()

        for (filename in dir.list()) {
//            println("Filename: $filename, Prefix: $prefix, Regexp: $regexStr")
            if (
                    filename.startsWith(prefix) && regex.matches("${filename.removePrefix(prefix)}")
            ) {
//                if (filename.contains("fuzzed"))
//                println("Matched $filename w/ regex $regexStr")
                val lines = File("$TEST_DIR/$filename").readLines()
                var i = 1
                while (i < lines.size) {
                    val split = lines[i].split(SEP, limit = 2)
                    data[split[0]] = split[1]
                    i++
                }
            }
        }

        return data
    }

    private fun combineStatistics(filename: String, regexStr: String, strategy: Strategy, group:String) {
        val dash = if ( group == "" ) "" else "-"
        val outputFile = File("$TEST_DIR/$COMBINED_STATISTICS_PREFIX${strategy.name}$dash${group}-${filename}")
//        println("OUTPUT FILE ${outputFile}")

        val statsToCombine = when( group ) {
            "" -> arrayOf(PROJECTION_STATISTICS_PREFIX, "$EXTRACTION_STATISTICS_PREFIX${strategy.name}-")
            "unrolled" -> arrayOf(UNROLLED_STATISTICS_PREFIX, "$EXTRACTION_STATISTICS_PREFIX${strategy.name}-")
            else -> arrayOf(FUZZ_STATISTICS_PREFIX + group.substring(7) + "-", "$EXTRACTION_STATISTICS_PREFIX${strategy.name}-")
        }

        val headersToCombine = when( group ) {
            "" -> arrayOf(PROJECTION_STATISTICS_HEADER, EXTRACTION_STATISTICS_HEADER)
            "unrolled" -> arrayOf(FUZZ_STATISTICS_HEADER, EXTRACTION_STATISTICS_HEADER)
            else -> arrayOf(FUZZ_STATISTICS_HEADER, EXTRACTION_STATISTICS_HEADER)
        }

        val bigData = HashMap<String, Map<String, String>>() // prefix -> choreography_id -> data
//        for (prefix in statsToCombine) {
            bigData[statsToCombine[0]] = retrieveTestFileData(statsToCombine[0], regexStr)
            bigData[statsToCombine[1]] = retrieveTestFileData("${statsToCombine[1]}${group}$dash", regexStr)
//        }

        outputFile.printWriter().use { out ->
            out.println("id$SEP" + headersToCombine.joinToString(SEP) { it.split(SEP, limit = 2)[1] })

            for (key in bigData[statsToCombine[0]]!!.keys.map { Integer.parseInt(it.substring(1)) }.sorted()) {
                val ckey = "C$key"
                out.print("$ckey$SEP")
                out.println(bigData.toSortedMap(reverseOrder()).map { it.value[ckey] }.joinToString(SEP))
            }
        }
    }

    private fun doTheRightThing(group:String, testName:String) {
        val dash = if ( group == "" ) "" else "-"
        val data = HashMap<String,MutableMap<String,MutableMap<String,String>>>() // strategy -> choreography -> header -> value

        val strategies = Strategy.values().filter { it != Strategy.Default }

        strategies.forEach { strategy ->
            data["$strategy"] = HashMap()
            val headers = Files.lines(Paths.get("$TEST_DIR/$COMBINED_STATISTICS_PREFIX${strategy.name}-$group$dash$testName")).findFirst().get().split(SEP)

            Files.lines(Paths.get("$TEST_DIR/$COMBINED_STATISTICS_PREFIX${strategy.name}-$group$dash$testName")).skip(1).forEach {
                val line = it.split(SEP)
                data["$strategy"]!![line[0]] = HashMap()
                for( i in 1 until line.size) {
                    data["$strategy"]!![line[0]]!![headers[i]] = line[i]
                }
            }
        }

        val outputFile = File("${OUTPUT_DIR}/stats-$group$dash${testName}")

        outputFile.printWriter().use { out ->
            PROJECTION_STATISTICS_HEADER.split(SEP).forEach { header ->
                out.write(header + SEP)
            }
            strategies.forEach { strategy ->
                EXTRACTION_STATISTICS_HEADER.split(SEP).stream().skip(2).forEach { header ->
                    out.write("$header-$strategy$SEP")
                }
            }
            out.println()

            data[Strategy.Random.toString()]!!.keys.sorted().forEach { choreography ->
                out.write("$choreography$SEP")
                PROJECTION_STATISTICS_HEADER.split(SEP).stream().skip(1).forEach { header ->
                    out.write(data[Strategy.Random.toString()]!![choreography]!![header] + SEP)
                }
                strategies.forEach { strategy ->
                    EXTRACTION_STATISTICS_HEADER.split(SEP).stream().skip(2).forEach { header ->
                        out.write(data[strategy.toString()]!![choreography]!![header] + SEP)
                    }
                }
                out.println()
            }
            out.flush()
        }
    }
}