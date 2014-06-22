PIPEAnalysis
============

A set of algorithms for the analysis of Petri nets found in the [PIPECore](https://github.com/sarahtattersall/PIPECore) project.

These libraries contain:
* Sequential and multi-core state space exploration algorithms. When testing with 8 virtual cores on a 3.2GHz i7 processor, 
  these algorithms successfully explored a Petri net with over a million states in under 6 minutes.
* Sequential and multi-core steady state solvers.
* Performance analysis metrics calculations for determining the average number of tokens 
  in each place and the average transition throughput.


## Maven integration
To use this library in Maven projects add this GitHub project as an external repository:

```
<repositories>
    <repository>
        <id>PIPEAnalysis-mvn-repo</id>
        <url>https://raw.github.com/sarahtattersall/PIPEAnalysis/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

Then either include the SNAPSHOT or latest release version in your dependencies:
```
<dependencies>
    <dependency>
        <groupId>uk.ac.imperial</groupId>
        <artifactId>pipe-analysis</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```
