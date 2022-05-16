# Network Syntax Guide
This document details how to write the input networks. As this project is still in early development, changes to syntax may not be reflected here immediately. Create an issue, or check the grammar specification [from the source file](/src/main/antlr4/Network.g4).

## Introduction to the basics
A network consists of parallel processes. Each process definition is separated by `|`
```
a{ ... } | b{ ... } | c{ ... } 
```
Each process must contain a main *behaviour* named `main`. Behaviours are similar to the instructions of a programming language, in that they describe the behaviour or actions that each process performs.
```
a{ main{ ... } }
```
The simplest (actionable) behaviours are send and receive. Send has the syntax `receivingProcessName!<message>` where `receivingProcessname` is the name of a different process that performs a matching receving action. The receive behaviour is even simpler: `sendingProcessname?`, and waits for a message from `sendingProcessName`.  There is also termination, written as `stop`, which simpy terminates the process. Example:
```
a{ main{ b!<HelloWorld>; stop } } | b{ main{ a?; stop } }
```
The above example is the simplest valid network, that extracts to a choreography that does something. The semicolon `;` is not simply a separator. It defines the behaviour to the right of it, as the *continuation* of the behaviour to the left of it. This simply means, after the behaviour to the left has executed (or *reduced*), the behaviour to the right is the next one to execute. Notice that `stop` does not have a continuation.

A more involved behaviour is the conditional. A conditional evaluates some expression internally, then pick between two behaviours to continue at. The network language does not care about the expression itself. Both cases will be evaluated during extraction.
```
a{ main{ if someExpression then b!<yes>; stop else b!<no>; stop } } | b{ main { a?; stop } }
```
The behaviour after the `then` represents what the process will do if the expression evaluates to true, while the behaviour after the `else` represents what will happen if it evaluates to false instead. As said before, the actual expression does not matter for extraction.

## Repeating behaviour with procedures
A process can define any number of *procedures* (including zero) which can be invoked by a *procedure invocation*. In regular programming terms, these correspond to function definitions, and function calls. Procedure names are local to each process, meaning multiple processes can have different procedures with the same name.
```
a{ def SendOnce{ b!<1>; stop } def SendTwice{ b!<2>; SendOnce } main{ SendTwice } } | b{ main{ a?; a?; stop } }
```
To create a loop, simply define a recursive procedure.
```
a{ def X{ ... ; X} main { X } } | ...
```
Loops do not need to be synchronized between processes, as long as the interactions line up. However if the loop only runs as long as a condition holds, how does one process inform the others that it broke out of the loop? The answer list in two new behaviours. The offering behaviour waits for another process to select among several possible behaviours, each given a *label*. The syntax is `selectingProcess&{continue: ..., retry: ..., terminate: ...}`. The behaviour must offer at least one behaviour, but can offer as many as it wants. The select behaviour is simpler, and works similar to the send behaviour. Its syntax is `offeringProces+label` where `label` is the label of one of the offered behaviours. Example:
```
a{ main {if e then b+get; b?; stop else b+set; b!<value>; stop } } | 
b{ main { a&{get: a!<value>; stop, set: a?; stop } }
```
This allows controlling conditional loops among the processes of the network.
```
a{ def X{ if space then b+more; b?; X else b+fin; stop } main { X } } |
b{ def X{ a&{more: a!<data>; X, fin: stop} } main{ X } }
```
Note that the names of procedures does not matter as long as they are unique within their own process. It is also not necessary for the procedure invocations to line up. The algorithm looks solely on what the processes do and how they behave. Not how they are defined.

## Generalized continuations
The conditional, offering, and procedure invocation behaviours are called *branching behaviours* because the behaviour following them are chosen at runtime, and cannot be glanced from their definition. To allow more flexibility with behaviour definitions, these behaviours can optionally also have a continuation. If the branching behaviour enters a branch with a behaviour that does not terminate or loop, it will resume the branching behaviours' continuation once all behaviours in the branch has acted. Thus an example from the previous section can be simplified slightly.
```
a{ main {if e then b+get; b?; else b+set; b!<value>; continue stop } } | 
b{ main { a&{get: a!<value>; , set: a?; }; stop }
```
The reason the conditional's continuation follows after `continue` but the offering's does not is to resolve an ambiguity on where the else branch stops, and the continuation starts. Of course, not all branches needs to resume the continuation if it is there. If the behaviour `stop` is executed, the process terminates no matter what. It is also possible to have nested conditionals `... else a&{fun: X; c?;}; c+do; continue a?; stop`

While this allows for networks to use general recursion, only systems using tail recursion can currently be extracted.

Nested conditionals with fewer continuations than conditionals, can lead to ambiguities which the parser will refuse. For example 
```
if e1 then a+L1; else if e2 then a+L2; else a+L3; continue a!<end>; stop
```
makes it unclear if the `continue` belongs to the outer, or inner conditional. In some cases the ambiguity could be resolved from context, but the parser does not check for those cases. Therefore, you should always add `endif` to indicate the end of your nested conditionals. The above example could be resolved as `endif continue a!<end>; stop` to append the continuation to the outer conditional, or `continue a!<end>; stop endif` to append to the inner conditional. If none of your nested conditionals use continuations, you do not need to insert `endif`. 

## Process spawning and variable process names
Processes can be spawned at runtime. A spawned process retains its parent's procedure definitions, but otherwise acts like any other process. the syntax is `spawn childName with child_behaviour continue parent_behaviour` where `childName` is the name of the spawned process. The child will assume the `child_behaviour` as its main behaviour, while the parent continues by executing `parent_behaviour`.
```
a{ def fin{ a?; stop } main{ spawn child with a!<hello>; fin continue child?; child!<goodbye>; stop } } 
```
This creates one issue though. If two processes are spawned with the same name, which one does its process name refer to? And how does a process know which processes are available? To solve this, processes store process names as *variables*. A variable is initialized to be assigned its own name, so by default `a!<m>` does really send a message to a process names `a`. If a process spawns a child, the name it gives the child is a variable that is bound to the actual name of the process. The parent can use this variable as if it was its actual name. However if a process spawns two processes with the same name, the child processes gets unique actual names in the network, but the name the parent assigned points to the latter process. If two different processes spawns children and assigns them the same name, the variable still points to the right processes, because variables are per-process.

The next problem is letting processes communicate with children that are not theirs (and the other way around). Luckily in real systems, every process does not magically know if a process is spawned or not, so the act of *introducing* processes is fairly intuitive. Introduction is done by two kinds of behaviours. Introduce, and introductee. Introductee is simple. it receives a message from a process it already can communicate with, with the actual name of a process, which it then binds to a variable. The syntax is `introducer?processVariable` where `introducer` is the process it already knows, and `processVariable` is the name of the variable the other process gets assigned. The process can from then on communicate like normal: `processVariable!<Hello>`. The co-behaviour introduce is also fairly simple. Its syntax is `otherProcess1<->otherProcess2`. Unlike the previous kinds of communication, this is a three-way interaction. The process with the introduce behaviour already know `otherProcess1` and `otherProcess2`, but those processes may not know each other. The two other processes have a corresponding introductee behaviour, where they assign each other to a variable.
```
a{ def X{ spawn worker with a?master; master!<result>; stop continue worker<->b; X } main{ X } } |
b{ def X{ a?slave; slave?; X } main{ X } }
```
When a process spawns a child, the child inherits all variable assignments from its parent, including itself. This may seem redundant, But otherwise the child could not tell other processes how to interacti with it. For example `spawn c with spawn d with c?; stop continue d!<hi>;` or `spawn child with X(child) continue ...`. 

Another area where variable process names are used are *parameterized procedures*. This allows procedure definitions to specify any amount of parameter variables. Likewise, a procedure invocation can specify parameter values, which gets bound to the parameter variables upon invocation. The syntax for both is optional, and simply a comma separated list within regular parentheses, ex `def X(a,b,c){ p?d; X(d,a,b) }`
```
a{ def osci(p){ p?; p!<resp>; osci(p) } main { spawn b with osci(a) continue b!<start>; osci(b) } }
```

A final note about variables. Variables can be assigned the value of other variables. For example `a?b; X(b)` would not pass `b` as a parameter value to X, but rather the value `b` is assigned to. 

## Valid names and expressions, reserved keywords, and other syntax information
Valid process and procedure names can consist of any combination of upper/lowercase letters and digits, as long as there is at least one character, and it is not a reserved keyword. 

Expressions, which is the messages sent by the send behaviour, offering labels, and conditional expressions, have the same requirements as process and procedure names.

Spaces, tabs, and newlines, are always ignored.

If you wish for a behaviour to resume to a previous branching behaviour's continuation, you must write it as if there where a continuation to that behaviour. For example `a?; b?` is syntactically wrong, but `a?; b?;` will resume a continuation. If it is contained within a branching behaviour that optionally has a continuation, but doesn't, it will return to that behaviour's parent's continuation instead if it exists. For example `... else a&{end: stop, cont, a!<ok>;} continue a+res;...` is valid, and returns to the conditional's continuation, even though the offering behaviour does not define a continuation.

The reserved keywords are:
`if`,`then`,`else`,`continue`,`endif`,`stop`,`spawn`,`with`,`def`,`main`,
