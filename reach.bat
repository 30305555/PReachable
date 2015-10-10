@echo off

cd C:\Users\Takeshi\workspace\Z_PingReachable
pause
java -version
rem java -Xms1024m -server -cp Reachable.jar sue.reach.ReachabilityTest yahoo.co.jp 1
java -Xmx256m -server -cp Reachable.jar -Dcom.sun.management.jmxremote=true sue.reach.TestReach 1
pause
