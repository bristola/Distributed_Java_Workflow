package com.web;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.TopologicalOrderIterator;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class WorkflowFramework {

    private AWSUtilities aws;

    public WorkflowFramework() {
        aws = new AWSUtilities();
    }

    public void run() {
        List<Task> tasks;
        try {
            tasks = readTasks();
        } catch (Exception e) {
            System.out.println("Some error occured!");
            return;
        }

        List<Machine> machines = getMachines(tasks);
        int numberOfMachines = machines.size();

        System.out.println("Creating machines...");
        aws.getConfigContents();
        List<String> ids = null;
        try {
            ids = aws.provisionMachines(numberOfMachines);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        fillMachines(machines, ids);

        List<String> ips = new ArrayList<String>();
        for (Machine machine : machines) {
            ips.add(machine.getIP());
        }

        System.out.println("Setting up machines...");
        aws.setupMachines(ips, aws.getPathToPem());
        transferDependencies(tasks);

        SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> taskMapping;
        try {
            taskMapping = createGraph(tasks);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Task exitTask = getExitTask(tasks, taskMapping);

        System.out.println("Executing tasks...");
        try {
            executeTasks(tasks, machines, taskMapping, exitTask);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Terminating instances...");
        aws.terminateInstances(ids);

    }

    public void transferDependencies(List<Task> tasks) {

        for (Task task : tasks) {
            for (String dep : task.getDependencies()) {
                aws.transferFile(task.getMachine().getIP(), aws.getPathToPem(), "ubuntu", "../jar/"+dep, "jar/");
            }
        }

    }

    public Thread createMachineThread(Machine machine, List<Task> tasks) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (Task task : tasks) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                    // Wait for inputs to be available
                    for (String input : task.getInputs()) {
                        File file = new File("../upload/input_files/"+input);
                        while (!file.exists()) {
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    System.out.println("Executing "+task.getName()+" on machine "+machine.getName()+" at "+dtf.format(LocalDateTime.now()));

                    // Transfer code and input files
                    aws.transferFile(machine.getIP(), aws.getPathToPem(), "ubuntu", task.getJavaPath(), "src/com/web/");
                    for (String input : task.getInputs()) {
                        aws.transferFile(machine.getIP(), aws.getPathToPem(), "ubuntu", "../upload/input_files/"+input, "upload/input_files/");
                    }

                    // Run code
                    String[] commands = new String[2];
                    String[] s = task.getJavaPath().split("/");
                    String className = s[s.length-1].replaceAll(".java","");
                    commands[0] = "cd class && javac -d '../class/' -cp '../jar/*' ../src/com/web/*.java";
                    commands[1] = "cd class && java -cp .:../jar/* com.web."+className;
                    aws.executeCommands(machine.getIP(), aws.getPathToPem(), "ubuntu", commands);

                    // Get output files
                    for (String output : task.getOutputs()) {
                        aws.getFile(machine.getIP(), aws.getPathToPem(), "ubuntu", "./upload/txt_files/"+output, "../upload/input_files/"+output);
                    }

                }
            }
        });
        t.start();
        return t;
    }

    public void executeTasks(List<Task> tasks, List<Machine> machines, SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> taskMapping, Task exitTask) throws InterruptedException {

        TopologicalOrderIterator<String, DefaultWeightedEdge> iterator = new TopologicalOrderIterator<String, DefaultWeightedEdge>(taskMapping);
        List<Task> topologicalOrder = new ArrayList<Task>();
        while (iterator.hasNext()) {
            String cur = iterator.next();
            for (Task task : tasks) {
                if (cur.equals(task.getName())) {
                    topologicalOrder.add(task);
                    break;
                }
            }
        }

        List<Thread> threads = new ArrayList<Thread>();

        for (Machine machine : machines) {
            List<Task> machineTasks = new ArrayList<Task>();
            for (Task task : topologicalOrder) {
                if (task.getMachine().equals(machine)) {
                    machineTasks.add(task);
                }
            }
            Thread thread = createMachineThread(machine, machineTasks);
            threads.add(thread);
        }

        for (Thread t : threads) {
            t.join();
        }

        moveAndRemoveFiles(tasks, exitTask);

    }

    public void moveAndRemoveFiles(List<Task> tasks, Task exitTask) {
        for (Task task : tasks) {
            if (task.equals(exitTask)) {
                for (String output : task.getOutputs()) {
                    File file = new File("../upload/input_files/"+output);
                    file.renameTo(new File("../upload/txt_files/"+output));
                }
            } else {
                for (String output : task.getOutputs()) {
                    try {
                        File file = new File("../upload/input_files/"+output);
                        file.delete();
                    } catch (Exception e) {
                        System.out.println("Couldn't delete "+output);
                    }
                }
            }
        }
    }

    public Task getEntryTask(List<Task> tasks, SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> taskMapping) {
        for (Task task : tasks) {
            List<String> predecessors = Graphs.predecessorListOf(taskMapping, task.getName());
            if (predecessors.size() == 0) {
                return task;
            }
        }
        return null;
    }

    public Task getExitTask(List<Task> tasks, SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> taskMapping) {
        for (Task task : tasks) {
            List<String> successors = Graphs.successorListOf(taskMapping, task.getName());
            if (successors.size() == 0) {
                return task;
            }
        }
        return null;
    }

    public SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> createGraph(List<Task> tasks) throws IOException {

        SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> graph =
            new SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);

        for (Task task : tasks) {
            graph.addVertex(task.getName());
        }

        File file = new File("../edges.txt");
        Scanner scan = new Scanner(file);
        while (scan.hasNext()) {
            String[] line = scan.nextLine().split(" ");
            graph.addEdge(line[0], line[1]);
        }

        return graph;

    }

    public void fillMachines(List<Machine> machines, List<String> ids) {

        if (machines.size() != ids.size()) {
            System.out.println("Uneven size of machines and ids!");
            return;
        }

        for (int i = 0; i < machines.size(); i++) {
            Machine machine = machines.get(i);
            String id = ids.get(i);
            String ip = aws.getIpFromId(id);
            machine.setID(id);
            machine.setIP(ip);
        }
    }

    public List<Machine> getMachines(List<Task> tasks) {

        List<Machine> machines = new ArrayList<Machine>();

        for (Task task : tasks) {
            Machine cur = task.getMachine();
            if (!machines.contains(cur)) {
                machines.add(cur);
            } else {
                task.setMachine(machines.get(machines.indexOf(cur)));
            }
        }

        return machines;

    }

    public List<Task> readTasks() throws IOException {
        File file = new File("../tasks.txt");
        Scanner scan = new Scanner(file);
        List<Task> tasks = new ArrayList<Task>();
        while(scan.hasNext()) {
            String name = "";
            String javaFile = "";
            Machine machine = null;
            List<String> inputs = new ArrayList<String>();
            List<String> outputs = new ArrayList<String>();
            List<String> dependencies = new ArrayList<String>();
            while(scan.hasNext()) {
                String line = scan.nextLine();
                if (line.equals(""))
                    break;
                String varType = line.split("=")[0].trim();
                String value = line.split("=")[1].trim();
                switch(varType) {
                    case "name":
                        name = value;
                        break;
                    case "machine":
                        machine = new Machine(value);
                        break;
                    case "javaFilePath":
                        javaFile = value;
                        break;
                    case "input":
                        inputs.add(value);
                        break;
                    case "output":
                        outputs.add(value);
                        break;
                    case "dependency":
                        dependencies.add(value);
                        break;
                    default:
                        System.out.println("Invalid variable in tasks.txt");
                        break;
                }
            }
            if (!name.equals("")) {
                Task task = new Task(name, javaFile, machine, inputs, outputs, dependencies);
                tasks.add(task);
            }
        }
        scan.close();
        return tasks;
    }

}
