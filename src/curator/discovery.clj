(ns ^{:doc "Namespace for service discovery"} curator.discovery
    (:require [clojure.edn :as edn]
              [curator.framework :refer (time-units)])
    (:import [org.apache.curator.x.discovery ServiceDiscovery ServiceDiscoveryBuilder ServiceInstance ServiceType UriSpec ProviderStrategy DownInstancePolicy ServiceProvider ServiceCache]
             [org.apache.curator.x.discovery.details InstanceSerializer JsonInstanceSerializer InstanceProvider]
             [org.apache.curator.x.discovery.strategies RandomStrategy RoundRobinStrategy StickyStrategy]
             [java.io ByteArrayInputStream InputStreamReader PushbackReader]))

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

(defn ^ServiceInstance service-instance
  "name: my-service
   uri-spec: \"{scheme}://foo.com:{port}\"
   port: 1234
   payload is serialized using json, only supports strings for now"
  [name uri-spec port & {:keys [id address ssl-port service-type payload]}]
  {:pre [(or (nil? payload) (string? payload))]}
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

(defn uri [^ServiceInstance service-instance]
  (.buildUriSpec service-instance))

(defn json-serializer []
  (JsonInstanceSerializer. String))

(defn ^ServiceDiscovery service-discovery
  [curator-framework service-instance & {:keys [base-path serializer payload-class]
                                         :or   {base-path     "/foo"
                                                payload-class String
                                                serializer    (json-serializer)}}]
  {:pre [(.startsWith ^String base-path "/")]}
  (-> (dotonn (ServiceDiscoveryBuilder/builder payload-class)
              (.client curator-framework)
              (.basePath base-path)
              (.serializer (json-serializer))
              (.thisInstance service-instance))
      (.build)))

(defn services
  "Returns the names of the services registered."
  [^ServiceDiscovery service-discovery]
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


(defn down-instance-policy
  ([] (down-instance-policy 30 :seconds 2))
  ([timeout timeout-unit error-threshold]
     {:pre [(some time-units [timeout-unit])]}
     (DownInstancePolicy. timeout (time-units timeout-unit) error-threshold)))

(defn ^ServiceProvider service-provider
  "Creates a service provider for a named service s."
  [^ServiceDiscovery service-discovery s & {:keys [strategy down-instance-policy]
                                            :or   {strategy             (random-strategy)
                                                   down-instance-policy (down-instance-policy)}}]
  (-> (doto (.serviceProviderBuilder service-discovery)
        (.serviceName s)
        (.downInstancePolicy down-instance-policy)
        (.providerStrategy strategy))
      (.build)))

(defn service-cache
  "Creates a service cache (rather than reading ZooKeeper each time) for
   the service named s"
  [^ServiceDiscovery service-discovery s]
  (-> (.serviceCacheBuilder service-discovery)
      ( .name s)
      (.build)))

(defn note-error
  "Clients should use this to indicate a problem when trying to
   connect to a service instance. The instance may be marked as down
   depending on the service provider's down instance policy."
  [^ServiceProvider service-provider ^ServiceInstance instance]
  (.noteError service-provider instance))

(defmulti instances (fn [^Object x & args] (.getClass x)))
(defmethod instances ServiceDiscovery [^ServiceDiscovery sd s] (.queryForInstances sd s))
(defmethod instances ServiceCache [^ServiceCache sc] (.getInstances sc))

(defmulti instance (fn [^Object x & args] (.getClass x)))
(defmethod instance ServiceProvider [^ServiceProvider provider] (.getInstance provider))
(defmethod instance ServiceCache [cache ^ProviderStrategy strategy] (.getInstance strategy cache))
