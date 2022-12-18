# concurrency-comparison-java-golang
In this project, a detailed comparison has been done between JAVA and GOLAND on multiple parameters where their are performances has been accessed.


## Experiments Performed
### Matrix Multiplication Experiment Results
In the matrix multiplication experiment, the execution time for both Java and
Go were almost on the same lines for each increment of thread.The compile time
for Java and Go were very much different. Go merely took a few milliseconds
to compile the complete code but Java took 7 seconds to build the code. But
the size of the GO binary file is 2.5 MB and Java jar file 68KB. For memory
bounded systems, Java must be used where we need to be careful with memory
allocation or usage.Below is the image of the execution time table and graph :

### Hashmap Cache Experiment Results
#### sync.Map(GO) VS ConcurrentHashMap(Java)
Within GO, the sync.Map performs far worse than the map using locks. Within
Java, the concurrentHashMap performs better than the map using locks. Between
GO and Java, locks perform better in GO than JAVA whereas concurrentHashMap
performs better in Java than GO. The reason for the above conclusions is concurrentHashMap allows performing concurrent read and write which
is not the case in GO. 
#### sync.Map(GO) VS ConcurrentHashMap(Java)
When comparing GO and JAVA, GO’s sync.Map for GET and MultiGET operations
is extremely slower than JAVA.JAVA wins comprehensively here as Java
does a lock-free volatile implementation as compared to GO’s sync.map. B

#### intMap(GO) VS intMap(Java)
Java is better than GO in almost cases of INTMAP because GO uses arrays of
structs while JAVA uses link list of nodes.