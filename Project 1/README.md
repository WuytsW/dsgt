# SOAP and REST Solutions
Go to the directory for each service (REST and SOAP) and compile the files with the following command on your machine:
```bash
mvn clean package
```
Then, move the `.jar` files to the Azure VM:
```bash
scp /path/to/file username@a:/path/to/destination
```
*An example:*
```bash
scp /home/kjetteren/Documents/dsgt2-rest/dsgt-rest/target/food-rest-service-0.0.1-SNAPSHOT.jar azureuser@dsgt.eastus.cloudapp.azure.com:~/REST
scp /home/kjetteren/Documents/dsgt2-soap/dsgt-soap/target/SpringSOAP-0.0.1-SNAPSHOT.jar azureuser@dsgt.eastus.cloudapp.azure.com:~/SOAP
```
To run the files on the Azure VM, SSH into the VM and run with the following command:
```bash
java -jar /path/to/file
```
*An example:*
```bash
java -jar ~/REST/food-rest-service-0.0.1-SNAPSHOT.jar
java -jar ~/SOAP/SpringSOAP-0.0.1-SNAPSHOT.jar
```
To test that it is running, run one of the commands using the `.xml` file or run a command stored in `.sh` files. *An example (ran from the folders where the requests are stored):*
```bash
curl -X POST dsgt.eastus.cloudapp.azure.com:8080/rest/meals -H 'Content-type:application/json' -d @new-meal.json -v
curl --header "content-type: text/xml" -d @request.xml http://dsgt.eastus.cloudapp.azure.com@:8080/ws
```
# RMI Solutions
Go to the `dsgt-rmi` directory and compile the files with the following command on your machine:
```bash
ant jar-all
```
Three files are compiled:
- `common.jar` (Contains the shared classes in the `hotel` package, such as `BookingDetail`, `BookingManagerInterface`, and `Room`)
- `server.jar` (Contains the RMI server main class `hotel.BookingManager` and server logic)
- `client.jar` (Contains the RMI client main class `staff.BookingClient` and related client logic)
Move the `.jar` files to the Azure VM:
```bash
scp /path/to/file username@a:/path/to/destination
```
*An example:*
```bash
scp /home/kjetteren/Documents/dsgt/dsgt1-rmi/dsgt-rmi/jar/common.jar azureuser@dsgt.eastus.cloudapp.azure.com:~/RMI
scp /home/kjetteren/Documents/dsgt/dsgt1-rmi/dsgt-rmi/jar/server.jar azureuser@dsgt.eastus.cloudapp.azure.com:~/RMI
scp /home/kjetteren/Documents/dsgt/dsgt1-rmi/dsgt-rmi/jar/client.jar azureuser@dsgt.eastus.cloudapp.azure.com:~/RMI
```
To run the files on the Azure VM, SSH into the VM and run with the following command with the public DNS:
```bash
java -jar /path/to/file dns
```
*An example:*
```bash
java -jar ~/RMI/server.jar dsgt.eastus.cloudapp.azure.com
```
Run the `client.jar` file with the public DNS to test whether it's running. *Note: the client can be run from the VM or local machine. An example:*
```bash
java -jar ~/RMI/clent.jar dsgt.eastus.cloudapp.azure.com
```
