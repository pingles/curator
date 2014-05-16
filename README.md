Curator
=======

Clojure library to interface with [Apache Curator](http://curator.apache.org/): "a keeper of a museum or other collection - A [ZooKeeper](http://zookeeper.apache.org) Keeper".

![latest curator version](https://clojars.org/curator/latest-version.svg)

# Service Discovery

```clojure
(ns myservice
  (:require [curator.framework :refer (curator-framework)]
            [curator.discovery :refer (service-discovery service-instance start instances services)]))

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
(start client)

;; next we create our service discovery, which uses the client
;; in the background to register our service instance
(def discovery (service-discovery client instance :base-path "/foo"))
(start discovery)

;; now we can see which services are registered
(services sd)
;; ["some-service"]

;; and their registered instances
(instances discovery service-name)
;; [#<ServiceInstance ServiceInstance{name='service-name', id='d859d052-0df0-40aa-925e-358154953a19', address='192.168.1.241', port=1234, sslPort=null, payload=testing 123, registrationTimeUTC=1400195776978, serviceType=DYNAMIC, uriSpec=org.apache.curator.x.discovery.UriSpec@6c2ac0dc}>]

;; we can also use the service-provider to help provide
;; access to an instance using different strategies: random, round-robin and sticky
(def p (service-provider sd "some-service" :strategy (round-robin-strategy)))

(instance p)
;; #<ServiceInstance ServiceInstance{name='service-name', id='d859d052-0df0-40aa-925e-358154953a19', address='192.168.1.241', port=1234, sslPort=null, payload=testing 123, registrationTimeUTC=1400195776978, serviceType=DYNAMIC, uriSpec=org.apache.curator.x.discovery.UriSpec@6c2ac0dc}>


```
