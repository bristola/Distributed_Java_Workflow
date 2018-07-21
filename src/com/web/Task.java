package com.web;

import java.util.List;
import java.util.Arrays;

public class Task {

    private String name;
    private String javaPath;
    private Machine machine;
    private List<String> inputs;
    private List<String> outputs;
    private List<String> dependencies;
    private boolean complete = false;
    private boolean waiting = false;

    public Task(String name, String javaPath, Machine machine, List<String> inputs, List<String> outputs, List<String> dependencies) {
        this.name = name;
        this.javaPath = javaPath;
        this.machine = machine;
        this.inputs = inputs;
        this.outputs = outputs;
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public Machine getMachine() {
        return machine;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public boolean getWaiting() {
        return waiting;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

}
