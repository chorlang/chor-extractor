# Project is WIP

## Overview
This projects aim to implement an algorithm for extracting choreographies in Java, based on the work of Luíz Cruz-Filipe, Fabrizio Montesi, and Larisa Safina.

## Compiling and Executing
First generate the network parser using maven as described in the below section. 
Then compile SimpleTest.java in the package executable, and execute the compiled class. The current implementation will only run simple tests on a hardcoded network, although the network will be extracted (if possible).

## Generating network parser
The project uses Antlr4 to generate parsers for network grammars. Build target "antlr4" with Maven.
