(ns ^{:doc "Namespace for service discovery"} curator.discovery
    (:require [clojure.edn :as edn])
    (:import [org.apache.curator.x.discovery ServiceDiscoveryBuilder ServiceInstance ServiceType UriSpec]
             [org.apache.curator.x.discovery.details InstanceSerializer JsonInstanceSerializer]
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
  [curator-framework service-instance & {:keys [base-path serializer]
                                         :or   {base-path  "/foo"
                                                serializer (json-serializer)}}]
  {:pre [(.startsWith base-path "/")]}
  (-> (dotonn (ServiceDiscoveryBuilder/builder ServicePayload)
              (.client curator-framework)
              (.basePath base-path)
              (.serializer (json-serializer))
              (.thisInstance service-instance))
      (.build)))

(defn start [service-discovery]
  (.start service-discovery))

(defn close [service-discovery]
  (.close service-discovery))


(defn services
  "Returns the names of the services registered."
  [service-discovery]
  (.queryForNames service-discovery))

(defn instances
  "Returns instances registered for service named s."
  [service-discovery s]
  (.queryForInstances service-discovery s))
