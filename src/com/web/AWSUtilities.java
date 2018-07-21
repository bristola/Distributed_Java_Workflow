package com.web;

import java.util.*;
import java.io.*;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;

/*
You must have an AWS credentials file located at ~/.aws/credentials
In class directory:
javac -d "../class/" -cp "../jar/*" ../src/com/web/*.java
java -cp .:../jar/* com.web.AWSUtilities
(On windows: java -cp .;../jar/* com.web.AWSUtilities)
*/

public class AWSUtilities {

    private AmazonEC2 ec2;

    private static String pemName;
    private static String machineType;
    private static String securityGroup;
    private static String ami;
    private static String pathToPem;
    private static String dropBoxKey;

    public AWSUtilities() {
        this(Regions.US_WEST_2); // Default server is US west 2
    }

    public AWSUtilities(Regions region) {
        try {
            AWSCredentials creds = new ProfileCredentialsProvider().getCredentials();
            AWSStaticCredentialsProvider credsProvider = new AWSStaticCredentialsProvider(creds);
            ec2 = AmazonEC2ClientBuilder.standard()
                        .withCredentials(credsProvider)
                        .withRegion(region)
                        .build();
        } catch (Exception e) {
            System.out.println("Something went wrong!");
            e.printStackTrace();
        }
    }

    public void getConfigContents() {
        try {
            File config = new File("../config.txt");
            Scanner scan = new Scanner(config);
            while (scan.hasNext()) {
                String line = scan.nextLine();
                String varType = line.split("=")[0].trim();
                String value = line.split("=")[1].trim();
                switch(varType) {
                    case "key name":
                        pemName = value;
                        break;
                    case "machine type":
                        machineType = value;
                        break;
                    case "security group":
                        securityGroup = value;
                        break;
                    case "image-id":
                        ami = value;
                        break;
                    case "absolute path to .pem file":
                        pathToPem = value;
                        break;
                    case "drop-box token":
                        dropBoxKey = value;
                        break;
                    default:
                        System.out.println("Invalid variable in config.txt");
                        break;
                }
            }
            scan.close();
        } catch (Exception e) {
            System.out.println("Could not read config file!");
        }
    }

    public List<String> provisionMachines(int num) throws InterruptedException {
        List<String> ips = new ArrayList<String>();

        RunInstancesRequest req = new RunInstancesRequest()
                                        .withInstanceType(machineType)
                                        .withMinCount(num)
                                        .withMaxCount(num)
                                        .withKeyName(pemName)
                                        .withImageId(ami)
                                        .withSecurityGroupIds(securityGroup);

        RunInstancesResult result = ec2.runInstances(req);

        System.out.println("Waiting 60 seconds for instances to start running...");

        Thread.sleep(60000);

        List<Instance> instances = result.getReservation().getInstances();
        for (Instance i : instances) {
            ips.add(i.getInstanceId());
        }

        return ips;
    }

    public List<String> provisionMachines(int num, String pemFile, String type, String security, String imageID) throws InterruptedException {
        List<String> ips = new ArrayList<String>();

        RunInstancesRequest req = new RunInstancesRequest()
                                        .withInstanceType(type)
                                        .withMinCount(num)
                                        .withMaxCount(num)
                                        .withKeyName(pemFile)
                                        .withImageId(imageID)
                                        .withSecurityGroupIds(security);

        RunInstancesResult result = ec2.runInstances(req);

        System.out.println("Waiting 60 seconds for instances to start running...");

        Thread.sleep(60000);

        List<Instance> instances = result.getReservation().getInstances();
        for (Instance i : instances) {
            ips.add(i.getInstanceId());
        }

        return ips;
    }

    public void terminateInstances(List<String> ids) {
        TerminateInstancesRequest tir = new TerminateInstancesRequest(ids);
        ec2.terminateInstances(tir);
    }

    public String getIpFromId(String id) {
        DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(id);
        DescribeInstancesResult dir = ec2.describeInstances(request);
        List<Reservation> reservations = dir.getReservations();
        for (Reservation reservation : reservations) {
            for (Instance instance : reservation.getInstances()) {
                return instance.getPublicIpAddress();
            }
        }
        return null;
    }

    public List<String> getIpFromIds(List<String> ids) {
        List<String> ip = new ArrayList<String>();
        DescribeInstancesResult dir = ec2.describeInstances();
        List<Reservation> reservations = dir.getReservations();
        for (Reservation reservation : reservations) {
            for (Instance instance : reservation.getInstances()) {
                if (ids.contains(instance.getInstanceId())) {
                    ip.add(instance.getPublicIpAddress());
                }
            }
        }
        return ip;
    }

    public void transferWebScraper(List<String> ips, String pemLocation) {
        List<Thread> threads = new ArrayList<Thread>();
        for (String ip : ips) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    transferFile(ip, pemLocation, "ubuntu", "../src/com/web/DistributedWebScraperV1.java", "src/com/web/");
                    transferFile(ip, pemLocation, "ubuntu", "../jar/jsoup-1.7.2.jar", "jar/");
                    transferFile(ip, pemLocation, "ubuntu", "../src/com/web/SQLConnector.java","src/com/web/");
                    transferFile(ip, pemLocation, "ubuntu", "../jar/mysql-connector-java-5.1.25-bin.jar", "jar/");
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void runCode(List<String> ips, String pemLocation) {
        String[] classes = {
            "class01",
            "class02",
            "class03",
            "class04"
        };

        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < ips.size(); i++) {
            threads.add(createClassThread(ips.get(i), classes[i], pemLocation));
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public Thread createClassThread(String ip, String classType, String pemLocation) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                String[] commands = new String[4];
                commands[0] = "cd class";
                commands[1] = "cd class && javac -d '../class/' -cp '../jar/*' ../src/com/web/*.java";
                commands[2] = "cd class && java -cp .:../jar/* com.web.DistributedWebScraperV1 everything " + classType;
                commands[3] = "cd class && java -cp .:../jar/* com.web.SQLConnector";
                executeCommands(ip, pemLocation, "ubuntu", commands);
            }
        });
        t.start();
        return t;
    }

    public void setupMachines(List<String> ips, String pemLocation) {
        String[] commands = {
            "sudo apt-get update",
            "export DEBIAN_FRONTEND=noninteractive",
            "sudo apt-get -y install default-jre",
            "sudo apt-get -y install default-jdk",
            "sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password password admin'",
            "sudo debconf-set-selections <<< 'mysql-server mysql-server/root_password_again password admin'",
            "sudo apt-get -y install mysql-server",
            "sudo sed -i -- 's/127.0.0.1/0.0.0.0/g' /etc/mysql/my.cnf",
            "sudo service mysql restart",
            "touch script.txt",
            "echo \"GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'admin' WITH GRANT OPTION;\" >> script.txt",
            "echo \"FLUSH PRIVILEGES;\" >> script.txt",
            "mysql < script.txt -u root -padmin",
            "mkdir -p class/com/web",
            "mkdir -p src/com/web",
            "mkdir jar",
            "mkdir upload",
            "mkdir -p upload/csv_files",
            "mkdir -p upload/txt_files",
            "mkdir -p upload/htmls",
            "mkdir -p upload/input_files"
        };
        List<Thread> threads = new ArrayList<Thread>();
        for (String ip : ips) {
            Thread t = new Thread (new Runnable() {
                public void run() {
                    executeCommands(ip, pemLocation, "ubuntu", commands);
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void transferFile(String ip, String pemLocation, String username, String fileToTransfer, String dest) {
        try {
            Session s = getSession(ip, pemLocation, username);

            ChannelSftp c = (ChannelSftp)s.openChannel("sftp");
            c.connect();

            File f = new File(fileToTransfer);

            c.put(new FileInputStream(f), dest+f.getName());

            c.disconnect();
            s.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getFiles(List<String> ips, String pemLocation) {
        String[] files = {
            "bcr-class01.txt",
            "bcr-class02.txt",
            "bcr-class03.txt",
            "bcr-class04.txt"
        };

        List<String> fileNames = new ArrayList<String>();

        for (int i = 0; i < ips.size(); i++) {
            String fileName = "../upload/"+files[i];
            getFile(ips.get(i), pemLocation, "ubuntu", "./upload/txt_files/"+files[i], fileName);
            fileNames.add(fileName);
        }

        return fileNames;

    }

    public List<String> getFiles2(List<String> ips, String pemLocation) {
        String[] files = {
            "class01-textData.txt",
            "class02-textData.txt",
            "class03-textData.txt",
            "class04-textData.txt"
        };

        List<String> fileNames = new ArrayList<String>();

        for (int i = 0; i < ips.size(); i++) {
            String fileName = "../upload/"+files[i];
            getFile(ips.get(i), pemLocation, "ubuntu", "./upload/txt_files/"+files[i], fileName);
            fileNames.add(fileName);
        }

        return fileNames;

    }

    public void getFile(String ip, String pemLocation, String username, String fileToGet, String dest) {
        try {
            Session s = getSession(ip, pemLocation, username);

            OutputStream fos = new FileOutputStream(dest);

            ChannelSftp c = (ChannelSftp)s.openChannel("sftp");

            c.connect();

            c.get(fileToGet, fos);

            fos.close();

            c.disconnect();
            s.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Session getSession(String ip, String pemLocation, String username) throws JSchException {
        JSch j = new JSch();
        j.addIdentity(pemLocation);

        java.util.Properties p = new java.util.Properties();
        p.put("StrictHostKeyChecking", "no");

        Session s = j.getSession(username, ip, 22);
        s.setConfig(p);
        s.connect();

        return s;
    }

    public void executeCommands(String ip, String pemLocation, String username, String[] commands) {
        try {
            Session s = getSession(ip, pemLocation, username);

            for (String command : commands) {
                ChannelExec c = (ChannelExec)s.openChannel("exec");
                c.setCommand(command);
                c.connect();

                while(!c.isClosed()) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                c.disconnect();
            }

            s.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void combineFiles(List<String> files, String newFileName) {

        try {
            StringBuilder builder = new StringBuilder();

            for (String file : files) {
                File cur = new File(file);
                Scanner scan = new Scanner(cur);
                while (scan.hasNext()) {
                    builder.append(scan.nextLine()+"\n");
                }
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(newFileName));
            bw.write(builder.toString());
            bw.close();

        } catch(Exception e) {
            System.out.println("Couldn't combine files!");
            e.printStackTrace();
        }

    }

    public void uploadToDropBox(String fileName, String saveName, String dropBoxKey) {
        DbxRequestConfig drc = new DbxRequestConfig("CMPSC441/Lab4", "en_US");
        DbxClientV2 client = new DbxClientV2(drc, dropBoxKey);

        try {
            FileInputStream fis = new FileInputStream(fileName);
            client.files().uploadBuilder("/"+saveName).uploadAndFinish(fis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPemName() {
        return pemName;
    }

    public String getMachineType() {
        return machineType;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public String getAMI() {
        return ami;
    }

    public String getPathToPem() {
        return pathToPem;
    }

}
