# Distributed Java Workflow
## Execute java files organized in a acyclic graph distributively on AWS

- - - -

## Workflow Framework

* WorkflowFramework.java holds the code for the generalized distributed workflow framework.
* Executes java code which take in some input file and outputs some file 
* Executes all on AWS ec2 machines.
* You must specify all of your tasks in the tasks.txt file.
    * Must specify the task name
    * Path to java file from the classes directory
    * Name of the machine to execute the task on
    * All of the inputs the task needs to execute (Intermediate tasks should have the parent task's output files as input).
    * The outputs that the task produces.
* You must specify the data flow or the edges in the edges.txt file.
    * Each line is a new edge, tasks that are connected should be separated by a space.
* All of you java files for the tasks must contain the package com.web
* You must place your input files for our entry task in the upload/input_files directory.
* The final output from the exit task will be found in the upload/txt_files directory.

- - - -

### Executing the Code

* Please enter your AWS credentials on your computer.
* Enter your machine aws info in the config.txt file.
* Add all you task information to tasks.txt in the example format.
* Add all of the edges of your workflow graph to the edges.txt file.
* Traverse to the class directory
* Compile with javac -d "../class/" -cp "../jar/\*" ../src/com/web/\*.java
* Execute with java -cp .:../jar/\* com.web.MovieFinder (java -cp .;../jar/\* com.web.MovieFinder on windows)
