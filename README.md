# Choreographic Extractor
This project aims to autogenerate choreographic descriptions of distributed systems. This can help developers ensure their implementations adhere to their protocol, better analyze the behaviour of their systems, and ensure the absence of deadlocks.

The project is still in early development and may undergo big changes. An early [precompiled binary](#pre-compiled-command-line-application) is provided for convenience.

Based on the work of Luíz Cruz-Filipe, Fabrizio Montesi, and Larisa Safina.

## How it works
The program takes as input a *network*, which is a description of several parallel processes, reduced to all actions that are relevant for choreographies: interactions, and conditionals. It then simulates the processes to generate a tree-like graph called an *Symbolic Execution Graph* showing all paths of execution. It then converts all loops to procedures (functions), and traverses the graph to generate the corresponding choreography. 

## Compiling and running

First, generate the parser for converting string representations of networks to the applications internal representation. The project uses Antlr4 to generate its parsers. To do this, build target "antlr4" with Maven. 
When using IntelliJ, this can be achieved by creating a new Maven build target, and setting the  field "Command line" to `compile antlr4:antlr4`

The project uses Java 17 with preview features. When compiling and running, use the parameter `--enable-preview`. If using Intellij, this may automatically be picked up by the maven configuration. Although several classes are executable, no class is the correct "main" class, so you may want to create your own.

## Using in code
To use the application in code, simply run
```java
String networkDescription = getInput(); //Insert your own function here
var result = Extraction.newExtractor().extract(networkDesciption);
System.out.println(result.program.toString());
```
to parse the input string, extract the choreography, and write it to the console output. 

### Details
The function `extract()` returns a record with two fields: `program` and `extractionInfo`. The first is the result of the extraction. A program contains a public list `choreographies` of all extracted choreographies. The reason there may be multiple, is because a pre-processing step attempts to split the network into smaller networks that interact independently of the other split networks. Each such split is extracted independently. If all processes of your input is interdependent, there will be only one entry. If extraction fails, the choreography will be `null`. Calling `toString()` on the program will return the choreographic description of the system.

The other field, `extractionInfo` contains information generated during extraction. It is a list of record instances, where their index in the list corresponds to the same index of the choreography in `result.program.choreographies` whose extraction generated the data. The record is the following: 
```java
public static record Data(DirectedPseudograph<Node,Label> symbolicExecutionGraph,
                          DirectedPseudograph<Node,Label> unrolledGraph,
                          int badLoopCount, int nodeCount,
                          BuildGraphResult result, Node rootNode){}
```
The `symbolicExecutionGraph` is the graph generated during extraction, and is made using the library JgraphT. `badLoopCount` is how many times the program attempted to create a loop in the SEG but failed because not all processes was involved in the loop. `nodeCount` is how many vertices of the SEG. `result` is an enum that can be `OK` or `FAIL` depending on if extraction was successful or not. `rootNode` is the initial node of the SEG, representing the state of the network before starting the simulation. Each outgoing edge then represents an interaction, or conditional evaluation, and the node it leads to then contains the state of the network resulting from that operation.

## Pre-compiled command-line application
If you simply want to try out choreographic extraction, a simple command-line application is available under releases on GitHub. Although the algorithm is theoretically sound (proof will be released later), I make no guarantees for the correctness of the implementation.

Minimal working example (you **must** use java 17):
```bash
echo 'a{main{b!<hello>;b?;stop}} | b{main{a?;a!<world>;stop}}' | java -jar --enable-preview chorextr.jar
```
Use the `--help` option to see more options.

## Program input
The input is plaintext describing the network to extract a choreography from. The description details only the information relevant to choreographies, namely interactions, and conditionals. For more details and a guide, see the [network syntax guide](NetworkSyntax.md)

