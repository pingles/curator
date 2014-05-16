(ns ^{:doc "Namespace for service discovery"} curator.discovery
    (:require [clojure.edn :as edn])
    (:import [org.apache.curator.x.discovery ServiceDiscovery ServiceDiscoveryBuilder ServiceInstance ServiceType UriSpec ProviderStrategy DownInstancePolicy ServiceProvider]
             [org.apache.curator.x.discovery.details InstanceSerializer JsonInstanceSerializer]
             [org.apache.curator.x.discovery.strategies RandomStrategy RoundRobinStrategy StickyStrategy]
             [java.io ByteArrayInputStream InputStreamReader PushbackReader]
             [java.util.concurrent TimeUnit]))

(defmacro dotonn [x & forms]
  (let [gx (gensym)]
    `(let [~gx ~x]
       ~@(map (fn [f]
                (if (seq? f)
                  `(when ~@(next f)
                     (~(first f) ~gx ~@(next f)))
                  `(~f ~gx)))
              forms)
       ~gx)))

(defn uri-spec*
  "Creates a templated UriSpec from a string format.
   Example: e.g. \"{scheme}://foo.com:{port}\"
   Substitutions can include: scheme, name, id, address,
   port, ssl-port, registration-time-utc, service-type"
  [s]
  (UriSpec. s))

(defn service-instance
  "name: my-service
   uri-spec: \"{scheme}://foo.com:{port}\"
   port: 1234
   payload is serialized using json, only supports strings for now"
  [name uri-spec port & {:keys [id address ssl-port service-type payload]}]
  {:pre [(string? payload)]}
  (let [service-types {:dynamic   ServiceType/DYNAMIC
                       :static    ServiceType/STATIC
                       :permanent ServiceType/PERMANENT}]
    (-> (dotonn (ServiceInstance/builder)
                (.payload payload)
                (.name name)
                (.id id)
                (.address address)
                (.port port)
                (.sslPort ssl-port)
                (.uriSpec (uri-spec* uri-spec))
                (.serviceType (service-types service-type)))
        (.build))))

(defn uri [service-instance]
  (.buildUriSpec service-instance))

(defn json-serializer []
  (JsonInstanceSerializer. String))

(defn service-discovery
  [curator-framework service-instance & {:keys [base-path serializer payload-class]
                                         :or   {base-path     "/foo"
                                                payload-class String
                                                serializer    (json-serializer)}}]
  {:pre [(.startsWith base-path "/")]}
  (-> (dotonn (ServiceDiscoveryBuilder/builder payload-class)
              (.client curator-framework)
              (.basePath base-path)
              (.serializer (json-serializer))
              (.thisInstance service-instance))
      (.build)))

(defn services
  "Returns the names of the services registered."
  [service-discovery]
  (.queryForNames service-discovery))



(defn random-strategy
  []
  (RandomStrategy. ))

(defn round-robin-strategy
  []
  (RoundRobinStrategy. ))

(defn sticky-strategy
  [^ProviderStrategy strategy]
  (StickyStrategy. strategy))

(def time-units {:hours        TimeUnit/HOURS
                 :milliseconds TimeUnit/MILLISECONDS
                 :seconds      TimeUnit/SECONDS
                 :minutes      TimeUnit/MINUTES
                 :days         TimeUnit/DAYS
                 :microseconds TimeUnit/MICROSECONDS
                 :nanoseconds  TimeUnit/NANOSECONDS})

(defn down-instance-policy
  ([] (down-instance-policy 30 :seconds 2))
  ([timeout timeout-unit error-threshold]
     {:pre [(some time-units [timeout-unit])]}
     (DownInstancePolicy. timeout (time-units timeout-unit) error-threshold)))

(defn service-provider
  "Creates a service provider for a named service s."
  [service-discovery s & {:keys [strategy down-instance-policy]
                          :or   {strategy             (random-strategy)
                                 down-instance-policy (down-instance-policy)}}]
  (-> (doto (.serviceProviderBuilder service-discovery)
        (.serviceName s)
        (.downInstancePolicy down-instance-policy)
        (.providerStrategy strategy))
      (.build)))

(defn instances
  "Returns instances registered for service named s."
  [^ServiceDiscovery service-discovery s]
  (.queryForInstances service-discovery s))

(defn instance
  "Returns a service instance using the provider's selection policy"
  [^ServiceProvider service-provider]
  (.getInstance service-provider))
