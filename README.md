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

## Leadership Election
Leadership elections can be useful when you need to have multiple instances of a service/process and one of the designated as the leader/master/coordinator etc.

Before some task is started a leader election will be performed- participating network nodes are unaware of which node will be leader but a single node will receive a callback to say they have been elected leader. After the election all other nodes recognise a single node as the leader.

```clojure
(ns myservice
  (:require [curator.framework :refer (curator-framework)]
            [curator.leader :refer (leader-selector)]))

;; first, create the curator client
(def c (curator-framework "localhost:2181"))
(.start c)

;; we create our leader selector, providing the base path for the
;; leadership group and a function that will be called when we become
;; the leader.
;; this function needs to block whilst it acts as leader; once it returns
;; another election is performed and (potentially) a new leader elected.
(def selector (let [zk-path "/pingles/curator/myservice-leader"]
                (leader-selector c zk-path (fn [curator-framework participant-id]
                                             (println "Became leader:" participant-id)
                                             (Thread/sleep (* 20 1000))))))

;; we can start the selector, every 20s it'll print that we became leader
(.start selector)
;; Became leader 7c6e684d-e9fa-4786-9e48-4cb37a57894d
;; Became leader 7c6e684d-e9fa-4786-9e48-4cb37a57894d
;; Became leader 7c6e684d-e9fa-4786-9e48-4cb37a57894d

;; we can stop this by closing the leader-selector
(.close selector)

;; we can see who else participates in the leadership election
(participants selector)
;; [#<Participant Participant{id='aebaaff9-4aa8-4bbd-a789-0515c1a43b5f', isLeader=true}> #<Participant Participant{id='7c6e684d-e9fa-4786-9e48-4cb37a57894d', isLeader=false}>]
  
;; if, for some reason, whilst we're leader we want to give up
;; leadership we can (although we'd still have to stop doing what
;; we were doing as leader). this would trigger a re-election of
;; a different node.
(interrupt-leadership selector)

;; now we can look at the participants again and see that the other
;; instance has become the leader
(participants selector)
;; [#<Participant Participant{id='7c6e684d-e9fa-4786-9e48-4cb37a57894d', isLeader=true}> #<Participant Participant{id='aebaaff9-4aa8-4bbd-a789-0515c1a43b5f', isLeader=false}>]
```

When using a leadership selector it's worth noting the behaviour in the event of a partition with the ZooKeeper quorum.

In the event that our connection becomes suspended or lost we should cease to expect that we're the leader. The remaining nodes, however, won't recognise the absence of the leader up until `curator.framework/curator-framework`'s session timeout, as specified with the `:session-timeout-millis` option. By default this is set to 40 seconds, so we'll wait 40 seconds for the leader to retain its session. Once this timeout expires the remaining nodes will hold another election.

Having long session timeouts ensures we don't cycle very quickly in the event of intermittent connectivity problems, but comes at the expense of how quickly we can elect a new leader.