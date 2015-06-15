(ns curator.leader-latch
  (:import [java.util UUID]
           [org.apache.curator.framework.recipes.leader LeaderLatchListener LeaderLatch LeaderLatch$CloseMode]))

(defn listener
  [leaderfn notleaderfn]
  (reify LeaderLatchListener
    (isLeader [this] (leaderfn))
    (notLeader [this] (notleaderfn))))

(defn ^LeaderLatch leader-latch
  "Creates a Leader Latch.
 path: The path in ZooKeeper for storing this leadership group
 leaderfn: Function that will be called when the current process becomes
    leader. This function should block and only return when the
    process wishes to release leadership to someone else.
 notleaderfn: Optional function that will be called when the connection state
    changes and we are not the leader.
 participant-id: Uniquely identifies this participant. Defaults to UUID/randomUUID"
  [curator-framework ^String path leaderfn & {:keys [participant-id notleaderfn close-mode]
                                              :or   {participant-id (str (UUID/randomUUID))
                                                     notleaderfn    (constantly nil)}}]
  {:pre [(.startsWith path "/") (not (nil? participant-id))]}
  (doto (LeaderLatch. curator-framework path participant-id (if (= :notify-leader close-mode)
                                                              LeaderLatch$CloseMode/NOTIFY_LEADER
                                                              LeaderLatch$CloseMode/SILENT))
    (.addListener (listener leaderfn notleaderfn))))

(defn leader?
  [^LeaderLatch leader-latch]
  (.hasLeadership leader-latch))

(defn leader
  [^LeaderLatch leader-latch]
  (.getLeader leader-latch))

(defn participants
  [^LeaderLatch leader-latch]
  (.getParticipants leader-latch))