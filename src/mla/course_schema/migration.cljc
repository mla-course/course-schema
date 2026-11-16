(ns mla.course-schema.migration
  "Pure course-data migration pipeline."
  (:require [mla.course-schema.version :as v]))

;;; Version detection

(defn detect-version
  "Return the declared version of a raw parsed course map as an integer."
  [raw]
  (let [v (:version raw)]
    (if (and (integer? v) (not (neg? v))) v 0)))

(def ^:private migrations
  "Map of {from-version → migration-fn}: {N migrate-vN->vN+1}."
  {})

(def ^:private rollbacks
  "Map of {from-version → rollback-fn}, the inverse of `migrations`:
   {N migrate-vN->vN-1}. Only add an entry when downgrading is needed and
   possible without silent data loss."
  {})

(defn- run-steps
  "Thread `course` through `registry`'s fn for each version in `steps`, stamping
   `:version` with `(next-version step)` after each one. Throws a descriptive
   ex-info if a step is unregistered."
  [registry next-version steps course]
  (reduce (fn [c step]
            (if-let [f (get registry step)]
              (assoc (f c) :version (next-version step))
              (throw (ex-info (str "No migration registered for v" step
                                   " in mla.course-schema.migration.")
                              {:step step :registered (keys registry)}))))
          course
          steps))

(defn- result [course from to]
  {:course       course
   :migrated?    (not= from to)
   :from-version from
   :to-version   to})

(defn migrate
  "Migrate `course` from its detected version up to CURRENT_VERSION."
  [course]
  (let [from   (detect-version course)
        target v/CURRENT_VERSION]
    (when (> from target)
      (throw (ex-info (str "Course is v" from ", newer than supported v" target)
                      {:from from :target target})))
    (result (assoc (run-steps migrations inc (range from target) course) :version target)
            from target)))

(defn rollback
  "Downgrade `course` from its detected version down to `target`
   (0 <= target <= detected version). Mirrors `migrate`; see `rollbacks`."
  [course target]
  (let [from (detect-version course)]
    (when-not (<= 0 target from)
      (throw (ex-info (str "Cannot roll back v" from " to v" target)
                      {:from from :target target})))
    (result (run-steps rollbacks dec (range from target -1) course) from target)))
