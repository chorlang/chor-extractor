# Project is WIP

## Overview
This projects aim to implement an algorithm for extracting choreographies in Java, based on the work of Luíz Cruz-Filipe, Fabrizio Montesi, and Larisa Safina.

## Compiling and Executing
First generate the network parser using maven as described in the below section. This only needs to be done once.
You can check things are working by compiling SimpleTest.java from the package executable, and execute the compiled class, to run a hardcoded example of network extraction.

Instead of compiling SimpleTest.java, you can compile ExtractionTesting.java (also from package executable). Executing it with the name of a test as argument will run that test. 
Executing without any argument will make it enter interactive mode. It will list possible tests, and allow you to enter the desired test to run.

## Generating extraction.network parser
The project uses Antlr4 to generate parsers for network grammars. Build target "antlr4" with Maven.

When using IntelliJ, this can be achieved by creating a new Maven build target, and setting the  field "Command line" to `compile antlr4:antlr4`