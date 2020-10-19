# connect-cities

This application determines if there is a path between two cities. It reads a file which consists of the city pairs, 
showing there is path between origin and destination cities and prints the response "yes" or "no" meant to inform the user if there is a path between the two cities. 

The application is REST based API, which can be invoked by passing origin and destination values (city pair), which returns result showing if two cities are connected. 
The possible path can also be indirect, meaning if city A connects to city B, and B to city C, the application would determine that there is a path between city A and city C. 

The file which contains city paris is cities.txt, located in /resources directory. 

To compile the application please execute:

mvn clean install

To run the application please execute:

mvn spring-boot:run

To run all tests please execute:

mvn test

Some sample requests are:

http://localhost:8080/connected?origin=Boston&destination=Newark 

http://localhost:8080/connected?origin=Boston&destination=Philadelphia

http://localhost:8080/connected?origin=Philadelphia&destination=Albany
