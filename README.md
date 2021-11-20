# CSC365-lab7

**Team:** <br />
Michael Long (mjlong@calpoly.edu) <br />
Henry Pigg (hpigg@calpoly.edu) <br />

Database worked out of: mjlong

###Compile/Runtime instructions: <br />
_From the console with JDBS driver jar in same dir.: _ <br />
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:. <br />
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/mjlong?autoReconnect=true\&useSSL=false <br />
export HP_JDBC_USER=mjlong <br />
export HP_JDBC_PW=csc365-F2021_013777227 <br />
javac *.java <br />
java InnReservations <br />

All runtime instructions are given within CLI.

###Known bugs:
No known bugs

###Contributions:
####hpigg:
* FR2
* FR3
    * git log shows mjlong commited the FR3 merge. The merge had a rediculous amount of complicated 
    conflicts, so I pushed the changes to a new branch and mjlong merged in the changes.
* CLI
* Transaction Control
####mjlong:
* FR1
* FR4
* FR5
* FR6
