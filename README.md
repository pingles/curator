Curator
=======

Clojure library to interface with [Apache Curator](http://curator.apache.org/): "a keeper of a museum or other collection - A [ZooKeeper](http://zookeeper.apache.org) Keeper".

![latest curator version](https://clojars.org/curator/latest-version.svg)

Curator provides some ZooKeeper-implemented recipes that are extremely useful when building distributed systems.

* Service Discovery. This helps services find each other at runtime, services can register and unregister as they become available. 
* Leader Election. This enables a collection of processes to elect a single process as the leader, typically for one process to organise/coordinate the others.

At a lower-level Apache Curator also provides implementations of:

* Distributed Locks, Semaphores and Barriers.
* Distributed Counters

This library aims to make it more pleasant to work with Apache Curator in a Clojure way.

# Examples
## Service Discovery

```clojure
(ns myservice
  (:require [curator.framework :refer (curator-framework)]
            [curator.discovery :refer (service-discovery service-instance service-provider instance instances services)]))

;; services (and their instances) are registered by name
(def service-name "some-service")

;; create an instance of our service. we don't specify address
;; so it'll use a public address from our network interfaces.
;; payloads must be strings for now
(def instance (service-instance service-name
                                "{scheme}://{address}:{port}"
                                1234
                                :payload "testing 123"))
                                
;; now we create the curator-framework client, connecting to
;; our local quorum                                
(def client (curator-framework "localhost:2181"))
(.start client)

;; next we create our service discovery, which uses the client
;; in the background to register our service instance
(def discovery (service-discovery client instance :base-path "/foo"))
(.start discovery)

;; now we can see which services are registered
(services discovery)
;; ["some-service"]

;; and their registered instances
(instances discovery service-name)
;; [#<ServiceInstance ServiceInstance{name='some-service, id='d859d052-0df0-40aa-925e-358154953a19', address='192.168.1.241', port=1234, sslPort=null, payload=testing 123, registrationTimeUTC=1400195776978, serviceType=DYNAMIC, uriSpec=org.apache.curator.x.discovery.UriSpec@6c2ac0dc}>]

(def select-strategy (round-robin-strategy))

;; we can also use the service-provider to help provide
;; access to an instance using different strategies: random, round-robin and sticky
(def p (service-provider discovery "some-service" :strategy select-strategy))
(.start p)

(instance p)
;; #<ServiceInstance ServiceInstance{name='service-name', id='d859d052-0df0-40aa-925e-358154953a19', address='192.168.1.241', port=1234, sslPort=null, payload=testing 123, registrationTimeUTC=1400195776978, serviceType=DYNAMIC, uriSpec=org.apache.curator.x.discovery.UriSpec@6c2ac0dc}>

;; Apache Curator also provides a service cache that can be used to avoid
;; retrieving details from ZooKeeper each time. Instead, it'll watch for updates
;; and keep data in-process
(def sc (service-cache discovery "some-service"))
(.start sc)

;; we can use instances to retrieve all instances from the cache
(instances sc)

;; or we can use a strategy to determine how we pick a single instance
(instance sc select-strategy)

;; we should close everything we've started when we're done
(.close p)
(.close sc)
(.close discovery)
(.close client)
```
