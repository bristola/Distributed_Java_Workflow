# CMPSC441 Final Project
## Team 1

- - - -

## Workflow Framework

* Framework.java holds the code for the generalized distributed workflow framework.
* Executes distributed on a aws ec2 machines.
* You must specify all of your tasks in the tasks.txt file.
    * Must specify the task name
    * Path to java file from the classes directory
    * Name of the machine to execute the task on
    * All of the inputs the task needs to execute.
    * The outputs that the task produces.
* You must specify the data flow or the edges in the edges.txt file.
    * Each line is a new edge, tasks that are connected should be separated by a space.
* All of you java files for the tasks must contain the package com.web
* The code will take the tasks, make a graph, and assign them to correct machines.
* All of a machine's tasks are executed on their own thread, so multiple machines can be working at the same time.
* All tasks wait for the inputs to be present in the input_file directory in upload.
* You must place your input files for our entry task in the upload/input_files directory.
* The final output from the exit task will be found in the upload/txt_files directory.

- - - -

## Distributed Web Scraper

* The code in this framework is already loaded up with the distributed web scraper.
* All necessary config files are present with the correct tasks and data.
* All java tasks are there.
* This shows how the framework works.
* The tasks hold the computations that must be completed, including actually scraping the web sites in tasks 1, 3, 4, 5, 6.
* Tasks 2, 7, 8, 9 deal with MySQL database.

### Executing the Code

* Please enter your aws credentials on your computer.
* Enter your machine aws info in the config.txt file.
* Traverse to the class directory
* Compile with javac -d "../class/" -cp "../jar/\*" ../src/com/web/\*.java
* Execute with java -cp .:../jar/\* com.web.MovieFinder (java -cp .;../jar/\* com.web.MovieFinder on windows)
