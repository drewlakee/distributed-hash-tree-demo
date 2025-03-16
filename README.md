# distributed-hash-tree-demo

An example project to observe a possible way of a distributed in-memory cache implementation 
with an invalidation protocol and delta fetching from a data-set over the network.

Initial concept for implementation was Merkle Tree with binary hash-tree, 
but eventually there's been done an experimentation with non-binary tree 

<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/9/95/Hash_Tree.svg/1024px-Hash_Tree.svg.png">

Implementation consists three processes:

1) Worker.kt processes a data-set, creates an instant snapshot with positioned delta over time
2) Api.kt on each request handles client's hash tree differences with the instant snapshot 
and executes invalidation protocol or sends positioned deltas
3) Client.kt holds in-memory data-set invalidating and updating over time with help of Api's coordination  

Benefits:

* Network I/O optimization with data-set's delta partitions
* Lazy modified data-set fetching

## How to observe the protocol

1) Spin up mongodb docker container with a port binding on ```mongodb://localhost:27017```
2) Execute Worker.kt in parallel
3) Execute Api.kt in parallel
4) Execute Client.kt in parallel

Perform data manipulations with modify/add operations over ```careers``` collection and it's documents,
observe Client.kt logging results